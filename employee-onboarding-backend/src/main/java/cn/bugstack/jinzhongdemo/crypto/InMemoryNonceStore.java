package cn.bugstack.jinzhongdemo.crypto;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 进程内 nonce 存储：ConcurrentHashMap 记录过期时间，供本地开发在没有 Redis 时使用。
 *
 * <p>限制（生产不可用）：</p>
 * <ul>
 *   <li>进程重启后已占用的 nonce 丢失——只影响重启前 10 分钟内的重放窗口；</li>
 *   <li>多实例部署时各实例互不可见，重放防护失效，生产必须切回 {@code jwe.nonce-store: redis}。</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "jwe.nonce-store", havingValue = "memory")
public class InMemoryNonceStore implements NonceStore {

    /** 每写入这么多次顺带清理一次过期键，避免长时间运行后 map 无限增长 */
    private static final int CLEANUP_INTERVAL = 256;

    private final Map<String, Long> expireAtMap = new ConcurrentHashMap<>();
    private final AtomicInteger writeCount = new AtomicInteger();

    @Override
    public boolean saveIfAbsent(String key, Duration ttl) {
        long now = System.currentTimeMillis();
        long newExpireAt = now + ttl.toMillis();
        Long previous = expireAtMap.putIfAbsent(key, newExpireAt);

        // 已存在且未过期 → 重复 nonce（重放）
        if (previous != null && previous >= now) {
            return false;
        }
        // 首次出现，或旧记录已过期：写入新的过期时间
        if (previous != null) {
            expireAtMap.put(key, newExpireAt);
        }
        if (writeCount.incrementAndGet() % CLEANUP_INTERVAL == 0) {
            expireAtMap.entrySet().removeIf(entry -> entry.getValue() < now);
        }
        return true;
    }
}
