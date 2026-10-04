package com.openrec.config;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

import org.junit.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializer;

import com.openrec.proto.JsonRes;
import com.openrec.proto.model.Item;
import com.openrec.service.rank.RankService;

import tools.jackson.databind.json.JsonMapper;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class SerializationCompatibilityTest {
    private final com.fasterxml.jackson.databind.ObjectMapper legacy =
        new com.fasterxml.jackson.databind.ObjectMapper();

    @Test
    public void redisReadsLegacyObjectsAndWritesValuesReadableByLegacyClients() throws Exception {
        RedisSerializer<Object> serializer = (RedisSerializer<Object>)new RedisConfig()
            .redisJsonTemplate(mock(RedisConnectionFactory.class)).getValueSerializer();
        String oldValue = "{\"id\":\"item_1\",\"attrs\":{\"score\":1.5,\"enabled\":true}}";
        Map<?, ?> decoded = (Map<?, ?>)serializer.deserialize(oldValue.getBytes(StandardCharsets.UTF_8));
        assertEquals("item_1", decoded.get("id"));
        assertEquals(legacy.readValue(oldValue, Map.class), legacy.readValue(serializer.serialize(decoded), Map.class));
        assertEquals("\"item_1\"", new String(serializer.serialize("item_1"), StandardCharsets.UTF_8));
        assertNull(serializer.deserialize(new byte[0]));
    }

    @Test
    public void responseEnvelopeRetainsLegacyFieldNamesAndTypes() throws Exception {
        Item item = new Item();
        item.setId("item_1");
        JsonRes<Item> response = new JsonRes<>(item);
        assertEquals(legacy.readTree(legacy.writeValueAsBytes(response)),
            legacy.readTree(JsonMapper.builder().build().writeValueAsBytes(response)));
    }

    @Test
    public void rankRequestRetainsSnakeCaseTransportFields() throws Exception {
        RankService.RankUserItems request = new RankService.RankUserItems("user_1", Arrays.asList("item_1"));
        byte[] json = JsonMapper.builder().build().writeValueAsBytes(request);
        assertEquals(legacy.readTree(legacy.writeValueAsBytes(request)), legacy.readTree(json));
        assertTrue(legacy.readTree(json).has("user_id"));
        assertTrue(legacy.readTree(json).has("candidate_ids"));
        assertFalse(legacy.readTree(json).has("userId"));
    }
}
