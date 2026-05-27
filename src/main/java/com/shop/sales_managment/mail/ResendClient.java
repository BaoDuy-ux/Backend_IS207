package com.shop.sales_managment.mail;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Minimal Resend API client (HTTPS).
 * Docs: https://resend.com/docs/api-reference/emails/send-email
 */
public class ResendClient {
    private static final URI SEND_URI = URI.create("https://api.resend.com/emails");
    private static final ObjectMapper om = new ObjectMapper();

    private final HttpClient http;
    private final String apiKey;

    public ResendClient(String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();
    }

    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    public void sendHtml(String from, String fromName, String to, String subject, String html) throws Exception {
        if (!isConfigured()) throw new IllegalStateException("RESEND_API_KEY chưa được cấu hình");

        // Resend expects "from" as "Name <email@domain>" or "email@domain"
        String fromValue = from;
        if (fromName != null && !fromName.isBlank() && from != null && from.contains("@")) {
            fromValue = fromName.trim() + " <" + from.trim() + ">";
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("from", fromValue);
        payload.put("to", new String[]{to});
        payload.put("subject", subject);
        payload.put("html", html);

        byte[] body = om.writeValueAsBytes(payload);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(SEND_URI)
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        HttpResponse<byte[]> res = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
        int code = res.statusCode();
        if (code >= 200 && code < 300) return;

        String text = new String(res.body() == null ? new byte[0] : res.body(), StandardCharsets.UTF_8);
        throw new RuntimeException("Resend send failed (" + code + "): " + text);
    }
}

