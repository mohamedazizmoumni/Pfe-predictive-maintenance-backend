package com.pfe.predictive.ml.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfe.predictive.ml.config.MlServiceProperties;
import com.pfe.predictive.ml.dto.ModelInfoResponse;
import com.pfe.predictive.ml.dto.PredictionRequest;
import com.pfe.predictive.ml.dto.PredictionResponse;
import com.pfe.predictive.ml.exception.MlServiceUnavailableException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PythonMlClientTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void healthCheckSuccess() throws Exception {
        startServer();
        server.createContext("/health", jsonHandler(200, "{\"status\":\"ok\"}"));

        PythonMlClient client = createClient(defaultProperties(server.getAddress().getPort()));

        Map<String, Object> response = client.health("corr-1");

        assertEquals("ok", response.get("status"));
    }

    @Test
    void modelInfoSuccess() throws Exception {
        startServer();
        server.createContext("/model-info", jsonHandler(200,
                "{\"feature_count\":89,\"feature_names\":[\"f1\",\"f2\"],\"metrics\":{\"mae\":1.2},\"model_loaded\":true}"));

        PythonMlClient client = createClient(defaultProperties(server.getAddress().getPort()));

        ModelInfoResponse info = client.modelInfo("corr-1");

        assertEquals(89, info.getFeatureCount());
        assertEquals(true, info.getModelLoaded());
        assertNotNull(info.getMetrics());
    }

    @Test
    void successfulPredictionAndContractShape() throws Exception {
        startServer();
        AtomicReference<String> bodyCapture = new AtomicReference<>();
        server.createContext("/predict", exchange -> {
            String payload = new String(exchange.getRequestBody().readAllBytes());
            bodyCapture.set(payload);
            byte[] response = "{\"prediction\":[12.3,9.8]}".getBytes();
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        PythonMlClient client = createClient(defaultProperties(server.getAddress().getPort()));
        PredictionResponse prediction = client.predict(new PredictionRequest(List.of(sample(89), sample(89))), "corr-1", 1L);

        assertEquals(2, prediction.getPrediction().size());
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(bodyCapture.get());
        assertTrue(node.has("features"));
        assertEquals(2, node.get("features").size());
        assertEquals(89, node.get("features").get(0).size());
        assertEquals(0.1, node.get("features").get(0).get(0).asDouble(), 0.0001);
        assertEquals(88.1, node.get("features").get(0).get(88).asDouble(), 0.0001);
    }

    @Test
    void timeoutHandling() throws Exception {
        startServer();
        server.createContext("/predict", exchange -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] response = "{\"prediction\":[1.0]}".getBytes();
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        MlServiceProperties properties = defaultProperties(server.getAddress().getPort());
        properties.getTimeout().setRead(Duration.ofMillis(100));
        properties.getRetry().setMaxAttempts(2);
        properties.getRetry().setBackoff(Duration.ofMillis(10));

        PythonMlClient client = createClient(properties);

        assertThrows(MlServiceUnavailableException.class,
            () -> client.predict(new PredictionRequest(List.of(sample(89))), "corr-1", 1L));
    }

    @Test
    void serviceUnavailableHandling() {
        MlServiceProperties properties = defaultProperties(1);
        properties.setBaseUrl("http://127.0.0.1:1");
        properties.getRetry().setMaxAttempts(2);
        properties.getRetry().setBackoff(Duration.ofMillis(10));
        PythonMlClient client = createClient(properties);

        assertThrows(MlServiceUnavailableException.class,
                () -> client.health("corr-1"));
    }

    private void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
    }

    private HttpHandler jsonHandler(int status, String body) {
        return exchange -> {
            byte[] bytes = body.getBytes();
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        };
    }

    private PythonMlClient createClient(MlServiceProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) properties.getTimeout().getConnect().toMillis());
        requestFactory.setReadTimeout((int) properties.getTimeout().getRead().toMillis());

        RestClient restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();

        return new PythonMlClient(restClient, properties);
    }

    private MlServiceProperties defaultProperties(int port) {
        MlServiceProperties properties = new MlServiceProperties();
        properties.setBaseUrl("http://127.0.0.1:" + port);
        properties.getTimeout().setConnect(Duration.ofSeconds(1));
        properties.getTimeout().setRead(Duration.ofSeconds(1));
        properties.getRetry().setMaxAttempts(2);
        properties.getRetry().setBackoff(Duration.ofMillis(10));
        return properties;
    }

    private List<Double> sample(int size) {
        return java.util.stream.IntStream.range(0, size)
                .mapToDouble(i -> i + 0.1)
                .boxed()
                .toList();
    }
}
