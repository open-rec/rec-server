package com.openrec.graph.node;

import static com.openrec.graph.RecParams.SCENE;

import java.util.List;

import org.assertj.core.util.Lists;

import com.openrec.graph.GraphContext;
import com.openrec.graph.config.HotConfig;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.tools.anno.Export;
import com.openrec.proto.model.ScoreResult;
import com.openrec.service.redis.RedisService;
import com.openrec.util.BeanUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HotNode extends SyncNode<HotConfig> {
    private RedisService redisService = BeanUtil.getBean(RedisService.class);
    private String bizType = "hot";
    private String FILTER_KEY_FORMAT = "%s:{%s}";
    @Export("hotItems")
    private List<ScoreResult> hotItems;

    public HotNode(NodeConfig nodeConfig) {
        super(nodeConfig);
        this.hotItems = Lists.newArrayList();
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

        int size = config.getContent().getSize();

        hotItems = redisService.getZSet(key, 0, Double.MAX_VALUE, size);
        log.info("{} with hot item size:{}", getName(), hotItems.size());
    }
}
