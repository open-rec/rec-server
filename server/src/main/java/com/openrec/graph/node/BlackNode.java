package com.openrec.graph.node;

import com.openrec.graph.GraphContext;
import com.openrec.graph.RecEventType;
import com.openrec.graph.config.FilterConfig;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.tools.anno.Export;
import com.openrec.service.redis.RedisService;
import com.openrec.util.BeanUtil;
import com.openrec.util.TimeUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.stream.Collectors;

import static com.openrec.graph.RecParams.SCENE;
import static com.openrec.graph.RecParams.USER_ID;

@Slf4j
public class BlackNode extends SyncNode<FilterConfig> {

    private RedisService redisService = BeanUtil.getBean(RedisService.class);
    private String bizType = "black";
    private String FILTER_KEY_FORMAT = "%s:{%s}";
    @Export("blackItemSet")
    private Set<String> blackItemSet;


    public BlackNode(NodeConfig nodeConfig) {
        super(nodeConfig);
    }

    @Override
    public void run(GraphContext context) {

        String scene = context.getParams().getValueToString(SCENE);
        String key = String.format(FILTER_KEY_FORMAT, bizType, scene);

        int timeout = config.getTimeout();
        boolean open = config.isOpen();

        if (!open) {
            log.info("{} not open, just return", getName());
            return;
        }

        blackItemSet = redisService.getZSet(key, 0, Double.MAX_VALUE, Integer.MAX_VALUE)
                .stream().map(i -> i.getId()).collect(Collectors.toSet());
        log.info("{} with black item size:{}", getName(), blackItemSet.size());
    }
}
