package com.openrec.graph.node;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.openrec.graph.GraphContext;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.tools.anno.Export;
import com.openrec.graph.tools.anno.Import;
import com.openrec.proto.model.ScoreResult;
import com.openrec.proto.model.VectorResult;
import com.openrec.service.es.EsService;
import com.openrec.util.BeanUtil;
import com.openrec.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.assertj.core.util.Lists;
import org.springframework.util.CollectionUtils;
import sun.java2d.pipe.AAShapePipe;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.openrec.graph.RecParams.SCENE;


@Slf4j
public class EmbeddingNode extends SyncNode<Void> {


    private EsService esService = BeanUtil.getBean(EsService.class);

    private String bizType = "embedding";

    private String INDEX_FORMAT = "%s-item-vector-index";

    private String VECTORS_QUERY="{\n" +
            "  \"query\": {\n" +
            "    \"constant_score\": {\n" +
            "      \"filter\": {\n" +
            "          \"terms\": {\n" +
            "            \"id\": %s\n" +
            "          }\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";
    private String VECTOR_RECALL = "{\n" +
            "  \"knn\": {\n" +
            "    \"field\": \"vector\",\n" +
            "    \"query_vector\": %s,\n" +
            "    \"k\": 10,\n" +
            "    \"num_candidates\": 20\n" +
            "  },\n" +
            "  \"fields\": [ \"id\"],\n" +
            "  \"size\": %d\n" +
            "}";

    @Import("triggerItems")
    private List<ScoreResult> triggerItems;

    @Export("embeddingItems")
    private List<ScoreResult> embeddingItems;




    public EmbeddingNode(NodeConfig nodeConfig) {
        super(nodeConfig);
        this.embeddingItems = Lists.newArrayList();
    }


    private List<List<Double>> getVectors(String indexName, List<String> items) {
        List<List<Double>> vectors=null;
        try {
            SearchResponse response=esService.search(indexName,
                    String.format(VECTORS_QUERY, JsonUtil.objToJson(items)), VectorResult.class);
        } catch (IOException e) {
            log.error("{} query vectors failed: {}", ExceptionUtils.getStackTrace(e));
        }
        return vectors;
    }

    private List<Double> parseVectors(List<List<Double>> vectors){
        return null;
    }

    private void recallItems(String indexName, List<Double> vector) {

    }

    @Override
    public void run(GraphContext context) {
        String scene = context.getParams().getValueToString(SCENE);
        String indexName = String.format(INDEX_FORMAT, scene);

        int timeout = config.getTimeout();
        boolean open = config.isOpen();

        if (!open) {
            log.info("{} not open, just return", getName());
            return;
        }

        List<String> itemIds = triggerItems.stream().map(i->i.getId()).collect(Collectors.toList());
        List<List<Double>> vectors= getVectors(indexName, itemIds);
        if(!CollectionUtils.isEmpty(vectors)) {
            List<Double> vector=parseVectors(vectors);
            recallItems(indexName, vector);
        }
        log.info("{} with embedding size:{}", getName(), embeddingItems.size());
    }
}
