package io.wdsj.asw.bukkit.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.wdsj.asw.bukkit.api.moderation.LlmModerationCategory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompatibleLlmChatClientTest {
    private static final String CLASSIFICATION = "{\\\"category\\\":\\\"clean\\\",\\\"secondary_categories\\\":[],\\\"confidence\\\":1.0,\\\"severity\\\":\\\"none\\\",\\\"signals\\\":[],\\\"explanation\\\":\\\"Clean.\\\"}";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void usesChatCompletionsEndpoint() throws Exception {
        AtomicReference<RequestCapture> request = new AtomicReference<>();
        server = startServer("/chat/completions", request, responseFor(LlmApiMode.CHAT_COMPLETIONS));

        assertEquals(rawClassification(), client(LlmApiMode.CHAT_COMPLETIONS, false)
                .classify("system", "user"));

        JsonNode body = requestBody(request);
        assertTrue(body.has("messages"));
        assertFalse(body.has("response_format"));
    }

    @Test
    void usesResponsesEndpointWithoutProviderStorage() throws Exception {
        AtomicReference<RequestCapture> request = new AtomicReference<>();
        server = startServer("/responses", request, responseFor(LlmApiMode.RESPONSES));

        assertEquals(rawClassification(), client(LlmApiMode.RESPONSES, false)
                .classify("system", "user"));

        JsonNode body = requestBody(request);
        assertFalse(body.get("store").asBoolean());
        assertTrue(body.has("input"));
        assertFalse(body.has("text"));
    }

    @Test
    void usesAnthropicMessagesEndpointAndHeaders() throws Exception {
        AtomicReference<RequestCapture> request = new AtomicReference<>();
        server = startServer("/messages", request, responseFor(LlmApiMode.ANTHROPIC_MESSAGES));

        assertEquals(rawClassification(), client(LlmApiMode.ANTHROPIC_MESSAGES, false)
                .classify("system", "user"));

        RequestCapture capture = request.get();
        assertNotNull(capture);
        assertEquals("test-key", capture.headers().getFirst("x-api-key"));
        assertEquals("2023-06-01", capture.headers().getFirst("anthropic-version"));
        JsonNode body = OBJECT_MAPPER.readTree(capture.body());
        assertTrue(body.has("messages"));
        assertEquals("disabled", body.path("thinking").path("type").asText());
    }

    @Test
    void usesConfiguredAnthropicThinkingMode() throws Exception {
        AtomicReference<RequestCapture> request = new AtomicReference<>();
        server = startServer("/messages", request, responseFor(LlmApiMode.ANTHROPIC_MESSAGES));

        assertEquals(rawClassification(), client(LlmApiMode.ANTHROPIC_MESSAGES, true)
                .classify("system", "user"));

        JsonNode body = requestBody(request);
        assertEquals("enabled", body.path("thinking").path("type").asText());
    }

    private HttpServer startServer(String path, AtomicReference<RequestCapture> request, String response) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        httpServer.createContext(path, exchange -> respond(exchange, request, response));
        httpServer.start();
        return httpServer;
    }

    private CompatibleLlmChatClient client(LlmApiMode mode, boolean anthropicThinkingEnabled) {
        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        return new CompatibleLlmChatClient(new LlmChatDetectionService.LlmSettings(
                baseUrl,
                mode,
                "2023-06-01",
                anthropicThinkingEnabled,
                "test-key",
                "test-model",
                5,
                128,
                0.0D,
                false,
                1,
                1,
                0,
                1,
                256,
                2.5D,
                Map.of(LlmModerationCategory.CLEAN, new LlmCategoryPolicy(-1.0D, -1.0D)),
                "",
                false
        ));
    }

    private static JsonNode requestBody(AtomicReference<RequestCapture> request) throws IOException {
        RequestCapture capture = request.get();
        assertNotNull(capture);
        return OBJECT_MAPPER.readTree(capture.body());
    }

    private static void respond(HttpExchange exchange, AtomicReference<RequestCapture> request, String response) throws IOException {
        request.set(new RequestCapture(
                new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8),
                new Headers(exchange.getRequestHeaders())
        ));
        byte[] payload = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, payload.length);
        exchange.getResponseBody().write(payload);
        exchange.close();
    }

    private static String responseFor(LlmApiMode mode) {
        return switch (mode) {
            case CHAT_COMPLETIONS -> """
                    {"id":"chatcmpl-test","created":0,"model":"test-model","choices":[{"index":0,"message":{"role":"assistant","content":"%s"},"finish_reason":"stop"}]}
                    """.formatted(CLASSIFICATION);
            case RESPONSES -> """
                    {"id":"resp-test","created_at":0,"completed_at":0,"model":"test-model","status":"completed","output":[{"type":"message","role":"assistant","content":[{"type":"output_text","text":"%s"}]}]}
                    """.formatted(CLASSIFICATION);
            case ANTHROPIC_MESSAGES -> """
                    {"id":"msg-test","type":"message","role":"assistant","model":"test-model","stop_reason":"end_turn","usage":{"input_tokens":1,"output_tokens":1},"content":[{"type":"text","text":"%s"}]}
                    """.formatted(CLASSIFICATION);
        };
    }

    private static String rawClassification() {
        return CLASSIFICATION.replace("\\\"", "\"");
    }

    private record RequestCapture(String body, Headers headers) {
    }
}
