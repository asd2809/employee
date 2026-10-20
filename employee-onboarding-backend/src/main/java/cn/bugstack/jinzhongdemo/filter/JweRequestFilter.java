package cn.bugstack.jinzhongdemo.filter;

import cn.bugstack.jinzhongdemo.crypto.JweBusinessException;
import cn.bugstack.jinzhongdemo.crypto.JweDecryptionService;
import cn.bugstack.jinzhongdemo.crypto.JweErrorCode;
import cn.bugstack.jinzhongdemo.crypto.JweErrorResponder;
import cn.bugstack.jinzhongdemo.crypto.JweProperties;
import cn.bugstack.jinzhongdemo.crypto.NonceReplayService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * JWE 统一解密校验过滤器（在进入 Controller 之前执行）。
 *
 * <p>处理流程与需求一一对应：</p>
 * <ol>
 *   <li>命中 jwe.rules 的请求，读取 Body 中的 JWE 密文；</li>
 *   <li>nimbus 解密：库内部完成 RSA-OAEP 解出 CEK、A256GCM 解密、authTag 完整性校验，
 *       报文被篡改直接抛异常 → 返回业务码 40003；</li>
 *   <li>取出 timestamp，与服务器系统时间比较，偏差超过阈值 → 返回业务码 40006；</li>
 *   <li>取出 nonce，Redis SETNX 占位，已存在 → 判定重放，返回业务码 40008；</li>
 *   <li>全部通过后，用解密出的 data 替换请求体，转发给 Controller 执行业务逻辑。</li>
 * </ol>
 *
 * <p>注意：本过滤器只负责「报文是否可信」，<b>不负责身份</b>。
 * 身份仍由 JWT（请求头 Authorization）+ Spring Security 校验，两者职责不混淆。
 * 因此本过滤器排在 JwtAuthenticationFilter 之后。</p>
 *
 * <p>本 JWE 方案只解决传输层面的窃听、报文篡改、重放攻击；
 * 不能防御 SQL 注入、XSS，注入防护需要后端参数化查询、输入校验，前端 XSS 转义单独实现。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JweRequestFilter extends OncePerRequestFilter {

    private final JweProperties jweProperties;
    private final JweDecryptionService jweDecryptionService;
    private final NonceReplayService nonceReplayService;
    private final JweErrorResponder jweErrorResponder;
    private final ObjectMapper objectMapper;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final List<JweRule> rules = new ArrayList<>();

    /** 一条加密规则：HTTP 方法 + Ant 风格路径 */
    private record JweRule(String method, String pattern) {
    }

    @PostConstruct
    public void initRules() {
        for (String raw : jweProperties.getRules()) {
            if (!StringUtils.hasText(raw)) {
                continue;
            }
            String[] parts = raw.trim().split("\\s+", 2);
            if (parts.length != 2) {
                throw new IllegalStateException("jwe.rules 格式应为 \"HTTP方法 路径\"，当前配置：" + raw);
            }
            rules.add(new JweRule(parts[0].toUpperCase(Locale.ROOT), parts[1]));
        }
        log.info("JWE 已启用：{}，加密规则 {}", jweProperties.isEnabled(), rules);
    }

    /** 未开启加密、或未命中规则时，直接放行（保证未纳入范围的接口行为完全不变） */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!jweProperties.isEnabled() || rules.isEmpty()) {
            return true;
        }
        String uri = request.getRequestURI();
        String method = request.getMethod();
        return rules.stream()
                .noneMatch(rule -> rule.method().equalsIgnoreCase(method) && pathMatcher.match(rule.pattern(), uri));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // 1. 受保护接口必须是 JWE 密文，否则直接拒绝
            byte[] rawBody = request.getInputStream().readAllBytes();
            String jweCompact = new String(rawBody, StandardCharsets.UTF_8).trim();
            if (!StringUtils.hasText(jweCompact)) {
                throw new JweBusinessException(JweErrorCode.JWE_MISSING);
            }
            // JWE 紧凑序列化为五段式：header.encryptedKey.iv.ciphertext.tag
            // 段数不符说明客户端根本没加密（例如直接发了明文 JSON），给出更准确的业务码
            if (jweCompact.split("\\.", -1).length != 5) {
                throw new JweBusinessException(JweErrorCode.JWE_MISSING);
            }

            // 2. 解密（含 authTag 完整性校验，篡改会抛 40003）
            String payloadJson = jweDecryptionService.decrypt(jweCompact);

            // 3. 解析载荷：{data, timestamp, nonce}
            JsonNode payload = parsePayload(payloadJson);
            JsonNode dataNode = payload.get("data");
            if (dataNode == null || !dataNode.isObject()) {
                throw new JweBusinessException(JweErrorCode.PAYLOAD_INVALID, "data 字段缺失或不是对象");
            }

            // 4. 时间戳校验：Math.abs(服务器时间 - timestamp) > 阈值 直接拒绝
            validateTimestamp(payload.get("timestamp"));

            // 5. nonce 防重放：Redis 中已存在即判定为重放
            JsonNode nonceNode = payload.get("nonce");
            if (nonceNode == null || !nonceNode.isTextual() || !StringUtils.hasText(nonceNode.asText())) {
                throw new JweBusinessException(JweErrorCode.NONCE_INVALID);
            }
            nonceReplayService.checkAndStore(nonceNode.asText());

            // 6. 校验全部通过：把 data 作为新的请求体转发给 Controller，业务代码无感知
            byte[] decryptedBody = objectMapper.writeValueAsBytes(dataNode);
            filterChain.doFilter(new DecryptedBodyRequestWrapper(request, decryptedBody), response);
        } catch (JweBusinessException e) {
            log.warn("JWE 校验失败：{}", e.getMessage());
            jweErrorResponder.respond(response, e.getErrorCode());
        } catch (Exception e) {
            // 兜底：任何未预期异常都不放行，避免加密校验被绕过
            log.error("JWE 过滤器内部异常", e);
            jweErrorResponder.respondInternalError(response);
        }
    }

    /** 载荷 JSON 解析失败说明密文内部结构被破坏 */
    private JsonNode parsePayload(String payloadJson) {
        try {
            JsonNode node = objectMapper.readTree(payloadJson);
            if (node == null || !node.isObject()) {
                throw new JweBusinessException(JweErrorCode.PAYLOAD_INVALID);
            }
            return node;
        } catch (JweBusinessException e) {
            throw e;
        } catch (IOException e) {
            throw new JweBusinessException(JweErrorCode.PAYLOAD_INVALID);
        }
    }

    /** 时间戳必须是毫秒数，且与服务器时间偏差在允许范围内 */
    private void validateTimestamp(JsonNode timestampNode) {
        if (timestampNode == null || !timestampNode.isNumber()) {
            throw new JweBusinessException(JweErrorCode.TIMESTAMP_INVALID);
        }
        long clientTimestamp = timestampNode.asLong();
        long toleranceMillis = jweProperties.getTimestampToleranceSeconds() * 1000L;
        long diff = Math.abs(System.currentTimeMillis() - clientTimestamp);
        if (diff > toleranceMillis) {
            log.warn("请求时间戳超期：客户端={}，服务器={}，偏差={}ms", clientTimestamp, System.currentTimeMillis(), diff);
            throw new JweBusinessException(JweErrorCode.TIMESTAMP_EXPIRED);
        }
    }
}
