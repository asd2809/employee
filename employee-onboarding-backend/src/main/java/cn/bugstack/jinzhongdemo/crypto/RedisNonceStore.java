package cn.bugstack.jinzhongdemo.crypto;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 基于 Redis 的 nonce 存储（默认实现）。
 *
 * <p>SETNX + TTL 原子占位，多实例后端共用同一份 Redis，重放请求无论打到哪个实例都能识别。</p>
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "jwe.nonce-store", havingValue = "redis", matchIfMissing = true)
public class RedisNonceStore implements NonceStore {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean saveIfAbsent(String key, Duration ttl) {
        Boolean firstTime = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", ttl);
        return Boolean.TRUE.equals(firstTime);
    }
}
