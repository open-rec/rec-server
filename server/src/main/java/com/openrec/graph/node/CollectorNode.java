package com.openrec.graph.node;

import com.google.common.collect.Maps;
import com.openrec.graph.GraphContext;
import com.openrec.graph.config.NodeConfig;
import com.openrec.graph.tools.anno.Import;
import com.openrec.proto.model.ScoreResult;
import com.openrec.service.redis.RedisService;
import com.openrec.util.BeanUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.openrec.graph.RecParams.*;

@Slf4j
public class CollectorNode extends SyncNode<Void> {



    @Import("operationItems")
    private List<ScoreResult> finalItems;

    private RedisService redisService = BeanUtil.getBean(RedisService.class);

    public CollectorNode(NodeConfig nodeConfig) {
        super(nodeConfig);
    }

    private void writeFakeExpose(GraphContext context, List<ScoreResult> finalItems) {
        String scene = context.getParams().getValueToString(SCENE);
        String type = "expose";
        String biz = "event";
        String userId = context.getParams().getValueToString(USER_ID);
        String fakeExposeKey = String.format("%s:{%s}:%s:%s", biz, userId, scene, type);
        Map<String, Double> fakeExposeMap = Maps.newHashMap();
        finalItems.stream().forEach(si-> fakeExposeMap.put(si.getId(), (double) (System.currentTimeMillis()/1000)));
        if(!CollectionUtils.isEmpty(fakeExposeMap)) {
            redisService.addZSets(fakeExposeKey, fakeExposeMap);
        }
        log.info("write fake expose size:{}", finalItems.size());
    }


    @Override
    public void run(GraphContext context) {
        finalItems = finalItems.subList(0, Math.min(finalItems.size(), context.getParams().getValueToInt(SIZE)));
        context.setResult(finalItems);

        writeFakeExpose(context, finalItems);
        log.info("{} return with final item size:{}", getName(), finalItems.size());
    }
}
