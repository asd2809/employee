package cn.bugstack.jinzhongdemo.filter;

import cn.bugstack.jinzhongdemo.crypto.JweErrorCode;
import cn.bugstack.jinzhongdemo.crypto.JweErrorResponder;
import cn.bugstack.jinzhongdemo.crypto.JweProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 强制 HTTPS 过滤器。
 *
 * <p>底层通信必须走 HTTPS，否则 JWE 之外的其余请求（含 JWT 令牌本身）仍可能被中间人窃听。
 * JWE 是 HTTPS 之上的叠加防护，<b>不能替代 HTTPS</b>。</p>
 *
 * <p>是否启用由 {@code jwe.require-https} 控制：本地开发经 vite 代理走 http，需关闭；
 * 生产环境开启后，非 HTTPS 请求一律返回 40010。</p>
 *
 * <p>同时兼容反向代理：检查 X-Forwarded-Proto 头（nginx 终结 TLS 的场景），
 * 若应用本身未配置 forward-headers-strategy，也能正确识别原始协议。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HttpsEnforcementFilter extends OncePerRequestFilter {

    private final JweProperties jweProperties;
    private final JweErrorResponder jweErrorResponder;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 未开启强制 HTTPS 时完全不介入
        return !jweProperties.isRequireHttps();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!isSecure(request)) {
            log.warn("拒绝非 HTTPS 请求：{} {}", request.getMethod(), request.getRequestURI());
            jweErrorResponder.respond(response, JweErrorCode.HTTPS_REQUIRED);
            return;
        }
        // 已启用 HTTPS，输出 HSTS 提示浏览器后续强制走 HTTPS（仅对 HTTPS 响应有意义）
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        filterChain.doFilter(request, response);
    }

    private boolean isSecure(HttpServletRequest request) {
        if (request.isSecure()) {
            return true;
        }
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        return forwardedProto != null && forwardedProto.equalsIgnoreCase("https");
    }
}
