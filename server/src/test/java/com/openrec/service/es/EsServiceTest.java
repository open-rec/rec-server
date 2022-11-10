package com.openrec.service.es;

import com.openrec.RecServer;
import com.openrec.proto.model.VectorResult;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = RecServer.class)
public class EsServiceTest {

    private static final String TEST_INDEX_NAME = "test_vector_index";
    private static final String TEST_INDEX_DEF = "{\n" +
            "  \"mappings\": {\n" +
            "    \"properties\": {\n" +
            "      \"id-vector\": {\n" +
            "        \"type\": \"dense_vector\",\n" +
            "        \"dims\": 10,\n" +
            "        \"index\": true,\n" +
            "        \"similarity\": \"l2_norm\"\n" +
            "      },\n" +
            "      \"id\": {\n" +
            "        \"type\": \"keyword\"\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";

    private static final String TEST_QUERY = "{\n" +
            "  \"query\": {\n" +
            "    \"constant_score\": {\n" +
            "      \"filter\": {\n" +
            "          \"terms\": {\n" +
            "            \"id\": [\"3\"]\n" +
            "          }\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";

    @Autowired
    private EsService esService;

    @Test
    public void test() throws IOException {
        esService.createIndex(TEST_INDEX_NAME, TEST_INDEX_DEF);
        List<Pair<Integer, Object>> bulkList = Lists.newArrayList();
        for (int i = 0; i < 10; i++) {
            bulkList.add(Pair.of(i, new VectorResult(String.valueOf(i), Arrays.asList(new Double[]{0.1, 0.2, 0.3}))));
        }
        esService.bulk(TEST_INDEX_NAME, bulkList);
        esService.search(TEST_INDEX_NAME, TEST_QUERY);
        esService.deleteIndex(TEST_INDEX_NAME);
    }
}