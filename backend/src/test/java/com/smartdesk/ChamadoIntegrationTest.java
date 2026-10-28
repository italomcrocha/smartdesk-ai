package com.smartdesk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChamadoIntegrationTest {
    static final ObjectMapper JSON = new ObjectMapper();
    static final AtomicReference<String> reply = new AtomicReference<>();
    static final AtomicInteger status = new AtomicInteger(200);
    static final AtomicInteger calls = new AtomicInteger();
    static final AtomicReference<String> sentKey = new AtomicReference<>();
    static final AtomicReference<String> sentBody = new AtomicReference<>();
    static final HttpServer GEMINI = startStub();
    @LocalServerPort int port;
    final HttpClient http = HttpClient.newHttpClient();
    static final String VALID = "{\"categoria\":\"TI\",\"prioridade\":\"CRITICA\",\"resumo\":\"Computador indisponível.\",\"acaoRecomendada\":\"Acionar o suporte de TI.\"}";

    static HttpServer startStub() {
        try {
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/v1beta/models/test-model:generateContent", exchange -> {
                calls.incrementAndGet();
                sentKey.set(exchange.getRequestHeaders().getFirst("x-goog-api-key"));
                sentBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                byte[] bytes = reply.get().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(status.get(), bytes.length);
                try (var out = exchange.getResponseBody()) { out.write(bytes); }
            });
            server.start(); return server;
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
    @DynamicPropertySource static void config(DynamicPropertyRegistry registry) {
        registry.add("gemini.base-url", () -> "http://127.0.0.1:" + GEMINI.getAddress().getPort());
        registry.add("gemini.api-key", () -> "test-only-secret");
        registry.add("gemini.model", () -> "test-model");
    }
    @BeforeEach void reset() throws Exception { status.set(200); calls.set(0); setText(VALID); }
    @AfterAll static void stop() { GEMINI.stop(0); }
    void setText(String text) throws Exception {
        reply.set(JSON.writeValueAsString(Map.of("candidates", List.of(Map.of("finishReason", "STOP",
            "content", Map.of("parts", List.of(Map.of("text", text))))))));
    }
    HttpResponse<String> post(String body) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/chamados/analisar"))
            .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
    @Test void roundTrip() throws Exception {
        var response = post("{\"descricao\":\"  Computador não liga.  \"}");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(JSON.readTree(response.body()).path("prioridade").asText()).isEqualTo("CRITICA");
        assertThat(sentKey.get()).isEqualTo("test-only-secret");
        var body = JSON.readTree(sentBody.get());
        assertThat(body.path("contents").path(0).path("parts").path(0).path("text").asText()).isEqualTo("Computador não liga.");
        assertThat(body.path("generationConfig").path("responseMimeType").asText()).isEqualTo("application/json");
        assertThat(response.body()).doesNotContain("test-only-secret");
    }
    @Test void acceptsMarkdown() throws Exception {
        setText("```json\n" + VALID + "\n```");
        assertThat(post("{\"descricao\":\"Computador não liga\"}").statusCode()).isEqualTo(200);
    }
    @ParameterizedTest @ValueSource(strings = {"", " ", "not json", "null", "{}", "{\"categoria\":\"Segredo\"}", "[]"})
    void rejectsInvalidModelOutput(String text) throws Exception {
        setText(text);
        var response = post("{\"descricao\":\"Computador não liga\"}");
        assertThat(response.statusCode()).isEqualTo(502);
        assertThat(response.body()).contains("erro").doesNotContain("test-only-secret", "Exception");
    }
    @Test void rejectsInvalidEnumAndTrailingTokens() throws Exception {
        setText(VALID.replace("CRITICA", "URGENTE"));
        assertThat(post("{\"descricao\":\"Teste\"}").statusCode()).isEqualTo(502);
        setText(VALID + " {}");
        assertThat(post("{\"descricao\":\"Teste\"}").statusCode()).isEqualTo(502);
    }
    @ParameterizedTest @ValueSource(strings = {"{}", "{\"descricao\":\"   \"}", "{\"descricao\":null}", "invalid-json"})
    void rejectsInvalidInputWithoutCallingGemini(String body) throws Exception {
        assertThat(post(body).statusCode()).isEqualTo(400);
        assertThat(calls.get()).isZero();
    }
    @Test void rejectsOversizedInput() throws Exception {
        assertThat(post(JSON.writeValueAsString(Map.of("descricao", "a".repeat(10001)))).statusCode()).isEqualTo(400);
        assertThat(calls.get()).isZero();
    }
    @Test void handlesProviderErrors() throws Exception {
        for (int[] mapping : new int[][]{{429, 429}, {503, 503}, {401, 502}}) {
            status.set(mapping[0]); reply.set("{\"error\":\"test-only-secret internal provider error\"}");
            var response = post("{\"descricao\":\"Teste\"}");
            assertThat(response.statusCode()).isEqualTo(mapping[1]);
            assertThat(response.body()).contains("erro").doesNotContain("test-only-secret", "internal provider");
        }
    }
    @Test void handlesBlockedOrTruncatedResponse() throws Exception {
        reply.set("{\"candidates\":[{\"finishReason\":\"MAX_TOKENS\"}]}");
        assertThat(post("{\"descricao\":\"Teste\"}").statusCode()).isEqualTo(502);
        reply.set("{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}");
        assertThat(post("{\"descricao\":\"Teste\"}").statusCode()).isEqualTo(502);
    }
    @Test void corsAllowsLocalFrontendsAndRejectsUnknownOrigin() throws Exception {
        for (String origin : List.of("http://localhost:5500", "http://localhost:3000", "http://127.0.0.1:5500", "https://unknown.example")) {
            var response = http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/chamados/analisar"))
                .header("Origin", origin).header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type").method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
            if (origin.startsWith("https")) assertThat(response.statusCode()).isEqualTo(403);
            else assertThat(response.headers().firstValue("Access-Control-Allow-Origin")).contains(origin);
        }
    }
    @Test void servesFrontendWithoutSecrets() throws Exception {
        for (String path : List.of("/", "/style.css", "/script.js")) {
            var response = http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).doesNotContain("test-only-secret", "x-goog-api-key", "generativelanguage.googleapis.com");
        }
    }
}
