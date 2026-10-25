"""Language resolution, localized strings and localized audio assets."""

from types import SimpleNamespace

import pytest

from core.utils import spoken_text
from core.utils.spoken_text import MESSAGES, asset, options, resolve_language, text


def make_conn(tts_config=None, selected="EdgeTTS"):
    config = {"selected_module": {"TTS": selected}, "TTS": {}}
    if tts_config is not None:
        config["TTS"][selected] = tts_config
    return SimpleNamespace(config=config)


@pytest.mark.parametrize(
    "tts_config, expected",
    [
        ({"voice": "en-US-JennyNeural"}, "en"),
        ({"voice": "ar-AE-FatimaNeural"}, "ar"),
        ({"voice": "zh-CN-XiaoxiaoNeural"}, "zh"),
        ({"voice": "zh_female_wanwanxiaohe"}, "zh"),
        # private_voice wins over voice
        ({"private_voice": "ar-SA-ZariyahNeural", "voice": "en-US-JennyNeural"}, "ar"),
        # voice prefix wins over the language label
        ({"voice": "en-GB-SoniaNeural", "language": "Chinese"}, "en"),
        # label only, case-insensitive
        ({"voice": "alloy", "language": "ENGLISH"}, "en"),
        ({"voice": "alloy", "language": "Arabic (UAE)"}, "ar"),
        ({"language": "Mandarin"}, "zh"),
        ({"language": "Cantonese"}, "zh"),
        ({"language": "中文"}, "zh"),
        ({"language": "普通话"}, "zh"),
        ({"language": "粤语"}, "zh"),
        ({"language": "Japanese"}, "en"),
        ({}, "en"),
    ],
)
def test_resolve_language_from_tts_config(tts_config, expected):
    assert resolve_language(make_conn(tts_config)) == expected


@pytest.mark.parametrize(
    "conn",
    [
        None,
        SimpleNamespace(),
        SimpleNamespace(config=None),
        SimpleNamespace(config={}),
        SimpleNamespace(config={"selected_module": {}}),
        SimpleNamespace(config={"selected_module": {"TTS": "Missing"}, "TTS": {}}),
        SimpleNamespace(config={"selected_module": None, "TTS": None}),
        SimpleNamespace(config={"selected_module": {"TTS": "X"}, "TTS": {"X": "bad"}}),
        SimpleNamespace(config={"selected_module": {"TTS": "X"}, "TTS": {"X": {"voice": 42}}}),
    ],
)
def test_resolve_language_defaults_to_english(conn):
    assert resolve_language(conn) == "en"


def test_unbound_device_is_english():
    conn = make_conn({"voice": "zh-CN-XiaoxiaoNeural"})
    conn.need_bind = True
    assert resolve_language(conn) == "en"
    conn.need_bind = False
    assert resolve_language(conn) == "zh"


def test_every_message_has_english():
    for key, entry in MESSAGES.items():
        assert entry.get("en"), key


def test_text_per_language():
    assert text(make_conn({"voice": "en-US-JennyNeural"}), "goodbye") == MESSAGES["goodbye"]["en"]
    assert text(make_conn({"voice": "ar-AE-FatimaNeural"}), "goodbye") == MESSAGES["goodbye"]["ar"]
    assert text(make_conn({"voice": "zh-CN-XiaoxiaoNeural"}), "goodbye") == "再见，下次再聊~"
    assert text(None, "goodbye") == MESSAGES["goodbye"]["en"]


def test_text_formatting():
    en = text(make_conn({"voice": "en-US-JennyNeural"}), "bind_code_prompt", code="123456")
    ar = text(make_conn({"voice": "ar-AE-FatimaNeural"}), "bind_code_prompt", code="123456")
    zh = text(make_conn({"voice": "zh-CN-XiaoxiaoNeural"}), "bind_code_prompt", code="123456")
    assert en.endswith("123456") and "{code}" not in en
    assert "123456" in ar
    assert zh == "请登录控制面板，输入123456，绑定设备。"


def test_text_falls_back_to_english(monkeypatch):
    monkeypatch.setitem(MESSAGES, "only_en", {"en": "Hello {name}"})
    conn = make_conn({"voice": "ar-AE-FatimaNeural"})
    assert text(conn, "only_en", name="Sam") == "Hello Sam"


def test_text_tolerates_missing_kwargs_and_unknown_key():
    conn = make_conn({"voice": "en-US-JennyNeural"})
    assert text(conn, "bind_code_prompt") == MESSAGES["bind_code_prompt"]["en"]
    assert text(conn, "no_such_key") == "no_such_key"


def test_options_lists():
    ar = make_conn({"voice": "ar-AE-FatimaNeural"})
    prompts = options(ar, "music_play_prompts", song="X")
    assert prompts and all("X" in p for p in prompts)
    assert options(None, "wakeup_responses") == MESSAGES["wakeup_responses"]["en"]


@pytest.fixture
def assets_dir(tmp_path, monkeypatch):
    root = tmp_path / "config" / "assets"
    root.mkdir(parents=True)
    monkeypatch.chdir(tmp_path)
    return root


def touch(path):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(b"RIFF")


def test_asset_prefers_language_dir(assets_dir):
    for sub in ("", "en/", "ar/"):
        touch(assets_dir / f"{sub}bind_code/1.wav")
    ar = make_conn({"voice": "ar-AE-FatimaNeural"})
    en = make_conn({"voice": "en-US-JennyNeural"})
    assert asset(ar, "bind_code/1.wav") == "config/assets/ar/bind_code/1.wav"
    assert asset(en, "bind_code/1.wav") == "config/assets/en/bind_code/1.wav"


def test_asset_falls_back_to_english_then_original(assets_dir):
    touch(assets_dir / "x.wav")
    touch(assets_dir / "en" / "x.wav")
    touch(assets_dir / "y.wav")
    ar = make_conn({"voice": "ar-AE-FatimaNeural"})
    assert asset(ar, "x.wav") == "config/assets/en/x.wav"
    assert asset(ar, "y.wav") == "config/assets/y.wav"
    assert asset(ar, "missing.wav") == "config/assets/missing.wav"


def test_asset_chinese_uses_original_recordings(assets_dir):
    touch(assets_dir / "x.wav")
    touch(assets_dir / "en" / "x.wav")
    zh = make_conn({"voice": "zh-CN-XiaoxiaoNeural"})
    assert asset(zh, "x.wav") == "config/assets/x.wav"
    touch(assets_dir / "zh" / "x.wav")
    assert asset(zh, "x.wav") == "config/assets/zh/x.wav"


def test_generated_assets_exist():
    """The shipped en/ar prompts cover every file the server plays."""
    import os

    server_root = os.path.dirname(os.path.dirname(os.path.abspath(spoken_text.__file__)))
    server_root = os.path.dirname(server_root)
    names = ["bind_code.wav", "bind_not_found.wav", "max_output_size.wav", "wakeup_words_short.wav"]
    names += [f"bind_code/{d}.wav" for d in range(10)]
    for lang in ("en", "ar"):
        for name in names:
            assert os.path.isfile(os.path.join(server_root, "config", "assets", lang, name)), (lang, name)
