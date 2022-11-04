package com.openrec.service.redis;

import com.openrec.proto.model.ScoreResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.*;
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

    public void addZSets(String key, Map<String, Double> valueScoreSet) {
        Set<ZSetOperations.TypedTuple<String>> tupleSet = new HashSet<>();
        for (Map.Entry<String, Double> entry : valueScoreSet.entrySet()) {
            tupleSet.add(new DefaultTypedTuple<>(entry.getKey(), entry.getValue()));
        }
        redisTemplate.opsForZSet().add(key, tupleSet);
    }

    public List<ScoreResult> getZSet(String key, double scoreMin, double scoreMax, int size) {
        Set<ZSetOperations.TypedTuple<String>> tupleSet = redisTemplate.opsForZSet()
                .rangeByScoreWithScores(key, scoreMin, scoreMax, 0, size);
        return tupleSet.stream().map(t -> new ScoreResult(t.getValue(), t.getScore()))
                .collect(Collectors.toList());
    }

    public List<ScoreResult> getZSet(List<String> keys, double scoreMin, double scoreMax, int size) {

        String tmpKey = "tmp_" + UUID.randomUUID().toString();
        redisTemplate.opsForZSet().unionAndStore(tmpKey, keys, tmpKey);
        Set<ZSetOperations.TypedTuple<String>> tupleSet = redisTemplate.opsForZSet()
                .rangeByScoreWithScores(tmpKey, scoreMin, scoreMax, 0, size);
        List<ScoreResult> mergeResult = tupleSet.stream().map(t -> new ScoreResult(t.getValue(), t.getScore()))
                .collect(Collectors.toList());
        redisTemplate.delete(tmpKey);
        return mergeResult;
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

    public void removeKs(List<String> keys) {
        redisTemplate.delete(keys);
    }

    public List<Object> getVs(List<String> keys) {
        return redisTemplate.opsForValue().multiGet(keys);
    }
}
