package com.openrec.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openrec.controller.sys.OperateController;
import com.openrec.controller.api.PushController;
import com.openrec.controller.api.QueryController;
import com.openrec.controller.api.RecommendController;
import com.openrec.proto.JsonReq;
import com.openrec.proto.biz.push.EventReq;
import com.openrec.proto.biz.push.ItemReq;
import com.openrec.proto.biz.push.UserReq;
import com.openrec.proto.biz.recommend.RecommendReq;
import com.openrec.proto.biz.recommend.RecommendRes;
import com.openrec.proto.model.Item;
import com.openrec.proto.model.ScoreResult;
import com.openrec.proto.model.User;
import com.openrec.service.operate.OperateService;
import com.openrec.service.metrics.ApiMetricsService;
import com.openrec.service.push.PushService;
import com.openrec.service.query.QueryService;
import com.openrec.service.rank.RankService;
import com.openrec.service.rec.RecService;
import com.openrec.service.redis.RedisService;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.client.RestTemplate;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.openrec.ab.AbExperimentService;

public class ControllerAndServiceUnitTest {
    @Test
    public void defaultControllerReturnsReadyResponses() {
        DefaultController controller = new DefaultController();
        assertEquals("hello, rec server start.", controller.home().block());
        assertEquals("health check", controller.health().block().getData());
    }

    @Test
    public void pushAndOperateControllersDelegateBodies() {
        PushService push = mock(PushService.class);
        PushController controller = new PushController();
        ReflectionTestUtils.setField(controller, "pushService", push);
        ReflectionTestUtils.setField(controller, "apiMetricsService", new ApiMetricsService(new SimpleMeterRegistry()));
        UserReq users = new UserReq(); ItemReq items = new ItemReq(); EventReq events = new EventReq();
        assertTrue(controller.pushUser(new JsonReq<>(users)).block().isStatus());
        assertTrue(controller.pushItem(new JsonReq<>(items)).block().isStatus());
        assertTrue(controller.pushEvent(new JsonReq<>(events)).block().isStatus());
        verify(push).pushUser(users); verify(push).pushItem(items); verify(push).pushEvent(events);

        OperateService operate = mock(OperateService.class);
        OperateController operateController = new OperateController();
        ReflectionTestUtils.setField(operateController, "operateService", operate);
        java.util.Set<String> blacklist = Collections.singleton("i");
        assertTrue(operateController.set(new JsonReq<>(blacklist)).block().isStatus());
        verify(operate).setBlacklist(blacklist);
    }

    @Test
    public void queryAndRecommendControllersWrapServiceResults() {
        QueryService query = mock(QueryService.class);
        QueryController controller = new QueryController();
        ReflectionTestUtils.setField(controller, "queryService", query);
        User user = new User(); Item item = new Item();
        when(query.queryUser("u")).thenReturn(user);
        when(query.queryItem("i")).thenReturn(item);
        when(query.queryEvent("u", "s", "t")).thenReturn(Collections.singletonList(new ScoreResult("i", 1)));
        assertSame(user, controller.getUser("u").block().getData());
        assertSame(item, controller.getItem("i").block().getData());
        assertEquals(1, controller.getEvents("u", "s", "t").block().getData().size());

        AbExperimentService experiments = mock(AbExperimentService.class);
        RecommendController recommendController = new RecommendController();
        ReflectionTestUtils.setField(recommendController, "abExperimentService", experiments);
        ReflectionTestUtils.setField(recommendController, "apiMetricsService", new ApiMetricsService(new SimpleMeterRegistry()));
        RecommendReq req = new RecommendReq(); RecommendRes<Item> res = new RecommendRes<>();
        when(experiments.resolve(req)).thenReturn("default");
        doReturn(res).when(experiments).execute(req);
        assertSame(res, recommendController.recommend(new JsonReq<>(req)).block().getData());
        assertEquals(RecommendReq.TARGET_ITEM, req.getTargetType());
        assertSame(res, recommendController.recommendItem(new JsonReq<>(req)).block().getData());

        RecommendReq userReq = new RecommendReq();
        com.openrec.proto.JsonRes<RecommendRes<User>> unsupported =
            recommendController.recommendUser(new JsonReq<>(userReq)).block();
        assertEquals(com.openrec.proto.ProtoCode.NOT_IMPLEMENTED, unsupported.getCode());
        assertFalse(unsupported.isStatus());
        assertNull(unsupported.getData());
        assertEquals(RecommendReq.TARGET_USER, userReq.getTargetType());
        assertEquals(RecommendReq.TARGET_USER, userReq.getParams().get("targetType"));
    }

