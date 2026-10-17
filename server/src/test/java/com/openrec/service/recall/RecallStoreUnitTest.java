package com.openrec.service.recall;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.openrec.proto.model.ScoreResult;
import com.openrec.service.es.EsService;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;

public class RecallStoreUnitTest {

    @Test
    public void elasticsearchIsAvailableWithoutStoreSelection() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getBeanFactory().registerSingleton("esService", mock(EsService.class));
            context.scan("com.openrec.service.recall");
            context.refresh();
            assertEquals(1, context.getBeansOfType(RecallStore.class).size());
            assertSame(context.getBean(ElasticsearchRecallStore.class), context.getBean(RecallStore.class));
        }
    }

    @Test
    public void elasticsearchUsesAlgorithmAliasesAndSceneFilters() throws IOException {
        EsService es = mock(EsService.class);
        ElasticsearchRecallStore store = elasticsearchStore(es);

        SearchResponse<RecallDocument> hotResponse = response(document("scene-1", "hot-1", null, 0.9), 1.0);
        when(es.search(eq("openrec-recall-hot-active"), anyString(), eq(RecallDocument.class), eq("1000ms")))
            .thenReturn(hotResponse);
        assertEquals(Collections.singletonList("hot-1"), ids(store.hot("hot", "scene-1", 10)));
        verify(es).search(eq("openrec-recall-hot-active"), org.mockito.ArgumentMatchers.contains("scene-1"),
            eq(RecallDocument.class), eq("1000ms"));

        SearchResponse<RecallDocument> newResponse = response(document("scene-1", "new-1", null, 0.75), 1.0);
        when(es.search(eq("openrec-recall-new-active"), anyString(), eq(RecallDocument.class), eq("1000ms")))
            .thenReturn(newResponse);
        assertEquals(0.75, store.newest("new", "scene-1", 100, 200, 10).get(0).getScore(), 0.000001);
        verify(es).search(eq("openrec-recall-new-active"), org.mockito.ArgumentMatchers.contains("publish_time"),
            eq(RecallDocument.class), eq("1000ms"));
    }

    @Test
    public void elasticsearchI2iSumsDuplicateCandidateScores() throws IOException {
        EsService es = mock(EsService.class);
        ElasticsearchRecallStore store = elasticsearchStore(es);
        SearchResponse<RecallDocument> i2iResponse = response(Arrays.asList(document("scene-1", null, "same", 0.7),
            document("scene-1", null, "other", 0.8), document("scene-1", null, "same", 0.6)));
        when(es.search(eq("openrec-recall-item-cf-i2i-active"), anyString(), eq(RecallDocument.class), eq("1000ms")))
            .thenReturn(i2iResponse);

        List<ScoreResult> result = store.i2i("item-cf-i2i", "scene-1", Arrays.asList("a", "b"), 2);
        assertEquals(Arrays.asList("same", "other"), ids(result));
        assertEquals(1.3, result.get(0).getScore(), 0.000001);
        assertEquals(0.8, result.get(1).getScore(), 0.000001);
    }

    @Test
    public void elasticsearchU2iUsesConfiguredAliasAndUserKey() throws IOException {
        EsService es = mock(EsService.class);
        ElasticsearchRecallStore store = elasticsearchStore(es);
        SearchResponse<RecallDocument> response = response(document("scene-1", "personal", null, 0.8), 1.0);
        when(es.search(eq("openrec-recall-user-cf-u2i-active"), anyString(), eq(RecallDocument.class), eq("1000ms")))
            .thenReturn(response);

        assertEquals(Collections.singletonList("personal"), ids(store.u2i("user-cf-u2i", "scene-1", "user-1", 10)));
        verify(es).search(eq("openrec-recall-user-cf-u2i-active"), org.mockito.ArgumentMatchers.contains("user-1"),
            eq(RecallDocument.class), eq("1000ms"));
    }

    @Test
    public void elasticsearchEmbeddingReadsIdFieldFromExistingIndex() throws IOException {
        EsService es = mock(EsService.class);
        ElasticsearchRecallStore store = elasticsearchStore(es);

        RecallDocument trigger = new RecallDocument();
        trigger.setId("trigger");
        trigger.setVector(Arrays.asList(1.0, 2.0));
        RecallDocument recalled = new RecallDocument();
        recalled.setId("embedding-result");
        when(es.search(eq("scene-1-item-seq-emb-index"), anyString(), eq(RecallDocument.class), eq("50ms")))
            .thenReturn(response(trigger, 1.0), response(recalled, 0.75));

        List<ScoreResult> result =
            store.embedding("item-seq-emb", "scene-1", Collections.singletonList("trigger"), 5, 50);
        assertEquals(Collections.singletonList("embedding-result"), ids(result));
        assertEquals(0.75, result.get(0).getScore(), 0.000001);
    }

    @Test
    public void elasticsearchQueryEmbeddingSearchesDirectlyWithRequestedSize() throws IOException {
        EsService es = mock(EsService.class);
        ElasticsearchRecallStore store = elasticsearchStore(es);
        RecallDocument recalled = new RecallDocument();
        recalled.setId("query-result");
        when(es.search(eq("query-title-emb-index"), anyString(), eq(RecallDocument.class), eq("50ms")))
            .thenReturn(response(recalled, 0.85));

        List<ScoreResult> result = store.queryEmbedding("query-title-emb", Arrays.asList(1.0, 2.0), 25, 50);
        assertEquals(Collections.singletonList("query-result"), ids(result));
        assertEquals(0.85, result.get(0).getScore(), 0.000001);
        verify(es).search(eq("query-title-emb-index"),
            org.mockito.ArgumentMatchers.argThat(query -> query.contains("\"k\":25,\"num_candidates\":50")
                && query.contains("\"_source\":{\"includes\":[\"id\"]}")),
            eq(RecallDocument.class), eq("50ms"));

        assertEquals(Collections.emptyList(),
            store.queryEmbedding("query-title-emb", Arrays.asList(1.0, Double.NaN), 25, 50));
    }

    @Test
    public void elasticsearchTextSearchUsesBm25ScoreAndSceneFilter() throws IOException {
        EsService es = mock(EsService.class);
        ElasticsearchRecallStore store = elasticsearchStore(es);
        RecallDocument recalled = new RecallDocument();
        recalled.setScene("scene-1");
        recalled.setItem("track-1");
        when(es.search(eq("openrec-recall-item-search-active"), anyString(), eq(RecallDocument.class), eq("50ms")))
            .thenReturn(response(recalled, 3.25));

        List<ScoreResult> result =
            store.textSearch("item-search", "scene-1", "blue train", "text", "30%", "item", 25, 50);

        assertEquals(Collections.singletonList("track-1"), ids(result));
        assertEquals(3.25, result.get(0).getScore(), 0.000001);
        verify(es).search(eq("openrec-recall-item-search-active"),
            org.mockito.ArgumentMatchers.contains("minimum_should_match"), eq(RecallDocument.class), eq("50ms"));
    }

    @Test
    public void elasticsearchUserSearchReturnsUserIdsAndBm25Score() throws IOException {
        EsService es = mock(EsService.class);
        ElasticsearchRecallStore store = elasticsearchStore(es);
        RecallDocument recalled = new RecallDocument();
        recalled.setUser("user-2");
        recalled.setItem("wrong-item");
        when(es.search(eq("openrec-recall-user-search-active"), anyString(), eq(RecallDocument.class), eq("50ms")))
            .thenReturn(response(recalled, 2.5));
        List<ScoreResult> result = store.textSearch("user-search", "scene-1", "jazz", "text", "30%", "user", 25, 50);
        assertEquals(Collections.singletonList("user-2"), ids(result));
        assertEquals(2.5, result.get(0).getScore(), 0.000001);
    }

    @Test
    public void vectorAverageRejectsMixedDimensions() {
        assertEquals(Arrays.asList(2.0, 3.0),
            ElasticsearchRecallStore.average(Arrays.asList(Arrays.asList(1.0, 2.0), Arrays.asList(3.0, 4.0))));
        try {
            ElasticsearchRecallStore.average(Arrays.asList(Arrays.asList(1.0), Arrays.asList(2.0, 3.0)));
            throw new AssertionError("expected inconsistent vector dimensions to fail");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    private static ElasticsearchRecallStore elasticsearchStore(EsService es) {
        ElasticsearchRecallStore store = new ElasticsearchRecallStore();
        ReflectionTestUtils.setField(store, "esService", es);
        ReflectionTestUtils.setField(store, "indexPrefix", "openrec-recall");
        ReflectionTestUtils.setField(store, "aliasSuffix", "active");
        ReflectionTestUtils.setField(store, "maxI2iHits", 10000);
        return store;
    }

    private static RecallDocument document(String scene, String item, String rightItem, double score) {
        RecallDocument document = new RecallDocument();
        document.setScene(scene);
        document.setItem(item);
        document.setRightItem(rightItem);
        document.setScore(score);
        return document;
    }

    private static SearchResponse<RecallDocument> response(RecallDocument document, double score) {
        String id = document.getItem() != null ? document.getItem() : document.getId();
        Hit<RecallDocument> hit = Hit.of(builder -> builder.index("idx").id(id).source(document).score(score));
        return responseFromHits(Collections.singletonList(hit));
    }

    private static SearchResponse<RecallDocument> response(List<RecallDocument> documents) {
        List<Hit<RecallDocument>> hits = documents.stream()
            .map(document -> Hit.<
                RecallDocument>of(builder -> builder.index("idx").id(document.getRightItem()).source(document)))
            .collect(Collectors.toList());
        return responseFromHits(hits);
    }

    private static SearchResponse<RecallDocument> responseFromHits(List<Hit<RecallDocument>> hitList) {
        return SearchResponse.of(builder -> builder.took(1).timedOut(false)
            .shards(shards -> shards.total(1).successful(1).failed(0)).hits(hits -> hits.hits(hitList)));
    }

    private static List<String> ids(List<ScoreResult> results) {
        return results.stream().map(ScoreResult::getId).collect(Collectors.toList());
    }
}
