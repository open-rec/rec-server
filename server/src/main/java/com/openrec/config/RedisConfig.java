package com.openrec.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.clients.jedis.JedisPoolConfig;

@Configuration
public class RedisConfig {

    @Value("${redis.hostName}")
    private String hostName;

    @Value("${redis.port}")
    private int port;

    @Value("${redis.timeout}")
    private int timeout;

    @Value("${redis.maxTotal}")
    private int maxTotal;

    @Value("${redis.maxWaitMillis}")
    private int maxWaitMillis;

    @Value("${redis.testOnBorrow}")
    private boolean testOnBorrow;


    @Autowired
    private Environment environment;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(maxTotal);
        jedisPoolConfig.setMaxWaitMillis(maxWaitMillis);
        jedisPoolConfig.setTestOnBorrow(testOnBorrow);
        JedisConnectionFactory jedisFactory = new JedisConnectionFactory();
        jedisFactory.setHostName(hostName);
        jedisFactory.setPort(port);
        jedisFactory.setTimeout(timeout);
        jedisFactory.setPoolConfig(jedisPoolConfig);
        return jedisFactory;
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String,String> redisTemplate=new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.afterPropertiesSet();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        //redisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<Object>());
        return redisTemplate;
    }
}
