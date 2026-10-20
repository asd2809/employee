package cn.bugstack.jinzhongdemo.crypto;

import lombok.Getter;

/**
 * JWE 传输层校验的错误码。
 *
 * <p>需求要求「区分异常类型：身份非法、密文篡改、时间过期、nonce 重复，返回不同业务码」，
 * 因此这里为每一种失败原因定义独立的业务码，便于前端与日志定位问题。</p>
 *
 * <p>注意：身份非法（JWT 校验失败）由 Spring Security 的 RestAuthenticationEntryPoint 返回 401，
 * 与 JWE 的报错互不混淆——JWT 负责「你是谁」，JWE 负责「报文是否可信」。</p>
 */
@Getter
public enum JweErrorCode {

    /** 密文本身不是合法的 JWE 紧凑格式 */
    JWE_MALFORMED(40001, 400, "密文格式错误"),

    /** alg/enc 不在白名单（防降级攻击） */
    JWE_ALGORITHM_NOT_ALLOWED(40002, 400, "不支持的加密算法，仅允许 RSA-OAEP + A256GCM"),

    /** GCM authTag 校验失败，报文被篡改 */
    JWE_TAMPERED(40003, 400, "报文完整性校验失败，疑似被篡改"),

    /** 受保护接口必须传 JWE 密文 */
    JWE_MISSING(40004, 400, "请求体必须为 JWE 密文"),

    /** 载荷缺少 timestamp 或不是合法毫秒时间戳 */
    TIMESTAMP_INVALID(40005, 400, "请求时间戳缺失或非法"),

    /** 时间偏差超过允许范围，拒绝（防重放的第一道闸） */
    TIMESTAMP_EXPIRED(40006, 400, "请求已过期，请检查客户端系统时间"),

    /** 载荷缺少 nonce 或格式非法 */
    NONCE_INVALID(40007, 400, "请求随机数 nonce 缺失或非法"),

    /** Redis 中已存在该 nonce，判定为重放攻击 */
    NONCE_REPLAY(40008, 400, "检测到重放攻击，该请求已被处理过"),

    /** 解密后载荷结构不是 {data, timestamp, nonce} */
    PAYLOAD_INVALID(40009, 400, "密文载荷结构非法"),

    /** 开启了强制 HTTPS 但请求不是 https */
    HTTPS_REQUIRED(40010, 403, "必须使用 HTTPS 访问"),

    /** nonce 存储（Redis）不可用，按安全优先原则拒绝请求 */
    NONCE_STORE_UNAVAILABLE(50001, 500, "nonce 校验服务暂不可用"),

    /** 过滤器内部未预期异常，一律拒绝请求 */
    INTERNAL_ERROR(50000, 500, "服务端处理请求异常");

    /** 业务码，与 HTTP 状态码区分开，便于前端按业务码做不同处理 */
    private final int code;

    /** 对应的 HTTP 状态码 */
    private final int httpStatus;

    /** 提示信息 */
    private final String message;

    JweErrorCode(int code, int httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
