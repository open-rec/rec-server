package com.openrec.config;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringRunner;

import com.openrec.RecServer;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import tools.jackson.databind.ObjectMapper;

import static org.junit.Assert.*;

/** Starts the real WebFlux server without requiring external data services. */
@RunWith(SpringRunner.class)
@ActiveProfiles("standalone")
@SpringBootTest(classes = RecServer.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BootRuntimeCompatibilityTest {
    @MockitoBean
    private ElasticsearchClient esClient;

    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory;

    @Autowired
    private Environment environment;

    @Autowired
    private ObjectMapper mapper;

    @Test
    public void webFluxHealthRetainsThePublicEnvelope() throws Exception {
        HttpResponse<String> response = get("/health");
        assertEquals(200, response.statusCode());
        assertEquals(200, mapper.readTree(response.body()).get("code").intValue());
        assertTrue(mapper.readTree(response.body()).get("status").booleanValue());
        assertEquals("health check", mapper.readTree(response.body()).get("data").asText());
    }

    @Test
    public void openApiAndPrometheusAreAvailable() throws Exception {
        HttpResponse<String> docs = get("/v3/api-docs");
        assertEquals(200, docs.statusCode());
        assertTrue(mapper.readTree(docs.body()).get("paths").has("/api/recommend/item"));
        HttpResponse<String> metrics = get("/actuator/prometheus");
        assertEquals(200, metrics.statusCode());
        assertTrue(metrics.body().contains("jvm_memory_used_bytes"));
    }

    private HttpResponse<String> get(String path) throws Exception {
        String url = "http://127.0.0.1:" + environment.getProperty("local.server.port") + path;
        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(HttpRequest.newBuilder(URI.create(url)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        }
    }
}
