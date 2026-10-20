package cn.bugstack.jinzhongdemo.controller;

import cn.bugstack.jinzhongdemo.common.Result;
import cn.bugstack.jinzhongdemo.config.JweKeyProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JWE 公钥分发接口。
 *
 * <p>前端只需要公钥即可加密，因此这个接口无需认证（permitAll）。
 * 公钥本身就是公开信息，被拦截也不构成风险；私钥始终只保存在后端内存/配置中。</p>
 *
 * <p>返回值同时给出 PEM 与 JWK 两种格式，前端用 jose 的 {@code importSPKI} 或
 * {@code importJWK} 均可；{@code keyId} 用于前端判断后端密钥是否已变更（变更后需重新拉取）。</p>
 */
@RestController
@RequestMapping("/api/crypto")
@RequiredArgsConstructor
public class JwePublicKeyController {

    private final JweKeyProvider jweKeyProvider;

    @GetMapping("/public-key")
    public Result<Map<String, Object>> publicKey() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("algorithm", "RSA-OAEP");
        data.put("encryption", "A256GCM");
        data.put("keyId", jweKeyProvider.getPublicKeyId());
        // ephemeral=true 表示后端使用的是启动时生成的临时密钥，重启后会变化
        data.put("ephemeral", jweKeyProvider.isEphemeral());
        data.put("pem", jweKeyProvider.getPublicKeyPem());
        data.put("jwk", jweKeyProvider.getPublicJwkJson());
        return Result.success(data);
    }
}
