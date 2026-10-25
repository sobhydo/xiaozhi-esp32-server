"""OpenAI-compatible provider: IFM needs reasoning_content on every assistant message in history."""

from core.providers.llm.openai.openai import LLMProvider

DIALOGUE = [
    {"role": "system", "content": "You are Malek."},
    {"role": "user", "content": "Hi"},
    {"role": "assistant", "content": "Hello!"},
    {"role": "assistant", "content": "", "reasoning_content": "kept"},
    {"role": "user", "content": "What is 2+2?"},
]


def make_provider(base_url):
    return LLMProvider({"api_key": "test", "model_name": "m", "base_url": base_url})


def test_ifm_assistant_messages_get_empty_reasoning_content():
    messages = make_provider("https://api.ifm.ai/v1")._apply_reasoning_history(DIALOGUE)
    assert messages[2] == {"role": "assistant", "content": "Hello!", "reasoning_content": ""}
    assert messages[3]["reasoning_content"] == "kept"
    assert "reasoning_content" not in messages[1]
    # The shared dialogue is not mutated
    assert "reasoning_content" not in DIALOGUE[2]


def test_other_platforms_are_unchanged():
    messages = make_provider("https://api.openai.com/v1")._apply_reasoning_history(DIALOGUE)
    assert messages is DIALOGUE
