package com.openrec.service.embedding;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.openrec.config.EsConfig;
import com.openrec.graph.GraphContext;
import com.openrec.graph.config.EmbeddingConfig;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.node.item.QueryEmbeddingNode;
import com.openrec.proto.model.ScoreResult;
import com.openrec.service.es.EsService;
import com.openrec.service.recall.ElasticsearchRecallStore;
import com.openrec.util.JsonUtil;

import co.elastic.clients.elasticsearch.ElasticsearchClient;

/** Opt-in GPU + Elasticsearch test. Creates and removes only a unique test index. */
public class EmbeddingRecallIntegrationTest {
    @Test
    public void queryTextRetrievesOfflineTitleVectors() throws Exception {
        String fixture = System.getenv("EMBEDDING_LIVE_FIXTURE");
        assumeTrue("requires a live embedding endpoint and isolated candidate fixture", fixture != null);
        JsonArray documents = JsonParser.parseString(Files.readString(Path.of(fixture))).getAsJsonArray();
        String scene = "openrec-embedding-it-" + UUID.randomUUID();
        String index = scene + "-query-title-emb-index";
        EsConfig esConfig = new EsConfig();
        ReflectionTestUtils.setField(esConfig, "host", "127.0.0.1");
        ReflectionTestUtils.setField(esConfig, "port", 9200);
        ReflectionTestUtils.setField(esConfig, "user", "elastic");
        ReflectionTestUtils.setField(esConfig, "password",
            System.getenv().getOrDefault("ELASTIC_PASSWORD", "openrec-es-password"));
        ElasticsearchClient es = esConfig.esClient();
        try {
            es.indices().create(i -> i.index(index).mappings(m -> m.properties("id", p -> p.keyword(k -> k))
                .properties("vector", p -> p.denseVector(v -> v.dims(1024).index(true).similarity("cosine")))));
            for (JsonElement document : documents) {
                Map<String, Object> data = JsonUtil.jsonToObj(document.toString(), Map.class);
                data.remove("title");
                es.index(i -> i.index(index).id(data.get("id").toString()).document(data));
            }
            es.indices().refresh(i -> i.index(index));
            EmbeddingService embeddings =
                new EmbeddingService(true, System.getenv("EMBEDDING_LIVE_ENDPOINT"), "Qwen/Qwen3-Embedding-0.6B", "",
                    "Given a search query, retrieve relevant items that match the query", 1024, 500, 2, 4096);
            String query = documents.get(0).getAsJsonObject().get("title").getAsString();
            assertEquals(1024, embeddings.embedQuery(query, 2000).size());
            EsService esService = new EsService();
            ReflectionTestUtils.setField(esService, "esClient", es);
            ElasticsearchRecallStore store = new ElasticsearchRecallStore();
            ReflectionTestUtils.setField(store, "esService", esService);
            EmbeddingConfig content = new EmbeddingConfig();
            content.setTableName(scene + "-query-title-emb");
            content.setRecallType("query_qwen_emb");
            content.setSize(4);
            NodeConfig<EmbeddingConfig> config = new NodeConfig<>();
            config.setName("query_qwen_emb");
            config.setContent(content);
            config.setOpen(true);
            config.setTimeout(800);
            QueryEmbeddingNode node = new QueryEmbeddingNode(config);
            ReflectionTestUtils.setField(node, "embeddingService", embeddings);
            ReflectionTestUtils.setField(node, "recallStore", store);
            GraphContext context = new GraphContext();
            context.addParam("query", query);
            NodeConfig<com.openrec.graph.config.UserTriggerConfig> triggerConfig = new NodeConfig<>();
            triggerConfig.setName("user_trigger");
            triggerConfig.setOpen(true);
            triggerConfig.setContent(new com.openrec.graph.config.UserTriggerConfig());
            com.openrec.graph.node.item.TriggerNode trigger =
                new com.openrec.graph.node.item.TriggerNode(triggerConfig);
            trigger.run(context);
            context.exportNodeData(trigger);
            node.run(context);
            List<ScoreResult> results = (List<ScoreResult>)context.getData("recall:query_qwen_emb");
            assertEquals(4, results.size());
            assertTrue(results.stream().allMatch(result -> Double.isFinite(result.getScore())));
            context.addParam("scene", "unrelated-scene");
            node.run(context);
            List<ScoreResult> otherScene = (List<ScoreResult>)context.getData("recall:query_qwen_emb");
            assertEquals(results.stream().map(ScoreResult::getId).toList(),
                otherScene.stream().map(ScoreResult::getId).toList());
        } finally {
            es.indices().delete(i -> i.index(index));
            es._transport().close();
        }
    }
}
