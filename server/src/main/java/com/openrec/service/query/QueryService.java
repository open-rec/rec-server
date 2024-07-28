package com.openrec.service.query;

import java.util.LinkedHashMap;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Autowired
    private ObjectMapper objectMapper;

    public Item queryItem(String id) {
        String itemKey = String.format(ITEM_KEY, id);
        return queryJsonData(itemKey, Item.class);
    }

    public User queryUser(String id) {
        String userKey = String.format(USER_KEY, id);
        return queryJsonData(userKey, User.class);
    }

    public <T> T queryJsonData(String key, Class<T> objClass) {
        LinkedHashMap data = redisService.getJsonV(key);
        return objectMapper.convertValue(data, objClass);
    }
    public List<ScoreResult> queryEvent(String userId, String scene, String type) {
        String eventKey = String.format(EVENT_KEY, userId, scene, type);
        return redisService.getZSet(eventKey, 0, Double.MAX_VALUE,
            Integer.MAX_VALUE);
    }
}
