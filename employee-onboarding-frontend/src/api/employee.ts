import { encryptPayload } from '@/utils/jwe'

export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export interface LoginRequest {
  account: string
  password: string
}

export interface LoginUser {
  id: number
  account: string
  name: string
  gender: number
  phone: string
  email: string
  role: number
}

/** 登录响应：JWT 令牌 + 登录用户信息 */
export interface LoginResponse {
  token: string
  tokenType: string
  expiresIn: number
  user: LoginUser
}

export interface CreateEmployeeRequest {
  account: string
  password: string
  name: string
  gender: number
  phone: string
  email: string
}

export interface EmployeeListItem {
  id: number
  account: string
  name: string
  gender: number
  phone: string
  email: string
  role: number
}

/** 修改资料：账号、角色、密码不在该接口内，避免越权 */
export interface UpdateEmployeeRequest {
  name: string
  gender: number
  phone: string
  email: string
}

/** 修改密码：本人修改必传 oldPassword；管理员重置他人员工密码时可不传 */
export interface ChangePasswordRequest {
  oldPassword?: string
  newPassword: string
}

const TOKEN_KEY = 'onboarding-token'
const USER_KEY = 'onboarding-admin'

/** JWE 密文的 Content-Type，后端过滤器据此识别（实际按原始 Body 读取，不依赖它） */
const JWE_CONTENT_TYPE = 'application/jose'

export function getToken(): string | null {
  return sessionStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string): void {
  sessionStorage.setItem(TOKEN_KEY, token)
}

/** 清除全部登录态（令牌 + 用户信息） */
export function clearSession(): void {
  sessionStorage.removeItem(TOKEN_KEY)
  sessionStorage.removeItem(USER_KEY)
}

async function request<T>(url: string, options?: RequestInit): Promise<T> {
  let response: Response
  const token = getToken()

  try {
    response = await fetch(url, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        // 已登录时自动携带令牌，后端据此鉴权
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...options?.headers,
      },
    })
  } catch {
    throw new Error('无法连接服务器，请确认后端服务已启动')
  }

  let result: ApiResult<T>
  try {
    result = (await response.json()) as ApiResult<T>
  } catch {
    throw new Error('服务器返回了无法识别的数据')
  }

  if (!response.ok || result.code !== 200) {
    // 令牌失效时清除登录态并回到登录页（登录接口自身的 401 是密码错误，不在此列）
    if (response.status === 401 && !url.endsWith('/login')) {
      clearSession()
      window.location.reload()
    }
    throw new Error(result.message || '请求失败，请稍后重试')
  }

  return result.data
}

/**
 * 受 JWE 保护的请求：业务参数先加密成 JWE 密文再作为 Body 发送。
 *
 * 若后端因密钥轮换（例如临时密钥重启）导致解密失败，业务码为 40003，
 * 此时清掉公钥缓存重试一次，避免用户手动刷新。
 */
async function secureRequest<T>(url: string, data: unknown, options?: RequestInit): Promise<T> {
  const body = await encryptPayload(data)
  try {
    return await request<T>(url, {
      ...options,
      method: options?.method ?? 'POST',
      headers: { 'Content-Type': JWE_CONTENT_TYPE, ...options?.headers },
      body,
    })
  } catch (error) {
    if (error instanceof Error && error.message.includes('完整性校验失败')) {
      const retriedBody = await encryptPayload(data, true)
      return await request<T>(url, {
        ...options,
        method: options?.method ?? 'POST',
        headers: { 'Content-Type': JWE_CONTENT_TYPE, ...options?.headers },
        body: retriedBody,
      })
    }
    throw error
  }
}

export function login(payload: LoginRequest) {
  // 登录请求体经 JWE 加密，账号密码不会以明文出现在抓包中
  return secureRequest<LoginResponse>('/api/employees/login', payload)
}

export function createEmployee(payload: CreateEmployeeRequest) {
  // 新建员工同样经 JWE 加密
  return secureRequest<{ id: number }>('/api/employees', payload)
}

export function getEmployees() {
  return request<EmployeeListItem[]>('/api/employees')
}

export function getCurrentUser() {
  return request<LoginUser>('/api/employees/me')
}

export function getEmployee(id: number) {
  return request<EmployeeListItem>(`/api/employees/${id}`)
}

/** 修改员工资料（本人或管理员），经 JWE 加密 */
export function updateEmployee(id: number, payload: UpdateEmployeeRequest) {
  return secureRequest<EmployeeListItem>(`/api/employees/${id}`, payload, { method: 'PUT' })
}

/** 修改/重置密码，独立接口，经 JWE 加密 */
export function changePassword(id: number, payload: ChangePasswordRequest) {
  return secureRequest<null>(`/api/employees/${id}/password`, payload, { method: 'PUT' })
}

/** 删除员工（逻辑删除），仅管理员，经 JWE 加密 */
export function deleteEmployee(id: number) {
  return secureRequest<null>(`/api/employees/${id}`, {}, { method: 'DELETE' })
}
