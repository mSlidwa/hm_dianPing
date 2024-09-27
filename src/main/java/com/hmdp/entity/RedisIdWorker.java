package com.hmdp.entity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdWorker {
    private static final long BEGIN_TIMETIMESTAMP = 1640995200L;
   @Autowired
    private StringRedisTemplate redisTemplate;

    public long nextId(String keyPrefix){
        //1.生成时间戳31魏
        LocalDateTime now = LocalDateTime.now();
        long epochSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = epochSecond-BEGIN_TIMETIMESTAMP;
        //2.生成序列号
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        long count = redisTemplate.opsForValue().increment("icr:"+keyPrefix+":"+ date);

        return timestamp << 32 | count ;
    }

}
