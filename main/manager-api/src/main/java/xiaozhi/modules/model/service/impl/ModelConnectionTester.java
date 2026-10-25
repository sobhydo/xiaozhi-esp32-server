package xiaozhi.modules.model.service.impl;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import xiaozhi.modules.model.dto.ModelTestResultDTO;

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
        if (!"LLM".equalsIgnoreCase(modelType) || provideCode == null) {
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
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            throw new IllegalArgumentException("URL must start with http:// or https://");
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
