package cn.bugstack.jinzhongdemo.config;

import cn.bugstack.jinzhongdemo.crypto.JweProperties;
import com.nimbusds.jose.jwk.RSAKey;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

/**
 * 后端 RSA 密钥提供者。
 *
 * <p>安全约束：<b>RSA 私钥只保存在后端</b>，前端只持有公钥。因此 JWE 只能由前端加密、后端解密。</p>
 *
 * <p>密钥来源优先级：
 * 1) 配置了 {@code jwe.private-key}（PKCS#8 PEM）→ 使用固定密钥，生产环境必须走这一条；
 * 2) 未配置 → 启动时生成临时 RSA-2048 密钥对，方便本地开发，但重启后密钥变化，
 *    前端缓存的公钥会失效需要重新获取。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JweKeyProvider {

    private static final int RSA_KEY_SIZE = 2048;

    private final JweProperties jweProperties;

    /** 当前使用的 RSA 密钥（含私钥，仅存于后端内存） */
    @Getter
    private RSAKey rsaKey;

    /** 是否为启动时生成的临时密钥 */
    @Getter
    private boolean ephemeral;

    @PostConstruct
    public void init() {
        String privateKeyPem = jweProperties.getPrivateKey();
        if (StringUtils.hasText(privateKeyPem)) {
            RSAKey configuredKey;
            try {
                configuredKey = RSAKey.parseFromPEMEncodedObjects(privateKeyPem).toRSAKey();
            } catch (Exception e) {
                throw new IllegalStateException("jwe.private-key 解析失败，请确认为 PKCS#8 PEM 私钥", e);
            }
            if (!configuredKey.isPrivate()) {
                throw new IllegalStateException("jwe.private-key 必须是包含私钥的 PKCS#8 PEM");
            }
            this.rsaKey = configuredKey;
            this.ephemeral = false;
            log.info("JWE 已加载配置的 RSA 私钥，keyId={}", configuredKey.getKeyID());
            return;
        }

        // 本地开发兜底：生成临时密钥对，重启即失效
        this.rsaKey = generateEphemeralKey();
        this.ephemeral = true;
        log.warn("未配置 jwe.private-key，已生成临时 RSA 密钥对（重启后失效，前端需重新获取公钥）。"
                + "生产环境请在配置中提供固定私钥。当前公钥：\n{}", getPublicKeyPem());
    }

    private RSAKey generateEphemeralKey() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(RSA_KEY_SIZE);
            KeyPair keyPair = generator.generateKeyPair();
            RSAKey publicOnly = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic()).build();
            // keyId 用 JWK 指纹，保证同一密钥的 id 稳定，便于前端缓存
            return new RSAKey.Builder(publicOnly.toPublicJWK())
                    .privateKey((RSAPrivateKey) keyPair.getPrivate())
                    .keyID(publicOnly.toPublicJWK().computeThumbprint().toString())
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("生成 RSA 临时密钥失败", e);
        }
    }

    /** 对外公开的公钥（X.509/SPKI PEM），前端用 jose 的 importSPKI 导入 */
    public String getPublicKeyPem() {
        // 仅做 PEM 文本封装：真正的密钥编码由 JDK 完成，密码学运算全部交给 nimbus
        byte[] encoded;
        try {
            encoded = rsaKey.toRSAPublicKey().getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("导出 RSA 公钥失败", e);
        }
        String base64 = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(encoded);
        return "-----BEGIN PUBLIC KEY-----\n" + base64 + "\n-----END PUBLIC KEY-----";
    }

    /** 对外公开的公钥（JWK JSON），便于前端按需选择导入方式 */
    public String getPublicJwkJson() {
        try {
            return rsaKey.toPublicJWK().toJSONString();
        } catch (Exception e) {
            throw new IllegalStateException("导出 RSA 公钥 JWK 失败", e);
        }
    }

    /** 公钥指纹，用于前端比对公钥是否发生变化 */
    public String getPublicKeyId() {
        return rsaKey.getKeyID() == null ? "" : rsaKey.getKeyID();
    }
}
