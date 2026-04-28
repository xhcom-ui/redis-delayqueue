package com.openclaw.delayqueue.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclaw.delayqueue.model.HttpCallResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Component
public class HttpExecutor {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Duration timeout;

    public HttpExecutor(ObjectMapper objectMapper, @Value("${delay-queue.http-timeout-seconds}") long timeoutSeconds) {
        this.objectMapper = objectMapper;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    public HttpCallResult execute(String method, String url, String headersJson, String body) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            String normalizedMethod = method == null || method.isBlank() ? "POST" : method.toUpperCase();
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url)).timeout(timeout);
            applyHeaders(builder, headersJson);
            if (hasBody(normalizedMethod)) {
                builder.method(normalizedMethod, HttpRequest.BodyPublishers.ofString(body == null ? "" : body));
            } else {
                builder.method(normalizedMethod, HttpRequest.BodyPublishers.noBody());
            }

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return new HttpCallResult(response.statusCode(), response.body(), null);
        } catch (Exception e) {
            return new HttpCallResult(null, null, e.getMessage());
        }
    }

    private void applyHeaders(HttpRequest.Builder builder, String headersJson) throws Exception {
        if (headersJson == null || headersJson.isBlank()) {
            builder.header("Content-Type", "application/json");
            return;
        }
        Map<String, String> headers = objectMapper.readValue(headersJson, new TypeReference<>() {});
        headers.forEach((key, value) -> {
            if (key != null && !key.isBlank() && value != null) {
                builder.header(key, value);
            }
        });
    }

    private boolean hasBody(String method) {
        return !"GET".equals(method) && !"DELETE".equals(method);
    }
}
