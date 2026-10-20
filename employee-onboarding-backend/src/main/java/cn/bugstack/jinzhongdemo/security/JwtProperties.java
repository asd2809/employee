package cn.bugstack.jinzhongdemo.security;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 相关配置，对应 application.yml 中的 jwt.* 配置项。
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** 签名密钥，HS256 要求至少 32 字节 */
    private String secret;

    /** 签发者 */
    private String issuer = "employee-onboarding-backend";

    /** 令牌有效期（秒） */
    private Long expireSeconds = 7200L;

    /** 令牌所在请求头名称 */
    private String header = "Authorization";

    /** 令牌前缀（含尾部空格） */
    private String prefix = "Bearer ";
}
