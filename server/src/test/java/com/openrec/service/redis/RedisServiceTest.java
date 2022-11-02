package com.openrec.service.redis;

import com.openrec.RecServer;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = RecServer.class)
public class RedisServiceTest {

    @Autowired
    private RedisService redisService;

    @Test
    public void test() {
        redisService.addKv("testK", "testV");
        Assert.assertEquals(redisService.getV("testK"), "testV");
        redisService.removeK("testK");
        Assert.assertNull(redisService.getV("testK"));
    }
}