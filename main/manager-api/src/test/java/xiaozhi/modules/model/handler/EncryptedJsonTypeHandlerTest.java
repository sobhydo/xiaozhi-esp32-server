package xiaozhi.modules.model.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

class EncryptedJsonTypeHandlerTest {

    private final EncryptedJsonTypeHandler handler = new EncryptedJsonTypeHandler(JSONObject.class);

    @AfterEach
    void resetKey() {
        ModelSecretCipher.setKey(null);
    }

    private static JSONObject config() {
        return new JSONObject()
                .set("type", "openai")
                .set("model_name", "gpt-4o-mini")
                .set("api_key", "sk-test-1234567890")
                .set("nested", new JSONObject().set("secret", "inner-secret").set("label", "x"));
    }

    @Test
    void roundTripEncryptsSecretsOnlyAndDecryptsOnRead() {
        ModelSecretCipher.setKey("unit-test-key");

        String stored = handler.toJson(config());
        JSONObject raw = JSONUtil.parseObj(stored);
        assertTrue(raw.getStr("api_key").startsWith(ModelSecretCipher.PREFIX));
        assertTrue(raw.getJSONObject("nested").getStr("secret").startsWith(ModelSecretCipher.PREFIX));
        assertEquals("gpt-4o-mini", raw.getStr("model_name"));
        assertEquals("openai", raw.getStr("type"));
        assertEquals("x", raw.getJSONObject("nested").getStr("label"));
        assertFalse(stored.contains("sk-test-1234567890"));

        JSONObject read = (JSONObject) handler.parse(stored);
        assertEquals("sk-test-1234567890", read.getStr("api_key"));
        assertEquals("inner-secret", read.getJSONObject("nested").getStr("secret"));
        assertEquals("gpt-4o-mini", read.getStr("model_name"));
    }

    @Test
    void sameValueEncryptsWithFreshIv() {
        ModelSecretCipher.setKey("unit-test-key");
        assertNotEquals(ModelSecretCipher.encrypt("abc"), ModelSecretCipher.encrypt("abc"));
    }

    @Test
    void maskedEmptyAndAlreadyEncryptedValuesAreUntouched() {
        ModelSecretCipher.setKey("unit-test-key");
        String already = ModelSecretCipher.encrypt("real");
        JSONObject in = new JSONObject()
                .set("api_key", "sk-t**********7890")
                .set("token", "")
                .set("secret", already);

        JSONObject raw = JSONUtil.parseObj(handler.toJson(in));
        assertEquals("sk-t**********7890", raw.getStr("api_key"));
        assertEquals("", raw.getStr("token"));
        assertEquals(already, raw.getStr("secret"));
    }

    @Test
    void plainLegacyValuesAreReadable() {
        ModelSecretCipher.setKey("unit-test-key");
        String legacy = "{\"type\":\"openai\",\"api_key\":\"sk-plain\"}";
        JSONObject read = (JSONObject) handler.parse(legacy);
        assertEquals("sk-plain", read.getStr("api_key"));
    }

    @Test
    void withoutKeyValuesPassThrough() {
        ModelSecretCipher.setKey(null);
        assertFalse(ModelSecretCipher.isEnabled());
        String stored = handler.toJson(config());
        assertEquals("sk-test-1234567890", JSONUtil.parseObj(stored).getStr("api_key"));
        assertEquals("sk-test-1234567890", ((JSONObject) handler.parse(stored)).getStr("api_key"));
    }

    @Test
    void wrongKeyOrMissingKeyDecryptsToEmpty() {
        ModelSecretCipher.setKey("key-one");
        String stored = handler.toJson(config());

        ModelSecretCipher.setKey("key-two");
        assertEquals("", ((JSONObject) handler.parse(stored)).getStr("api_key"));

        ModelSecretCipher.setKey(null);
        assertEquals("", ((JSONObject) handler.parse(stored)).getStr("api_key"));
        assertEquals("gpt-4o-mini", ((JSONObject) handler.parse(stored)).getStr("model_name"));
    }
}
