package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.RedisIdWorker;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.UserHolder;
import jodd.util.CollectionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */

//dev1测试
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Autowired
    private ISeckillVoucherService iSeckillVoucherService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedisIdWorker redisIdWorker;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        //静态资源在静态代码块中初始化
        SECKILL_SCRIPT =new DefaultRedisScript<>();
        //ClassPathResource就是默认的resource文件，这里指定寻找路径
        SECKILL_SCRIPT.setLocation(new ClassPathResource("addOrder.lua"));
    }
//     阻塞队列
    private BlockingQueue<VoucherOrder> blockingQueue=new ArrayBlockingQueue<>(1024*1024);

    /**
     *  同步判断
     * @param voucherId
     * @return 生成订单Id
     */
    @Override
    public Result seckillVoucher(Long voucherId) {
        Long id = UserHolder.getUser().getId();
        //执行lua结果
        Long result = stringRedisTemplate.execute(SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString()
                , id.toString());
        //判断是否为0，0为有购买资格
        int r=result.intValue();
        if (r!=0){
            return Result.fail(r==1?"库存不足":"无法重复下单");
        }
        //下单信息保存到阻塞队列
        long orderId = redisIdWorker.nextId("order");
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(orderId);
        voucherOrder.setVoucherId(voucherId);
        voucherOrder.setUserId(id);
        blockingQueue.add(voucherOrder);
        //返回订单id
        return Result.ok(orderId);
    }



    /**
     *  优惠券秒杀
     * @param voucherId
     * @return
     */
//    @Override
//    public Result seckillVoucher(Long voucherId) {
//        //查找是否存在该券
//        QueryWrapper<SeckillVoucher> seckillVoucherQueryWrapper = new QueryWrapper<>();
//        seckillVoucherQueryWrapper.eq("voucher_id",voucherId);
//        SeckillVoucher one = iSeckillVoucherService.getOne(seckillVoucherQueryWrapper);
//        if (ObjectUtil.isEmpty(one)){
//            return Result.fail("未查到代金券信息");
//        }
//        //查看该券是否在可使用时间范围内
//        if(LocalDateTime.now().isBefore(one.getBeginTime())){
//            return Result.fail("代金券暂未开放购买");
//        }
//        if (LocalDateTime.now().isAfter(one.getEndTime())){
//            return Result.fail("活动已结束");
//        }
//        //查看该券库存是否充足
//        if (one.getStock()<1){
//            return Result.fail("库存不足");
//        }
//        Result result=null;
//        //实现一人一单
//        SimpleRedisLock simpleRedisLock = new SimpleRedisLock(stringRedisTemplate);
//        if (!simpleRedisLock.tryLock(100)) {
//            return Result.fail("只能购买一次");
//        }
//        try{ //调用方法实现库存减少并生成订单（利用乐观锁避免商品超卖
//            VoucherOrderServiceImpl x=(VoucherOrderServiceImpl)AopContext.currentProxy();
//            result = x.StockReduceAndGetOrder(voucherId);
//            return result;
//        }catch (Exception e){
//            log.error("eeee");
//        }finally {
//            simpleRedisLock.unLock();
//        }
//        return result;
//    }

    @Transactional
    public Result StockReduceAndGetOrder(Long voucherId) {
        //判断当前用户是否拥有订单
        Long id = UserHolder.getUser().getId();
        Integer count = query().eq("user_id", id).eq("voucher_Id", voucherId).count();
        if (count>0){
            return Result.fail("已有购买记录");
        }
        //尝试减少库存，利用版本号stock判断数据是否在操作前被修改
        boolean update = iSeckillVoucherService.update()
                .setSql("stock=stock-1")
                .gt("stock", 0)
                .eq("voucher_id", voucherId).update();
        if (!update){
            return Result.fail("库存不足");
        }
        //库存减量成功，插入订单
        VoucherOrder voucherOrder = new VoucherOrder();
        Long voucherOrderId=redisIdWorker.nextId(voucherId.toString());
        voucherOrder.setId(voucherOrderId);
        voucherOrder.setVoucherId(voucherId);
        voucherOrder.setUserId(id);
        save(voucherOrder);
        //返回生成订单ID
        return Result.ok(voucherOrderId);
    }


}
