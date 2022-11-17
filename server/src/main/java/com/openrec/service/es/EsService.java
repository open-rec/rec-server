package com.openrec.service.es;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.transport.endpoints.BooleanResponse;

@Service
public class EsService {

    private static final String DEFAULT_TIMEOUT = "1000ms";

    @Autowired
    private ElasticsearchClient esClient;

    public boolean createIndex(String indexName, String indexDef) throws IOException {
        ExistsRequest existsRequest = ExistsRequest.of(i -> i.index(indexName));
        BooleanResponse response = esClient.indices().exists(existsRequest);
        if (!response.value()) {
            CreateIndexRequest indexRequest =
                CreateIndexRequest.of(i -> i.index(indexName).withJson(new StringReader(indexDef)));
            return esClient.indices().create(indexRequest).acknowledged();
        }
        return true;
    }

    public void deleteIndex(String indexName) throws IOException {
        DeleteIndexRequest request = DeleteIndexRequest.of(i -> i.index(indexName));
        esClient.indices().delete(request);
    }

    public void bulk(String indexName, List<Pair<Integer, Object>> docs) throws IOException {
        BulkRequest.Builder bulkReqBuilder = new BulkRequest.Builder();
        for (Pair<Integer, Object> doc : docs) {
            bulkReqBuilder.operations(
                op -> op.index(i -> i.index(indexName).id(String.valueOf(doc.getLeft())).document(doc.getRight())));
        }
        esClient.bulk(bulkReqBuilder.build());
    }

    public <T> SearchResponse<T> search(String indexName, String query, Class<T> clazz) throws IOException {
        return search(indexName, query, clazz, DEFAULT_TIMEOUT);
    }

    public <T> SearchResponse<T> search(String indexName, String query, Class<T> clazz, String timeout)
        throws IOException {
        SearchRequest request =
            SearchRequest.of(i -> i.index(indexName).timeout(timeout).withJson(new StringReader(query)));
        return esClient.search(request, clazz);
    }
}
