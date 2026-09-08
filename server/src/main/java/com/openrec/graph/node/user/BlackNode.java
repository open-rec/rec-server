package com.openrec.graph.node.user;

import java.util.Set;

import com.google.common.collect.Sets;
import com.openrec.graph.GraphContext;
import com.openrec.graph.config.FilterConfig;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.node.AbstractSyncNode;
import com.openrec.graph.tools.anno.Export;
import com.openrec.service.redis.RedisService;
import com.openrec.util.BeanUtil;

/** Reads user identifiers from the dedicated global user blacklist. */
public class BlackNode extends AbstractSyncNode<FilterConfig> {
    @Export("blackUserSet")
    private Set<String> blackUserSet = Sets.newHashSet();
    private RedisService redisService = BeanUtil.getBean(RedisService.class);

    public BlackNode(NodeConfig nodeConfig) {
        super(nodeConfig);
    }

    @Override
    public void run(GraphContext context) {
        if (!config.isOpen())
            return;
        Set<String> values = redisService.getSet("user:black");
        blackUserSet = values == null ? Sets.newHashSet() : Sets.newHashSet(values);
    }
}
