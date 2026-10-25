package xiaozhi.modules.model.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpRequest;
import java.util.List;

import org.junit.jupiter.api.Test;

import cn.hutool.json.JSONObject;
import xiaozhi.common.exception.RenException;
import xiaozhi.modules.model.dto.ModelTestResultDTO;
import xiaozhi.modules.model.dto.ProviderVoiceDTO;

class ModelConnectionTesterTest {

    private final ModelConnectionTester tester = new ModelConnectionTester();

    @Test
    void openaiRequestUsesBaseUrlAndBearer() {
        HttpRequest r = tester.buildRequest("LLM", "openai", new JSONObject()
                .set("api_key", "sk-1").set("model_name", "m").set("base_url", "https://api.ifm.ai/v1/"));
        assertEquals("https://api.ifm.ai/v1/chat/completions", r.uri().toString());
        assertEquals("POST", r.method());
        assertEquals("Bearer sk-1", r.headers().firstValue("Authorization").orElse(""));

        HttpRequest d = tester.buildRequest("LLM", "openai", new JSONObject().set("api_key", "k").set("model_name", "m"));
        assertEquals("https://api.openai.com/v1/chat/completions", d.uri().toString());
    }

    @Test
    void geminiAndClaudeRequests() {
        HttpRequest g = tester.buildRequest("LLM", "gemini",
                new JSONObject().set("api_key", "g").set("model_name", "gemini-3.5-flash"));
        assertEquals("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent",
                g.uri().toString());
        assertEquals("g", g.headers().firstValue("x-goog-api-key").orElse(""));

        HttpRequest c = tester.buildRequest("LLM", "claude", new JSONObject().set("api_key", "a"));
        assertEquals("https://api.anthropic.com/v1/models?limit=1", c.uri().toString());
        assertEquals("GET", c.method());
        assertEquals("2023-06-01", c.headers().firstValue("anthropic-version").orElse(""));
        HttpRequest c2 = tester.buildRequest("LLM", "claude",
                new JSONObject().set("api_key", "a").set("base_url", "https://proxy.example/v1"));
        assertEquals("https://proxy.example/v1/models?limit=1", c2.uri().toString());
    }

    @Test
    void unsupportedOrInvalidInputsDoNotThrow() {
        assertNull(tester.buildRequest("TTS", "huoshan", new JSONObject()));
        assertNull(tester.buildRequest("VAD", "openai", new JSONObject()));
        ModelTestResultDTO na = tester.test("LLM", "ollama", new JSONObject());
        assertFalse(na.isOk());
        assertEquals("Test not available for this provider yet", na.getMessage());

        ModelTestResultDTO noKey = tester.test("LLM", "openai", new JSONObject().set("model_name", "m"));
        assertFalse(noKey.isOk());
        assertEquals("API key is required", noKey.getMessage());

        ModelTestResultDTO badUrl = tester.test("LLM", "openai",
                new JSONObject().set("api_key", "k").set("model_name", "m").set("base_url", "not a url"));
        assertFalse(badUrl.isOk());

        for (String internal : new String[] { "http://127.0.0.1:8002/v1", "http://localhost/v1",
                "http://10.1.50.237:8002", "http://192.168.1.1", "http://172.16.0.5", "http://169.254.169.254/latest",
                "http://100.64.0.1", "http://[::1]/v1", "http://[fd00::1]/v1", "http://0.0.0.0" }) {
            ModelTestResultDTO r = tester.test("LLM", "openai",
                    new JSONObject().set("api_key", "k").set("model_name", "m").set("base_url", internal));
            assertFalse(r.isOk(), internal);
            assertEquals("Private or internal addresses are not allowed", r.getMessage(), internal);
        }
        assertThrows(IllegalArgumentException.class, () -> ModelConnectionTester.assertPublicUrl("file:///etc/passwd"));
        assertThrows(IllegalArgumentException.class, () -> ModelConnectionTester.assertPublicUrl("ftp://8.8.8.8"));
        ModelConnectionTester.assertPublicUrl("https://8.8.8.8/v1");
    }

    @Test
    void errorTextExtractsProviderMessageAndTrims() {
        assertEquals("Incorrect API key", ModelConnectionTester
                .errorText("{\"error\":{\"message\":\"Incorrect API key\",\"type\":\"invalid_request_error\"}}"));
        assertEquals("bad", ModelConnectionTester.errorText("[{\"error\":{\"code\":400,\"message\":\"bad\"}}]"));
        assertEquals("plain text", ModelConnectionTester.errorText(" plain text "));
        String longText = "x".repeat(500);
        assertEquals(300, ModelConnectionTester.trim(longText).length());
        assertTrue(ModelConnectionTester.errorText("").length() > 0);
    }

    // ---- phase 2: ASR/TTS ----

