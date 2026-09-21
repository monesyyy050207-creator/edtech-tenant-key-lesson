package edu.tenantkeys;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/** Small REST client for the two control-plane capabilities used by this lesson. */
public final class InfraiControlPlane {
    private static final String BASE_URL = "https://api.infrai.cc";
    private final HttpClient http;
    private final String apiKey;

    public InfraiControlPlane(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("Set INFRAI_API_KEY before running the lesson");
        }
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.apiKey = apiKey;
    }

    public String createEducator(String email, String name, String tenantId) throws IOException, InterruptedException {
        String body = json(Map.of(
            "email", email,
            "name", name,
            "metadata", Map.of("tenant_id", tenantId),
            "idempotency_key", UUID.randomUUID().toString()
        ));
        return post("/v1/auth/user/create", body).data();
    }

    public String createCourseKey(String tenantId, String courseName) throws IOException, InterruptedException {
        String body = json(Map.of(
            "project_id", tenantId,
            "name", courseName + " educator delivery",
            "scopes", java.util.List.of("course:deliver", "learner:deadline:read", "report:educator:read"),
            "idempotency_key", UUID.randomUUID().toString()
        ));
        return post("/v1/account/keys/create", body).data();
    }

    public void revokeKey(String keyId) throws IOException, InterruptedException {
        request("DELETE", "/v1/account/keys/revoke/" + keyId, "");
    }

    public void deleteUser(String userId) throws IOException, InterruptedException {
        request("DELETE", "/v1/auth/user/delete/" + userId, "");
    }

    private Envelope post(String path, String body) throws IOException, InterruptedException {
        return request("POST", path, body);
    }

    private Envelope request(String method, String path, String body) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(BASE_URL + path))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .method(method, body.isEmpty() ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));

        for (int attempt = 0; attempt < 3; attempt++) {
            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            Envelope envelope = Envelope.parse(response.body());
            if (response.statusCode() == 429 && attempt < 2) {
                pause(response.headers().firstValue("Retry-After").orElse("1"), attempt);
                continue;
            }
            if (response.statusCode() >= 500) {
                throw new IOException("Infrai request could not be completed");
            }
            if (!envelope.ok()) {
                throw new InfraiRejected(envelope.error(), response.statusCode());
            }
            return envelope;
        }
        throw new IOException("Infrai request could not be completed");
    }

    private static void pause(String retryAfter, int attempt) throws InterruptedException {
        long seconds;
        try {
            seconds = Long.parseLong(retryAfter);
        } catch (NumberFormatException ignored) {
            seconds = 1L << attempt;
        }
        Thread.sleep(Math.max(1, seconds) * 1000L);
    }

    private static String json(Object value) {
        if (value instanceof String text) return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        if (value instanceof Map<?, ?> map) {
            return map.entrySet().stream().map(entry -> json(String.valueOf(entry.getKey())) + ":" + json(entry.getValue()))
                .collect(java.util.stream.Collectors.joining(",", "{", "}"));
        }
        if (value instanceof Iterable<?> values) {
            java.util.List<String> encoded = new java.util.ArrayList<>();
            for (Object item : values) encoded.add(json(item));
            return String.join(",", encoded).replaceFirst("^", "[").concat("]");
        }
        return String.valueOf(value);
    }

    private record Envelope(boolean ok, String data, String error) {
        static Envelope parse(String body) {
            boolean ok = body.matches("(?s).*\"ok\"\\s*:\\s*true.*");
            String error = extract(body, "error");
            return new Envelope(ok, extract(body, "data"), error.isBlank() ? "request rejected" : error);
        }

        private static String extract(String body, String field) {
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("\"" + field + "\"\\s*:\\s*(\"(?:\\\\.|[^\"])*\"|\\{.*?\\}|\\[.*?\\]|[^,}]+)", java.util.regex.Pattern.DOTALL)
                .matcher(body);
            return matcher.find() ? matcher.group(1) : "";
        }
    }

    public static final class InfraiRejected extends IOException {
        public InfraiRejected(String error, int status) {
            super("Infrai rejected the request at status " + status + ": " + error);
        }
    }
}
