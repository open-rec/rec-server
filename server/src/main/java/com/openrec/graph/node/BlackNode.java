package com.openrec.graph.node;

import java.util.Set;

import com.google.common.collect.Sets;
import com.openrec.graph.GraphContext;
import com.openrec.graph.config.FilterConfig;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.tools.anno.Export;
import com.openrec.service.redis.RedisService;
import com.openrec.util.BeanUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BlackNode extends SyncNode<FilterConfig> {

    private RedisService redisService = BeanUtil.getBean(RedisService.class);
    private String bizType = "black";
    private String BLACK_KEY_FORMAT = "%s";
    @Export("blackItemSet")
    private Set<String> blackItemSet;

    public BlackNode(NodeConfig nodeConfig) {
        super(nodeConfig);
        this.blackItemSet = Sets.newHashSet();
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

        blackItemSet = redisService.getSet(key);
        log.info("{} with black item size:{}", getName(), blackItemSet.size());
    }
}
