if(redis.call('get',KEYS[1])==ARGV[1]) then
    returnredis.call('del',KEYS[1])
end
return 0