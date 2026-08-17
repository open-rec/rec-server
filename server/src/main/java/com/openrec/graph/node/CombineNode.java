package com.openrec.graph.node;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.assertj.core.util.Lists;

import com.openrec.graph.GraphContext;
import com.openrec.graph.config.CombineConfig;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.tools.anno.Export;
import com.openrec.graph.tools.anno.Import;
import com.openrec.proto.model.ScoreResult;

import lombok.extern.slf4j.Slf4j;

/**
 * Merges the recall channels into one candidate list, dropping anything filtered, blacklisted or
 * already used as a trigger.
 * <p>
 * Channels are merged in a fixed order and de-duplicated: an item surfaced by more than one channel
 * is kept once, attributed to the first channel that produced it. Without that, the same item takes
 * several slots of the size budget and reaches the client more than once.
 */
@Slf4j
public class CombineNode extends SyncNode<CombineConfig> {

    static final String CHANNEL_I2I = "i2i";
    static final String CHANNEL_EMBEDDING = "embedding";
    static final String CHANNEL_HOT = "hot";
    static final String CHANNEL_NEW = "new";

    @Import("i2iItems")
    private List<ScoreResult> i2iItems;

    @Import("embeddingItems")
    private List<ScoreResult> embeddingItems;

    @Import("newItems")
    private List<ScoreResult> newItems;

    @Import("hotItems")
    private List<ScoreResult> hotItems;

    @Import("filterItemSet")
    private Set<String> filterItemSet;

    @Import("blackItemSet")
    private Set<String> blackItemSet;

    @Import("triggerItems")
    private List<ScoreResult> triggerItems;

    @Export("combineItems")
    private List<ScoreResult> combineItems;

    public CombineNode(NodeConfig nodeConfig) {
        super(nodeConfig);
        this.combineItems = Lists.newArrayList();
    }

    @Override
    public void run(GraphContext context) {

        int size = config.getContent().getSize();
        Set<String> triggerItemSet = triggerItems.stream().map(ScoreResult::getId).collect(Collectors.toSet());

        // insertion-ordered, so the channel order below doubles as the tie-break for duplicates
        Map<String, ScoreResult> candidates = new LinkedHashMap<>();
        int[] counters = new int[] {0, 0, 0};   // filtered, blacklisted, trigger

        collect(i2iItems, CHANNEL_I2I, candidates, triggerItemSet, counters);
        collect(embeddingItems, CHANNEL_EMBEDDING, candidates, triggerItemSet, counters);
        collect(hotItems, CHANNEL_HOT, candidates, triggerItemSet, counters);
        collect(newItems, CHANNEL_NEW, candidates, triggerItemSet, counters);

        combineItems = Lists.newArrayList();
        for (ScoreResult candidate : candidates.values()) {
            if (combineItems.size() >= size) {
                break;
            }
            combineItems.add(candidate);
        }

        log.info(
            "{} with result size:{}, candidates:{}, filter count:{}, black count:{}, trigger count:{}",
            getName(), combineItems.size(), candidates.size(), counters[0], counters[1], counters[2]);
    }

    private void collect(List<ScoreResult> items, String channel, Map<String, ScoreResult> candidates,
        Set<String> triggerItemSet, int[] counters) {
        if (items == null) {
            return;
        }
        for (ScoreResult item : items) {
            String id = item.getId();
            if (filterItemSet.contains(id)) {
                counters[0]++;
                continue;
            }
            if (blackItemSet.contains(id)) {
                counters[1]++;
                continue;
            }
            if (triggerItemSet.contains(id)) {
                counters[2]++;
                continue;
            }
            if (candidates.containsKey(id)) {
                continue;
            }
            item.setRecallFrom(channel);
            candidates.put(id, item);
        }
    }
}
