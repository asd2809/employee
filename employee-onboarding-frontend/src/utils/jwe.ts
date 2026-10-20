import { CompactEncrypt, importSPKI, type CryptoKey } from 'jose'

/**
 * JWE（RFC7516）应用层加密工具。
 *
 * 算法组合：RSA-OAEP（密钥加密）+ A256GCM（内容加密）。
 * 使用 jose 成熟库完成全部密码学运算：CEK 随机生成、RSA-OAEP 封装、
 * AES-256-GCM 加密与 authTag 计算均由库内部完成，不手工拼接 IV、authTag 或二进制数组。
 *
 * 明文载荷结构（整个对象都在 JWE 内部，不会明文传输）：
 * {
 *   data: {},              // 业务请求参数
 *   timestamp: 毫秒时间戳,  // 后端据此判断请求是否过期
 *   nonce: UUID v4         // 后端据此在 Redis 中判重，抵御重放攻击
 * }
 *
 * 安全说明：本方案只解决传输层面的窃听、报文篡改、重放攻击；
 * 不能防御 SQL 注入、XSS，注入防护需要后端参数化查询、输入校验，前端 XSS 转义单独实现。
 */

/** 后端公钥接口地址（经 vite 代理转发到后端） */
const PUBLIC_KEY_URL = '/api/crypto/public-key'

/** 缓存的后端公钥，避免每次请求都重新拉取 */
let cachedPublicKey: CryptoKey | null = null
/** 缓存公钥对应的 keyId，用于判断后端密钥是否已轮换 */
let cachedKeyId = ''

interface PublicKeyResponse {
  code: number
  message: string
  data: {
    algorithm: string
    encryption: string
    keyId: string
    ephemeral: boolean
    pem: string
    jwk: string
  }
}

/**
 * 获取后端 RSA 公钥（带缓存）。
 * 若后端密钥已轮换（keyId 变化），缓存会失效并重新拉取。
 */
async function getPublicKey(forceRefresh = false): Promise<CryptoKey> {
  if (!forceRefresh && cachedPublicKey) {
    return cachedPublicKey
  }

  const response = await fetch(PUBLIC_KEY_URL)
  if (!response.ok) {
    throw new Error('获取加密公钥失败，请确认后端服务已启动')
  }
  const result = (await response.json()) as PublicKeyResponse
  if (result.code !== 200 || !result.data?.pem) {
    throw new Error('加密公钥响应异常')
  }

  // 签名算法固定为 RSA-OAEP，与后端白名单一致
  cachedPublicKey = await importSPKI(result.data.pem, 'RSA-OAEP')
  cachedKeyId = result.data.keyId
  return cachedPublicKey
}

/** 清除公钥缓存（后端重启导致临时密钥变化时使用） */
export function clearPublicKeyCache(): void {
  cachedPublicKey = null
  cachedKeyId = ''
}

/** 生成 UUID v4 作为 nonce（优先使用浏览器原生实现，兼容降级到 getRandomValues） */
function generateNonce(): string {
  if (typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  const bytes = new Uint8Array(16)
  crypto.getRandomValues(bytes)
  bytes[6] = (bytes[6]! & 0x0f) | 0x40 // version 4
  bytes[8] = (bytes[8]! & 0x3f) | 0x80 // variant 10
  const hex = Array.from(bytes, (b) => b.toString(16).padStart(2, '0')).join('')
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`
}

/**
 * 把业务参数包装成 {data, timestamp, nonce} 并加密为 JWE 紧凑序列化字符串。
 *
 * @param data 业务请求参数（即原本直接发送的 JSON）
 * @param forceRefreshKey 是否强制刷新公钥后重新加密（用于密钥轮换后的重试）
 * @returns 五段式 JWE 字符串，直接作为 HTTP Body 发送
 */
export async function encryptPayload(data: unknown, forceRefreshKey = false): Promise<string> {
  const publicKey = await getPublicKey(forceRefreshKey)

  const payload = {
    data,
    // 毫秒时间戳：后端与服务器时间比对，偏差超过 5 分钟拒绝
    timestamp: Date.now(),
    // 一次性随机数：后端写入 Redis，10 分钟内重复出现即判定为重放
    nonce: generateNonce(),
  }

  const plaintext = new TextEncoder().encode(JSON.stringify(payload))

  // CompactEncrypt 内部自动生成随机 CEK、IV，并计算 authTag
  return new CompactEncrypt(plaintext)
    .setProtectedHeader({ alg: 'RSA-OAEP', enc: 'A256GCM' })
    .encrypt(publicKey)
}

/** 当前缓存公钥的 keyId（调试用） */
export function getCachedKeyId(): string {
  return cachedKeyId
}