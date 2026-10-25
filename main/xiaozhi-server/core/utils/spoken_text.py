"""Localized text and audio prompts that the device speaks or shows.

The language follows the connection's selected TTS voice, so what the user
hears matches the voice they picked. English is the default; Arabic and
Chinese are also supported.
"""

import os
import random

DEFAULT_LANGUAGE = "en"
SUPPORTED_LANGUAGES = ("en", "ar", "zh")
ASSETS_DIR = "config/assets"

_VOICE_PREFIXES = {"en": "en", "ar": "ar", "zh": "zh"}
_LABEL_KEYWORDS = (
    ("en", ("english", "英语", "英文")),
    ("ar", ("arabic", "عربي", "عربية", "阿拉伯")),
    ("zh", ("chinese", "mandarin", "cantonese", "中文", "普通话", "粤语")),
)


def _tts_config(conn):
    config = getattr(conn, "config", None)
    if not isinstance(config, dict):
        return {}
    selected = (config.get("selected_module") or {}).get("TTS")
    if not selected:
        return {}
    tts_config = (config.get("TTS") or {}).get(selected)
    return tts_config if isinstance(tts_config, dict) else {}


def resolve_language(conn) -> str:
    """Return "en", "ar" or "zh" for this connection. Never raises."""
    try:
        # A device not yet bound to an agent has no voice of its own; the
        # server's fallback config would otherwise pick the language.
        if getattr(conn, "need_bind", False) is True:
            return DEFAULT_LANGUAGE
        tts_config = _tts_config(conn)
        for field in ("private_voice", "voice"):
            voice = tts_config.get(field)
            if isinstance(voice, str) and voice.strip():
                prefix = voice.strip().lower().replace("_", "-").split("-", 1)[0]
                if prefix in _VOICE_PREFIXES:
                    return _VOICE_PREFIXES[prefix]
        label = tts_config.get("language")
        if isinstance(label, str) and label.strip():
            label = label.strip().lower()
            for lang, keywords in _LABEL_KEYWORDS:
                if any(keyword in label for keyword in keywords):
                    return lang
    except Exception:
        pass
    return DEFAULT_LANGUAGE


