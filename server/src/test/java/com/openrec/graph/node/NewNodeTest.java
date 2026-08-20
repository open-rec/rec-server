package com.openrec.graph.node;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.openrec.graph.GraphContext;
import com.openrec.graph.config.NewConfig;
import com.openrec.graph.config.NodeConfig;
import com.openrec.proto.model.ScoreResult;
import com.openrec.service.recall.RecallStore;

public class NewNodeTest {

    @Test
    public void run() {
        NewConfig newConfig = new NewConfig();

        GraphContext context = new GraphContext();
        context.addParam("scene", "scene-1");

        NodeConfig<NewConfig> nodeConfig = new NodeConfig<>();
        nodeConfig.setContent(newConfig);
        nodeConfig.setOpen(false);
        context.addConfig("new", nodeConfig);

        NewNode newNode = new NewNode(nodeConfig);
        newNode.run(context);
    }

    @Test
    public void normalizesTimestampDomainScoreBeforeExport() {
        NewConfig content = new NewConfig();
        content.setDuration(86400);
        content.setSize(10);
        NodeConfig<NewConfig> config = new NodeConfig<>();
        config.setName("new");
        config.setContent(content);
        config.setOpen(true);

        RecallStore recallStore = mock(RecallStore.class);
        when(recallStore.newest(eq("scene-1"), anyLong(), anyLong(), eq(10)))
            .thenAnswer(invocation -> {
                return Collections.singletonList(new ScoreResult("item-1", 0.75));
            });

        NewNode node = new NewNode(config);
        ReflectionTestUtils.setField(node, "recallStore", recallStore);
        GraphContext context = new GraphContext();
        context.addParam("scene", "scene-1");

        node.run(context);
        context.exportNodeData(node);

        List<ScoreResult> result = (List<ScoreResult>)context.getData("newItems");
        assertEquals(0.75, result.get(0).getScore(), 0.000001);
    }
}
