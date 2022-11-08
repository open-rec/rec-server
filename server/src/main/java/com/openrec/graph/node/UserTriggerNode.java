package com.openrec.graph.node;

import com.openrec.graph.GraphContext;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.config.UserTriggerConfig;
import com.openrec.graph.tools.anno.Export;
import com.openrec.proto.model.ScoreResult;
import com.openrec.service.redis.RedisService;
import com.openrec.util.BeanUtil;
import com.openrec.util.TimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;

import java.util.List;
import java.util.stream.Collectors;

import static com.openrec.graph.RecParams.*;

@Slf4j
public class UserTriggerNode extends SyncNode<UserTriggerConfig> {
    private RedisService redisService = BeanUtil.getBean(RedisService.class);
    private String bizType = "event";
    private String filterType = "click";
    private String FILTER_KEY_FORMAT = "%s:{%s}:%s:%s";
    @Export("triggerItems")
    private List<ScoreResult> triggerItems;

    public UserTriggerNode(NodeConfig nodeConfig) {
        super(nodeConfig);
        this.triggerItems = Lists.newArrayList();
    }

    @Override
    public void run(GraphContext context) {

        String scene = context.getParams().getValueToString(SCENE);
        String userId = context.getParams().getValueToString(USER_ID);
        List<String> itemIds = context.getParams().getValueToList(ITEM_IDS);
        String key = String.format(FILTER_KEY_FORMAT, bizType, userId, scene, filterType);

        int timeout = config.getTimeout();
        boolean open = config.isOpen();

        if (!open) {
            log.info("{} not open, just return", getName());
            return;
        }

        int size = config.getContent().getSize();
        if (itemIds != null) {
            triggerItems.addAll(itemIds.stream().
                    map(i -> new ScoreResult(i, TimeUtil.nowSecs()))
                    .collect(Collectors.toList()));
        }
        triggerItems.addAll(redisService.getZSet(key, 0, Double.MAX_VALUE, size));
        log.info("{} with trigger size:{}", getName(), triggerItems.size());
    }
}
