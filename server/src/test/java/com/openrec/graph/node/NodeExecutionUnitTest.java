package com.openrec.graph.node;

import com.openrec.graph.GraphContext;
import com.openrec.graph.config.*;
import com.openrec.proto.model.ScoreResult;
import com.openrec.service.rank.RankService;
import com.openrec.service.recall.RecallStore;
import com.openrec.service.redis.RedisService;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.env.MockEnvironment;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class NodeExecutionUnitTest {
    private static <T> NodeConfig<T> config(String name, T content, boolean open) {
        NodeConfig<T> config = new NodeConfig<>();
        config.setName(name); config.setContent(content); config.setOpen(open); config.setTimeout(50);
        return config;
    }

    @Test public void userTriggerCombinesExplicitAndStoredItems() {
        RedisService redis = mock(RedisService.class);
        UserTriggerConfig content = new UserTriggerConfig(); content.setSize(5);
        UserTriggerNode node = new UserTriggerNode(config("trigger", content, true));
        ReflectionTestUtils.setField(node, "redisService", redis);
        when(redis.getZSet("event:{u}:s:click", 0, Double.MAX_VALUE, 5))
            .thenReturn(Collections.singletonList(new ScoreResult("stored", 2)));
        GraphContext context = new GraphContext(); context.addParam("scene", "s"); context.addParam("userId", "u");
        context.addParam("itemIds", Arrays.asList("explicit1", "explicit2"));
        node.run(context); context.exportNodeData(node);
        List<ScoreResult> result = (List<ScoreResult>) context.getData("triggerItems");
        assertEquals(3, result.size()); assertEquals("stored", result.get(2).getId());
    }

    @Test public void i2iUsesOneKeyPerTrigger() {
        RecallStore recallStore = mock(RecallStore.class);
        I2iConfig content = new I2iConfig(); content.setSize(4);
        content.setTableName("item-cf-i2i"); content.setRecallType("item_cf_i2i");
        I2iNode node = new I2iNode(config("item_cf_i2i", content, true));
        ReflectionTestUtils.setField(node, "recallStore", recallStore);
        ReflectionTestUtils.setField(node, "triggerItems", Arrays.asList(new ScoreResult("a", 1), new ScoreResult("b", 2)));
        when(recallStore.i2i("item-cf-i2i", "s", Arrays.asList("a", "b"), 4))
            .thenReturn(Collections.singletonList(new ScoreResult("result", 3)));
        GraphContext context = new GraphContext(); context.addParam("scene", "s");
        node.run(context); context.exportNodeData(node);
        assertEquals("result", ((List<ScoreResult>) context.getData("i2iItems")).get(0).getId());
        verify(recallStore).i2i("item-cf-i2i", "s", Arrays.asList("a", "b"), 4);
        assertSame(context.getData("i2iItems"), context.getData("recall:item_cf_i2i"));
    }

    @Test public void u2iLooksUpRequestUserAndExportsConfiguredChannel() {
        RecallStore recallStore = mock(RecallStore.class);
        U2iConfig content = new U2iConfig(); content.setSize(4);
        content.setTableName("user-cf-u2i"); content.setRecallType("user_cf_u2i");
        U2iNode node = new U2iNode(config("userCf", content, true));
        ReflectionTestUtils.setField(node, "recallStore", recallStore);
        List<ScoreResult> recalled = Collections.singletonList(new ScoreResult("result", 3));
        when(recallStore.u2i("user-cf-u2i", "s", "u", 4)).thenReturn(recalled);
        GraphContext context = new GraphContext();
        context.addParam("scene", "s"); context.addParam("userId", "u");
        node.run(context); context.exportNodeData(node);
        assertSame(recalled, context.getData("u2iItems"));
        assertSame(recalled, context.getData("recall:user_cf_u2i"));
    }

    @Test public void rankFusesScoresAndHandlesFailureAndClosedMode() {
        RankConfig content = new RankConfig(); content.setSize(2);
        RankService rank = mock(RankService.class);
        when(rank.isOpen()).thenReturn(true);
        RankNode node = new RankNode(config("rank", content, true));
        ReflectionTestUtils.setField(node, "rankService", rank);
        List<ScoreResult> recalled = Arrays.asList(new ScoreResult("a", 2), new ScoreResult("b", 3), new ScoreResult("c", 4));
        ReflectionTestUtils.setField(node, "combineItems", recalled);
        ReflectionTestUtils.setField(node, "userFeatureMap", Collections.singletonMap("userId", "u"));
        when(rank.score("u", Arrays.asList("a", "b"))).thenReturn(Collections.singletonMap("a", 5d));
        GraphContext context = new GraphContext(); node.run(context); context.exportNodeData(node);
        List<ScoreResult> ranked = (List<ScoreResult>) context.getData("rankItems");
        assertEquals(7d, ranked.get(0).getScore(), 0); assertEquals(3d, ranked.get(1).getScore(), 0);
        assertEquals(Double.valueOf(2), ranked.get(0).getRecallScore()); assertEquals(Double.valueOf(5), ranked.get(0).getRankScore());

        RankNode failing = new RankNode(config("rank", content, true));
        ReflectionTestUtils.setField(failing, "rankService", rank);
        ReflectionTestUtils.setField(failing, "combineItems", Collections.singletonList(new ScoreResult("x", 1)));
        ReflectionTestUtils.setField(failing, "userFeatureMap", Collections.singletonMap("userId", "u"));
        when(rank.score(eq("u"), eq(Collections.singletonList("x")))).thenThrow(new RuntimeException("down"));
        failing.run(context);

        RankNode serviceClosed = new RankNode(config("rank", content, true));
        RankService closedRank = mock(RankService.class);
        when(closedRank.isOpen()).thenReturn(false);
        ReflectionTestUtils.setField(serviceClosed, "rankService", closedRank);
        ReflectionTestUtils.setField(serviceClosed, "combineItems", recalled);
        serviceClosed.run(context);
        context.exportNodeData(serviceClosed);
        assertSame(recalled, context.getData("rankItems"));
        verify(closedRank, never()).score(anyString(), anyList());

        RankNode closed = new RankNode(config("rank", content, false));
        ReflectionTestUtils.setField(closed, "combineItems", recalled); closed.run(context);
        context.exportNodeData(closed); assertSame(recalled, context.getData("rankItems"));
    }

    @Test public void rankAppliesConfiguredMultiChannelScoreStrategy() {
        RankScoreStrategyConfig strategy = new RankScoreStrategyConfig();
        strategy.setRecallAggregation("sum");
        strategy.setRecallWeight(0.2);
        strategy.setRankWeight(0.9);
        strategy.setChannels(Arrays.asList(
            new RankChannelWeightConfig("item_cf_i2i", 2),
            new RankChannelWeightConfig("item_seq_emb", 1),
            new RankChannelWeightConfig("hot", 2),
            new RankChannelWeightConfig("new", 1)));
        RankConfig content = new RankConfig(); content.setSize(1); content.setScoreStrategy(strategy);
        RankService rank = mock(RankService.class); when(rank.isOpen()).thenReturn(true);
        when(rank.score("u", Collections.singletonList("a"))).thenReturn(Collections.singletonMap("a", 0.5));
        ScoreResult item = new ScoreResult("a", 0.4);
        item.addRecallScore("item_cf_i2i", 0.4); item.addRecallScore("hot", 0.3);
        RankNode node = new RankNode(config("rank", content, true));
        ReflectionTestUtils.setField(node, "rankService", rank);
        ReflectionTestUtils.setField(node, "combineItems", Collections.singletonList(item));
        ReflectionTestUtils.setField(node, "userFeatureMap", Collections.singletonMap("userId", "u"));

        GraphContext context = new GraphContext(); node.run(context); context.exportNodeData(node);
        ScoreResult result = ((List<ScoreResult>) context.getData("rankItems")).get(0);
        assertEquals(0.73, result.getScore(), 0.000001);
        assertEquals(Double.valueOf(1.4), result.getRecallFusionScore());
        assertEquals(Double.valueOf(0.5), result.getRankScore());
    }

    @Test public void collectorLimitsResultAndWritesExposeOnlyWhenNonEmpty() {
        RedisService redis = mock(RedisService.class);
        CollectorNode node = new CollectorNode(config("collector", null, true));
        ReflectionTestUtils.setField(node, "redisService", redis);
        ReflectionTestUtils.setField(node, "finalItems", Arrays.asList(new ScoreResult("a", 1), new ScoreResult("b", 2)));
        GraphContext context = new GraphContext(); context.addParam("size", 1); context.addParam("userId", "u"); context.addParam("scene", "s");
        node.run(context); assertEquals(1, ((List<?>) context.getResult()).size());
        verify(redis).addZSets(eq("event:{u}:s:expose"), argThat(map -> map.containsKey("a")));

        CollectorNode empty = new CollectorNode(config("collector", null, true));
        ReflectionTestUtils.setField(empty, "redisService", redis); ReflectionTestUtils.setField(empty, "finalItems", Collections.emptyList());
        empty.run(context); verifyNoMoreInteractions(redis);
    }

    @Test public void collectorDoesNotWriteSyntheticExposureInCluster() {
        RedisService redis = mock(RedisService.class);
        MockEnvironment cluster = new MockEnvironment();
        cluster.setProperty("collector.fake-expose.enabled", "false");
        CollectorNode node = new CollectorNode(config("collector", null, true));
        ReflectionTestUtils.setField(node, "redisService", redis);
        ReflectionTestUtils.setField(node, "environment", cluster);
        ReflectionTestUtils.setField(node, "finalItems", Collections.singletonList(new ScoreResult("a", 1)));
        GraphContext context = new GraphContext(); context.addParam("size", 1);
        node.run(context);
        assertEquals(1, ((List<?>) context.getResult()).size());
        verifyNoInteractions(redis);
    }

    @Test public void simpleFeatureBlackSearchAndOperationNodesWork() {
        GraphContext context = new GraphContext(); context.addParam("userId", "u");
        UserFeatureNode user = new UserFeatureNode(config("userFeature", null, true)); user.run(context); context.exportNodeData(user);
        assertEquals("u", ((Map<?, ?>) context.getData("userFeatureMap")).get("userId"));
        new ItemFeatureNode(config("itemFeature", null, true)).run(context);
        new SearchNode(config("search", null, true)).run(context);

        RedisService redis = mock(RedisService.class); when(redis.getSet("black")).thenReturn(Collections.singleton("x"));
        BlackNode black = new BlackNode(config("black", new FilterConfig(), true)); ReflectionTestUtils.setField(black, "redisService", redis);
        black.run(context); context.exportNodeData(black); assertEquals(Collections.singleton("x"), context.getData("blackItemSet"));

        OperationConfig operationConfig = new OperationConfig(); operationConfig.setOperationName("missing");
        OperationNode operation = new OperationNode(config("operation", operationConfig, true));
        List<ScoreResult> input = Collections.singletonList(new ScoreResult("x", 1));
        ReflectionTestUtils.setField(operation, "rankItems", input); operation.run(context); context.exportNodeData(operation);
        assertSame(input, context.getData("operationItems"));
    }

    @Test public void blackNodeExportsStructuredDislikeRules() {
        RedisService redis = mock(RedisService.class);
        when(redis.getSet("black")).thenReturn(Collections.singleton("global-id"));
        when(redis.getZSet(eq("event:{u}:s:dislike"), anyDouble(), anyDouble(), eq(1000)))
            .thenReturn(Arrays.asList(new ScoreResult("id:item-1", 1),
                new ScoreResult("category:sports", 1), new ScoreResult("tag:nba", 1)));
        FilterConfig.TypeFilterConfig dislike = new FilterConfig.TypeFilterConfig();
        dislike.setDuration(2592000); dislike.setSize(1000);
        FilterConfig content = new FilterConfig();
        content.setFilterMap(Collections.singletonMap("dislike", dislike));
        BlackNode node = new BlackNode(config("black", content, true));
        ReflectionTestUtils.setField(node, "redisService", redis);
        GraphContext context = new GraphContext();
        context.addParam("userId", "u"); context.addParam("scene", "s");
        node.run(context); context.exportNodeData(node);
        assertEquals(new HashSet<>(Arrays.asList("global-id", "item-1")), context.getData("blackItemSet"));
        assertEquals(Collections.singleton("sports"), context.getData("blackCategorySet"));
        assertEquals(Collections.singleton("nba"), context.getData("blackTagSet"));
    }

    @Test public void combineMatchesNegativeCategoryAndTags() {
        com.openrec.proto.model.Item item = new com.openrec.proto.model.Item();
        item.setCategory("sports"); item.setTags("football,nba");
        assertTrue(CombineNode.isNegativeFeedbackMatch(item, Collections.singleton("sports"),
            Collections.emptySet()));
        assertTrue(CombineNode.isNegativeFeedbackMatch(item, Collections.emptySet(),
            Collections.singleton("nba")));
        assertFalse(CombineNode.isNegativeFeedbackMatch(item, Collections.singleton("news"),
            Collections.singleton("music")));
    }
}
