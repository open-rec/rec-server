package com.openrec.graph.node;

import static com.openrec.graph.RecParams.SCENE;

import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.util.Lists;

import com.openrec.graph.GraphContext;
import com.openrec.graph.config.I2iConfig;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.tools.anno.Export;
import com.openrec.graph.tools.anno.Import;
import com.openrec.proto.model.ScoreResult;
import com.openrec.service.redis.RedisService;
import com.openrec.util.BeanUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class I2iNode extends SyncNode<I2iConfig> {

    private RedisService redisService = BeanUtil.getBean(RedisService.class);
    private String bizType = "i2i";
    private String FILTER_KEY_FORMAT = "%s:{%s}:%s";

    @Import("triggerItems")
    private List<ScoreResult> triggerItems;

    @Export("i2iItems")
    private List<ScoreResult> i2iItems;

    public I2iNode(NodeConfig nodeConfig) {
        super(nodeConfig);
        this.i2iItems = Lists.newArrayList();
    }

    @Override
    public void run(GraphContext context) {

        String scene = context.getParams().getValueToString(SCENE);
        List<String> keys =
            triggerItems.stream().map(i -> String.format(String.format(FILTER_KEY_FORMAT, bizType, i.getId(), scene)))
                .collect(Collectors.toList());

        int timeout = config.getTimeout();
        boolean open = config.isOpen();

        if (!open) {
            log.info("{} not open, just return", getName());
            return;
        }

        int size = config.getContent().getSize();

        i2iItems = redisService.getZSet(keys, 0, Double.MAX_VALUE, size);
        log.info("{} with i2i size:{}", getName(), i2iItems.size());
    }
}
