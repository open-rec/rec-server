package com.openrec.service.redis;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import com.openrec.proto.model.ScoreResult;

@Service
public class RedisService {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedisTemplate redisJsonTemplate;

    public void addSet(String key, String value) {
        redisTemplate.opsForSet().add(key, value);
    }

    public void addSets(String key, Set<String> values) {
        redisTemplate.opsForSet().add(key, values.toArray());
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
        Set<ZSetOperations.TypedTuple<String>> tupleSet =
            redisTemplate.opsForZSet().reverseRangeByScoreWithScores(key, scoreMin, scoreMax, 0, size);
        return tupleSet.stream().map(t -> new ScoreResult(t.getValue().replaceAll("^\"|\"$", ""), t.getScore())).collect(Collectors.toList());
    }

    public List<ScoreResult> getZSet(List<String> keys, double scoreMin, double scoreMax, int size) {

        String tmpKey = "tmp_" + UUID.randomUUID().toString();
        redisTemplate.opsForZSet().unionAndStore(tmpKey, keys, tmpKey);
        Set<ZSetOperations.TypedTuple<String>> tupleSet =
            redisTemplate.opsForZSet().reverseRangeByScoreWithScores(tmpKey, scoreMin, scoreMax, 0, size);
        List<ScoreResult> mergeResult =
            tupleSet.stream().map(t -> new ScoreResult(t.getValue(), t.getScore())).collect(Collectors.toList());
        redisTemplate.delete(tmpKey);
        return mergeResult;
    }

    public void addKv(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public void addKvs(Map<String, Object> kvs) {
        redisTemplate.opsForValue().multiSet(kvs);
    }

    public <T> T getV(String key) {
        return (T)redisTemplate.opsForValue().get(key);
    }

    public LinkedHashMap getJsonV(String key) {
        return (LinkedHashMap) redisJsonTemplate.opsForValue().get(key);
    }
    public void removeK(String key) {
        redisTemplate.delete(key);
    }

    public void removeKs(List<String> keys) {
        redisTemplate.delete(keys);
    }

    public <T> List<T> getVs(List<String> keys) {
        return redisTemplate.opsForValue().multiGet(keys);
    }
}
