package com.openrec.graph.node;

import com.openrec.graph.GraphContext;
import com.openrec.graph.config.NewConfig;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.tools.anno.Export;
import com.openrec.proto.model.ScoreResult;
import com.openrec.service.redis.RedisService;
import com.openrec.util.BeanUtil;
import com.openrec.util.TimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;

import java.util.List;

import static com.openrec.graph.RecParams.SCENE;

@Slf4j
public class NewNode extends SyncNode<NewConfig> {

    private RedisService redisService = BeanUtil.getBean(RedisService.class);
    private String bizType = "new";
    private String FILTER_KEY_FORMAT = "%s:{%s}";
    @Export("newItems")
    private List<ScoreResult> newItems;

    public NewNode(NodeConfig nodeConfig) {
        super(nodeConfig);
        this.newItems = Lists.newArrayList();
    }

    @Override
    public void run(GraphContext context) {

        String scene = context.getParams().getValueToString(SCENE);
        String key = String.format(FILTER_KEY_FORMAT, bizType,
                scene);

        int timeout = config.getTimeout();
        boolean open = config.isOpen();

        if (!open) {
            log.info("{} not open, just return", getName());
            return;
        }

        int duration = config.getContent().getDuration();
        int size = config.getContent().getSize();
        long nowSecs = TimeUtil.nowSecs();

        newItems = redisService.getZSet(key, nowSecs - duration, nowSecs, size);
        log.info("{} with new item size:{}", getName(), newItems.size());
    }
}
