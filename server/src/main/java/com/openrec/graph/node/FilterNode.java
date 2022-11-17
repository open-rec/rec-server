package com.openrec.graph.node;

import static com.openrec.graph.RecParams.SCENE;
import static com.openrec.graph.RecParams.USER_ID;

import java.util.Set;
import java.util.stream.Collectors;

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
public class FilterNode extends SyncNode<FilterConfig> {

    private RedisService redisService = BeanUtil.getBean(RedisService.class);
    private String bizType = "event";
    private String filterType = "expose";
    private String FILTER_KEY_FORMAT = "%s:{%s}:%s:%s";
    @Export("filterItemSet")
    private Set<String> exposeItemSet;

    public FilterNode(NodeConfig nodeConfig) {
        super(nodeConfig);
        this.exposeItemSet = Sets.newHashSet();
    }

    @Override
    public void run(GraphContext context) {

        String scene = context.getParams().getValueToString(SCENE);
        String userId = context.getParams().getValueToString(USER_ID);
        String key = String.format(FILTER_KEY_FORMAT, bizType, userId, scene, filterType);

        int timeout = config.getTimeout();
        boolean open = config.isOpen();

        if (!open) {
            log.info("{} not open, just return", getName());
            return;
        }

        int duration = config.getContent().getFilterMap().get(RecEventType.EXPOSE.toString()).getDuration();
        int size = config.getContent().getFilterMap().get(RecEventType.EXPOSE.toString()).getSize();
        long nowSecs = TimeUtil.nowSecs();

        exposeItemSet = redisService.getZSet(key, nowSecs - duration, nowSecs, size).stream().map(i -> i.getId())
            .collect(Collectors.toSet());
        log.info("{} with expose item size:{}", getName(), exposeItemSet.size());
    }
}
