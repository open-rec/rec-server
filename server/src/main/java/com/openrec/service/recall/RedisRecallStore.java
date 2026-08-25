package com.openrec.service.recall;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.openrec.proto.model.ScoreResult;
import com.openrec.service.redis.RedisService;

/**
 * Redis recall implementation retained for development and correctness comparison only.
 * It stores mutable sorted sets in place and therefore cannot stage or atomically switch versions.
 */
@Service
@ConditionalOnProperty(name = "recall.store", havingValue = "redis", matchIfMissing = true)
public class RedisRecallStore implements RecallStore {

    private static final String SCENE_KEY = "%s:{%s}";
    private static final String I2I_KEY = "%s:{%s}:%s";
    private static final String U2I_KEY = "%s:{%s}:%s";

    @Autowired
    private RedisService redisService;

    @Override
    public List<ScoreResult> hot(String tableName, String scene, int size) {
        return redisService.getZSet(String.format(SCENE_KEY, tableName, scene), 0, Double.MAX_VALUE, size);
    }

    @Override
    public List<ScoreResult> newest(
        String tableName, String scene, long startTime, long endTime, int size) {
        List<ScoreResult> result = redisService.getZSet(
            String.format(SCENE_KEY, tableName, scene), startTime, endTime, size);
        if (endTime > 0) {
            result.forEach(item -> item.setScore(item.getScore() / endTime));
        }
        return result;
    }

    @Override
    public List<ScoreResult> i2i(
        String tableName, String scene, List<String> triggerItems, int size) {
        if (triggerItems == null || triggerItems.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> keys = new java.util.ArrayList<>();
        for (String trigger : triggerItems) {
            keys.add(String.format(I2I_KEY, tableName, trigger, scene));
        }
        return redisService.getZSet(keys, 0, Double.MAX_VALUE, size);
    }

    @Override
    public List<ScoreResult> u2i(String tableName, String scene, String userId, int size) {
        if (userId == null || userId.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return redisService.getZSet(
            String.format(U2I_KEY, tableName, userId, scene), 0, Double.MAX_VALUE, size);
    }

    @Override
    public List<ScoreResult> embedding(
        String tableName, String scene, List<String> triggerItems, int size, long timeoutMillis) {
        return Collections.emptyList();
    }
}
