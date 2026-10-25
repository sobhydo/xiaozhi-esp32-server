package xiaozhi.modules.model.handler;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import xiaozhi.common.utils.SensitiveDataUtils;

/**
 * JacksonTypeHandler that encrypts sensitive config fields (api_key, secret, token, ...) on write and
 * decrypts "enc:v1:" values on read. See {@link ModelSecretCipher}.
 */
public class EncryptedJsonTypeHandler extends JacksonTypeHandler {

    public EncryptedJsonTypeHandler(Class<?> type) {
        super(type);
    }

    public EncryptedJsonTypeHandler(Class<?> type, Field field) {
        super(type, field);
    }

    @Override
    public String toJson(Object obj) {
        return encryptJson(super.toJson(obj));
    }

    @Override
    public Object parse(String json) {
        return super.parse(decryptJson(json));
    }

    /** Encrypts sensitive string fields of a JSON object document (no-op without a key). */
    public static String encryptJson(String json) {
        if (!ModelSecretCipher.isEnabled() || StringUtils.isBlank(json)) {
            return json;
        }
        return transform(json, true);
    }

    /** Decrypts every "enc:v1:" string value of a JSON document. */
    public static String decryptJson(String json) {
        if (json == null || !json.contains(ModelSecretCipher.PREFIX)) {
            return json;
        }
        return transform(json, false);
    }

    private static String transform(String json, boolean encrypt) {
        try {
            JsonNode root = getObjectMapper().readTree(json);
            if (root == null || !(root.isObject() || root.isArray())) {
                return json;
            }
            boolean changed = walk(root, encrypt);
            return changed ? getObjectMapper().writeValueAsString(root) : json;
        } catch (Exception e) {
            // Not valid JSON: leave untouched rather than break reads/writes
            return json;
        }
    }

    private static boolean walk(JsonNode node, boolean encrypt) {
        boolean changed = false;
        if (node instanceof ObjectNode obj) {
            Iterator<Map.Entry<String, JsonNode>> it = obj.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                JsonNode v = e.getValue();
                if (v.isTextual()) {
                    String text = v.asText();
                    String replaced = encrypt ? encryptValue(e.getKey(), text) : ModelSecretCipher.decrypt(text);
                    if (!text.equals(replaced)) {
                        e.setValue(obj.textNode(replaced));
                        changed = true;
                    }
                } else if (v.isContainerNode()) {
                    changed |= walk(v, encrypt);
                }
            }
        } else if (node instanceof ArrayNode arr) {
            for (JsonNode child : arr) {
                if (child.isContainerNode()) {
                    changed |= walk(child, encrypt);
                }
            }
        }
        return changed;
    }

    private static String encryptValue(String key, String value) {
        if (!SensitiveDataUtils.isSensitiveField(key) || value.isEmpty()
                || SensitiveDataUtils.isMaskedValue(value) || ModelSecretCipher.isEncrypted(value)) {
            return value;
        }
        return ModelSecretCipher.encrypt(value);
    }
}
