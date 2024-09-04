package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class loginInterceptor implements HandlerInterceptor {


    private StringRedisTemplate stringRedisTemplate;

    public loginInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                    //该逻辑移动到RefushLoginInterceptor中
//        //1.获取session   no
//        //1.1获取token    yes
//        String token= request.getHeader("authorization");
//        if (StrUtil.isBlank(token)) {
//            //未查到user，拦截并返回错误代码
//            response.setStatus(401);
//            return false;
//        }
//        //1.2基于token从redis获取user
//        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(RedisConstants.LOGIN_USER_KEY + token);
//
//        //2.user是否存在，不存在拦截，存在放行
//        if(entries.isEmpty()){
//            //未查到user，拦截并返回错误代码
//            response.setStatus(401);
//            return false;
//        }
//        //3.获取user并将获取到的hash数据转换为UserDTO对象
//        UserDTO userDTO = BeanUtil.fillBeanWithMap(entries, new UserDTO(), false);
//        //查到user，保存到ThreadLocal中
//        UserHolder.saveUser(userDTO);

        //.刷新token有效期
//        stringRedisTemplate.expire(RedisConstants.LOGIN_USER_KEY + token,RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);

        //该处只判断是否要拦截
        //查看当前是否有已经登录的user
        if(UserHolder.getUser()==null){
            response.setStatus(401);
            return false;
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
