import json
from collections import OrderedDict
from types import SimpleNamespace

import anthropic

from config.logger import setup_logging
from core.providers.llm.base import LLMProviderBase
from core.utils.util import check_model_key

TAG = __name__
logger = setup_logging()

# Voice replies must start fast; Anthropic recommends this line for latency-sensitive routes
LATENCY_HINT = "Latency-sensitive; begin your visible answer immediately."
FALLBACK_BETA = "server-side-fallback-2026-07-01"
REPLAY_CACHE_SIZE = 256


class LLMProvider(LLMProviderBase):
    """Anthropic Claude via the official SDK.

    The server keeps dialogue in OpenAI chat format, so each request is converted to the
    Messages API and tool calls are returned in the same shape the OpenAI provider yields.
    """

    def __init__(self, config):
        self.model_name = config.get("model_name") or "claude-opus-5-5"
        self.api_key = config.get("api_key")
        self.base_url = config.get("base_url") or None
        max_tokens = config.get("max_tokens")
        self.max_tokens = int(max_tokens) if str(max_tokens or "").strip() else 16000
        # Thinking depth; "low" keeps voice replies quick
        self.effort = config.get("effort") or "low"
        self.use_fallbacks = str(config.get("fallbacks", "default")).lower() not in ("", "off", "none", "false")

        model_key_msg = check_model_key("LLM", self.api_key)
        if model_key_msg:
            logger.bind(tag=TAG).error(model_key_msg)
        self.client = anthropic.Anthropic(api_key=self.api_key, base_url=self.base_url)
        # Original assistant turns (thinking blocks included) keyed by tool_use id, so a tool
        # loop replays them unchanged instead of the lossy OpenAI-format copy
        self._replay = OrderedDict()

    # ---------- OpenAI dialogue -> Messages API ----------

    def _convert_dialogue(self, dialogue):
        system_parts, messages = [], []

        def append(role, blocks):
            if messages and messages[-1]["role"] == role:
                messages[-1]["content"].extend(blocks)
            else:
                messages.append({"role": role, "content": list(blocks)})

        for msg in dialogue:
            role = msg.get("role")
            content = msg.get("content")
            if role == "system":
                if content:
                    system_parts.append(content if isinstance(content, str) else json.dumps(content, ensure_ascii=False))
            elif role == "user":
                if content:
                    append("user", [{"type": "text", "text": content if isinstance(content, str) else json.dumps(content, ensure_ascii=False)}])
            elif role == "assistant":
                tool_calls = msg.get("tool_calls") or []
                replay = next((self._replay[tc.get("id")] for tc in tool_calls if tc.get("id") in self._replay), None)
                if replay is not None:
                    append("assistant", replay)
                    continue
                blocks = []
                if content:
                    blocks.append({"type": "text", "text": content})
                for tc in tool_calls:
                    fn = tc.get("function", {})
                    try:
                        args = json.loads(fn.get("arguments") or "{}")
                    except (TypeError, ValueError):
                        args = {}
                    blocks.append({"type": "tool_use", "id": tc.get("id"), "name": fn.get("name"), "input": args})
                if blocks:
                    append("assistant", blocks)
            elif role == "tool":
                result = content if isinstance(content, str) else json.dumps(content, ensure_ascii=False)
                append("user", [{"type": "tool_result", "tool_use_id": msg.get("tool_call_id"), "content": result or ""}])

        if messages and messages[0]["role"] != "user":
            messages.insert(0, {"role": "user", "content": [{"type": "text", "text": "Hello"}]})
        system = "\n\n".join(system_parts + [LATENCY_HINT])
        return system, messages

    @staticmethod
    def _convert_tools(functions):
        tools = []
        for f in functions or []:
            fn = f.get("function", f)
            tools.append({
                "name": fn.get("name"),
                "description": fn.get("description", ""),
                "input_schema": fn.get("parameters") or {"type": "object", "properties": {}},
            })
        return tools

    def _remember(self, message):
        content = [block.model_dump(exclude_none=True) for block in message.content]
        for block in message.content:
            if block.type == "tool_use":
                self._replay[block.id] = content
        while len(self._replay) > REPLAY_CACHE_SIZE:
            self._replay.popitem(last=False)

    # ---------- Streaming ----------

    def _stream(self, dialogue, functions=None):
        system, messages = self._convert_dialogue(dialogue)
        params = {
            "model": self.model_name,
            "max_tokens": self.max_tokens,
            "system": system,
            "messages": messages,
            "output_config": {"effort": self.effort},
        }
        tools = self._convert_tools(functions)
        if tools:
            params["tools"] = tools
        if self.use_fallbacks:
            return self.client.beta.messages.stream(betas=[FALLBACK_BETA], fallbacks="default", **params)
        return self.client.messages.stream(**params)

    def response(self, session_id, dialogue, **kwargs):
        with self._stream(dialogue) as stream:
            for text in stream.text_stream:
                yield text
            final = stream.get_final_message()
        if final.stop_reason == "refusal":
            logger.bind(tag=TAG).warning(f"Claude declined the request: {getattr(final, 'stop_details', None)}")

    def response_with_functions(self, session_id, dialogue, functions=None, **kwargs):
        with self._stream(dialogue, functions) as stream:
            for text in stream.text_stream:
                yield text, None
            final = stream.get_final_message()

        if final.stop_reason == "refusal":
            logger.bind(tag=TAG).warning(f"Claude declined the request: {getattr(final, 'stop_details', None)}")
            return
        usage = getattr(final, "usage", None)
        if usage is not None:
            logger.bind(tag=TAG).info(f"Claude tokens: input {usage.input_tokens}, output {usage.output_tokens}")

        tool_uses = [b for b in final.content if b.type == "tool_use"]
        if not tool_uses:
            return
        if final.stop_reason == "max_tokens":
            # A truncated tool input must not run
            logger.bind(tag=TAG).error("Claude tool call was cut off by max_tokens; not running it")
            return
        self._remember(final)
        yield None, [
            SimpleNamespace(
                index=i,
                id=block.id,
                type="function",
                function=SimpleNamespace(name=block.name, arguments=json.dumps(block.input, ensure_ascii=False)),
            )
            for i, block in enumerate(tool_uses)
        ]
