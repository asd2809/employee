package cn.bugstack.jinzhongdemo.service;

import cn.bugstack.jinzhongdemo.dto.LoginRequest;
import cn.bugstack.jinzhongdemo.dto.LoginResponse;
import cn.bugstack.jinzhongdemo.security.JwtTokenProvider;
import cn.bugstack.jinzhongdemo.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 登录服务：校验账号密码并签发 JWT。
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public LoginResponse login(LoginRequest request) {
        if (!StringUtils.hasText(request.getAccount()) || !StringUtils.hasText(request.getPassword())) {
            throw new IllegalArgumentException("账号和密码不能为空");
        }
        // 交给 DaoAuthenticationProvider 校验，密码错误会抛出 BadCredentialsException
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getAccount(), request.getPassword()));
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        String token = jwtTokenProvider.generateToken(loginUser);
        return new LoginResponse(token, "Bearer", jwtTokenProvider.getExpireSeconds(), loginUser.toLoginVO());
    }
}
