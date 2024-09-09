package com.hmdp.entity;

import cn.hutool.core.io.resource.StringResource;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisData;
import jdk.internal.org.jline.utils.Timeout;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
@Component
//Redis工具类
public class CacheClient {

    StringRedisTemplate redisTemplate;
    public CacheClient(StringRedisTemplate redisTemplate){
        this.redisTemplate=redisTemplate;
    }

    public void setValueTimeout(String key, Object value, Long time, TimeUnit timeUnit){
        //保存数据并设置过期时间。
        redisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value),time,timeUnit);
    }

    public void setValueExpire(String key,Object value,Long time, TimeUnit timeUnit){
        RedisData redisData = new RedisData();
        redisData.setData(value);
        //设置逻辑过期时间，时间单位设置为秒
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(timeUnit.toSeconds(time)));
        redisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(redisData));
    }


    public <R,T> R getCacehById(T id, Class<R> type, String redisConstants, Function<T,R> get){
        try {
            //1.从redis中查询商铺缓存
            String r = redisTemplate.opsForValue().get(redisConstants+id);
            //2.判断是否存在
            if (StrUtil.isNotBlank(r)){
                //3.存在，直接返回
                return JSONUtil.toBean(r,type);
            }
            if(r!=null){
                return null;
            }
            //解决缓存击穿问题
            Boolean lock=true;
            Boolean b;
            while(lock){
                //上锁
                b = tryLocak(id.toString());
                //未获取到锁
                if (b){
                    lock=false;
                }
                Thread.sleep(50);
            }
            String string = redisTemplate.opsForValue().get(redisConstants + id);
            if (StrUtil.isNotBlank(string)){
                //存在直接返回
                unLocak(id.toString());
                return JSONUtil.toBean(string,type);
            }

            //4.不存在，根据id查询数据库
            R sql = get.apply(id);
            Thread.sleep(200);
            if (sql==null){
                //5.不存在，返回错误,并且为防止缓存穿透缓存空对象
                this.setValueTimeout(redisConstants+id,"",RedisConstants.CACHE_NULL_TTL,TimeUnit.MINUTES);
                return null;
            }
            //6.存在，写入redis
            this.setValueTimeout(redisConstants,JSONUtil.toJsonStr(sql),RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
            return sql;
        }catch (Exception e){
            log.debug(e.toString());
        }finally {
            unLocak(id.toString());
        }
        return null;
    }


    private Boolean tryLocak(String keyid){
        Boolean b = redisTemplate.opsForValue().setIfAbsent(RedisConstants.LOCK_SHOP_KEY+keyid, "1", 10, TimeUnit.MINUTES);
        return BooleanUtil.isTrue(b);
    }

    private void unLocak(String keyid){
        redisTemplate.delete(RedisConstants.LOCK_SHOP_KEY+keyid);
    }
}
