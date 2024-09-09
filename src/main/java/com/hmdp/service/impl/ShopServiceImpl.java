package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;


    //缓存穿透解决方案 ：redis缓存空对象
    //缓存击穿解决方案：利用锁机制恢复redis空窗期或redis设置逻辑字段
    @Override
    public Result queryById(Long id) {
        try {
            //1.从redis中查询商铺缓存
            String shop = stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_SHOP_KEY+id);
            //2.判断是否存在
            if (StrUtil.isNotBlank(shop)){
                //3.存在，直接返回
                return Result.ok(JSONUtil.toBean(shop,Shop.class));
            }

            if(shop!=null){
                return Result.fail("商户不存在");
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
            String string = stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_SHOP_KEY + id);
            if (StrUtil.isNotBlank(string)){
                //存在直接返回
                unLocak(id.toString());
                return Result.ok(JSONUtil.toBean(string,Shop.class));
            }

            //4.不存在，根据id查询数据库
            Shop sqlShop = getById(id);
            Thread.sleep(200);
            if (sqlShop==null){
                //5.不存在，返回错误,并且为防止缓存穿透缓存空对象
                stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY+id,"",RedisConstants.CACHE_NULL_TTL,TimeUnit.MINUTES);
                return Result.fail("商户不存在");
            }
            //6.存在，写入redis
            stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY+id,JSONUtil.toJsonStr(sqlShop),RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
            return Result.ok(sqlShop);
        }catch (Exception e){
            log.debug(e.toString());
        }finally {
            unLocak(id.toString());
        }
        return Result.fail("服务出错");
    }

    //使用redis的setnx机制模拟锁机制
    //获取锁
    private Boolean tryLocak(String keyid){
        Boolean b = stringRedisTemplate.opsForValue().setIfAbsent(RedisConstants.LOCK_SHOP_KEY+keyid, "1", 10, TimeUnit.MINUTES);
        return BooleanUtil.isTrue(b);
    }

    private void unLocak(String keyid){
        stringRedisTemplate.delete(RedisConstants.LOCK_SHOP_KEY+keyid);
    }


    @Override
    public void updateId(Shop shop) {
        //先将数据保存到mysql中
        updateById(shop);
        //将redis缓存中的该数据删除
        stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_KEY+shop.getId());
    }

}
