package config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.List;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public DefaultRedisScript<List> tokenBucketScript() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setResultType(List.class);
        script.setScriptText("""
            local key = KEYS[1]
            local capacity = tonumber(ARGV[1])
            local refill_amount = tonumber(ARGV[2])
            local refill_interval = tonumber(ARGV[3])
            local now = tonumber(ARGV[4])

            local bucket = redis.call('HMGET', key, 'tokens', 'last_refill')
            local tokens = tonumber(bucket[1])
            local last_refill = tonumber(bucket[2])

            -- ilk erişim de bucketi max kapasiteyle başlat
            if tokens == nil then
                tokens = capacity
                last_refill = now
            end

            -- gecen sureye gore token eklenir
            local elapsed = now - last_refill
            local intervals = math.floor(elapsed / (refill_interval * 1000))

            if intervals > 0 then
                -- token sayısı kapasiteden fazla olamaz
                tokens = math.min(capacity, tokens + (intervals * refill_amount))
                last_refill = now
            end

            -- token varsa istek geçer, allowed 1 olur
            local allowed = 0
            if tokens > 0 then
                tokens = tokens - 1
                allowed = 1
            end

            -- bucket durumunu güncelliyoruz ve TTL koyuyoruz
            redis.call('HSET', key, 'tokens', tokens, 'last_refill', last_refill)
            redis.call('EXPIRE', key, refill_interval * 2)

            local next_refill = last_refill + (refill_interval * 1000)

            return{tokens, allowed, next_refill}
        """);
        return script;
    }


}
