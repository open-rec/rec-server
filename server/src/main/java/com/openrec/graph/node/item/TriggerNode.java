package com.openrec.graph.node.item;

import com.openrec.graph.node.*;

import static com.openrec.graph.RecParams.*;

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

import com.openrec.graph.GraphContext;
import com.openrec.graph.RequestTrigger;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.config.UserTriggerConfig;
import com.openrec.graph.tools.anno.Export;
import com.openrec.service.redis.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import com.openrec.util.TimeUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TriggerNode extends AbstractSyncNode<UserTriggerConfig> {
    @Autowired
    private RedisService redisService;
    private String bizType = "event";
    private String filterType = "click";
    private String FILTER_KEY_FORMAT = "%s:{%s}:%s:%s";

    @Export(RequestTrigger.KEY)
    private RequestTrigger requestTrigger;

    public TriggerNode(NodeConfig nodeConfig) {
        super(nodeConfig);
    }

    @Override
    public void run(GraphContext context) {

        requestTrigger = RequestTrigger.fromParams(context);
        List<RequestTrigger.TriggerItem> triggerItems = Lists.newArrayList();
        String scene = context.getParams().getValueToString(SCENE);
        String userId = requestTrigger.triggerSelf();
        List<String> itemIds = context.getParams().getValueToList(ITEM_IDS);
        String key = String.format(FILTER_KEY_FORMAT, bizType, userId, scene, filterType);

        boolean open = config.isOpen();

        if (!open) {
            log.info("{} not open, just return", getName());
            return;
        }

        int size = config.getContent().getSize();
        if (itemIds != null) {
            triggerItems.addAll(itemIds.stream().filter(i -> i != null && !i.isBlank())
                .map(i -> new RequestTrigger.TriggerItem(i, TimeUtil.nowSecs())).collect(Collectors.toList()));
        }
        if (!userId.isEmpty() && scene != null && !scene.isBlank()) {
            triggerItems.addAll(redisService.getZSet(key, 0, Double.MAX_VALUE, size).stream()
                .map(item -> new RequestTrigger.TriggerItem(item.getId(), item.getScore()))
                .collect(Collectors.toList()));
        }
        requestTrigger = requestTrigger.withItems(triggerItems);
        log.info("{} with trigger size:{}", getName(), triggerItems.size());
    }
}
