package xiaozhi.modules.model.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpRequest;

import org.junit.jupiter.api.Test;

import cn.hutool.json.JSONObject;
import xiaozhi.modules.model.dto.ModelTestResultDTO;

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
        assertNull(tester.buildRequest("TTS", "openai", new JSONObject()));
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
}
