package com.openrec.service.redis;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class RedisServiceUnitTest {

    @Mock
    private RedisTemplate redisTemplate;
    @Mock
    private RedisTemplate redisJsonTemplate;
    @Mock
    private ValueOperations plainValues;
    @Mock
    private ValueOperations jsonValues;
    @Mock
    private ZSetOperations zSetOperations;
    @Mock
    private SetOperations setOperations;

    private RedisService service;

    @Before
    public void setUp() {
        service = new RedisService();
        ReflectionTestUtils.setField(service, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(service, "redisJsonTemplate", redisJsonTemplate);
        when(redisJsonTemplate.opsForValue()).thenReturn(jsonValues);
        when(redisTemplate.opsForValue()).thenReturn(plainValues);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
    }

    @Test
    public void entityWritesAndReadsUseJsonSerializer() {
        Map<String, Object> entities = Collections.singletonMap("item:{item-1}", Collections.singletonMap("id", "item-1"));
        service.addKvs(entities);
        verify(jsonValues).multiSet(entities);
        verify(plainValues, never()).multiSet(entities);

        List<String> keys = Collections.singletonList("item:{item-1}");
        List<Map<String, String>> stored = Collections.singletonList(Collections.singletonMap("id", "item-1"));
        when(jsonValues.multiGet(keys)).thenReturn(stored);
        Assert.assertEquals(stored, service.getVs(keys));
        verify(jsonValues).multiGet(keys);
    }

    @Test
    public void removesAllRequestedSortedSetMembers() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        List<String> members = java.util.Arrays.asList("item-1", "item-2");

        service.removeZSetValues("new:{scene-1}", members);

        verify(zSetOperations).remove("new:{scene-1}", "item-1", "item-2");
    }

    @Test
    public void ignoresEmptySortedSetRemoval() {
        service.removeZSetValues("new:{scene-1}", Collections.emptyList());

        verify(redisTemplate, never()).opsForZSet();
    }

    @Test
    public void plainSetValueAndDeleteOperationsDelegate() {
        service.addSet("set", "a");
        service.addSets("set", new java.util.LinkedHashSet<>(java.util.Arrays.asList("a", "b")));
        when(setOperations.members("set")).thenReturn(Collections.singleton("a"));
        Assert.assertEquals(Collections.singleton("a"), service.getSet("set"));
        service.addKv("key", "value"); when(plainValues.get("key")).thenReturn("value");
        Assert.assertEquals("value", service.getV("key"));
        LinkedHashMap json = new LinkedHashMap(); json.put("id", "i"); when(jsonValues.get("json")).thenReturn(json);
        Assert.assertSame(json, service.getJsonV("json"));
        service.removeK("key"); service.removeKs(java.util.Arrays.asList("a", "b"));
        verify(redisTemplate).delete("key"); verify(redisTemplate).delete(java.util.Arrays.asList("a", "b"));
    }

    @Test
    public void sortedSetsAreWrittenReadMergedAndUnquoted() {
        service.addZSet("z", "a", 1d);
        service.addZSets("z", Collections.singletonMap("b", 2d));
        ZSetOperations.TypedTuple<String> quoted = new org.springframework.data.redis.core.DefaultTypedTuple<>("\"a\"", 3d);
        when(zSetOperations.reverseRangeByScoreWithScores("z", 0, 10, 0, 5))
            .thenReturn(Collections.singleton(quoted));
        Assert.assertEquals("a", service.getZSet("z", 0, 10, 5).get(0).getId());

        when(zSetOperations.reverseRangeByScoreWithScores(anyString(), eq(0d), eq(10d), eq(0L), eq(5L)))
            .thenReturn(Collections.singleton(new org.springframework.data.redis.core.DefaultTypedTuple<>(null, 1d)));
        Assert.assertNull(service.getZSet(java.util.Arrays.asList("a", "b"), 0, 10, 5).get(0).getId());
        verify(zSetOperations).unionAndStore(anyString(), eq(java.util.Arrays.asList("a", "b")), anyString());
        verify(redisTemplate).delete(startsWith("tmp_"));
    }
}
