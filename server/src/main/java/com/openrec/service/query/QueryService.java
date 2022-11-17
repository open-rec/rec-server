package com.openrec.service.query;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.openrec.proto.model.Item;
import com.openrec.proto.model.ScoreResult;
import com.openrec.proto.model.User;
import com.openrec.service.redis.RedisService;

@Service
public class QueryService {

    private String USER_KEY = "user:{%s}";
    private String ITEM_KEY = "item:{%s}";
    private String EVENT_KEY = "event:{%s}:%s:%s";

    @Autowired
    private RedisService redisService;

    public Item queryItem(String id) {
        return redisService.getV(String.format(ITEM_KEY, id));
    }

    public User queryUser(String id) {
        return redisService.getV(String.format(USER_KEY, id));
    }

    public List<ScoreResult> queryEvent(String userId, String scene, String type) {
        return redisService.getZSet(String.format(EVENT_KEY, userId, scene, type), 0, Double.MAX_VALUE,
            Integer.MAX_VALUE);
    }
}
