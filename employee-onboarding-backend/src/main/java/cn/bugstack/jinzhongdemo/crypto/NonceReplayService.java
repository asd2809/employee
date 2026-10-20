package cn.bugstack.jinzhongdemo.crypto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * nonce 防重放校验。
 *
 * <p>原理：一个随机 nonce 只允许被使用一次。
 * 用 Redis 的 SETNX（setIfAbsent）+ 过期时间实现原子占位：
 * 占位成功说明是首次出现，占位失败说明该 nonce 已存在 → 判定为重放攻击。</p>
 *
 * <p>多实例部署时，所有后端实例共用同一个 Redis，因此重放请求无论打到哪个实例都会被识别。</p>
 *
 * <p>失败策略：Redis 不可用时采用「安全优先」——直接拒绝请求而不是放行，
 * 避免在存储不可用时重放防护静默失效。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NonceReplayService {

    private final StringRedisTemplate stringRedisTemplate;
    private final JweProperties jweProperties;

    /**
     * 校验并占用 nonce。
     *
     * @param nonce 载荷中的随机数（UUID v4）
     * @throws JweBusinessException nonce 重复（重放）或 Redis 不可用
     */
    public void checkAndStore(String nonce) {
        if (nonce == null || nonce.isBlank()) {
            throw new JweBusinessException(JweErrorCode.NONCE_INVALID);
        }
        String key = jweProperties.getNonceKeyPrefix() + nonce;
        Boolean firstTime;
        try {
            // NX + 过期时间：原子地「不存在则写入并设置 TTL」
            firstTime = stringRedisTemplate.opsForValue()
                    .setIfAbsent(key, "1", Duration.ofSeconds(jweProperties.getNonceTtlSeconds()));
        } catch (Exception e) {
            // Redis 不可用时拒绝请求（fail-closed），防止重放防护失效
            log.error("nonce 校验失败：Redis 不可用", e);
            throw new JweBusinessException(JweErrorCode.NONCE_STORE_UNAVAILABLE);
        }
        if (!Boolean.TRUE.equals(firstTime)) {
            log.warn("检测到重放攻击，nonce 已存在：{}", nonce);
            throw new JweBusinessException(JweErrorCode.NONCE_REPLAY);
        }
    }
}
