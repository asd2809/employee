package cn.bugstack.jinzhongdemo.crypto;

import cn.bugstack.jinzhongdemo.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 过滤器阶段统一输出错误 JSON。
 *
 * <p>过滤器在 Controller 之前执行，异常不会进入 {@code @RestControllerAdvice}，
 * 因此这里直接向响应流写出与业务接口一致的 {@link Result} 结构，
 * 保证前端拿到的错误格式不变。</p>
 */
@Component
@RequiredArgsConstructor
public class JweErrorResponder {

    private final ObjectMapper objectMapper;

    /**
     * 按错误码写出响应，HTTP 状态码与业务码都来自 {@link JweErrorCode}，便于前端区分异常类型。
     */
    public void respond(HttpServletResponse response, JweErrorCode errorCode) throws IOException {
        write(response, errorCode.getHttpStatus(), errorCode.getCode(), errorCode.getMessage());
    }

    /** 自定义提示（用于补充具体原因，例如载荷字段缺失细节） */
    public void respond(HttpServletResponse response, JweErrorCode errorCode, String detail) throws IOException {
        write(response, errorCode.getHttpStatus(), errorCode.getCode(), errorCode.getMessage() + "：" + detail);
    }

    /** 过滤器内部异常 */
    public void respondInternalError(HttpServletResponse response) throws IOException {
        respond(response, JweErrorCode.INTERNAL_ERROR);
    }

    private void write(HttpServletResponse response, int httpStatus, int code, String message) throws IOException {
        // 过滤器阶段响应尚未提交，重置后写出，避免与后续内容冲突
        response.reset();
        response.setStatus(httpStatus);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(code, message)));
        response.getWriter().flush();
    }
}
