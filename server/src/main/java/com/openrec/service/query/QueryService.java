package com.openrec.service.query;

import com.openrec.proto.model.Item;
import com.openrec.proto.model.ScoreResult;
import com.openrec.proto.model.User;
import com.openrec.service.redis.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QueryService {

    private String USER_KEY = "user:{%s}";
    private String ITEM_KEY = "item:{%s}";
    private String EVENT_KEY = "event:{%s}";

    @Autowired
    private RedisService redisService;


    public Item queryItem(String id) {
        return redisService.getV(String.format(ITEM_KEY, id));
    }

    public User queryUser(String id) {
        return redisService.getV(String.format(USER_KEY, id));
    }

    public List<ScoreResult> queryEvent(String userId, String scene, String type) {
        return redisService.getZSet(String.format(EVENT_KEY, scene, type, userId),
                0, Double.MAX_VALUE, Integer.MAX_VALUE);
    }
}