    @Test
    public void queryAndOperateServicesUseExpectedRedisKeys() {
        RedisService redis = mock(RedisService.class);
        QueryService query = new QueryService();
        ReflectionTestUtils.setField(query, "redisService", redis);
        ReflectionTestUtils.setField(query, "objectMapper", new ObjectMapper());
        LinkedHashMap<String, Object> userData = new LinkedHashMap<>(); userData.put("id", "u");
        LinkedHashMap<String, Object> itemData = new LinkedHashMap<>(); itemData.put("id", "i");
        when(redis.getJsonV("user:{u}")).thenReturn(userData);
        when(redis.getJsonV("item:{i}")).thenReturn(itemData);
        when(redis.getZSet(anyString(), eq(0d), eq(Double.MAX_VALUE), eq(Integer.MAX_VALUE)))
            .thenReturn(Collections.singletonList(new ScoreResult("i", 1)));
        assertEquals("u", query.queryUser("u").getId());
        assertEquals("i", query.queryItem("i").getId());
        assertEquals(1, query.queryEvent("u", "scene", "click").size());
        verify(redis).getZSet("event:{u}:scene:click", 0, Double.MAX_VALUE, Integer.MAX_VALUE);

        OperateService operate = new OperateService();
        ReflectionTestUtils.setField(operate, "redisService", redis);
        operate.setBlacklist(Collections.singleton("blocked"));
        verify(redis).addSets("black", Collections.singleton("blocked"));
    }

    @Test
    public void rankServiceReturnsScoresOnlyForSuccessfulResponse() {
        RestTemplate rest = mock(RestTemplate.class);
        RankService service = new RankService();
        ReflectionTestUtils.setField(service, "restTemplate", rest);
        ReflectionTestUtils.setField(service, "rankHost", "localhost");
        ReflectionTestUtils.setField(service, "rankPort", "8080");
        ReflectionTestUtils.setField(service, "environment", new MockEnvironment());
        RankService.RankItemScores success = new RankService.RankItemScores();
        success.setCode(0); success.setData(Collections.singletonMap("i", 2d));
        when(rest.postForObject(anyString(), any(), eq(RankService.RankItemScores.class))).thenReturn(success);
        assertEquals(2d, service.score("u", Arrays.asList("i")).get("i"), 0d);
        verify(rest).postForObject(eq("http://localhost:8080/model/score"), any(), eq(RankService.RankItemScores.class));

        RankService.RankItemScores failure = new RankService.RankItemScores(); failure.setCode(1);
        when(rest.postForObject(anyString(), any(), eq(RankService.RankItemScores.class))).thenReturn(failure);
        assertTrue(service.score("u", Collections.singletonList("i")).isEmpty());

        RankService.RankUserItems dto = new RankService.RankUserItems("u", Collections.singletonList("i"));
        assertEquals("u", dto.getUserId()); assertEquals("i", dto.getItemIds().get(0));
        failure.setStatus("error"); failure.setMessage("bad"); failure.setData(Collections.emptyMap());
        assertEquals("error", failure.getStatus()); assertEquals("bad", failure.getMessage());
    }

    @Test
    public void rankServicePrefersExactContainerEnvironmentAddress() {
        RestTemplate rest = mock(RestTemplate.class);
        RankService service = new RankService();
        ReflectionTestUtils.setField(service, "restTemplate", rest);
        ReflectionTestUtils.setField(service, "rankHost", "127.0.0.1");
        ReflectionTestUtils.setField(service, "rankPort", "8123");
        MockEnvironment environment = new MockEnvironment()
            .withProperty("RANK_HOST", "rank-engine").withProperty("RANK_PORT", "9123");
        ReflectionTestUtils.setField(service, "environment", environment);
        RankService.RankItemScores success = new RankService.RankItemScores();
        success.setCode(0); success.setData(Collections.singletonMap("i", 1d));
        when(rest.postForObject(anyString(), any(), eq(RankService.RankItemScores.class)))
            .thenReturn(success);

        service.score("u", Collections.singletonList("i"));

        verify(rest).postForObject(eq("http://rank-engine:9123/model/score"), any(),
            eq(RankService.RankItemScores.class));
    }

    @Test
    public void rankServiceIsAlwaysClosedInStandaloneProfile() {
        RankService service = new RankService();
        ReflectionTestUtils.setField(service, "open", true);
        MockEnvironment cluster = new MockEnvironment();
        cluster.setActiveProfiles("cluster");
        ReflectionTestUtils.setField(service, "environment", cluster);
        assertTrue(service.isOpen());

        MockEnvironment standalone = new MockEnvironment();
        standalone.setActiveProfiles("standalone");
        ReflectionTestUtils.setField(service, "environment", standalone);
        assertFalse(service.isOpen());
    }
}
