package com.openrec.service.es;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

@Service
public class EsService {

    @Autowired
    private ElasticsearchClient esClient;


    public boolean createIndex(String indexName, String indexDef) throws IOException {
        ExistsRequest existsRequest = ExistsRequest.of(i->i.index(indexName));
        BooleanResponse response = esClient.indices().exists(existsRequest);
        if(!response.value()) {
            CreateIndexRequest indexRequest = CreateIndexRequest
                    .of(i -> i.index(indexName).withJson(new StringReader(indexDef)));
            return esClient.indices().create(indexRequest).acknowledged();
        }
        return true;
    }

    public void deleteIndex(String indexName) throws IOException {
        DeleteIndexRequest request = DeleteIndexRequest
                .of(i -> i.index(indexName));
        esClient.indices().delete(request);
    }

    public void bulk(String indexName, List<Pair<Integer, Object>> docs) throws IOException {
        BulkRequest.Builder bulkReqBuilder = new BulkRequest.Builder();
        for (Pair<Integer, Object> doc : docs) {
            bulkReqBuilder.operations(op -> op.index(i -> i.index(indexName)
                    .id(String.valueOf(doc.getLeft()))
                    .document(doc.getRight())));
        }
        esClient.bulk(bulkReqBuilder.build());
    }

    public void search(String indexName, String query) throws IOException {
        SearchRequest request = SearchRequest
                .of(i -> i.index(indexName).withJson(new StringReader(query)));
        esClient.search(request, Void.class);
    }
}
