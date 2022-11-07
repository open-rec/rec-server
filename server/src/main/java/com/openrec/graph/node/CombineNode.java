package com.openrec.graph.node;

import com.openrec.graph.GraphContext;
import com.openrec.graph.config.CombineConfig;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.tools.anno.Export;
import com.openrec.graph.tools.anno.Import;
import com.openrec.proto.model.ScoreResult;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;

import java.util.List;
import java.util.Set;

@Slf4j
public class CombineNode extends SyncNode<CombineConfig> {
    @Import("i2iItems")
    private List<ScoreResult> i2iItems;

    @Import("newItems")
    private List<ScoreResult> newItems;

    @Import("hotItems")
    private List<ScoreResult> hotItems;

    @Import("filterItemSet")
    private Set<String> filterItemSet;

    @Export("combineItems")
    private List<ScoreResult> combineItems;

    public CombineNode(NodeConfig nodeConfig) {
        super(nodeConfig);
    }


    @Override
    public void run(GraphContext context) {

        List<ScoreResult> allItems = Lists.newArrayList();
        allItems.addAll(i2iItems);
        allItems.addAll(hotItems);
        allItems.addAll(newItems);

        int size = config.getContent().getSize();
        combineItems = Lists.newArrayList();
        for (int i = 0, count = 0; i < allItems.size() && count < size; i++) {
            ScoreResult scoreItem = allItems.get(i);
            if (!filterItemSet.contains(scoreItem.getId())) {
                combineItems.add(scoreItem);
                count++;
            }
        }
        log.info("{} with result size:{}", getName(), combineItems.size());
    }
}
