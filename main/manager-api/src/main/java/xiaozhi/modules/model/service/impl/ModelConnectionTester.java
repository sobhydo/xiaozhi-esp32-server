package xiaozhi.modules.model.service.impl;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import xiaozhi.common.exception.RenException;
import xiaozhi.modules.model.dto.ModelTestResultDTO;
import xiaozhi.modules.model.dto.ProviderVoiceDTO;

/**
 * BYOK: checks that a provider accepts the given key/model. Never throws; failures are reported as ok=false.
 */
@Slf4j
@Component
public class ModelConnectionTester {

    static final Duration TIMEOUT = Duration.ofSeconds(10);
    static final int MAX_MESSAGE = 300;
    static final String DEFAULT_OPENAI_BASE = "https://api.openai.com/v1";
    static final String DEFAULT_CLAUDE_BASE = "https://api.anthropic.com";
    static final String GEMINI_BASE = "https://generativelanguage.googleapis.com/v1beta/models/";
    public static final String DEFAULT_ELEVENLABS_BASE = "https://api.elevenlabs.io/v1";
    static final String DEFAULT_SCRIBE_MODEL = "scribe_v2";
    static final String DEFAULT_ELEVENLABS_VOICE = "21m00Tcm4TlvDq8ikWAM";
    /** OpenAI TTS voices (the API has no voice listing endpoint). */
    static final List<String> OPENAI_VOICES = List.of("alloy", "ash", "ballad", "coral", "echo", "fable", "nova",
            "onyx", "sage", "shimmer", "verse", "marin", "cedar");

    private final HttpClient httpClient;

    public ModelConnectionTester() {
        // No redirects: a public host could otherwise bounce the request to an internal address
        this(HttpClient.newBuilder().connectTimeout(TIMEOUT).followRedirects(HttpClient.Redirect.NEVER).build());
    }

