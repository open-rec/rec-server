package com.openrec.config;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpServer;
import com.openrec.service.rank.RankService.RankUserItems;
import com.openrec.service.rank.RankService.RankItemScores;
import org.junit.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.Assert.*;

public class RestConfigTest {
    @Test
    public void rankPostUsesHttp11WithoutUpgradeAndPreservesJsonContract() throws Exception {
        AtomicReference<String> upgrade = new AtomicReference<>();
        AtomicReference<String> request = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/model/score", exchange -> {
            upgrade.set(exchange.getRequestHeaders().getFirst("Upgrade"));
            request.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] body =
                "{\"code\":0,\"status\":\"success\",\"data\":{\"item_1\":0.75}}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(upgrade.get() == null ? 200 : 400, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            RankItemScores response = new RestConfig().restTemplate().postForObject(
                "http://127.0.0.1:" + server.getAddress().getPort() + "/model/score",
                new RankUserItems("user_0", Collections.singletonList("item_1")), RankItemScores.class);
            assertNull(upgrade.get());
            assertEquals(0.75, response.getData().get("item_1"), 0.0);
            assertEquals("user_0", new JsonMapper().readTree(request.get()).get("user_id").asText());
        } finally {
            server.stop(0);
        }
    }
}
