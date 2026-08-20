package com.openrec.graph.node;

import static com.openrec.graph.RecParams.SCENE;

import java.util.List;

import org.assertj.core.util.Lists;

import com.openrec.graph.GraphContext;
import com.openrec.graph.config.I2iConfig;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.tools.anno.Export;
import com.openrec.graph.tools.anno.Import;
import com.openrec.proto.model.ScoreResult;
import com.openrec.service.recall.RecallStore;
import com.openrec.util.BeanUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class I2iNode extends SyncNode<I2iConfig> {

    private RecallStore recallStore = BeanUtil.getBean(RecallStore.class);

    @Import("triggerItems")
    private List<ScoreResult> triggerItems;

    @Export("i2iItems")
    private List<ScoreResult> i2iItems;

    public I2iNode(NodeConfig nodeConfig) {
        super(nodeConfig);
        this.i2iItems = Lists.newArrayList();
    }

    @Override
    public void run(GraphContext context) {

        String scene = context.getParams().getValueToString(SCENE);
        boolean open = config.isOpen();

        if (!open) {
            log.info("{} not open, just return", getName());
            return;
        }

        int size = config.getContent().getSize();

        List<String> triggers = new java.util.ArrayList<>();
        for (ScoreResult trigger : triggerItems) {
            triggers.add(trigger.getId());
        }
        i2iItems = recallStore.i2i(scene, triggers, size);
        log.info("{} with i2i size:{}", getName(), i2iItems.size());
    }
}
