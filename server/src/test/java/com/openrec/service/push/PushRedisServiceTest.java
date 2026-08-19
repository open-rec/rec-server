package com.openrec.service.push;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openrec.proto.biz.push.EventReq;
import com.openrec.proto.biz.push.ItemReq;
import com.openrec.proto.biz.push.PushCmd;
import com.openrec.proto.model.Event;
import com.openrec.proto.model.Item;
import com.openrec.service.redis.RedisService;

@RunWith(MockitoJUnitRunner.class)
public class PushRedisServiceTest {

    @Mock
    private RedisService redisService;

    private PushRedisService service;
    private ObjectMapper objectMapper;

    @Before
    public void setUp() {
        service = new PushRedisService();
        objectMapper = new ObjectMapper();
        ReflectionTestUtils.setField(service, "redisService", redisService);
        ReflectionTestUtils.setField(service, "objectMapper", objectMapper);
    }

    private Item item(String id, String scene, String pubTime) {
        Item item = new Item();
        item.setId(id);
        item.setScene(scene);
        item.setPubTime(pubTime);
        item.setStatus(1);
        return item;
    }

    @Test
    public void insertItemWritesEntityAndAddsItToNewIndex() {
        Item newItem = item("item-1", "scene-1", "20");
        when(redisService.getVs(Collections.singletonList("item:{item-1}")))
            .thenReturn(Collections.singletonList(null));

        ItemReq request = new ItemReq();
        request.setCmd(PushCmd.INSERT);
        request.setData(Collections.singletonList(newItem));
        service.pushItem(request);

        verify(redisService).addKvs(Collections.singletonMap("item:{item-1}", newItem));
        verify(redisService).addZSets("new:{scene-1}", Collections.singletonMap("item-1", 20d));
        verify(redisService, never()).removeZSetValues(eq("new:{scene-1}"), eq(Collections.singletonList("item-1")));
    }

    @Test
    public void updateItemMovesNewIndexAndWritesJsonEntity() {
        Item oldItem = item("item-1", "old-scene", "10");
        Item newItem = item("item-1", "new-scene", "20");
        Map oldValue = objectMapper.convertValue(oldItem, Map.class);
        when(redisService.getVs(Collections.singletonList("item:{item-1}")))
            .thenReturn(Collections.singletonList(oldValue));

        ItemReq request = new ItemReq();
        request.setCmd(PushCmd.UPDATE);
        request.setData(Collections.singletonList(newItem));
        service.pushItem(request);

        verify(redisService).removeZSetValues("new:{old-scene}", Collections.singletonList("item-1"));
        verify(redisService).addKvs(Collections.singletonMap("item:{item-1}", newItem));
        verify(redisService).addZSets("new:{new-scene}", Collections.singletonMap("item-1", 20d));
    }

    @Test
    public void deleteItemCleansStoredAndRequestedScenes() {
        Item oldItem = item("item-1", "old-scene", "10");
        Item deleteItem = item("item-1", "request-scene", "10");
        when(redisService.getVs(Collections.singletonList("item:{item-1}")))
            .thenReturn(Collections.singletonList(objectMapper.convertValue(oldItem, Map.class)));

        ItemReq request = new ItemReq();
        request.setCmd(PushCmd.DELETE);
        request.setData(Collections.singletonList(deleteItem));
        service.pushItem(request);

        verify(redisService).removeZSetValues("new:{old-scene}", Collections.singletonList("item-1"));
        verify(redisService).removeZSetValues("new:{request-scene}", Collections.singletonList("item-1"));
        verify(redisService).removeKs(Collections.singletonList("item:{item-1}"));
    }

    @Test
    public void deleteEventRemovesItemsFromTheCorrectEventKey() {
        Event first = new Event();
        first.setUserId("user-1");
        first.setScene("scene-1");
        first.setType("click");
        first.setItemId("item-1");
        Event second = new Event();
        second.setUserId("user-1");
        second.setScene("scene-1");
        second.setType("click");
        second.setItemId("item-2");

        EventReq request = new EventReq();
        request.setCmd(PushCmd.DELETE);
        request.setData(Arrays.asList(first, second));
        service.pushEvent(request);

        verify(redisService).removeZSetValues(eq("event:{user-1}:scene-1:click"),
            eq(Arrays.asList("item-1", "item-2")));
    }
}
