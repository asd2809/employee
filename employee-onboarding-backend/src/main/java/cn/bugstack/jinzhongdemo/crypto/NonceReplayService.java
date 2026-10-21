package cn.bugstack.jinzhongdemo.crypto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * nonce 防重放校验。
 *
 * <p>原理：一个随机 nonce 只允许被使用一次。
 * 由 {@link NonceStore} 通过 SETNX（setIfAbsent）+ 过期时间实现原子占位：
 * 占位成功说明是首次出现，占位失败说明该 nonce 已存在 → 判定为重放攻击。</p>
 *
 * <p>存储实现由 {@code jwe.nonce-store} 选择：{@code redis}（默认，多实例共享）
 * 或 {@code memory}（进程内，本地开发无需 Redis）。</p>
 *
 * <p>失败策略：存储不可用时采用「安全优先」——直接拒绝请求而不是放行，
 * 避免在存储不可用时重放防护静默失效。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NonceReplayService {

    private final NonceStore nonceStore;
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
        boolean firstTime;
        try {
            // NX + 过期时间：原子地「不存在则写入并设置 TTL」
            firstTime = nonceStore.saveIfAbsent(
                    key, Duration.ofSeconds(jweProperties.getNonceTtlSeconds()));
        } catch (Exception e) {
            // 存储不可用时拒绝请求（fail-closed），防止重放防护失效
            log.error("nonce 校验失败：存储不可用", e);
            throw new JweBusinessException(JweErrorCode.NONCE_STORE_UNAVAILABLE);
        }
        if (!firstTime) {
            log.warn("检测到重放攻击，nonce 已存在：{}", nonce);
            throw new JweBusinessException(JweErrorCode.NONCE_REPLAY);
        }
    }
}
