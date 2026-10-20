package cn.bugstack.jinzhongdemo.config;

import cn.bugstack.jinzhongdemo.filter.HttpsEnforcementFilter;
import cn.bugstack.jinzhongdemo.filter.JweRequestFilter;
import cn.bugstack.jinzhongdemo.security.JwtAuthenticationFilter;
import cn.bugstack.jinzhongdemo.security.RestAccessDeniedHandler;
import cn.bugstack.jinzhongdemo.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置：无状态、JWT 过滤器鉴权、统一 JSON 错误响应。
 *
 * <p>过滤器顺序（请求自外向内）：</p>
 * <ol>
 *   <li>{@link HttpsEnforcementFilter}：强制 HTTPS，最先执行；</li>
 *   <li>{@link JwtAuthenticationFilter}：校验请求头中的 JWT 完成身份认证；</li>
 *   <li>{@link JweRequestFilter}：解密 JWE 报文并校验时间戳 / nonce（传输层可信性）。</li>
 * </ol>
 *
 * <p>职责边界：<b>JWT 管「你是谁」，JWE 管「报文是否可信」</b>，两者互不混淆。
 * JWE 是 HTTPS 之上的叠加防护，不能替代 HTTPS。</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JweRequestFilter jweRequestFilter;
    private final HttpsEnforcementFilter httpsEnforcementFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 前后端分离、基于令牌，不需要 CSRF 与 Session
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 登录接口、健康检查、JWE 公钥分发无需认证（公钥本就是公开信息）
                        .requestMatchers("/api/employees/login", "/api/crypto/public-key", "/demo", "/error").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                // 先做 HTTPS 强制，再做 JWT 身份认证，最后做 JWE 报文解密与时间戳/nonce 校验
                .addFilterBefore(httpsEnforcementFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(jweRequestFilter, JwtAuthenticationFilter.class);
        return http.build();
    }

    /** BCrypt 密码编码器（Spring Security 自带，带随机盐，安全性高于 MD5） */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }
}
