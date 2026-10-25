"""Claude provider: OpenAI-format dialogue and tools are converted to the Messages API."""

import json
from types import SimpleNamespace

from core.providers.llm.claude.claude import LATENCY_HINT, LLMProvider


def make_provider():
    return LLMProvider({"api_key": "sk-ant-test", "model_name": "claude-opus-5-5"})


def test_system_messages_go_to_system_param_with_latency_hint():
    system, messages = make_provider()._convert_dialogue([
        {"role": "system", "content": "You are Malek."},
        {"role": "user", "content": "Hi"},
    ])
    assert system == "You are Malek.\n\n" + LATENCY_HINT
    assert messages == [{"role": "user", "content": [{"type": "text", "text": "Hi"}]}]


def test_tool_calls_and_results_become_tool_use_and_tool_result():
    _, messages = make_provider()._convert_dialogue([
        {"role": "user", "content": "Volume to 80"},
        {"role": "assistant", "content": "", "tool_calls": [
            {"id": "call_1", "type": "function",
             "function": {"name": "self_audio_speaker_set_volume", "arguments": json.dumps({"volume": 80})}},
        ]},
        {"role": "tool", "tool_call_id": "call_1", "content": "ok"},
    ])
    assert messages[1] == {"role": "assistant", "content": [
        {"type": "tool_use", "id": "call_1", "name": "self_audio_speaker_set_volume", "input": {"volume": 80}},
    ]}
    assert messages[2] == {"role": "user", "content": [
        {"type": "tool_result", "tool_use_id": "call_1", "content": "ok"},
    ]}


def test_consecutive_same_role_messages_are_merged_and_first_turn_is_user():
    _, messages = make_provider()._convert_dialogue([
        {"role": "assistant", "content": "Hello!"},
        {"role": "user", "content": "a"},
        {"role": "user", "content": "b"},
    ])
    assert messages[0]["role"] == "user"
    assert messages[2]["content"] == [{"type": "text", "text": "a"}, {"type": "text", "text": "b"}]


def test_tool_loop_replays_original_assistant_turn():
    provider = make_provider()
    original = [{"type": "thinking", "thinking": "", "signature": "sig"},
                {"type": "tool_use", "id": "toolu_1", "name": "get_weather", "input": {"city": "Dubai"}}]
    provider._replay["toolu_1"] = original
    _, messages = provider._convert_dialogue([
        {"role": "user", "content": "Weather?"},
        {"role": "assistant", "content": "", "tool_calls": [
            {"id": "toolu_1", "function": {"name": "get_weather", "arguments": "{\"city\": \"Dubai\"}"}}]},
        {"role": "tool", "tool_call_id": "toolu_1", "content": "Sunny"},
    ])
    assert messages[1] == {"role": "assistant", "content": original}


def test_openai_tools_convert_to_input_schema():
    tools = LLMProvider._convert_tools([
        {"type": "function", "function": {"name": "play_music", "description": "Play a song",
                                          "parameters": {"type": "object", "properties": {"song": {"type": "string"}}}}},
        {"type": "function", "function": {"name": "handle_exit_intent", "description": "Exit"}},
    ])
    assert tools[0] == {"name": "play_music", "description": "Play a song",
                        "input_schema": {"type": "object", "properties": {"song": {"type": "string"}}}}
    assert tools[1]["input_schema"] == {"type": "object", "properties": {}}


def test_remember_keys_full_content_by_tool_use_id():
    provider = make_provider()
    block = SimpleNamespace(type="tool_use", id="toolu_9",
                            model_dump=lambda exclude_none: {"type": "tool_use", "id": "toolu_9", "name": "x", "input": {}})
    provider._remember(SimpleNamespace(content=[block]))
    assert provider._replay["toolu_9"] == [{"type": "tool_use", "id": "toolu_9", "name": "x", "input": {}}]
