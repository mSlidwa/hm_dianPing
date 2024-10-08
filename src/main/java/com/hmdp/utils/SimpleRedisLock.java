package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class SimpleRedisLock {
    private StringRedisTemplate stringRedisTemplate;
    private static String LOCK="lock:";

    private static String uuid= UUID.randomUUID().toString(true)+"-";

    //提前读取lua文件，以便每次直接使用
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static {
        //静态资源在静态代码块中初始化
        UNLOCK_SCRIPT=new DefaultRedisScript<>();
        //ClassPathResource就是默认的resource文件，这里指定寻找路径
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
    }


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

//    public void unLock(){
//        long id = Thread.currentThread().getId();
//        Long userId = UserHolder.getUser().getId();
//        String key=LOCK+userId;
//        String value = stringRedisTemplate.opsForValue().get(key);
//        if(value.equals(uuid+id)){
//            stringRedisTemplate.delete(key);
//        }
//    }
    public void unLock(){
       stringRedisTemplate.execute(UNLOCK_SCRIPT,
               Collections.singletonList(LOCK+UserHolder.getUser().getId()),
               uuid+Thread.currentThread().getId());
        }

}
