import { encryptPayload } from './jwe'

/**
 * axios 请求拦截器示例（仅作参考，项目当前使用原生 fetch，未强制引入 axios）。
 *
 * 若你的项目使用 axios，可这样接入 JWE：任何 POST 请求发出前，
 * 把 config.data 加密成 JWE 字符串，并改写 Content-Type。
 *
 * 为了让本文件在不安装 axios 的情况下也能通过类型检查，这里只声明用到的
 * 最小接口结构，不 import axios。使用时把真实的 axios 实例传进来即可。
 */

/** axios 拦截器用到的配置结构（最小子集） */
interface AxiosLikeRequestConfig {
  url?: string
  method?: string
  data?: unknown
  headers?: Record<string, unknown>
}

/** axios 实例的最小接口（仅声明我们用到的方法） */
interface AxiosLikeInstance {
  interceptors: {
    request: {
      use(fulfilled: (config: AxiosLikeRequestConfig) => Promise<AxiosLikeRequestConfig>): unknown
    }
  }
}

/** 需要加密的接口：与后端 jwe.rules 配置保持一致 */
const SECURE_RULES: Array<{ method: string; pattern: RegExp }> = [
  { method: 'post', pattern: /\/api\/employees\/login$/ },
  { method: 'post', pattern: /\/api\/employees$/ },
]

/** 判断当前请求是否命中加密规则 */
function shouldEncrypt(url: string | undefined, method: string | undefined): boolean {
  if (!url || !method) {
    return false
  }
  const lowerMethod = method.toLowerCase()
  // 去掉 query 后再匹配
  const path = url.split('?')[0] ?? url
  return SECURE_RULES.some((rule) => rule.method === lowerMethod && rule.pattern.test(path))
}

/**
 * 安装 JWE 请求拦截器。
 *
 * 使用示例：
 * ```ts
 * import axios from 'axios'
 * import { installJweInterceptor } from '@/utils/jwe-axios.example'
 * const http = axios.create({ baseURL: '/api' })
 * installJweInterceptor(http)
 * ```
 */
export function installJweInterceptor(instance: AxiosLikeInstance): void {
  instance.interceptors.request.use(async (config) => {
    if (!shouldEncrypt(config.url, config.method)) {
      return config
    }
    // 把业务参数包装成 {data, timestamp, nonce} 并加密
    const jwe = await encryptPayload(config.data)
    config.data = jwe
    config.headers = {
      ...config.headers,
      // 密文以 application/jose 发送，服务端按原始 Body 读取后解密
      'Content-Type': 'application/jose',
    }
    return config
  })
}
