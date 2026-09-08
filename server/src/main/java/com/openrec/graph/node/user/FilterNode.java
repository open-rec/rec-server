package com.openrec.graph.node.user;

import static com.openrec.graph.RecParams.SCENE;
import static com.openrec.graph.RecParams.USER_ID;

import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import com.openrec.graph.GraphContext;
import com.openrec.graph.RecEventType;
import com.openrec.graph.config.FilterConfig;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.node.AbstractSyncNode;
import com.openrec.graph.tools.anno.Export;
import com.openrec.proto.model.ScoreResult;
import com.openrec.service.redis.RedisService;
import com.openrec.util.BeanUtil;
import com.openrec.util.TimeUtil;

/** Filters users already exposed by a user-recommendation scene. */
public class FilterNode extends AbstractSyncNode<FilterConfig> {
    @Export("filterUserSet")
    private Set<String> filterUserSet = Sets.newHashSet();
    private RedisService redisService = BeanUtil.getBean(RedisService.class);

    public FilterNode(NodeConfig nodeConfig) {
        super(nodeConfig);
    }

    @Override
    public void run(GraphContext context) {
        if (!config.isOpen())
            return;
        FilterConfig.TypeFilterConfig filter = config.getContent().getFilterMap().get(RecEventType.EXPOSE.toString());
        long now = TimeUtil.nowSecs();
        String key = String.format("user-event:{%s}:%s:expose", context.getParams().getValueToString(USER_ID),
            context.getParams().getValueToString(SCENE));
        filterUserSet = redisService.getZSet(key, now - filter.getDuration(), now, filter.getSize()).stream()
            .map(ScoreResult::getId).collect(Collectors.toSet());
    }
}
