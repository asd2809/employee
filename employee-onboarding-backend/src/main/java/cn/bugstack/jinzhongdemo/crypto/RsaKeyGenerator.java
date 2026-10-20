package cn.bugstack.jinzhongdemo.crypto;

import com.nimbusds.jose.jwk.RSAKey;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

/**
 * RSA 公私钥生成工具（一次性使用，无需随应用启动）。
 *
 * <p>用法：直接运行本类的 main 方法，会输出一对 RSA-2048 密钥的 PEM 文本：</p>
 * <ul>
 *   <li><b>私钥</b>（PKCS#8，BEGIN PRIVATE KEY）→ 只配置在后端 {@code jwe.private-key}，绝不能下发前端、绝不能提交 git；</li>
 *   <li><b>公钥</b>（X.509，BEGIN PUBLIC KEY）→ 前端使用，用于加密。</li>
 * </ul>
 *
 * <p>生产环境务必使用固定密钥：否则每次重启密钥变化，前端已缓存的公钥作废，
 * 且多实例部署时各实例密钥不一致会导致解密失败。</p>
 */
public final class RsaKeyGenerator {

    private static final int KEY_SIZE = 2048;

    private RsaKeyGenerator() {
    }

    public static void main(String[] args) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(KEY_SIZE);
        KeyPair keyPair = generator.generateKeyPair();

        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

        String publicKeyPem = toPem(publicKey.getEncoded(), "PUBLIC KEY");
        String privateKeyPem = toPem(privateKey.getEncoded(), "PRIVATE KEY");

        System.out.println("================ RSA-" + KEY_SIZE + " 密钥对 ================");
        System.out.println("[公钥] 前端使用（jose 的 importSPKI 导入）：");
        System.out.println(publicKeyPem);
        System.out.println("[私钥] 仅后端使用，请配置到 jwe.private-key，切勿提交到代码仓库：");
        System.out.println(privateKeyPem);

        // 复用生成逻辑做一次自检：能正确解析回 RSAKey 才算有效
        RSAKey parsed = RSAKey.parseFromPEMEncodedObjects(privateKeyPem).toRSAKey();
        System.out.println("[自检] 私钥解析成功，keyId=" + parsed.toPublicJWK().computeThumbprint());
        System.out.println("提示：也可通过后端接口 GET /api/crypto/public-key 直接获取公钥，无需手工复制。");
    }

    /**
     * 把 JDK 编码出的密钥字节封装为 PEM 文本。
     * 这里只做 Base64 文本封装，不涉及任何加解密运算；IV、authTag、CEK 等一律由库内部处理。
     */
    private static String toPem(byte[] derEncoded, String type) {
        String base64 = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(derEncoded);
        return "-----BEGIN " + type + "-----\n" + base64 + "\n-----END " + type + "-----";
    }
}