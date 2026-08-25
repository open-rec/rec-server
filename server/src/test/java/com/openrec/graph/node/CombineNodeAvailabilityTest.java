package com.openrec.graph.node;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openrec.graph.GraphContext;
import com.openrec.graph.config.CombineConfig;
import com.openrec.graph.config.NodeConfig;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.openrec.proto.model.Item;
import com.openrec.proto.model.ScoreResult;
import com.openrec.service.redis.RedisService;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CombineNodeAvailabilityTest {

    private Item item(String scene, int status, String expireTime) {
        Item item = new Item();
        item.setId("item-1");
        item.setScene(scene);
        item.setStatus(status);
        item.setExpireTime(expireTime);
        return item;
    }

    @Test
    public void keepsActiveItemWithoutExpiry() {
        Assert.assertTrue(CombineNode.isAvailable(item("scene-1", 1, "0"), "scene-1", 100, true));
    }

    @Test
    public void filtersDisabledExpiredAndCrossSceneItems() {
        Assert.assertFalse(CombineNode.isAvailable(item("scene-1", 0, "0"), "scene-1", 100, true));
        Assert.assertFalse(CombineNode.isAvailable(item("scene-1", 1, "99"), "scene-1", 100, true));
        Assert.assertFalse(CombineNode.isAvailable(item("scene-2", 1, "0"), "scene-1", 100, true));
    }

    @Test
    public void filtersMalformedExpiryConservatively() {
        Assert.assertFalse(CombineNode.isAvailable(item("scene-1", 1, "invalid"), "scene-1", 100, true));
    }

    @Test
    public void canIgnoreLegacyExpiryWhileStillCheckingStatusAndScene() {
        Assert.assertTrue(CombineNode.isAvailable(item("scene-1", 1, "99"), "scene-1", 100, false));
        Assert.assertFalse(CombineNode.isAvailable(item("scene-1", 0, "99"), "scene-1", 100, false));
    }

    @Test
    public void mergesConfiguredDynamicRecallTypesWithoutFixedExportKeys() {
        CombineConfig content = new CombineConfig();
        content.setSize(10); content.setRecallTypes(Arrays.asList("item_cf_i2i", "content_i2i"));
        NodeConfig<CombineConfig> config = new NodeConfig<>();
        config.setName("combine"); config.setOpen(true); config.setContent(content);
        CombineNode node = new CombineNode(config);
        RedisService redis = mock(RedisService.class);
        Item available = item("scene-1", 1, "0");
        when(redis.getVs(anyList())).thenReturn(Collections.singletonList(available));
        ReflectionTestUtils.setField(node, "redisService", redis);
        ReflectionTestUtils.setField(node, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(node, "triggerItems", Collections.emptyList());
        ReflectionTestUtils.setField(node, "filterItemSet", Collections.emptySet());
        ReflectionTestUtils.setField(node, "blackItemSet", Collections.emptySet());
        ReflectionTestUtils.setField(node, "blackCategorySet", Collections.emptySet());
        ReflectionTestUtils.setField(node, "blackTagSet", Collections.emptySet());
        GraphContext context = new GraphContext(); context.addParam("scene", "scene-1");
        context.addData("recall:item_cf_i2i",
            Collections.singletonList(new ScoreResult("item-1", 0.8)));
        context.addData("recall:content_i2i",
            Collections.singletonList(new ScoreResult("item-1", 0.6)));

        node.run(context); context.exportNodeData(node);

        List<ScoreResult> result = (List<ScoreResult>)context.getData("combineItems");
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(Double.valueOf(0.8), result.get(0).getRecallScores().get("item_cf_i2i"));
        Assert.assertEquals(Double.valueOf(0.6), result.get(0).getRecallScores().get("content_i2i"));
    }
}
