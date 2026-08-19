package com.openrec.graph;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.openrec.graph.node.SyncNode;
import com.openrec.graph.tools.anno.Export;
import com.openrec.graph.tools.anno.Import;

public class GraphContextTest {

    private static final String TEST_STRING = "hello";
    private static final String TEST_STRING_KEY = "greet";
    private GraphContext graphContext;

    @Before
    public void setUp() throws Exception {
        this.graphContext = new GraphContext();
    }

    @Test
    public void testNodeData() {
        ExportNode exportNode = new ExportNode();
        exportNode.run(null);
        graphContext.exportNodeData(exportNode);
        Assert.assertEquals(graphContext.getData(TEST_STRING_KEY), TEST_STRING);

        ImportNode importNode = new ImportNode();
        graphContext.importNodeData(importNode);
        importNode.run(null);
        Assert.assertEquals(importNode.getStr(), TEST_STRING);

        graphContext.addParam("size", 3);
        Assert.assertEquals(3, graphContext.getParams().getValueToInt("size"));
        graphContext.addConfig("node", new com.openrec.graph.config.NodeConfig());
        Assert.assertNotNull(graphContext.getConfig("node"));
        graphContext.setResult("result");
        Assert.assertEquals("result", graphContext.getResult());
        graphContext.clean();
        Assert.assertEquals(0, graphContext.getParams().size());
    }

    static class ExportNode extends SyncNode {

        @Export(TEST_STRING_KEY)
        private String str;

        @Override
        public void run(GraphContext context) {
            this.str = TEST_STRING;
        }
    }

    static class ImportNode extends SyncNode {

        @Import(TEST_STRING_KEY)
        private String str;

        @Override
        public void run(GraphContext context) {
            System.out.printf("import str data: %s\n", str);

        }

        public String getStr() {
            return str;
        }
    }
}
