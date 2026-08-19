package com.openrec.graph;

import com.google.gson.Gson;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.config.NodeConfigTool;
import com.openrec.graph.config.NodeConfigType;
import org.junit.Test;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.*;

public class GraphCoreTypesTest {
    @Test
    public void graphParamsSupportsEveryTypedValueAndDefaults() {
        GraphParams params = new GraphParams();
        assertEquals("", params.getValueToString("missing"));
        assertEquals(0, params.getValueToInt("missing"));
        assertFalse(params.getValueToBool("missing"));
        assertEquals(0d, params.getValueToDouble("missing"), 0d);
        assertNull(params.getValueToList("missing"));
        assertNull(params.getValueToSet("missing"));
        assertNull(params.getValueToMap("missing"));

        params.put("string", "value");
        params.put("int", 3);
        params.put("bool", true);
        params.put("double", 2.5d);
        params.put("list", Arrays.asList("a", "b"));
        params.put("set", new HashSet<>(Collections.singletonList("a")));
        params.put("map", new HashMap<String, Integer>() {{ put("a", 1); }});
        assertEquals("value", params.getValueToString("string"));
        assertEquals(3, params.getValueToInt("int"));
        assertTrue(params.getValueToBool("bool"));
        assertEquals(2.5d, params.getValueToDouble("double"), 0d);
        assertEquals(2, params.getValueToList("list").size());
        assertEquals(1, params.getValueToSet("set").size());
        assertEquals(1, params.getValueToMap("map").size());
        assertEquals(7, params.size());
        params.clear();
        assertEquals(0, params.size());
    }

    @Test
    public void nodeConfigTypesAndGeneratedMethodsWork() {
        NodeConfig<String> config = new NodeConfig<>();
        config.setName("node");
        config.setOpen(true);
        config.setTimeout(123);
        config.setClazz("clazz");
        config.setConfigClazz("configClazz");
        config.setContent("content");
        assertEquals("node", config.getName());
        assertTrue(config.isOpen());
        assertEquals(123, config.getTimeout());
        assertEquals("clazz", config.getClazz());
        assertEquals("configClazz", config.getConfigClazz());
        assertEquals("content", config.getContent());
        assertNotNull(config.toString());

        NodeConfigType type = new NodeConfigType(String.class);
        assertEquals(NodeConfig.class, type.getRawType());
        assertArrayEquals(new Type[]{String.class}, type.getActualTypeArguments());
        assertNull(type.getOwnerType());
        Type canonicalType = NodeConfigTool.getNodeConfigType(String.class);
        assertTrue(canonicalType instanceof java.lang.reflect.ParameterizedType);
        assertEquals(NodeConfig.class, ((java.lang.reflect.ParameterizedType) canonicalType).getRawType());
        assertNotNull(new Gson().fromJson("{\"content\":\"x\"}", NodeConfigTool.getNodeConfigType(String.class)));
    }

    @Test
    public void graphConfigGeneratedMethodsWork() {
        GraphConfig config = new GraphConfig();
        config.setNodes(Collections.singletonList(new NodeConfig<>()));
        GraphConfig.NodeEdge edge = new GraphConfig.NodeEdge();
        edge.setFrom("from");
        edge.setTo("to");
        config.setEdges(Collections.singletonList(edge));
        assertEquals(1, config.getNodes().size());
        assertEquals("from", config.getEdges().get(0).getFrom());
        assertEquals("to", config.getEdges().get(0).getTo());
        assertNotNull(edge.toString());
        assertNotNull(config.toString());
        assertEquals(config, config);
        assertNotEquals(config, new GraphConfig());
    }

    @Test
    public void engineAcceptsParamsAndGracefullySkipsUnknownNodeClasses() {
        class Params { private String user = "u"; private int size = 2; }
        GraphEngine engine = GraphEngine.getSessionGraphEngine();
        engine.prepare(new Params());
        GraphConfig config = new GraphConfig();
        NodeConfig<Object> unknown = new NodeConfig<>();
        unknown.setName("unknown");
        unknown.setClazz("does.not.Exist");
        config.setNodes(Collections.<NodeConfig>singletonList(unknown));
        config.setEdges(Collections.emptyList());
        engine.buildGraph(config);
        engine.execGraph();
        assertNull(engine.getResult());
        engine.refresh();
    }
}
