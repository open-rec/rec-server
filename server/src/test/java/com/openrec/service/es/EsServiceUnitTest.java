package com.openrec.service.es;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class EsServiceUnitTest {

    @Test
    public void existingIndexDeleteBulkAndSearchDelegateToClient() throws IOException {
        ElasticsearchClient client = mock(ElasticsearchClient.class);
        ElasticsearchIndicesClient indices = mock(ElasticsearchIndicesClient.class);
        when(client.indices()).thenReturn(indices);
        when(indices.exists(any(ExistsRequest.class))).thenReturn(new BooleanResponse(true));
        EsService service = new EsService();
        ReflectionTestUtils.setField(service, "esClient", client);
        assertTrue(service.createIndex("idx", "{}"));
        service.deleteIndex("idx"); verify(indices).delete(any(DeleteIndexRequest.class));
        service.bulk("idx", Arrays.asList(Pair.of(1, "one"), Pair.of(2, "two")));
        verify(client).bulk(argThat((BulkRequest request) -> request.operations().size() == 2));

        SearchResponse<String> response = mock(SearchResponse.class);
        when(client.search(any(SearchRequest.class), eq(String.class))).thenReturn(response);
        assertSame(response, service.search("idx", "{\"query\":{\"match_all\":{}}}", String.class));
        assertSame(response, service.search("idx", "{\"query\":{\"match_all\":{}}}", String.class, "20ms"));
        verify(client, times(2)).search(any(SearchRequest.class), eq(String.class));
    }
}
