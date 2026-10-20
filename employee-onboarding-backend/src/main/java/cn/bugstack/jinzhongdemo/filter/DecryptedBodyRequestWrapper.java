package cn.bugstack.jinzhongdemo.filter;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * 解密后的请求体包装器。
 *
 * <p>前端发来的是 JWE 密文字符串，但 Controller 只认识业务 JSON。
 * 这里把「解密后的 data 部分」替换成新的请求体，并把 Content-Type 统一改写为 application/json，
 * 于是 Controller 里的 {@code @RequestBody DTO} 无需任何改动即可正常绑定——
 * 这是「不影响原业务代码」的关键。</p>
 */
public class DecryptedBodyRequestWrapper extends HttpServletRequestWrapper {

    /** 解密后的业务 JSON（原始载荷中的 data 字段） */
    private final byte[] body;

    public DecryptedBodyRequestWrapper(HttpServletRequest request, byte[] body) {
        super(request);
        this.body = body;
    }

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(body);
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return byteArrayInputStream.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                // 同步读取场景，无需异步回调
            }

            @Override
            public int read() {
                return byteArrayInputStream.read();
            }
        };
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }

    @Override
    public int getContentLength() {
        return body.length;
    }

    @Override
    public long getContentLengthLong() {
        return body.length;
    }

    /** 强制为 JSON，Spring 的 JSON 消息转换器据此选择 Jackson 反序列化 */
    @Override
    public String getContentType() {
        return MediaType.APPLICATION_JSON_VALUE;
    }

    /**
     * 同时改写请求头中的 Content-Type。
     *
     * <p>Spring MVC 解析 {@code @RequestBody} 时，是从请求头集合里读 Content-Type 的，
     * 仅覆盖 {@link #getContentType()} 不够——原始头的 application/jose 仍会保留，
     * 导致没有可用的 HttpMessageConverter 而抛出 415。因此这里把头也一并改写成 json。</p>
     */
    @Override
    public String getHeader(String name) {
        if (HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(name)) {
            return MediaType.APPLICATION_JSON_VALUE;
        }
        return super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        if (HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(name)) {
            return Collections.enumeration(List.of(MediaType.APPLICATION_JSON_VALUE));
        }
        return super.getHeaders(name);
    }
}
