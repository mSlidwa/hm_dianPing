package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class SimpleRedisLock {
    private StringRedisTemplate stringRedisTemplate;
    private static String LOCK="lock:";

    private static String uuid= UUID.randomUUID().toString(true)+"-";

    public SimpleRedisLock(StringRedisTemplate stringRedisTemplate){
        this.stringRedisTemplate=stringRedisTemplate;
    }
    /**
     * 利用redis的setnx实现分布式锁
     * @param time
     * @return
     */
    public boolean tryLock(long time){
        long id = Thread.currentThread().getId();
        Long userId = UserHolder.getUser().getId();
        String key=LOCK+userId;
        String value=uuid+id;
        Boolean result = stringRedisTemplate.opsForValue().setIfAbsent(key, value, time, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(result);
    }

    public void unLock(){
        long id = Thread.currentThread().getId();
        Long userId = UserHolder.getUser().getId();
        String key=LOCK+userId;
        String value = stringRedisTemplate.opsForValue().get(key);
        if(value.equals(uuid+id)){
            stringRedisTemplate.delete(key);
        }
    }


}
