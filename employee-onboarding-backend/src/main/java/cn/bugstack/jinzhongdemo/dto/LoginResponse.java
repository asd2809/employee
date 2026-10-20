package cn.bugstack.jinzhongdemo.dto;

import cn.bugstack.jinzhongdemo.vo.LoginVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应体：令牌 + 登录用户信息。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {

    /** 访问令牌 */
    private String token;

    /** 令牌类型，客户端需按 "Bearer xxx" 放入 Authorization 请求头 */
    private String tokenType;

    /** 有效期（秒） */
    private Long expiresIn;

    /** 登录用户信息 */
    private LoginVO user;
}
