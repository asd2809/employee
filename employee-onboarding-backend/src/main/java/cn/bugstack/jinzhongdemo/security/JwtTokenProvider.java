package cn.bugstack.jinzhongdemo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * JWT 的生成与解析。
 */
@Component
public class JwtTokenProvider {

    private final JwtProperties properties;
    private final SecretKey secretKey;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
        byte[] keyBytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("jwt.secret 至少需要 32 个字节，当前为 " + keyBytes.length);
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 根据登录用户签发令牌，subject 为用户 id，自定义声明携带用户名、昵称与角色。
     */
    public String generateToken(LoginUser user) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(properties.getExpireSeconds());
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .issuer(properties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .claim("username", user.getUsername())
                .claim("nickname", user.getNickname())
                .claim("role", user.getRole())
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 校验签名与有效期，并返回声明内容。令牌非法或过期时抛出 JwtException。
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(properties.getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getExpireSeconds() {
        return properties.getExpireSeconds();
    }
}
