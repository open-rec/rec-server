package com.openrec.service.embedding;

import static org.junit.Assert.*;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sun.net.httpserver.HttpServer;

public class EmbeddingServiceTest {
    private HttpServer server;
    private ExecutorService executor;
    private final AtomicReference<String> response =
        new AtomicReference<>("{\"model\":\"test\",\"data\":[{\"index\":0,\"embedding\":[3,4]}]}");
    private final AtomicReference<String> input = new AtomicReference<>();
    private final AtomicReference<String> auth = new AtomicReference<>();
    private final AtomicInteger status = new AtomicInteger(200);
    private final AtomicInteger calls = new AtomicInteger();
    private volatile long delay;

    @Before
    public void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        executor = Executors.newCachedThreadPool();
        server.setExecutor(executor);
        server.createContext("/v1/embeddings", exchange -> {
            calls.incrementAndGet();
            input.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            auth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] body = response.get().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status.get(), body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();
    }

    @After
    public void stop() {
        server.stop(0);
        executor.shutdownNow();
    }

    private EmbeddingService service(boolean enabled) {
        return new EmbeddingService(enabled, "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/embeddings",
            "test", "secret", "retrieve items", 2, 1000, 1, 100);
    }

    @Test
    public void sendsStandardRequestAndNormalizesResponse() {
        assertEquals(Arrays.asList(0.6, 0.8), service(true).embedQuery("jazz", 1000));
        assertTrue(input.get().contains("Instruct: retrieve items\\nQuery:jazz"));
        assertTrue(input.get().contains("\"encoding_format\":\"float\""));
        assertEquals("Bearer secret", auth.get());
    }

    @Test
    public void disabledBlankOversizeAndExpiredRequestsDoNotCallProvider() {
        assertTrue(service(false).embedQuery("jazz", 1000).isEmpty());
        EmbeddingService client = service(true);
        assertTrue(client.embedQuery(" ", 1000).isEmpty());
        assertTrue(client.embedQuery("a".repeat(101), 1000).isEmpty());
        assertTrue(client.embedQuery("jazz", 0).isEmpty());
        assertEquals(0, calls.get());
    }

    @Test
    public void rejectsInvalidResponsesWithoutFailingRecall() {
        EmbeddingService client = service(true);
        for (String body : Arrays.asList("not json", "{}", "{\"data\":[]}", "{\"data\":[{\"embedding\":[1]}]}",
            "{\"data\":[{\"embedding\":[0,0]}]}", "{\"data\":[{\"embedding\":[1,\"bad\"]}]}",
            "{\"model\":\"wrong\",\"data\":[{\"embedding\":[3,4]}]}")) {
            response.set(body);
            assertTrue(body, client.embedQuery("jazz", 1000).isEmpty());
        }
        status.set(503);
        assertTrue(client.embedQuery("jazz", 1000).isEmpty());
    }

    @Test
    public void providerTimeoutReleasesConcurrencyPermit() {
        EmbeddingService client = service(true);
        delay = 200;
        assertTrue(client.embedQuery("jazz", 30).isEmpty());
        delay = 0;
        assertEquals(Arrays.asList(0.6, 0.8), client.embedQuery("jazz", 1000));
    }
}
