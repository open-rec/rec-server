package com.openrec.service.operate;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.openrec.service.redis.RedisService;

@Service
public class OperateService {

    @Autowired
    private RedisService redisService;

    public void setBlacklist(Set<String> blacklist) {
        redisService.addSets("black", blacklist);
    }
}
