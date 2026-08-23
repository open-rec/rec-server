package com.openrec.service.rec;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.openrec.graph.GraphConfig;
import com.openrec.graph.RecTemplate;
import com.openrec.graph.config.NodeConfig;
import com.openrec.ab.AbExperimentService;
import com.openrec.util.JsonUtil;

public class ServingGraphServiceTest {

    private ServingGraphService service;
    private RecService recService;
    private AbExperimentService abExperimentService;

    @Before
    public void setUp() {
        service = new ServingGraphService();
        recService = new RecService();
        abExperimentService = new AbExperimentService(recService);
        ReflectionTestUtils.setField(service, "abExperimentService", abExperimentService);
    }

    @Test
    public void activatesNodeConfigsAtomically() {
        String graph = JsonUtil.objToJson(RecTemplate.toGraphConfig());
        Assert.assertEquals("graph-v2", service.activate(graph, "graph-v2").get("version"));
        Assert.assertTrue(String.valueOf(service.status().get("checksum")).startsWith("sha256:"));
    }

    @Test
    public void onlyUpdatesExistingNodeConfigs() {
        GraphConfig current = recService.getGraphConfig();
        String nodeName = current.getNodes().get(0).getName();
        int nodeCount = current.getNodes().size();
        String edges = JsonUtil.objToJson(current.getEdges());

        NodeConfig update = current.getNodes().get(0);
        update = JsonUtil.jsonToObj(JsonUtil.objToJson(update), NodeConfig.class);
        update.setTimeout(update.getTimeout() + 1);
        String graph = "{\"nodes\":[" + JsonUtil.objToJson(update) + ","
            + "{\"name\":\"new-node\",\"clazz\":\"does.not.Exist\",\"open\":true,\"timeout\":10}],"
            + "\"edges\":[{\"from\":\"new-node\",\"to\":\"" + nodeName + "\"}]}";

        service.activate(graph, "node-config-only");

        GraphConfig activated = (GraphConfig) service.status().get("graph");
        Assert.assertEquals(nodeCount, activated.getNodes().size());
        Assert.assertEquals(edges, JsonUtil.objToJson(activated.getEdges()));
        Assert.assertFalse(activated.getNodes().stream().anyMatch(node -> "new-node".equals(node.getName())));
        Assert.assertEquals(update.getTimeout(), activated.getNodes().get(0).getTimeout());
    }

    @Test
    public void activatesIndependentExperimentGraph() {
        GraphConfig graph = recService.getGraphConfig();
        NodeConfig update = JsonUtil.jsonToObj(JsonUtil.objToJson(graph.getNodes().get(0)), NodeConfig.class);
        update.setTimeout(update.getTimeout() + 10);
        service.activate("test1", "{\"nodes\":[" + JsonUtil.objToJson(update) + "],\"edges\":[]}", "ab-v1");

        Assert.assertEquals("ab-v1", service.status("test1").get("version"));
        Assert.assertNotEquals(update.getTimeout(), recService.getGraphConfig().getNodes().get(0).getTimeout());
    }
}
