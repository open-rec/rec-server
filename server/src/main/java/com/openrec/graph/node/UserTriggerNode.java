package com.openrec.graph.node;

import com.openrec.graph.GraphContext;
import com.openrec.graph.RecEventType;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.config.UserTriggerConfig;
import com.openrec.graph.tools.anno.Export;
import com.openrec.proto.model.ScoreResult;
import com.openrec.service.redis.RedisService;
import com.openrec.util.BeanUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static com.openrec.graph.RecParams.SCENE_ID;
import static com.openrec.graph.RecParams.USER_ID;

@Slf4j
public class UserTriggerNode extends SyncNode<UserTriggerConfig> {
    public UserTriggerNode(NodeConfig nodeConfig) {
        super(nodeConfig);
    }

    private RedisService redisService = BeanUtil.getBean(RedisService.class);
    private String bizType = "event";
    private String filterType = "click";
    private String FILTER_KEY_FORMAT = "%s:%s:%s:{%s}";


    @Export("triggerItems")
    private List<ScoreResult> triggerItems;


    @Override
    public void run(GraphContext context) {

        String sceneId = context.getParams().getValueToString(SCENE_ID);
        String userId = context.getParams().getValueToString(USER_ID);
        String key = String.format(FILTER_KEY_FORMAT, bizType, sceneId, filterType, userId);

        int timeout = config.getTimeout();
        boolean open = config.isOpen();

        if (!open) {
            log.info("{} not open, just return", getName());
            return;
        }

        int size = config.getContent().getSize();

        triggerItems = redisService.getZSet(key, 0, Double.MAX_VALUE, size);
        log.info("{} with trigger size:{}", getName(), triggerItems.size());
    }
}
