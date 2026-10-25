package xiaozhi.modules.model.handler;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.StringUtils;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * AES-256-GCM encryption of model secrets (API keys) at rest.
 * Key: environment variable MODEL_SECRET_KEY (any string, SHA-256 derived). Without it values pass through.
 * Stored format: "enc:v1:" + base64(iv[12] || ciphertext+tag).
 */
@Slf4j
@org.springframework.stereotype.Component
public class ModelSecretCipher {

    public static final String PREFIX = "enc:v1:";
    public static final String ENV_KEY = "MODEL_SECRET_KEY";

    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private static volatile SecretKeySpec key = deriveKey(System.getenv(ENV_KEY));

    @PostConstruct
    void logStatus() {
        if (key == null) {
            log.warn("{} is not set: model API keys are stored unencrypted", ENV_KEY);
        }
    }

    /** Replaces the key (null/blank disables encryption). For tests. */
    public static void setKey(String secret) {
        key = deriveKey(secret);
    }

    public static boolean isEnabled() {
        return key != null;
    }

    public static boolean isEncrypted(String value) {
        return value != null && value.startsWith(PREFIX);
    }

    /** Encrypts a plain value; returns it unchanged when encryption is disabled or it is already encrypted. */
    public static String encrypt(String plain) {
        SecretKeySpec k = key;
        if (k == null || plain == null || isEncrypted(plain)) {
            return plain;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, k, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            return PREFIX + Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("Model secret encryption failed", e);
        }
    }

    /** Decrypts an "enc:v1:" value; plain values pass through. Failure logs an error and returns "". */
    public static String decrypt(String value) {
        if (!isEncrypted(value)) {
            return value;
        }
        SecretKeySpec k = key;
        if (k == null) {
            log.error("Encrypted model secret found but {} is not set", ENV_KEY);
            return "";
        }
        try {
            byte[] in = Base64.getDecoder().decode(value.substring(PREFIX.length()));
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, k, new GCMParameterSpec(TAG_BITS, in, 0, IV_LENGTH));
            byte[] plain = cipher.doFinal(in, IV_LENGTH, in.length - IV_LENGTH);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Model secret decryption failed (wrong {}?): {}", ENV_KEY, e.getMessage());
            return "";
        }
    }

    private static SecretKeySpec deriveKey(String secret) {
        if (StringUtils.isBlank(secret)) {
            return null;
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(digest, "AES");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