    @Test
    void elevenLabsSpeechTestsMakeTinyRealRequests() {
        HttpRequest tts = tester.buildRequest("TTS", "elevenlabs", new JSONObject().set("api_key", "xi"));
        assertEquals("https://api.elevenlabs.io/v1/text-to-speech/21m00Tcm4TlvDq8ikWAM?output_format=mp3_22050_32",
                tts.uri().toString());
        assertEquals("POST", tts.method());
        assertEquals("xi", tts.headers().firstValue("xi-api-key").orElse(""));

        HttpRequest ttsBase = tester.buildRequest("TTS", "elevenlabs", new JSONObject().set("api_key", "xi")
                .set("api_base", "https://eu.elevenlabs.example/v1/").set("voice", "abc"));
        assertEquals("https://eu.elevenlabs.example/v1/text-to-speech/abc?output_format=mp3_22050_32",
                ttsBase.uri().toString());

        HttpRequest asr = tester.buildRequest("ASR", "elevenlabs", new JSONObject().set("api_key", "xi")
                .set("base_url", "https://api.elevenlabs.io/v1/speech-to-text"));
        assertEquals("https://api.elevenlabs.io/v1/speech-to-text", asr.uri().toString());
        assertEquals("POST", asr.method());
        assertTrue(asr.headers().firstValue("Content-Type").orElse("").startsWith("multipart/form-data; boundary="));
        HttpRequest stream = tester.buildRequest("ASR", "elevenlabs_stream", new JSONObject().set("api_key", "xi")
                .set("model_id", "scribe_v2_realtime"));
        assertEquals("https://api.elevenlabs.io/v1/speech-to-text", stream.uri().toString());

        byte[] wav = ModelConnectionTester.silentWav();
        assertEquals(44 + 16000, wav.length);
        assertEquals("RIFF", new String(wav, 0, 4, java.nio.charset.StandardCharsets.US_ASCII));

        ModelTestResultDTO noKey = tester.test("TTS", "elevenlabs", new JSONObject());
        assertFalse(noKey.isOk());
        assertEquals("API key is required", noKey.getMessage());
    }

    @Test
    void openAiSpeechTestsMakeTinyRealRequests() {
        HttpRequest tts = tester.buildRequest("TTS", "openai", new JSONObject().set("api_key", "sk")
                .set("api_url", "https://api.openai.com/v1/audio/speech"));
        assertEquals("https://api.openai.com/v1/audio/speech", tts.uri().toString());
        assertEquals("POST", tts.method());
        assertEquals("Bearer sk", tts.headers().firstValue("Authorization").orElse(""));

        HttpRequest asr = tester.buildRequest("ASR", "openai", new JSONObject().set("api_key", "sk")
                .set("base_url", "https://proxy.example/openai/v1/audio/transcriptions"));
        assertEquals("https://proxy.example/openai/v1/audio/transcriptions", asr.uri().toString());
        assertEquals("POST", asr.method());

        HttpRequest def = tester.buildRequest("ASR", "openai", new JSONObject().set("api_key", "sk"));
        assertEquals("https://api.openai.com/v1/audio/transcriptions", def.uri().toString());
    }

    @Test
    void speechTestsKeepSsrfGuard() {
        ModelTestResultDTO r = tester.test("TTS", "elevenlabs",
                new JSONObject().set("api_key", "xi").set("api_base", "http://127.0.0.1:9000/v1"));
        assertFalse(r.isOk());
        assertEquals("Private or internal addresses are not allowed", r.getMessage());
        ModelTestResultDTO r2 = tester.test("ASR", "openai",
                new JSONObject().set("api_key", "sk").set("base_url", "http://10.0.0.1/v1/audio/transcriptions"));
        assertFalse(r2.isOk());
        assertEquals("Private or internal addresses are not allowed", r2.getMessage());
    }

    @Test
    void providerVoices() {
        List<ProviderVoiceDTO> openai = tester.listProviderVoices("openai", new JSONObject());
        assertEquals(13, openai.size());
        assertEquals("alloy", openai.get(0).getVoiceId());
        assertEquals("cedar", openai.get(12).getName());

        HttpRequest req = tester.buildElevenLabsVoicesRequest(new JSONObject().set("api_key", "xi"));
        assertEquals("https://api.elevenlabs.io/v1/voices", req.uri().toString());
        assertEquals("xi", req.headers().firstValue("xi-api-key").orElse(""));

        List<ProviderVoiceDTO> parsed = ModelConnectionTester.parseElevenLabsVoices("{\"voices\":["
                + "{\"voice_id\":\"21m\",\"name\":\"Rachel\",\"category\":\"premade\","
                + "\"preview_url\":\"https://x/p.mp3\",\"labels\":{\"accent\":\"american\"}},"
                + "{\"name\":\"no id\"}]}");
        assertEquals(1, parsed.size());
        assertEquals("21m", parsed.get(0).getVoiceId());
        assertEquals("Rachel", parsed.get(0).getName());
        assertEquals("premade", parsed.get(0).getCategory());
        assertEquals("https://x/p.mp3", parsed.get(0).getPreviewUrl());
        assertEquals("american", parsed.get(0).getLabels().get("accent"));

        RenException internal = assertThrows(RenException.class, () -> tester.listProviderVoices("elevenlabs",
                new JSONObject().set("api_key", "xi").set("api_base", "http://192.168.1.10/v1")));
        assertEquals("Private or internal addresses are not allowed", internal.getMsg());
        assertThrows(RenException.class, () -> tester.listProviderVoices("elevenlabs", new JSONObject()));
        assertThrows(RenException.class, () -> tester.listProviderVoices("huoshan", new JSONObject()));
    }

    @Test
    void elevenLabsDeleteRequest() {
        HttpRequest del = tester.buildElevenLabsDeleteRequest(null, "xi", "abc");
        assertEquals("https://api.elevenlabs.io/v1/voices/abc", del.uri().toString());
        assertEquals("DELETE", del.method());
        assertEquals("xi", del.headers().firstValue("xi-api-key").orElse(""));
        assertFalse(tester.deleteElevenLabsVoice("http://127.0.0.1/v1", "xi", "abc"));
        assertFalse(tester.deleteElevenLabsVoice(null, "", "abc"));
    }
}