    ModelConnectionTester(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public ModelTestResultDTO test(String modelType, String provideCode, JSONObject config) {
        long start = System.currentTimeMillis();
        try {
            HttpRequest request = buildRequest(modelType, provideCode, config == null ? new JSONObject() : config);
            if (request == null) {
                return new ModelTestResultDTO(false, "Test not available for this provider yet", 0);
            }
            assertPublicUrl(request.uri().toString());
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            long latency = System.currentTimeMillis() - start;
            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                return new ModelTestResultDTO(true, "Connection OK", latency);
            }
            return new ModelTestResultDTO(false, trim("HTTP " + status + ": " + errorText(response.body())), latency);
        } catch (IllegalArgumentException e) {
            return new ModelTestResultDTO(false, trim(e.getMessage()), System.currentTimeMillis() - start);
        } catch (HttpTimeoutException e) {
            return new ModelTestResultDTO(false, "Timed out after " + TIMEOUT.toSeconds() + " s",
                    System.currentTimeMillis() - start);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ModelTestResultDTO(false, "Interrupted", System.currentTimeMillis() - start);
        } catch (Exception e) {
            String msg = StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName());
            return new ModelTestResultDTO(false, trim(msg), System.currentTimeMillis() - start);
        }
    }

    /** Returns the provider request, or null when no test exists for this type/provider. */
    HttpRequest buildRequest(String modelType, String provideCode, JSONObject config) {
        if (provideCode == null || modelType == null) {
            return null;
        }
        if ("ASR".equalsIgnoreCase(modelType) || "TTS".equalsIgnoreCase(modelType)) {
            return buildSpeechRequest(modelType.toUpperCase(), provideCode, config);
        }
        if (!"LLM".equalsIgnoreCase(modelType)) {
            return null;
        }
        String apiKey = config.getStr("api_key", "").trim();
        String model = config.getStr("model_name", "").trim();
        String baseUrl = config.getStr("base_url", "").trim();
        switch (provideCode) {
            case "openai": {
                require(apiKey, "API key is required");
                require(model, "Model is required");
                String base = stripTrailingSlash(StringUtils.defaultIfBlank(baseUrl, DEFAULT_OPENAI_BASE));
                String url = base.endsWith("/chat/completions") ? base : base + "/chat/completions";
                JSONObject body = new JSONObject()
                        .set("model", model)
                        .set("messages", new JSONArray().put(new JSONObject().set("role", "user").set("content", "ping")))
                        .set("max_tokens", 5);
                return HttpRequest.newBuilder(URI.create(url))
                        .timeout(TIMEOUT)
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                        .build();
            }
            case "gemini": {
                require(apiKey, "API key is required");
                require(model, "Model is required");
                String url = GEMINI_BASE + URLEncoder.encode(model, StandardCharsets.UTF_8) + ":generateContent";
                String body = "{\"contents\":[{\"parts\":[{\"text\":\"ping\"}]}]}";
                return HttpRequest.newBuilder(URI.create(url))
                        .timeout(TIMEOUT)
                        .header("x-goog-api-key", apiKey)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                        .build();
            }
            case "claude": {
                require(apiKey, "API key is required");
                String base = stripTrailingSlash(StringUtils.defaultIfBlank(baseUrl, DEFAULT_CLAUDE_BASE));
                if (base.endsWith("/v1")) {
                    base = base.substring(0, base.length() - 3);
                }
                return HttpRequest.newBuilder(URI.create(base + "/v1/models?limit=1"))
                        .timeout(TIMEOUT)
                        .header("x-api-key", apiKey)
                        .header("anthropic-version", "2023-06-01")
                        .GET()
                        .build();
            }
            default:
                return null;
        }
    }

    /**
     * ASR/TTS: make a tiny real request (0.5 s of silence to transcribe, or "Hi" to synthesize) so keys missing
     * the speech permission fail here rather than on the device. Listing models passes with restricted keys.
     */
    private HttpRequest buildSpeechRequest(String modelType, String provideCode, JSONObject config) {
        String apiKey = config.getStr("api_key", "").trim();
        boolean asr = "ASR".equals(modelType);
        switch (provideCode) {
            case "elevenlabs":
            case "elevenlabs_stream": {
                require(apiKey, "API key is required");
                String base = elevenLabsBase(modelType, config);
                if (asr) {
                    // The realtime model only exists on the websocket API; the batch model checks the same permission
                    String model = "elevenlabs_stream".equals(provideCode) ? DEFAULT_SCRIBE_MODEL
                            : StringUtils.defaultIfBlank(config.getStr("model_id", "").trim(), DEFAULT_SCRIBE_MODEL);
                    return multipartRequest(base + "/speech-to-text", "model_id", model)
                            .header("xi-api-key", apiKey)
                            .build();
                }
                String voice = StringUtils.defaultIfBlank(config.getStr("voice", "").trim(), DEFAULT_ELEVENLABS_VOICE);
                JSONObject body = new JSONObject().set("text", "Hi").set("model_id",
                        StringUtils.defaultIfBlank(config.getStr("model_id", "").trim(), "eleven_flash_v2_5"));
                return HttpRequest.newBuilder(URI.create(base + "/text-to-speech/"
                        + URLEncoder.encode(voice, StandardCharsets.UTF_8) + "?output_format=mp3_22050_32"))
                        .timeout(TIMEOUT)
                        .header("xi-api-key", apiKey)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                        .build();
            }
            case "openai": {
                require(apiKey, "API key is required");
                String base = openAiSpeechBase(modelType, config);
                if (asr) {
                    String model = StringUtils.defaultIfBlank(config.getStr("model_name", "").trim(),
                            "gpt-4o-mini-transcribe");
                    return multipartRequest(base + "/audio/transcriptions", "model", model)
                            .header("Authorization", "Bearer " + apiKey)
                            .build();
                }
                JSONObject body = new JSONObject()
                        .set("model", StringUtils.defaultIfBlank(config.getStr("model", "").trim(), "tts-1"))
                        .set("input", "Hi")
                        .set("voice", StringUtils.defaultIfBlank(config.getStr("voice", "").trim(), "alloy"));
                return HttpRequest.newBuilder(URI.create(base + "/audio/speech"))
                        .timeout(TIMEOUT)
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                        .build();
            }
            default:
                return null;
        }
    }

    /** POST multipart/form-data with one text field and a 0.5 s silent 16 kHz mono WAV as "file". */
    private static HttpRequest.Builder multipartRequest(String url, String field, String value) {
        String boundary = "----xiaozhi" + System.nanoTime();
        byte[] head = ("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + field + "\"\r\n\r\n" + value
                + "\r\n--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"test.wav\"\r\n"
                + "Content-Type: audio/wav\r\n\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] tail = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] wav = silentWav();
        byte[] body = new byte[head.length + wav.length + tail.length];
        System.arraycopy(head, 0, body, 0, head.length);
        System.arraycopy(wav, 0, body, head.length, wav.length);
        System.arraycopy(tail, 0, body, head.length + wav.length, tail.length);
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(TIMEOUT)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body));
    }

    static byte[] silentWav() {
        int sampleRate = 16000;
        int dataLen = sampleRate; // 0.5 s of 16-bit mono
        ByteBuffer b = ByteBuffer.allocate(44 + dataLen).order(ByteOrder.LITTLE_ENDIAN);
        b.put("RIFF".getBytes(StandardCharsets.US_ASCII)).putInt(36 + dataLen)
                .put("WAVE".getBytes(StandardCharsets.US_ASCII))
                .put("fmt ".getBytes(StandardCharsets.US_ASCII)).putInt(16).putShort((short) 1).putShort((short) 1)
                .putInt(sampleRate).putInt(sampleRate * 2).putShort((short) 2).putShort((short) 16)
                .put("data".getBytes(StandardCharsets.US_ASCII)).putInt(dataLen);
        return b.array();
    }

    /**
     * ElevenLabs API base (".../v1"): TTS configs use api_base; ASR configs hold a full endpoint in base_url,
     * cut after "/v1". Falls back to the public API.
     */
    public static String elevenLabsBase(String modelType, JSONObject config) {
        if (config == null) {
            return DEFAULT_ELEVENLABS_BASE;
        }
        String apiBase = config.getStr("api_base", "").trim();
        if (StringUtils.isNotBlank(apiBase)) {
            return stripTrailingSlash(apiBase);
        }
        String baseUrl = config.getStr("base_url", "").trim();
        if ("ASR".equalsIgnoreCase(modelType) && StringUtils.isNotBlank(baseUrl)) {
            int idx = baseUrl.indexOf("/v1");
            if (idx > 0) {
                return baseUrl.substring(0, idx + 3);
            }
        }
        return DEFAULT_ELEVENLABS_BASE;
    }

    /** OpenAI API base (".../v1") derived from a speech endpoint (api_url for TTS, base_url for ASR). */
    static String openAiSpeechBase(String modelType, JSONObject config) {
        String first = "TTS".equalsIgnoreCase(modelType) ? "api_url" : "base_url";
        String second = "TTS".equalsIgnoreCase(modelType) ? "base_url" : "api_url";
        String url = StringUtils.defaultIfBlank(config.getStr(first, "").trim(), config.getStr(second, "").trim());
        if (StringUtils.isBlank(url)) {
            return DEFAULT_OPENAI_BASE;
        }
        int idx = url.indexOf("/audio/");
        if (idx < 0 && url.endsWith("/audio")) {
            idx = url.length() - "/audio".length();
        }
        return stripTrailingSlash(idx > 0 ? url.substring(0, idx) : url);
    }

    /**
     * Voices available on the provider for this TTS config, for a picker. Throws RenException with the
     * provider's error (max 300 chars).
     */
    public List<ProviderVoiceDTO> listProviderVoices(String provideCode, JSONObject config) {
        JSONObject cfg = config == null ? new JSONObject() : config;
        if ("openai".equals(provideCode)) {
            return OPENAI_VOICES.stream().map(v -> new ProviderVoiceDTO(v, v, "premade", null, null)).toList();
        }
        if (!"elevenlabs".equals(provideCode)) {
            throw new RenException("Voice listing not available for this provider");
        }
        try {
            HttpRequest request = buildElevenLabsVoicesRequest(cfg);
            assertPublicUrl(request.uri().toString());
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status = response.statusCode();
            if (status < 200 || status >= 300) {
                throw new RenException(trim("HTTP " + status + ": " + errorText(response.body())));
            }
            return parseElevenLabsVoices(response.body());
        } catch (RenException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw new RenException(trim(e.getMessage()));
        } catch (HttpTimeoutException e) {
            throw new RenException("Timed out after " + TIMEOUT.toSeconds() + " s");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RenException("Interrupted");
        } catch (Exception e) {
            throw new RenException(trim(StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName())));
        }
    }

    HttpRequest buildElevenLabsVoicesRequest(JSONObject config) {
        String apiKey = config.getStr("api_key", "").trim();
        require(apiKey, "API key is required");
        return HttpRequest.newBuilder(URI.create(elevenLabsBase("TTS", config) + "/voices"))
                .timeout(TIMEOUT)
                .header("xi-api-key", apiKey)
                .GET()
                .build();
    }

    static List<ProviderVoiceDTO> parseElevenLabsVoices(String body) {
        List<ProviderVoiceDTO> result = new ArrayList<>();
        JSONObject root = JSONUtil.parseObj(body);
        JSONArray voices = root.getJSONArray("voices");
        if (voices == null) {
            return result;
        }
        for (Object item : voices) {
            if (!(item instanceof JSONObject v) || StringUtils.isBlank(v.getStr("voice_id"))) {
                continue;
            }
            JSONObject labels = v.getJSONObject("labels");
            Map<String, Object> labelMap = labels == null ? null : new LinkedHashMap<>(labels);
            result.add(new ProviderVoiceDTO(v.getStr("voice_id"), v.getStr("name"), v.getStr("category"),
                    v.getStr("preview_url"), labelMap));
        }
        return result;
    }

    /** Best-effort delete of an ElevenLabs voice; returns false (and logs) on any failure. */
    public boolean deleteElevenLabsVoice(String apiBase, String apiKey, String voiceId) {
        if (StringUtils.isAnyBlank(apiKey, voiceId)) {
            return false;
        }
        try {
            HttpRequest request = buildElevenLabsDeleteRequest(apiBase, apiKey, voiceId);
            assertPublicUrl(request.uri().toString());
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return true;
            }
            log.warn("ElevenLabs voice delete failed for {}: HTTP {} {}", voiceId, response.statusCode(),
                    trim(errorText(response.body())));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("ElevenLabs voice delete interrupted for {}", voiceId);
        } catch (Exception e) {
            log.warn("ElevenLabs voice delete failed for {}: {}", voiceId, e.getMessage());
        }
        return false;
    }

    HttpRequest buildElevenLabsDeleteRequest(String apiBase, String apiKey, String voiceId) {
        String base = stripTrailingSlash(StringUtils.defaultIfBlank(StringUtils.trim(apiBase), DEFAULT_ELEVENLABS_BASE));
        return HttpRequest.newBuilder(URI.create(base + "/voices/" + URLEncoder.encode(voiceId, StandardCharsets.UTF_8)))
                .timeout(TIMEOUT)
                .header("xi-api-key", apiKey)
                .DELETE()
                .build();
    }

    /**
     * Rejects URLs users could use to reach the server's own network (SSRF): non-http(s) schemes and hosts
     * resolving to loopback, private, link-local, CGNAT, unique-local or wildcard addresses.
     */
    public static void assertPublicUrl(String url) {
        URI uri;
        try {
            uri = URI.create(StringUtils.trimToEmpty(url));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid URL");
        }
        String scheme = StringUtils.lowerCase(uri.getScheme());
        // ws/wss: streaming ASR endpoints
        if (!"http".equals(scheme) && !"https".equals(scheme) && !"ws".equals(scheme) && !"wss".equals(scheme)) {
            throw new IllegalArgumentException("URL must start with http://, https://, ws:// or wss://");
        }
        String host = uri.getHost();
        if (StringUtils.isBlank(host)) {
            throw new IllegalArgumentException("Invalid URL");
        }
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Host not found: " + host);
        }
        for (InetAddress address : addresses) {
            if (isInternal(address)) {
                throw new IllegalArgumentException("Private or internal addresses are not allowed");
            }
        }
    }

    static boolean isInternal(InetAddress address) {
        if (address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress()
                || address.isAnyLocalAddress() || address.isMulticastAddress()) {
            return true;
        }
        byte[] b = address.getAddress();
        if (b.length == 4) {
            int first = b[0] & 0xff;
            int second = b[1] & 0xff;
            // 100.64.0.0/10 (CGNAT) and 0.0.0.0/8
            return (first == 100 && second >= 64 && second <= 127) || first == 0;
        }
        // fc00::/7 unique-local IPv6
        return (b[0] & 0xfe) == 0xfc;
    }

    /** Pulls error.message out of OpenAI/Gemini/Anthropic error bodies; falls back to the raw body. */
    static String errorText(String body) {
        if (StringUtils.isBlank(body)) {
            return "(empty response)";
        }
        try {
            Object parsed = JSONUtil.parse(body);
            if (parsed instanceof JSONArray arr && !arr.isEmpty()) {
                parsed = arr.get(0);
            }
            if (parsed instanceof JSONObject obj) {
                Object error = obj.get("error");
                if (error instanceof JSONObject err && StringUtils.isNotBlank(err.getStr("message"))) {
                    return err.getStr("message");
                }
                if (error instanceof String s && StringUtils.isNotBlank(s)) {
                    return s;
                }
                if (StringUtils.isNotBlank(obj.getStr("message"))) {
                    return obj.getStr("message");
                }
            }
        } catch (Exception ignored) {
            // not JSON
        }
        return body.trim();
    }

    static String trim(String message) {
        String m = StringUtils.defaultString(message).replaceAll("\\s+", " ").trim();
        return m.length() > MAX_MESSAGE ? m.substring(0, MAX_MESSAGE) : m;
    }

    private static void require(String value, String message) {
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException(message);
        }
    }

    private static String stripTrailingSlash(String url) {
        String u = url;
        while (u.endsWith("/")) {
            u = u.substring(0, u.length() - 1);
        }
        return u;
    }
}
