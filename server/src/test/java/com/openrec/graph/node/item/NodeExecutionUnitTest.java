package com.openrec.graph.node.item;

import com.openrec.graph.node.*;

import com.openrec.graph.node.item.*;
import com.openrec.graph.node.user.*;

import com.openrec.graph.GraphContext;
import com.openrec.graph.RequestTrigger;
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
        config.setName(name);
        config.setContent(content);
        config.setOpen(open);
        config.setTimeout(50);
        return config;
    }

    @Test
    public void userTriggerCombinesExplicitAndStoredItems() {
        RedisService redis = mock(RedisService.class);
        UserTriggerConfig content = new UserTriggerConfig();
        content.setSize(5);
        TriggerNode node = new TriggerNode(config("trigger", content, true));
        ReflectionTestUtils.setField(node, "redisService", redis);
        when(redis.getZSet("event:{u}:s:click", 0, Double.MAX_VALUE, 5))
            .thenReturn(Collections.singletonList(new ScoreResult("stored", 2)));
        GraphContext context = new GraphContext();
        context.addParam("scene", "s");
        context.addParam("userId", "u");
        context.addParam("itemIds", Arrays.asList("explicit1", "explicit2"));
        node.run(context);
        context.exportNodeData(node);
        List<RequestTrigger.TriggerItem> result = RequestTrigger.resolve(context).triggerItems();
        assertEquals(3, result.size());
        assertEquals("stored", result.get(2).id());
        assertEquals(2d, result.get(2).score(), 0d);
        assertEquals(Collections.singleton("requestTrigger"), context.extractNodeData(node).keySet());
        try {
            result.clear();
            fail("trigger items must be immutable");
        } catch (UnsupportedOperationException expected) {
            assertEquals(3, RequestTrigger.resolve(context).triggerItems().size());
        }
        GraphContext next = new GraphContext();
        next.addParam("query", "query only");
        node.run(next);
        next.exportNodeData(node);
        assertTrue(RequestTrigger.resolve(next).triggerItems().isEmpty());
        assertEquals(3, result.size());
    }

    @Test
    public void queryOnlyTriggerSkipsHistoryAndFeedsTextRecall() {
        RedisService redis = mock(RedisService.class);
        TriggerNode trigger = new TriggerNode(config("user_trigger", new UserTriggerConfig(), true));
        ReflectionTestUtils.setField(trigger, "redisService", redis);
        GraphContext context = new GraphContext();
        context.addParam("scene", "s");
        context.addParam("query", "blue train");
        trigger.run(context);
        context.exportNodeData(trigger);
        assertEquals(Collections.emptyList(), RequestTrigger.resolve(context).triggerItems());
        verifyNoInteractions(redis);
        // Downstream nodes consume the trigger snapshot, not later parameter changes.
        context.addParam("query", "changed");
        RecallStore store = mock(RecallStore.class);
        SearchConfig search = new SearchConfig();
        search.setTableName("item-search");
        search.setRecallType("item_search");
        search.setSize(5);
        search.setMinimumShouldMatch("30%");
        SearchNode node = new SearchNode(config("item_search", search, true));
        ReflectionTestUtils.setField(node, "recallStore", store);
        when(store.textSearch("item-search", "s", "blue train", "text", "30%", "item", 5, 50))
            .thenReturn(Collections.singletonList(new ScoreResult("matched", 1)));
        node.run(context);
        assertEquals("matched", ((List<ScoreResult>)context.getData("recall:item_search")).get(0).getId());
        U2iConfig u2i = new U2iConfig();
        u2i.setTableName("user-cf-u2i");
        u2i.setRecallType("user_cf_u2i");
        U2iNode userRecall = new U2iNode(config("user_cf_u2i", u2i, true));
        ReflectionTestUtils.setField(userRecall, "recallStore", store);
        userRecall.run(context);
        assertEquals(Collections.emptyList(), context.getData("recall:user_cf_u2i"));
        verify(store, never()).u2i(anyString(), anyString(), anyString(), anyInt());
    }

    @Test
    public void userTriggerExportsIdentityAndQueryWithoutHistory() {
        com.openrec.graph.node.user.TriggerNode trigger =
            new com.openrec.graph.node.user.TriggerNode(config("user_trigger", new UserTriggerConfig(), true));
        GraphContext context = new GraphContext();
        context.addParam("userId", "u");
        context.addParam("query", "jazz");
        trigger.run(context);
        context.exportNodeData(trigger);
        com.openrec.graph.RequestTrigger request = com.openrec.graph.RequestTrigger.resolve(context);
        assertEquals("u", request.triggerSelf());
        assertEquals("jazz", request.triggerQuery());
        assertEquals("u", RequestTrigger.resolve(context).triggerSelf());
        GraphContext queryOnly = new GraphContext();
        queryOnly.addParam("query", "jazz");
        trigger.run(queryOnly);
        queryOnly.exportNodeData(trigger);
        assertEquals("", RequestTrigger.resolve(queryOnly).triggerSelf());
        assertEquals("jazz", com.openrec.graph.RequestTrigger.resolve(queryOnly).triggerQuery());
    }

    @Test
    public void userClickHistorySeedsRecallAndSelfRemainsSeparate() {
        UserTriggerConfig content = new UserTriggerConfig();
        content.setSize(3);
        RedisService redis = mock(RedisService.class);
        when(redis.getZSet("user-event:{self}:s:click", 0, Double.MAX_VALUE, 3))
            .thenReturn(Arrays.asList(new ScoreResult("a", 100), new ScoreResult("b", 90)));
        com.openrec.graph.node.user.TriggerNode trigger =
            new com.openrec.graph.node.user.TriggerNode(config("user_trigger", content, true));
        ReflectionTestUtils.setField(trigger, "redisService", redis);
        GraphContext context = new GraphContext();
        context.addParam("scene", "s");
        context.addParam("userId", "self");
        trigger.run(context);
        context.exportNodeData(trigger);
        RequestTrigger seeds = RequestTrigger.resolve(context);
        assertEquals("self", seeds.triggerSelf());
        assertEquals(Arrays.asList("a", "b"), seeds.userSeedIds());
        assertEquals(100d, seeds.triggerUsers().get(0).score(), 0d);
        assertTrue(seeds.triggerItems().isEmpty());
        try {
            seeds.triggerUsers().clear();
            fail("user seeds must be immutable");
        } catch (UnsupportedOperationException expected) {
            assertEquals(2, seeds.triggerUsers().size());
        }
        RecallStore store = mock(RecallStore.class);
        when(store.u2u("user-cf-u2u", "s", "a", 5))
            .thenReturn(Collections.singletonList(new ScoreResult("shared", 0.4)));
        when(store.u2u("user-cf-u2u", "s", "b", 5))
            .thenReturn(Collections.singletonList(new ScoreResult("shared", 0.6)));
        U2uConfig recall = new U2uConfig();
        recall.setSize(5);
        recall.setTableName("user-cf-u2u");
        recall.setRecallType("user_cf_u2u");
        com.openrec.graph.node.user.U2uNode node =
            new com.openrec.graph.node.user.U2uNode(config("user_cf_u2u", recall, true));
        ReflectionTestUtils.setField(node, "recallStore", store);
        node.run(context);
        assertEquals(1d, ((List<ScoreResult>)context.getData("recall:user_cf_u2u")).get(0).getScore(), 0.000001);
        verify(store, never()).u2u(anyString(), anyString(), eq("self"), anyInt());
        when(redis.getZSet("user-event:{self}:s:click", 0, Double.MAX_VALUE, 3)).thenReturn(Collections.emptyList());
        trigger.run(context);
        context.exportNodeData(trigger);
        assertEquals(Collections.singletonList("self"), RequestTrigger.resolve(context).userSeedIds());
        assertTrue(RequestTrigger.resolve(context).triggerUsers().isEmpty());
        assertEquals(2, seeds.triggerUsers().size());
        GraphContext queryOnly = new GraphContext();
        queryOnly.addParam("scene", "s");
        queryOnly.addParam("query", "jazz");
        clearInvocations(redis);
        trigger.run(queryOnly);
        queryOnly.exportNodeData(trigger);
        verifyNoInteractions(redis);
        assertEquals("jazz", RequestTrigger.resolve(queryOnly).triggerQuery());
        assertTrue(RequestTrigger.resolve(queryOnly).userSeedIds().isEmpty());
    }

    @Test
    public void userToItemRecallConsumesTriggerIdentity() {
        GraphContext context = new GraphContext();
        context.addParam("scene", "s");
        context.addParam("userId", "unrelated");
        context.addData(com.openrec.graph.RequestTrigger.KEY,
            new com.openrec.graph.RequestTrigger(Collections.emptyList(), Collections.emptyList(), "seed", ""));
        U2iConfig content = new U2iConfig();
        content.setTableName("user-cf-u2i");
        content.setRecallType("user_cf_u2i");
        content.setSize(5);
        RecallStore store = mock(RecallStore.class);
        U2iNode node = new U2iNode(config("user_cf_u2i", content, true));
        ReflectionTestUtils.setField(node, "recallStore", store);
        when(store.u2i("user-cf-u2i", "s", "seed", 5)).thenReturn(Collections.emptyList());
        node.run(context);
        verify(store).u2i("user-cf-u2i", "s", "seed", 5);
    }

    @Test
    public void i2iUsesOneKeyPerTrigger() {
        RecallStore recallStore = mock(RecallStore.class);
        I2iConfig content = new I2iConfig();
        content.setSize(4);
        content.setTableName("item-cf-i2i");
        content.setRecallType("item_cf_i2i");
        I2iNode node = new I2iNode(config("item_cf_i2i", content, true));
        ReflectionTestUtils.setField(node, "recallStore", recallStore);
        ReflectionTestUtils.setField(node, "requestTrigger",
            new RequestTrigger(
                Arrays.asList(new RequestTrigger.TriggerItem("a", 1), new RequestTrigger.TriggerItem("b", 2)),
                Collections.emptyList(), "", ""));
        when(recallStore.i2i("item-cf-i2i", "s", Arrays.asList("a", "b"), 4))
            .thenReturn(Collections.singletonList(new ScoreResult("result", 3)));
        GraphContext context = new GraphContext();
        context.addParam("scene", "s");
        node.run(context);
        context.exportNodeData(node);
        assertEquals("result", ((List<ScoreResult>)context.getData("i2iItems")).get(0).getId());
        verify(recallStore).i2i("item-cf-i2i", "s", Arrays.asList("a", "b"), 4);
        assertSame(context.getData("i2iItems"), context.getData("recall:item_cf_i2i"));
    }

    @Test
    public void queryEmbeddingUsesRequestVectorAndExportsItsOwnChannel() {
        RecallStore recallStore = mock(RecallStore.class);
        EmbeddingConfig content = new EmbeddingConfig();
        content.setSize(4);
        content.setTableName("query-title-emb");
        content.setRecallType("query_qwen_emb");
        QueryEmbeddingNode node = new QueryEmbeddingNode(config("query_qwen_emb", content, true));
        ReflectionTestUtils.setField(node, "recallStore", recallStore);
        List<ScoreResult> recalled = Collections.singletonList(new ScoreResult("result", 0.8));
        when(recallStore.queryEmbedding(eq("query-title-emb"), eq(Arrays.asList(1.0, 2.0)), eq(4), anyLong()))
            .thenReturn(recalled);
        GraphContext context = new GraphContext();
        context.addParam("scene", "s");
        context.addParam("queryEmbedding", Arrays.asList(1, 2.0));

        node.run(context);
        context.exportNodeData(node);

        assertSame(recalled, context.getData("queryEmbeddingItems"));
        assertSame(recalled, context.getData("recall:query_qwen_emb"));
    }

    @Test
    public void queryTextIsEmbeddedAndUnavailableProviderSkipsVectorSearch() {
        RecallStore store = mock(RecallStore.class);
        com.openrec.service.embedding.EmbeddingService embeddings =
            mock(com.openrec.service.embedding.EmbeddingService.class);
        EmbeddingConfig content = new EmbeddingConfig();
        content.setSize(4);
        content.setTableName("query-title-emb");
        content.setRecallType("query_qwen_emb");
        NodeConfig<EmbeddingConfig> configuration = config("query_qwen_emb", content, true);
        configuration.setTimeout(800);
        QueryEmbeddingNode node = new QueryEmbeddingNode(configuration);
        ReflectionTestUtils.setField(node, "recallStore", store);
        ReflectionTestUtils.setField(node, "embeddingService", embeddings);
        GraphContext context = new GraphContext();
        context.addParam("scene", "s");
        context.addParam("query", "jazz");
        when(embeddings.embedQuery(eq("jazz"), anyLong())).thenReturn(Collections.emptyList());
        node.run(context);
        verifyNoInteractions(store);
        assertEquals(Collections.emptyList(), context.getData("recall:query_qwen_emb"));
        List<Double> vector = Arrays.asList(0.6, 0.8);
        when(embeddings.embedQuery(eq("jazz"), anyLong())).thenReturn(vector);
        List<ScoreResult> candidates = Collections.singletonList(new ScoreResult("item-1", 0.9));
        when(store.queryEmbedding(eq("query-title-emb"), eq(vector), eq(4), anyLong())).thenReturn(candidates);
        node.run(context);
        assertSame(candidates, context.getData("recall:query_qwen_emb"));
    }

    @Test
    public void searchUsesRequestTextAndExportsItsOwnChannel() {
        RecallStore recallStore = mock(RecallStore.class);
        SearchConfig content = new SearchConfig();
        content.setSize(20);
        content.setTableName("item-search");
        content.setRecallType("item_search");
        content.setQueryParam("query");
        content.setTextField("text");
        content.setMinimumShouldMatch("30%");
        SearchNode node = new SearchNode(config("item_search", content, true));
        ReflectionTestUtils.setField(node, "recallStore", recallStore);
        List<ScoreResult> recalled = Collections.singletonList(new ScoreResult("result", 2.5));
        when(recallStore.textSearch("item-search", "s", "blue train", "text", "30%", "item", 20, 50))
            .thenReturn(recalled);
        GraphContext context = new GraphContext();
        context.addParam("scene", "s");
        context.addParam("query", "blue train");

        node.run(context);
        context.exportNodeData(node);

        assertSame(recalled, context.getData("searchItems"));
        assertSame(recalled, context.getData("recall:item_search"));
    }

    @Test
    public void searchSkipsBlankQueriesAndDisabledNodes() {
        RecallStore recallStore = mock(RecallStore.class);
        SearchConfig content = new SearchConfig();
        content.setTableName("item-search");
        content.setRecallType("item_search");
        content.setQueryParam("searchText");
        SearchNode node = new SearchNode(config("search", content, true));
        ReflectionTestUtils.setField(node, "recallStore", recallStore);
        for (String query : Arrays.asList(null, "", "   ")) {
            GraphContext context = new GraphContext();
            if (query != null) {
                context.addParam("searchText", query);
            }
            node.run(context);
            context.exportNodeData(node);
            assertEquals(Collections.emptyList(), context.getData("searchItems"));
            assertEquals(Collections.emptyList(), context.getData("recall:item_search"));
        }
        SearchNode disabled = new SearchNode(config("search", content, false));
        ReflectionTestUtils.setField(disabled, "recallStore", recallStore);
        GraphContext context = new GraphContext();
        context.addParam("searchText", "query");
        disabled.run(context);
        verifyNoInteractions(recallStore);
    }

    @Test
    public void u2iLooksUpRequestUserAndExportsConfiguredChannel() {
        RecallStore recallStore = mock(RecallStore.class);
        U2iConfig content = new U2iConfig();
        content.setSize(4);
        content.setTableName("user-cf-u2i");
        content.setRecallType("user_cf_u2i");
        U2iNode node = new U2iNode(config("userCf", content, true));
        ReflectionTestUtils.setField(node, "recallStore", recallStore);
        List<ScoreResult> recalled = Collections.singletonList(new ScoreResult("result", 3));
        when(recallStore.u2i("user-cf-u2i", "s", "u", 4)).thenReturn(recalled);
        GraphContext context = new GraphContext();
        context.addParam("scene", "s");
        context.addParam("userId", "u");
        node.run(context);
        context.exportNodeData(node);
        assertSame(recalled, context.getData("u2iItems"));
        assertSame(recalled, context.getData("recall:user_cf_u2i"));
    }

    @Test
    public void rankFusesScoresAndHandlesFailureAndClosedMode() {
        RankConfig content = new RankConfig();
        content.setSize(2);
        RankService rank = mock(RankService.class);
        when(rank.isOpen()).thenReturn(true);
        RankNode node = new RankNode(config("rank", content, true));
        ReflectionTestUtils.setField(node, "rankService", rank);
        List<ScoreResult> recalled =
            Arrays.asList(new ScoreResult("a", 2), new ScoreResult("b", 3), new ScoreResult("c", 4));
        ReflectionTestUtils.setField(node, "combineItems", recalled);
        ReflectionTestUtils.setField(node, "userFeatureMap", Collections.singletonMap("userId", "u"));
        when(rank.score(eq("u"), eq(Arrays.asList("a", "b")), eq("item"), anyString(), anyMap(), anyMap()))
            .thenReturn(Collections.singletonMap("a", 5d));
        GraphContext context = new GraphContext();
        node.run(context);
        context.exportNodeData(node);
        List<ScoreResult> ranked = (List<ScoreResult>)context.getData("rankItems");
        assertEquals(7d, ranked.get(0).getScore(), 0);
        assertEquals(3d, ranked.get(1).getScore(), 0);
        assertEquals(Double.valueOf(2), ranked.get(0).getRecallScore());
        assertEquals(Double.valueOf(5), ranked.get(0).getRankScore());

        RankNode failing = new RankNode(config("rank", content, true));
        ReflectionTestUtils.setField(failing, "rankService", rank);
        ReflectionTestUtils.setField(failing, "combineItems", Collections.singletonList(new ScoreResult("x", 1)));
        ReflectionTestUtils.setField(failing, "userFeatureMap", Collections.singletonMap("userId", "u"));
        when(rank.score(eq("u"), eq(Collections.singletonList("x")), eq("item"), anyString(), anyMap(), anyMap()))
            .thenThrow(new RuntimeException("down"));
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
        ReflectionTestUtils.setField(closed, "combineItems", recalled);
        closed.run(context);
        context.exportNodeData(closed);
        assertSame(recalled, context.getData("rankItems"));
    }

    @Test
    public void rankAppliesConfiguredMultiChannelScoreStrategy() {
        RankScoreStrategyConfig strategy = new RankScoreStrategyConfig();
        strategy.setRecallAggregation("sum");
        strategy.setRecallWeight(0.2);
        strategy.setRankWeight(0.9);
        strategy.setChannels(
            Arrays.asList(new RankChannelWeightConfig("item_cf_i2i", 2), new RankChannelWeightConfig("item_seq_emb", 1),
                new RankChannelWeightConfig("hot", 2), new RankChannelWeightConfig("new", 1)));
        RankConfig content = new RankConfig();
        content.setSize(1);
        content.setScoreStrategy(strategy);
        RankService rank = mock(RankService.class);
        when(rank.isOpen()).thenReturn(true);
        when(rank.score(eq("u"), eq(Collections.singletonList("a")), eq("item"), anyString(), anyMap(), anyMap()))
            .thenReturn(Collections.singletonMap("a", 0.5));
        ScoreResult item = new ScoreResult("a", 0.4);
        item.addRecallScore("item_cf_i2i", 0.4);
        item.addRecallScore("hot", 0.3);
        RankNode node = new RankNode(config("rank", content, true));
        ReflectionTestUtils.setField(node, "rankService", rank);
        ReflectionTestUtils.setField(node, "combineItems", Collections.singletonList(item));
        ReflectionTestUtils.setField(node, "userFeatureMap", Collections.singletonMap("userId", "u"));

        GraphContext context = new GraphContext();
        node.run(context);
        context.exportNodeData(node);
        ScoreResult result = ((List<ScoreResult>)context.getData("rankItems")).get(0);
        assertEquals(0.73, result.getScore(), 0.000001);
        assertEquals(Double.valueOf(1.4), result.getRecallFusionScore());
        assertEquals(Double.valueOf(0.5), result.getRankScore());
    }

    @Test
    public void collectorLimitsResultAndWritesExposeOnlyWhenNonEmpty() {
        RedisService redis = mock(RedisService.class);
        CollectorNode node = new CollectorNode(config("collector", null, true));
        ReflectionTestUtils.setField(node, "redisService", redis);
        ReflectionTestUtils.setField(node, "finalItems",
            Arrays.asList(new ScoreResult("a", 1), new ScoreResult("b", 2)));
        GraphContext context = new GraphContext();
        context.addParam("size", 1);
        context.addParam("userId", "u");
        context.addParam("scene", "s");
        node.run(context);
        assertEquals(1, ((List<?>)context.getResult()).size());
        verify(redis).addZSets(eq("event:{u}:s:expose"), argThat(map -> map.containsKey("a")));

        CollectorNode empty = new CollectorNode(config("collector", null, true));
        ReflectionTestUtils.setField(empty, "redisService", redis);
        ReflectionTestUtils.setField(empty, "finalItems", Collections.emptyList());
        empty.run(context);
        verifyNoMoreInteractions(redis);
    }

    @Test
    public void internalWarmupSuppressesExposureButRequestParameterCannot() {
        RedisService redis = mock(RedisService.class);
        CollectorNode node = new CollectorNode(config("collector", null, true));
        ReflectionTestUtils.setField(node, "redisService", redis);
        ReflectionTestUtils.setField(node, "finalItems", Collections.singletonList(new ScoreResult("a", 1)));
        GraphContext context = new GraphContext();
        context.addParam("size", 1);
        context.addParam("userId", "u");
        context.addParam("scene", "s");
        context.setWarmup(true);
        node.run(context.forkExecution());
        verifyNoInteractions(redis);
        context.setWarmup(false);
        context.addParam("warmup", true);
        node.run(context);
        verify(redis).addZSets(eq("event:{u}:s:expose"), anyMap());
    }

    @Test
    public void collectorDoesNotWriteSyntheticExposureInCluster() {
        RedisService redis = mock(RedisService.class);
        MockEnvironment cluster = new MockEnvironment();
        cluster.setProperty("collector.fake-expose.enabled", "false");
        CollectorNode node = new CollectorNode(config("collector", null, true));
        ReflectionTestUtils.setField(node, "redisService", redis);
        ReflectionTestUtils.setField(node, "environment", cluster);
        ReflectionTestUtils.setField(node, "finalItems", Collections.singletonList(new ScoreResult("a", 1)));
        GraphContext context = new GraphContext();
        context.addParam("size", 1);
        node.run(context);
        assertEquals(1, ((List<?>)context.getResult()).size());
        verifyNoInteractions(redis);
    }

    @Test
    public void userNodesReuseMergeAndCollectionWithoutItemSideEffects() {
        CombineConfig content = new CombineConfig();
        content.setSize(10);
        content.setRecallTypes(Arrays.asList("user_cf_u2u", "content_u2u", "user_als_u2u"));
        com.openrec.graph.node.user.CombineNode combine =
            new com.openrec.graph.node.user.CombineNode(config("combine", content, true));
        GraphContext context = new GraphContext();
        context.addParam("userId", "self");
        context.addData("recall:user_cf_u2u", Arrays.asList(new ScoreResult("self", 9), new ScoreResult("u2", 0.7)));
        context.addData("recall:content_u2u", Arrays.asList(new ScoreResult("u2", 0.2), new ScoreResult("u3", 0.4)));
        context.addData("recall:user_als_u2u", Collections.singletonList(new ScoreResult("u3", 0.1)));
        ReflectionTestUtils.setField(combine, "requestTrigger", RequestTrigger.fromParams(context));
        combine.run(context);
        context.exportNodeData(combine);

        List<ScoreResult> merged = (List<ScoreResult>)context.getData("userCandidates");
        assertEquals(Arrays.asList("u2", "u3"), Arrays.asList(merged.get(0).getId(), merged.get(1).getId()));
        assertEquals(0.9, merged.get(0).getScore(), 0.000001);
        assertEquals(2, merged.get(0).getRecallScores().size());
        assertEquals(0.5, merged.get(1).getScore(), 0.000001);

        com.openrec.graph.node.user.CollectorNode collector =
            new com.openrec.graph.node.user.CollectorNode(config("collector", null, true));
        ReflectionTestUtils.setField(collector, "candidates", merged);
        context.addParam("size", 1);
        collector.run(context);
        assertEquals(Collections.singletonList("u2"), ((List<ScoreResult>)context.getResult()).stream()
            .map(ScoreResult::getId).collect(java.util.stream.Collectors.toList()));
    }

    @Test
    public void simpleFeatureBlackAndOperationNodesWork() {
        GraphContext context = new GraphContext();
        context.addParam("userId", "u");
        UserFeatureNode user = new UserFeatureNode(config("user_feature", null, true));
        user.run(context);
        context.exportNodeData(user);
        assertEquals("u", ((Map<?, ?>)context.getData("userFeatureMap")).get("userId"));
        new ItemFeatureNode(config("item_feature", null, true)).run(context);

        RedisService redis = mock(RedisService.class);
        when(redis.getSet("black")).thenReturn(Collections.singleton("x"));
        BlackNode black = new BlackNode(config("black", new FilterConfig(), true));
        ReflectionTestUtils.setField(black, "redisService", redis);
        black.run(context);
        context.exportNodeData(black);
        assertEquals(Collections.singleton("x"), context.getData("blackItemSet"));

        OperationConfig operationConfig = new OperationConfig();
        operationConfig.setOperationName("missing");
        OperationNode operation = new OperationNode(config("operation", operationConfig, true));
        List<ScoreResult> input = Collections.singletonList(new ScoreResult("x", 1));
        ReflectionTestUtils.setField(operation, "rankItems", input);
        operation.run(context);
        context.exportNodeData(operation);
        assertSame(input, context.getData("operationItems"));
    }

    @Test
    public void blackNodeExportsStructuredDislikeRules() {
        RedisService redis = mock(RedisService.class);
        when(redis.getSet("black")).thenReturn(Collections.singleton("global-id"));
        when(redis.getZSet(eq("event:{u}:s:dislike"), anyDouble(), anyDouble(), eq(1000))).thenReturn(Arrays.asList(
            new ScoreResult("id:item-1", 1), new ScoreResult("category:sports", 1), new ScoreResult("tag:nba", 1)));
        FilterConfig.TypeFilterConfig dislike = new FilterConfig.TypeFilterConfig();
        dislike.setDuration(2592000);
        dislike.setSize(1000);
        FilterConfig content = new FilterConfig();
        content.setFilterMap(Collections.singletonMap("dislike", dislike));
        BlackNode node = new BlackNode(config("black", content, true));
        ReflectionTestUtils.setField(node, "redisService", redis);
        GraphContext context = new GraphContext();
        context.addParam("userId", "u");
        context.addParam("scene", "s");
        node.run(context);
        context.exportNodeData(node);
        assertEquals(new HashSet<>(Arrays.asList("global-id", "item-1")), context.getData("blackItemSet"));
        assertEquals(Collections.singleton("sports"), context.getData("blackCategorySet"));
        assertEquals(Collections.singleton("nba"), context.getData("blackTagSet"));
    }

    @Test
    public void combineMatchesNegativeCategoryAndTags() {
        com.openrec.proto.model.Item item = new com.openrec.proto.model.Item();
        item.setCategory("sports");
        item.setTags("football,nba");
        assertTrue(CombineNode.isNegativeFeedbackMatch(item, Collections.singleton("sports"), Collections.emptySet()));
        assertTrue(CombineNode.isNegativeFeedbackMatch(item, Collections.emptySet(), Collections.singleton("nba")));
        assertFalse(
            CombineNode.isNegativeFeedbackMatch(item, Collections.singleton("news"), Collections.singleton("music")));
    }
}
