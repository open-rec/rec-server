package com.openrec.graph.node.user;

import com.openrec.graph.GraphContext;
import com.openrec.graph.RecParams;
import com.openrec.service.redis.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import com.openrec.graph.RequestTrigger;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.config.UserTriggerConfig;
import com.openrec.graph.node.AbstractSyncNode;
import com.openrec.graph.tools.anno.Export;

/** Supplies shared request triggers and the seed for user-to-user recall. */
public class TriggerNode extends AbstractSyncNode<UserTriggerConfig> {
    @Autowired
    private RedisService redisService;

    @Export(RequestTrigger.KEY)
    private RequestTrigger requestTrigger;

    public TriggerNode(NodeConfig nodeConfig) {
        super(nodeConfig);
    }

    @Override
    public void run(GraphContext context) {
        requestTrigger = RequestTrigger.fromParams(context);
        String scene = context.getParams().getValueToString(RecParams.SCENE);
        if (!config.isOpen() || requestTrigger.triggerSelf().isEmpty() || scene == null || scene.isBlank()
            || config.getContent().getSize() <= 0) {
            return;
        }
        String key = String.format("user-event:{%s}:%s:click", requestTrigger.triggerSelf(), scene);
        requestTrigger = requestTrigger
            .withUsers(redisService.getZSet(key, 0, Double.MAX_VALUE, config.getContent().getSize()).stream()
                .filter(user -> user.getId() != null && !user.getId().isBlank()
                    && !user.getId().equals(requestTrigger.triggerSelf()))
                .map(user -> new RequestTrigger.TriggerUser(user.getId(), user.getScore())).toList());
    }
}