MESSAGES = {
    # Wake word reply (fallback audio: wakeup_words_short.wav)
    "wakeup_reply": {
        "en": "I'm here!",
        "ar": "أنا هنا!",
        "zh": "我在这里哦！",
    },
    # Short replies spoken after a wake word (synthesized with the agent's voice)
    "wakeup_responses": {
        "en": [
            "I'm here, go ahead.",
            "I'm listening.",
            "Yes? How can I help?",
            "I'm all ears.",
            "Ready when you are.",
            "What can I do for you?",
        ],
        "ar": [
            "أنا هنا، تفضّل.",
            "أنا أستمع إليك.",
            "نعم؟ كيف أساعدك؟",
            "كلي آذان صاغية.",
            "أنا جاهز، تفضّل.",
            "ماذا يمكنني أن أفعل لك؟",
        ],
        "zh": [
            "我一直都在呢，您请说。",
            "在的呢，请随时吩咐我。",
            "来啦来啦，请告诉我吧。",
            "您请说，我正听着。",
            "请您讲话，我准备好了。",
            "请您说出指令吧。",
            "我认真听着呢，请讲。",
            "请问您需要什么帮助？",
            "我在这里，等候您的指令。",
        ],
    },
    # Simulated user greeting sent to the LLM after a wake word
    "wake_greeting": {
        "en": "Hey, hello!",
        "ar": "مرحبًا!",
        "zh": "嘿，你好呀",
    },
    # Daily output limit reached (audio: max_output_size.wav)
    "max_output_size": {
        "en": "Sorry, I need to take a break now. Let's talk again tomorrow. See you then, bye!",
        "ar": "عذرًا، عليّ أن أرتاح قليلًا الآن. لنتحدث مجددًا غدًا. إلى اللقاء!",
        "zh": "不好意思，我现在有点事情要忙，明天这个时候我们再聊，约好了哦！明天不见不散，拜拜！",
    },
    # Device binding
    "bind_code_invalid": {
        "en": "The binding code format is wrong. Please check the configuration.",
        "ar": "صيغة رمز الربط غير صحيحة. يرجى التحقق من الإعدادات.",
        "zh": "绑定码格式错误，请检查配置。",
    },
    # Shown on screen; the audio is bind_code.wav followed by the digits
    "bind_code_prompt": {
        "en": "Please log in to the control panel and enter this code to bind your device: {code}",
        "ar": "يرجى تسجيل الدخول إلى لوحة التحكم وإدخال هذا الرمز لربط جهازك: {code}",
        "zh": "请登录控制面板，输入{code}，绑定设备。",
    },
    "bind_not_found": {
        "en": "No version info was found for this device. Please set the OTA address correctly, then rebuild the firmware.",
        "ar": "لم يتم العثور على معلومات إصدار هذا الجهاز. يرجى ضبط عنوان OTA بشكل صحيح، ثم إعادة بناء البرنامج الثابت.",
        "zh": "没有找到该设备的版本信息，请正确配置 OTA地址，然后重新编译固件。",
    },
    # Conversation flow
    "goodbye": {
        "en": "Bye, talk to you next time!",
        "ar": "إلى اللقاء، نتحدث لاحقًا!",
        "zh": "再见，下次再聊~",
    },
    "goodbye_default": {
        "en": "Goodbye, have a wonderful day!",
        "ar": "مع السلامة، أتمنى لك يومًا سعيدًا!",
        "zh": "再见，祝您生活愉快！",
    },
    "network_error": {
        "en": "Oops, there's a network problem. Please try again in a moment!",
        "ar": "عذرًا، هناك مشكلة في الشبكة. يرجى المحاولة بعد قليل!",
        "zh": "哎呀，网络遇到点问题，请稍后再试下！",
    },
    "tool_timeout": {
        "en": "That took too long. Please try again in a moment.",
        "ar": "استغرق الأمر وقتًا طويلًا. يرجى المحاولة بعد قليل.",
        "zh": "工具调用超时，请一会再试下哈",
    },
    "server_restarting": {
        "en": "Server restarting...",
        "ar": "جارٍ إعادة تشغيل الخادم...",
        "zh": "服务器重启中...",
    },
    # Opening words for the end-of-conversation prompt
    "time_flies": {
        "en": "Time flies so fast",
        "ar": "الوقت يمرّ بسرعة كبيرة",
        "zh": "时间过得真快",
    },
    # Few-shot examples injected (temporarily) into the dialogue
    "fewshot_story_user": {
        "en": "Tell me a story",
        "ar": "احكِ لي قصة",
        "zh": "给我讲个故事吧",
    },
    "fewshot_story_reply": {
        "en": "Sure! What kind would you like: a fairy tale, an adventure, or something funny? Pick one and I'll begin.",
        "ar": "بكل سرور! أي نوع تحب: حكاية خيالية، أم مغامرة، أم قصة مضحكة؟ اختر واحدة وسأبدأ.",
        "zh": "好呀，你想听什么类型的呀？童话、冒险还是搞笑的？选一个我给你开讲~",
    },
    "fewshot_direct_answered": {
        "en": "Replied directly",
        "ar": "تم الرد مباشرة",
        "zh": "已直接回复",
    },
    "fewshot_bye_user": {
        "en": "Bye",
        "ar": "مع السلامة",
        "zh": "拜拜",
    },
    "exit_handled": {
        "en": "Exit intent handled",
        "ar": "تمت معالجة طلب الإنهاء",
        "zh": "退出意图已处理",
    },
    # Music
    "command_received": {
        "en": "Command received",
        "ar": "تم استلام الأمر",
        "zh": "指令已接收",
    },
    "music_playing": {
        "en": "Playing music for you",
        "ar": "جارٍ تشغيل الموسيقى لك",
        "zh": "正在为您播放音乐",
    },
    "music_error": {
        "en": "Something went wrong while playing music",
        "ar": "حدث خطأ أثناء تشغيل الموسيقى",
        "zh": "播放音乐时出错了",
    },
    "music_play_prompts": {
        "en": [
            "Now playing: {song}",
            "Enjoy the song: {song}",
            "Coming up: {song}",
            "Here's {song}",
            "Let's listen to {song}",
        ],
        "ar": [
            "يتم الآن تشغيل: {song}",
            "استمتع بأغنية: {song}",
            "إليك: {song}",
            "لنستمع معًا إلى: {song}",
        ],
        "zh": [
            "正在为您播放，《{song}》",
            "请欣赏歌曲，《{song}》",
            "即将为您播放，《{song}》",
            "现在为您带来，《{song}》",
            "让我们一起聆听，《{song}》",
            "接下来请欣赏，《{song}》",
            "此刻为您献上，《{song}》",
        ],
    },
    "hass_music_playing": {
        "en": "Playing {media}",
        "ar": "جارٍ تشغيل {media}",
        "zh": "正在播放{media}的音乐",
    },
    "hass_music_failed": {
        "en": "Music playback failed, error code: {code}",
        "ar": "فشل تشغيل الموسيقى، رمز الخطأ: {code}",
        "zh": "音乐播放失败，错误码: {code}",
    },
    # Role switching
    "role_unsupported": {
        "en": "Sorry, that role isn't supported",
        "ar": "عذرًا، هذا الدور غير مدعوم",
        "zh": "不支持的角色",
    },
    "role_switched": {
        "en": "Role switched. I'm {role_name} now.",
        "ar": "تم تغيير الدور. أنا الآن {role_name}.",
        "zh": "切换角色成功,我是{role}{role_name}",
    },
    # Device-to-device calls
    "call_no_mac": {
        "en": "Couldn't get this device's MAC address",
        "ar": "تعذّر الحصول على عنوان MAC لهذا الجهاز",
        "zh": "无法获取本机MAC地址",
    },
    "call_config_error": {
        "en": "Configuration error, please try again later",
        "ar": "خطأ في الإعدادات، يرجى المحاولة لاحقًا",
        "zh": "配置错误，请稍后再试",
    },
    "call_failed_retry": {
        "en": "The call failed, please try again later",
        "ar": "فشل الاتصال، يرجى المحاولة لاحقًا",
        "zh": "呼叫失败，请稍后再试",
    },
    "call_failed": {
        "en": "The call failed",
        "ar": "فشل الاتصال",
        "zh": "呼叫失败",
    },
    "call_answered": {
        "en": "Call answered",
        "ar": "تم الرد على المكالمة",
        "zh": "已成功接听",
    },
    "call_calling": {
        "en": "Calling {nickname}, please wait for them to answer",
        "ar": "جارٍ الاتصال بـ{nickname}، يرجى انتظار الرد",
        "zh": "正在呼叫{nickname}，请等待对方接听",
    },
}


