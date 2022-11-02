package com.openrec.service.redis;

import com.openrec.proto.model.ScoreResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RedisService {

    @Autowired
    private RedisTemplate redisTemplate;

    public void addSet(String key, String value) {
        redisTemplate.opsForSet().add(key, value);
    }

    public Set<String> getSet(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    public void addZSet(String key, String value, double score) {
        redisTemplate.opsForZSet().add(key, value, score);
    }

    public List<ScoreResult> getZSet(Object key, double scoreMin, double scoreMax, int size) {
        Set<ZSetOperations.TypedTuple<String>> tupleSet = redisTemplate.opsForZSet()
                .rangeByScoreWithScores(key, scoreMin, scoreMax, 0, size);
        return tupleSet.stream().map(t -> new ScoreResult(t.getValue(), t.getScore()))
                .collect(Collectors.toList());
    }

    public void addKv(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public void addKvs(Map<String, Object> kvs) {
        redisTemplate.opsForValue().multiSet(kvs);
    }

    public Object getV(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void removeK(String key) {
        redisTemplate.delete(key);
    }

    public List<Object> getVs(List<String> keys) {
        return redisTemplate.opsForValue().multiGet(keys);
    }
}
