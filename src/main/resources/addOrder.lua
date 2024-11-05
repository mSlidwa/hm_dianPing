-- 判断是否满足生成订单条件
local vocherId=ARGV[1]
local userId=ARGV[2]

local vocherName="seckill:stock:" .. vocherId
local orderName="seckill:order:" .. vocherId

if(tonumber(redis.call('get',vocherName))<=0) then
    return 1
end

if(redis.call("sismember",orderName,userId)==1) then
    return 2
end

redis.call('incrby',vocherName,-1)
redis.call('sadd',orderName,userId)
return 0