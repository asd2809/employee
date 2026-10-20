package cn.bugstack.jinzhongdemo.crypto;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * JWE 应用层加密配置，对应 application.yml 中的 jwe.* 配置项。
 *
 * <p>本方案只解决传输层面的窃听、报文篡改、重放攻击；
 * 不能防御 SQL 注入、XSS，注入防护需要后端参数化查询、输入校验，前端 XSS 转义单独实现。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwe")
public class JweProperties {

    /** 是否开启 JWE 解密校验，关闭后受保护接口恢复为明文 JSON（便于排障，生产必须开启） */
    private boolean enabled = true;

    /** 是否强制 HTTPS。本地开发走 http 需关闭；生产开启后会拒绝非 https 请求并输出 HSTS 头 */
    private boolean requireHttps = false;

    /**
     * 后端 RSA 私钥（PKCS#8 PEM 格式，可带 BEGIN/END 头）。
     * 私钥只允许保存在后端，前端只持有公钥。为空时启动会生成临时密钥对并打印公钥。
     */
    private String privateKey;

    /** 单次请求时间戳允许的偏差（秒），超过该偏差直接拒绝，默认 5 分钟 */
    private long timestampToleranceSeconds = 300L;

    /** nonce 在 Redis 中的存活时间（秒），默认 10 分钟；同一 nonce 在此期间内二次出现即判定为重放 */
    private long nonceTtlSeconds = 600L;

    /** Redis 中 nonce 键的前缀 */
    private String nonceKeyPrefix = "jwe:nonce:";

    /**
     * 需要 JWE 加密的请求规则，格式为 "HTTP方法 路径"，多个规则用逗号分隔。
     * 例：POST /api/employees/login,POST /api/employees
     * 只有命中规则的请求才会走解密校验，其余接口保持原有明文行为。
     */
    private List<String> rules = new ArrayList<>();
}
