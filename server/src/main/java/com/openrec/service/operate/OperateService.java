package com.openrec.service.operate;

import com.openrec.service.redis.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;


@Service
public class OperateService {

    @Autowired
    private RedisService redisService;

    public void setBlacklist(Set<String> blacklist) {
        redisService.addSets("black", blacklist);
    }
}
