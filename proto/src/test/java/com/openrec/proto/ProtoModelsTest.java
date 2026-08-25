package com.openrec.proto;

import com.openrec.proto.biz.push.EventReq;
import com.openrec.proto.biz.push.ItemReq;
import com.openrec.proto.biz.push.PushCmd;
import com.openrec.proto.biz.push.UserReq;
import com.openrec.proto.biz.recommend.RecommendReq;
import com.openrec.proto.biz.recommend.RecommendRes;
import com.openrec.proto.model.Event;
import com.openrec.proto.model.Item;
import com.openrec.proto.model.ScoreResult;
import com.openrec.proto.model.User;
import com.openrec.proto.model.VectorResult;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

public class ProtoModelsTest {

    @Test
    public void jsonRequestAndResponseConstructorsPreserveValues() {
        JsonReq<String> empty = new JsonReq<>();
        assertNotNull(empty.getRequestId());
        assertNull(empty.getBody());

        JsonReq<String> request = new JsonReq<>("body");
        request.setRequestId("request-id");
        assertEquals("request-id", request.getRequestId());
        assertEquals("body", request.getBody());
        assertEquals(request, new JsonReq<String>("body") {{ setRequestId("request-id"); }});
        assertEquals(request.hashCode(), request.hashCode());
        assertTrue(request.toString().contains("body"));

        assertResponse(new JsonRes<String>(), ProtoCode.SUCCESS, true, "", null);
        assertResponse(new JsonRes<>("data"), ProtoCode.SUCCESS, true, "", "data");
        assertResponse(new JsonRes<>("message", "data"), ProtoCode.SUCCESS, true, "message", "data");
        assertResponse(new JsonRes<>(false, "bad", "data"), ProtoCode.SUCCESS, false, "bad", "data");
        JsonRes<String> response = new JsonRes<>(ProtoCode.BAD_REQUEST, false, "bad", "data");
        assertResponse(response, 400, false, "bad", "data");
        response.setCode(500);
        response.setStatus(true);
        response.setMsg("changed");
        response.setData("new-data");
        assertResponse(response, 500, true, "changed", "new-data");
        assertTrue(response.toString().contains("changed"));
    }

    @Test
    public void parameterizedResponseTypeExposesItsTypes() {
        JsonResType type = new JsonResType(String.class);
        assertEquals(JsonRes.class, type.getRawType());
        assertArrayEquals(new Type[]{String.class}, type.getActualTypeArguments());
        assertNull(type.getOwnerType());
    }

    @Test
    public void recommendationAndPushDtosExposeAllProperties() throws Exception {
        RecommendReq request = new RecommendReq();
        exerciseBean(request);
        assertNotNull(request.toString());

        EventReq eventReq = new EventReq();
        eventReq.setCmd(PushCmd.UPDATE);
        eventReq.setData(Collections.singletonList(new Event()));
        assertEquals(PushCmd.UPDATE, eventReq.getCmd());
        assertEquals(1, eventReq.getData().size());

        ItemReq itemReq = new ItemReq();
        itemReq.setCmd(PushCmd.DELETE);
        itemReq.setData(Collections.singletonList(new Item()));
        assertEquals(PushCmd.DELETE, itemReq.getCmd());

        UserReq userReq = new UserReq();
        assertEquals(PushCmd.INSERT, userReq.getCmd());
        userReq.setData(Collections.singletonList(new User()));
        assertEquals(1, userReq.getData().size());
        assertEquals(3, PushCmd.values().length);
        assertEquals(PushCmd.INSERT, PushCmd.valueOf("INSERT"));
    }

    @Test
    public void resultAndDomainModelsCoverConstructorsAndAccessors() throws Exception {
        RecommendRes<String> empty = new RecommendRes<>();
        assertTrue(empty.getResults().isEmpty());
        RecommendRes<String> result = new RecommendRes<>(Collections.singletonList(new ScoreResult("a", 1d)));
        assertNull(result.getDetailInfos());
        result = new RecommendRes<>(result.getResults(), Collections.singletonList("detail"));
        assertEquals("detail", result.getDetailInfos().get(0));
        result.setResults(Collections.singletonList(new ScoreResult("b", 2d)));
        result.setDetailInfos(Arrays.asList("x", "y"));
        assertEquals(2, result.getDetailInfos().size());

        ScoreResult score = new ScoreResult();
        score.setId("id");
        score.setScore(3d);
        score.setRecallScore(2d);
        score.setRankScore(1d);
        score.addRecallScore("hot", 2d);
        score.addRecallScore("item_cf_i2i", 1d);
        assertEquals("hot", score.getRecallFrom());
        assertEquals(2, score.getRecallScores().size());
        ScoreResult sourced = new ScoreResult("id", 4d, "new");
        assertEquals("new", sourced.getRecallFrom());

        for (Object bean : Arrays.asList(new Event(), new Item(), new User())) {
            exerciseBean(bean);
            assertNotNull(bean.toString());
            assertEquals(bean, bean);
            assertNotEquals(bean, new Object());
        }
        VectorResult vector = new VectorResult("v", Arrays.asList(1d, 2d));
        assertEquals("v", vector.getId());
        assertEquals(2, vector.getVector().size());
        VectorResult blank = new VectorResult();
        blank.setId("blank");
        blank.setVector(Collections.singletonList(3d));
        assertEquals("blank", blank.getId());
    }

    private static void assertResponse(JsonRes<String> response, int code, boolean status, String msg, String data) {
        assertEquals(code, response.getCode());
        assertEquals(status, response.isStatus());
        assertEquals(msg, response.getMsg());
        assertEquals(data, response.getData());
    }

    private static void exerciseBean(Object bean) throws Exception {
        for (Method setter : bean.getClass().getMethods()) {
            if (!setter.getName().startsWith("set") || setter.getParameterTypes().length != 1) continue;
            Class<?> type = setter.getParameterTypes()[0];
            Object value = type == String.class ? "value" : type == int.class ? 7 :
                type == boolean.class ? true : type == java.util.List.class ? Collections.singletonList("value") :
                type == java.util.Map.class ? Collections.singletonMap("key", "value") : new Object();
            setter.invoke(bean, value);
            Method getter;
            try {
                getter = bean.getClass().getMethod((type == boolean.class ? "is" : "get") + setter.getName().substring(3));
            } catch (NoSuchMethodException ignored) {
                getter = bean.getClass().getMethod("get" + setter.getName().substring(3));
            }
            assertEquals(value, getter.invoke(bean));
        }
    }
}
