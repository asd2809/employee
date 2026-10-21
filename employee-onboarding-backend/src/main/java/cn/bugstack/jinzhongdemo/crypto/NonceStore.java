package cn.bugstack.jinzhongdemo.crypto;

import java.time.Duration;

/**
 * nonce 存储抽象。
 *
 * <p>两种实现：</p>
 * <ul>
 *   <li>{@link RedisNonceStore}：多实例共享，生产环境必须使用；</li>
 *   <li>{@link InMemoryNonceStore}：进程内存储，本地开发无需安装 Redis 即可运行。</li>
 * </ul>
 */
public interface NonceStore {

    /**
     * 原子占用 nonce：不存在则写入并设置过期时间。
     *
     * @param key nonce 键（含配置前缀）
     * @param ttl 有效期内重复出现即判定为重放
     * @return true 表示首次出现；false 表示仍在有效期内重复出现
     */
    boolean saveIfAbsent(String key, Duration ttl);
}
