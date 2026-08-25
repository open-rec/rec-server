package com.openrec.service.recall;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.openrec.proto.model.ScoreResult;
import com.openrec.service.es.EsService;
import com.openrec.util.JsonUtil;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import lombok.extern.slf4j.Slf4j;

/** Reads versioned offline recall tables through stable per-algorithm Elasticsearch aliases. */
@Slf4j
@Service
@ConditionalOnProperty(name = "recall.store", havingValue = "elasticsearch")
public class ElasticsearchRecallStore implements RecallStore {

    private static final String EMBEDDING_INDEX_FORMAT = "%s-%s-index";
    private static final String EMBEDDING_VECTORS_QUERY = "{\"query\":{\"constant_score\":{\"filter\":{"
        + "\"terms\":{\"id\":%s}}}}}}";
    private static final String EMBEDDING_RECALL_QUERY = "{\"knn\":{\"field\":\"vector\","
        + "\"query_vector\":%s,\"k\":10,\"num_candidates\":20},\"size\":%d}";

    @Autowired
    private EsService esService;

    @Value("${recall.elasticsearch.index-prefix:openrec-recall}")
    private String indexPrefix;

    @Value("${recall.elasticsearch.alias-suffix:active}")
    private String aliasSuffix;

    @Value("${recall.elasticsearch.max-i2i-hits:10000}")
    private int maxI2iHits;

    @Override
    public List<ScoreResult> hot(String tableName, String scene, int size) {
        String query = String.format("{\"query\":{\"term\":{\"scene\":%s}},"
            + "\"sort\":[{\"score\":\"desc\"},{\"item\":\"asc\"}],\"size\":%d}",
            JsonUtil.objToJson(scene), size);
        return scoredItems(search(tableName, query, "1000ms"), false);
    }

    @Override
    public List<ScoreResult> newest(
        String tableName, String scene, long startTime, long endTime, int size) {
        String query = String.format("{\"query\":{\"bool\":{\"filter\":["
                + "{\"term\":{\"scene\":%s}},{\"range\":{\"publish_time\":{\"gte\":%d,\"lte\":%d}}}]}},"
                + "\"sort\":[{\"score\":\"desc\"},{\"item\":\"asc\"}],\"size\":%d}",
            JsonUtil.objToJson(scene), startTime, endTime, size);
        return scoredItems(search(tableName, query, "1000ms"), false);
    }

    @Override
    public List<ScoreResult> i2i(
        String tableName, String scene, List<String> triggerItems, int size) {
        if (triggerItems == null || triggerItems.isEmpty()) {
            return Collections.emptyList();
        }
        String query = String.format("{\"query\":{\"bool\":{\"filter\":["
                + "{\"term\":{\"scene\":%s}},{\"terms\":{\"left_item\":%s}}]}},\"size\":%d}",
            JsonUtil.objToJson(scene), JsonUtil.objToJson(triggerItems), maxI2iHits);
        List<ScoreResult> edges = scoredItems(search(tableName, query, "1000ms"), true);
        Map<String, Double> merged = new LinkedHashMap<>();
        for (ScoreResult edge : edges) {
            merged.merge(edge.getId(), edge.getScore(), Double::sum);
        }
        return merged.entrySet().stream().map(entry -> new ScoreResult(entry.getKey(), entry.getValue()))
            .sorted(Comparator.comparingDouble(ScoreResult::getScore).reversed()
                .thenComparing(ScoreResult::getId))
            .limit(size).collect(Collectors.toList());
    }

    @Override
    public List<ScoreResult> u2i(String tableName, String scene, String userId, int size) {
        if (userId == null || userId.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String query = String.format("{\"query\":{\"bool\":{\"filter\":["
                + "{\"term\":{\"scene\":%s}},{\"term\":{\"user\":%s}}]}},"
                + "\"sort\":[{\"score\":\"desc\"},{\"item\":\"asc\"}],\"size\":%d}",
            JsonUtil.objToJson(scene), JsonUtil.objToJson(userId), size);
        return scoredItems(search(tableName, query, "1000ms"), false);
    }

    @Override
    public List<ScoreResult> embedding(
        String tableName, String scene, List<String> triggerItems, int size, long timeoutMillis) {
        if (triggerItems == null || triggerItems.isEmpty()) {
            return Collections.emptyList();
        }
        String index = String.format(EMBEDDING_INDEX_FORMAT, scene, tableName);
        String timeout = timeoutMillis + "ms";
        try {
            SearchResponse<RecallDocument> vectorsResponse = esService.search(index,
                String.format(EMBEDDING_VECTORS_QUERY, JsonUtil.objToJson(triggerItems)),
                RecallDocument.class, timeout);
            List<List<Double>> vectors = vectorsResponse.hits().hits().stream()
                .filter(hit -> hit.source() != null && hit.source().getVector() != null)
                .map(hit -> hit.source().getVector()).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(vectors)) {
                return Collections.emptyList();
            }
            SearchResponse<RecallDocument> recallResponse = esService.search(index,
                String.format(EMBEDDING_RECALL_QUERY, JsonUtil.objToJson(average(vectors)), size),
                RecallDocument.class, timeout);
            return recallResponse.hits().hits().stream().filter(hit -> hit.source() != null)
                .map(hit -> new ScoreResult(hit.source().getId(), hit.score()))
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Elasticsearch embedding recall failed: {}", ExceptionUtils.getStackTrace(e));
            return Collections.emptyList();
        }
    }

    static List<Double> average(List<List<Double>> vectors) {
        int dimension = vectors.get(0).size();
        List<Double> result = new ArrayList<>(dimension);
        for (int column = 0; column < dimension; column++) {
            double sum = 0;
            for (List<Double> vector : vectors) {
                if (vector.size() != dimension) {
                    throw new IllegalArgumentException("embedding vectors have inconsistent dimensions");
                }
                sum += vector.get(column);
            }
            result.add(sum / vectors.size());
        }
        return result;
    }

    private SearchResponse<RecallDocument> search(String algorithm, String query, String timeout) {
        try {
            return esService.search(alias(algorithm), query, RecallDocument.class, timeout);
        } catch (Exception e) {
            throw new IllegalStateException("Elasticsearch " + algorithm + " recall failed", e);
        }
    }

    private List<ScoreResult> scoredItems(SearchResponse<RecallDocument> response, boolean i2i) {
        if (response == null || response.hits() == null) {
            return Collections.emptyList();
        }
        List<ScoreResult> result = new ArrayList<>();
        response.hits().hits().forEach(hit -> {
            RecallDocument source = hit.source();
            if (source == null) {
                return;
            }
            String item = i2i ? source.getRightItem() : source.getItem();
            if (item != null) {
                double score = source.getScore() != null ? source.getScore()
                    : (hit.score() != null ? hit.score() : 0d);
                result.add(new ScoreResult(item, score));
            }
        });
        return result;
    }

    String alias(String algorithm) {
        return String.format("%s-%s-%s", indexPrefix, algorithm, aliasSuffix);
    }
}
