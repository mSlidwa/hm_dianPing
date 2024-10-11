package com.hmdp.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {
    @Bean
    public RedissonClient redissonClient(){
        Config config = new Config();
        //单一环境下使用useSingleServer指定节点
        config.useSingleServer().setAddress("redis://127.0.0.1:6379").setPassword("NoZuoNoDie");
        return Redisson.create(config);
    }

}
