package service;

import config.RateLimitConfig;
import jdk.jfr.internal.util.Rate;
import lombok.AllArgsConstructor;
import model.RateLimitResult;
import model.RateLimitRule;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@AllArgsConstructor
public class TockenBucketService {
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisScript<List> tokenBucketScript;;
    private final RateLimitConfig rateLimitConfig;
    private static final String KEY_PREFIX = "rate_limit:";

    // bu metot gelen endpointi yazdığımız lua script ile "dener"
    // redis keyini oluşturur
    public RateLimitResult tryRateLimit(String endpoint, String identifier){
        String redisKey = KEY_PREFIX + endpoint + identifier;
        RateLimitRule rule = rateLimitConfig.getRuleForPath(endpoint);

        String capacity = String.valueOf(rule.getCapacity());
        String refillAmount = "1";
        String refillInterval = String.valueOf(rule.getRefillRatePerMilli());
        String now = String.valueOf(System.currentTimeMillis());

        List<Integer> result = stringRedisTemplate.execute(
                tokenBucketScript,
                Collections.singletonList(redisKey),
                capacity, refillAmount, refillInterval, now
        );

        if(result == null || result.isEmpty())
            return RateLimitResult.allow(0,0);


        int remainingTokens = result.get(0);
        boolean allowed = result.get(1) == 1;
        long nextRefillAt = result.get(2);

        if(allowed){
            return RateLimitResult.allow(remainingTokens,nextRefillAt);
        }
        else
            return RateLimitResult.deny(nextRefillAt);
    }

    // admin endpointi için tüm rate limit durumunu sıfırlar
    public void reset(String identifier, String endpoint){
        String redisKey = KEY_PREFIX + endpoint + identifier;
        stringRedisTemplate.delete(redisKey);
    }

    // mevcut token sayısını redisten okur
    public int getRemainingTokens(String identifier, String endpoint){
        String redisKey = KEY_PREFIX + endpoint + identifier;
        Object tokens = stringRedisTemplate.opsForHash().get(redisKey, "tokens");

        if(tokens == null)
            return rateLimitConfig.getRuleForPath(endpoint).getCapacity();
        return Integer.parseInt(tokens.toString());
    }

}
