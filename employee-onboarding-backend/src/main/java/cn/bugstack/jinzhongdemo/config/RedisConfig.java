package cn.bugstack.jinzhongdemo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis 配置。
 *
 * <p>nonce 防重放依赖 Redis：所有后端实例必须连接同一个 Redis，
 * 这样重放请求无论被负载均衡打到哪个实例，都能识别出「nonce 已存在」。</p>
 *
 * <p>这里显式声明 {@link StringRedisTemplate}（键值均为字符串，正是 nonce 场景所需），
 * 连接信息由 spring.data.redis.* 提供，连接池由 Lettuce 管理。</p>
 */
@Configuration
public class RedisConfig {

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
