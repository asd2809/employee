package cn.bugstack.jinzhongdemo.crypto;

import lombok.Getter;

/**
 * JWE 校验失败时抛出的业务异常，由过滤器捕获后按 {@link JweErrorCode} 输出对应业务码与 HTTP 状态码。
 */
@Getter
public class JweBusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final JweErrorCode errorCode;

    public JweBusinessException(JweErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public JweBusinessException(JweErrorCode errorCode, String detail) {
        super(errorCode.getMessage() + "：" + detail);
        this.errorCode = errorCode;
    }
}
