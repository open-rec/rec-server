package com.openrec.graph.node;

import static com.openrec.graph.RecParams.SCENE;
import static com.openrec.graph.RecParams.USER_ID;

import java.util.Set;

import com.google.common.collect.Sets;
import com.openrec.graph.GraphContext;
import com.openrec.graph.RecEventType;
import com.openrec.graph.config.FilterConfig;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.tools.anno.Export;
import com.openrec.service.redis.RedisService;
import com.openrec.util.BeanUtil;
import com.openrec.util.TimeUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BlackNode extends SyncNode<FilterConfig> {

    private RedisService redisService = BeanUtil.getBean(RedisService.class);
    private String bizType = "black";
    private String BLACK_KEY_FORMAT = "%s";
    private String DISLIKE_KEY_FORMAT = "event:{%s}:%s:dislike";
    @Export("blackItemSet")
    private Set<String> blackItemSet;
    @Export("blackCategorySet")
    private Set<String> blackCategorySet;
    @Export("blackTagSet")
    private Set<String> blackTagSet;

    public BlackNode(NodeConfig nodeConfig) {
        super(nodeConfig);
        this.blackItemSet = Sets.newHashSet();
        this.blackCategorySet = Sets.newHashSet();
        this.blackTagSet = Sets.newHashSet();
    }

    @Override
    public void run(GraphContext context) {
        String key = String.format(BLACK_KEY_FORMAT, bizType);

        int timeout = config.getTimeout();
        boolean open = config.isOpen();

        if (!open) {
            log.info("{} not open, just return", getName());
            return;
        }

        Set<String> globalBlackItems = redisService.getSet(key);
        blackItemSet = globalBlackItems == null ? Sets.newHashSet() : Sets.newHashSet(globalBlackItems);
        blackCategorySet = Sets.newHashSet();
        blackTagSet = Sets.newHashSet();

        FilterConfig.TypeFilterConfig dislike = config.getContent() == null
            || config.getContent().getFilterMap() == null ? null
            : config.getContent().getFilterMap().get(RecEventType.DISLIKE.toString());
        if (dislike != null) {
            String userId = context.getParams().getValueToString(USER_ID);
            String scene = context.getParams().getValueToString(SCENE);
            long nowSecs = TimeUtil.nowSecs();
            redisService.getZSet(String.format(DISLIKE_KEY_FORMAT, userId, scene),
                nowSecs - dislike.getDuration(), nowSecs, dislike.getSize()).forEach(rule -> addRule(rule.getId()));
        }
        log.info("{} with black item size:{}, category size:{}, tag size:{}", getName(),
            blackItemSet.size(), blackCategorySet.size(), blackTagSet.size());
    }

    private void addRule(String rule) {
        if (rule == null) { return; }
        if (rule.startsWith("id:")) { blackItemSet.add(rule.substring(3)); }
        else if (rule.startsWith("category:")) { blackCategorySet.add(rule.substring(9)); }
        else if (rule.startsWith("tag:")) { blackTagSet.add(rule.substring(4)); }
    }
}