def _lookup(conn, key):
    entry = MESSAGES.get(key)
    if entry is None:
        return None
    lang = resolve_language(conn)
    value = entry.get(lang)
    if value is None:
        value = entry.get(DEFAULT_LANGUAGE)
    return value


def _format(value, kwargs):
    if not kwargs:
        return value
    try:
        return value.format(**kwargs)
    except (KeyError, IndexError, ValueError):
        return value


def text(conn, key, **kwargs) -> str:
    """Localized string for ``key``; falls back to English, then to the key."""
    value = _lookup(conn, key)
    if value is None:
        return key
    if isinstance(value, (list, tuple)):
        value = value[0] if value else ""
    return _format(value, kwargs)


def options(conn, key, **kwargs) -> list:
    """All localized variants for ``key`` (a list entry), formatted."""
    value = _lookup(conn, key)
    if value is None:
        return [key]
    if not isinstance(value, (list, tuple)):
        value = [value]
    return [_format(item, kwargs) for item in value]


def random_text(conn, key, **kwargs) -> str:
    """One random localized variant for ``key``."""
    return random.choice(options(conn, key, **kwargs) or [key])


def asset(conn, filename) -> str:
    """Path of a localized audio asset, falling back to English, then the original.

    The original files in config/assets are the Chinese recordings, so Chinese
    uses them directly instead of falling back to English.
    """
    original = f"{ASSETS_DIR}/{filename}"
    try:
        lang = resolve_language(conn)
        path = f"{ASSETS_DIR}/{lang}/{filename}"
        if os.path.exists(path):
            return path
        if lang == "zh":
            return original
        path = f"{ASSETS_DIR}/{DEFAULT_LANGUAGE}/{filename}"
        if os.path.exists(path):
            return path
    except Exception:
        pass
    return original
