package cn.bugstack.jinzhongdemo.crypto;

import cn.bugstack.jinzhongdemo.config.JweKeyProvider;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.crypto.RSADecrypter;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.ParseException;

/**
 * JWE 解密服务。
 *
 * <p>使用 nimbus-jose-jwt 成熟密码库完成解析，库内部自动完成：
 * RSA-OAEP 解密出 CEK、AES-256-GCM 解密正文、校验 authTag 完整性。
 * 报文被篡改、authTag 不匹配时库会直接抛出 JOSEException，本方法统一转为 JweBusinessException。</p>
 *
 * <p>安全加固：解密前先校验受保护头中的 alg/enc，只允许 RSA-OAEP + A256GCM，
 * 防止攻击者用弱算法（如 alg=none、RSA1_5、A128CBC-HS256）降级绕过。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JweDecryptionService {

    /** 内容加密算法白名单：只允许 AES-256-GCM */
    private static final EncryptionMethod ALLOWED_ENC = EncryptionMethod.A256GCM;

    private final JweKeyProvider jweKeyProvider;

    /**
     * 解析并解密 JWE 紧凑序列化字符串。
     *
     * @param jweCompact 前端产出的 JWE（五段式紧凑格式）
     * @return JWE 内部明文载荷（即 {data, timestamp, nonce} 的 JSON 字符串）
     * @throws JweBusinessException 密文格式错误、算法不允许、签名/authTag 校验失败（即报文被篡改）
     */
    public String decrypt(String jweCompact) {
        JWEObject jweObject;
        try {
            jweObject = JWEObject.parse(jweCompact);
        } catch (ParseException e) {
            log.warn("JWE 密文格式非法，无法解析", e);
            throw new JweBusinessException(JweErrorCode.JWE_MALFORMED);
        }

        // 算法白名单校验：禁止攻击者指定弱算法或 alg=none 降级
        JWEHeader header = jweObject.getHeader();
        if (!JWEAlgorithm.RSA_OAEP.equals(header.getAlgorithm())) {
            log.warn("JWE alg 不在白名单内：{}", header.getAlgorithm());
            throw new JweBusinessException(JweErrorCode.JWE_ALGORITHM_NOT_ALLOWED);
        }
        if (!ALLOWED_ENC.equals(header.getEncryptionMethod())) {
            log.warn("JWE enc 不在白名单内：{}", header.getEncryptionMethod());
            throw new JweBusinessException(JweErrorCode.JWE_ALGORITHM_NOT_ALLOWED);
        }

        try {
            // 只有持有私钥的后端能解密；GCM 的 authTag 校验一旦失败即抛 JOSEException
            jweObject.decrypt(new RSADecrypter(jweKeyProvider.getRsaKey().toRSAPrivateKey()));
        } catch (JOSEException e) {
            // authTag 校验失败 = 报文被篡改 / 密钥不匹配
            log.warn("JWE 解密或完整性校验失败（疑似报文被篡改）", e);
            throw new JweBusinessException(JweErrorCode.JWE_TAMPERED);
        }

        String payload = jweObject.getPayload() == null ? null : jweObject.getPayload().toString();
        if (payload == null || payload.isBlank()) {
            throw new JweBusinessException(JweErrorCode.JWE_MALFORMED);
        }
        return payload;
    }

    /** 当前后端持有的 RSA 密钥（供公钥接口输出 JWK，前端据此加密） */
    public RSAKey getRsaKey() {
        return jweKeyProvider.getRsaKey();
    }
}
