package com.openrec.graph.node;

import static com.openrec.graph.RecParams.SCENE;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.assertj.core.util.Lists;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openrec.graph.GraphContext;
import com.openrec.graph.config.CombineConfig;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.tools.anno.Export;
import com.openrec.graph.tools.anno.Import;
import com.openrec.proto.model.Item;
import com.openrec.proto.model.ScoreResult;
import com.openrec.service.redis.RedisService;
import com.openrec.util.BeanUtil;
import com.openrec.util.TimeUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * Merges the recall channels into one candidate list, dropping anything filtered, blacklisted or
 * already used as a trigger.
 * <p>
 * Channels are merged in a fixed order and de-duplicated: an item surfaced by more than one channel
 * is kept once, and ranks by the first channel's score. Without that, the same item takes several
 * slots of the size budget and reaches the client more than once.
 * <p>
 * De-duplicating does not discard the other channels though — each contribution is recorded in
 * {@link ScoreResult#getRecallScores()}, so downstream can still see that i2i and hot both produced
 * an item and what each thought of it.
 */
@Slf4j
public class CombineNode extends SyncNode<CombineConfig> {

    static final String CHANNEL_I2I = "item_cf_i2i";
    static final String CHANNEL_EMBEDDING = "item_seq_emb";
    static final String CHANNEL_HOT = "hot";
    static final String CHANNEL_NEW = "new";

    private RedisService redisService = BeanUtil.getBean(RedisService.class);
    private ObjectMapper objectMapper = BeanUtil.getBean(ObjectMapper.class);

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

    @Import("blackCategorySet")
    private Set<String> blackCategorySet;

    @Import("blackTagSet")
    private Set<String> blackTagSet;

    @Import("triggerItems")
    private List<ScoreResult> triggerItems;

    @Export("combineItems")
    private List<ScoreResult> combineItems;

    /** Item attributes used by contrib operation rules after ranking. */
    @Export("operationItemMap")
    private Map<String, Item> operationItemMap;

    public CombineNode(NodeConfig nodeConfig) {
        super(nodeConfig);
        this.combineItems = Lists.newArrayList();
        this.operationItemMap = new LinkedHashMap<>();
    }

    @Override
    public void run(GraphContext context) {

        int size = config.getContent().getSize();
        Set<String> triggerItemSet = triggerItems.stream().map(ScoreResult::getId).collect(Collectors.toSet());

        // insertion-ordered, so the channel order below doubles as the tie-break for duplicates
        Map<String, ScoreResult> candidates = new LinkedHashMap<>();
        int[] counters = new int[] {0, 0, 0, 0};   // exposed, blacklisted, trigger, unavailable

        List<String> recallTypes = config.getContent().getRecallTypes();
        if (recallTypes == null || recallTypes.isEmpty()) {
            // Backward compatibility for serving graphs created before dynamic recall channels.
            collect(i2iItems, CHANNEL_I2I, candidates, triggerItemSet, counters);
            collect(embeddingItems, CHANNEL_EMBEDDING, candidates, triggerItemSet, counters);
            collect(hotItems, CHANNEL_HOT, candidates, triggerItemSet, counters);
            collect(newItems, CHANNEL_NEW, candidates, triggerItemSet, counters);
        } else {
            for (String recallType : recallTypes) {
                @SuppressWarnings("unchecked")
                List<ScoreResult> items = (List<ScoreResult>) context.getData(
                    RecallNode.CHANNEL_PREFIX + recallType);
                collect(items, recallType, candidates, triggerItemSet, counters);
            }
        }

        List<ScoreResult> candidateList = Lists.newArrayList(candidates.values());
        if (candidateList.isEmpty()) {
            combineItems = Lists.newArrayList();
            log.info("{} with empty candidates", getName());
            return;
        }
        List<String> itemKeys = candidateList.stream().map(item -> String.format("item:{%s}", item.getId()))
            .collect(Collectors.toList());
        List<Object> itemValues = redisService.getVs(itemKeys);
        String scene = context.getParams().getValueToString(SCENE);
        long nowSecs = TimeUtil.nowSecs();
        boolean checkExpireTime = config.getContent().isCheckExpireTime();

        combineItems = Lists.newArrayList();
        operationItemMap = new LinkedHashMap<>();
        for (int i = 0; i < candidateList.size(); i++) {
            if (combineItems.size() >= size) {
                break;
            }
            Object value = itemValues == null || i >= itemValues.size() ? null : itemValues.get(i);
            Item item = value == null ? null : objectMapper.convertValue(value, Item.class);
            if (!isAvailable(item, scene, nowSecs, checkExpireTime)) {
                counters[3]++;
                continue;
            }
            if (isNegativeFeedbackMatch(item, blackCategorySet, blackTagSet)) {
                counters[1]++;
                continue;
            }
            combineItems.add(candidateList.get(i));
            operationItemMap.put(item.getId(), item);
        }

        log.info(
            "{} with result size:{}, candidates:{}, filter count:{}, black count:{}, trigger count:{}, unavailable count:{}",
            getName(), combineItems.size(), candidates.size(), counters[0], counters[1], counters[2], counters[3]);
    }

    static boolean isNegativeFeedbackMatch(Item item, Set<String> categories, Set<String> tags) {
        if (item == null) { return false; }
        if (categories != null && categories.contains(item.getCategory())) { return true; }
        if (tags == null || tags.isEmpty() || item.getTags() == null) { return false; }
        for (String tag : item.getTags().split("[,|]")) {
            if (tags.contains(tag.trim())) { return true; }
        }
        return false;
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
            if (blackItemSet != null && blackItemSet.contains(id)) {
                counters[1]++;
                continue;
            }
            if (triggerItemSet.contains(id)) {
                counters[2]++;
                continue;
            }
            ScoreResult existing = candidates.get(id);
            if (existing != null) {
                // Already surfaced by an earlier channel. The earlier score stays the one that
                // ranks, but this channel's contribution is recorded too — which channels agreed on
                // an item, and how strongly, is the point of keeping the breakdown.
                existing.addRecallScore(channel, item.getScore());
                continue;
            }
            item.addRecallScore(channel, item.getScore());
            candidates.put(id, item);
        }
    }

    static boolean isAvailable(Item item, String scene, long nowSecs, boolean checkExpireTime) {
        if (item == null || item.getStatus() == 0 || !Objects.equals(scene, item.getScene())) {
            return false;
        }
        if (!checkExpireTime) {
            return true;
        }
        String expireTime = item.getExpireTime();
        if (expireTime == null || expireTime.trim().isEmpty()) {
            return true;
        }
        try {
            long expireSecs = Long.parseLong(expireTime);
            return expireSecs <= 0 || expireSecs > nowSecs;
        } catch (NumberFormatException e) {
            log.warn("item:{} has invalid expireTime:{}, filtered", item.getId(), expireTime);
            return false;
        }
    }
}
