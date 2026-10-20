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
  account: string
  name: string
}

const TOKEN_KEY = 'onboarding-token'
const USER_KEY = 'onboarding-admin'

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

export function login(payload: LoginRequest) {
  return request<LoginResponse>('/api/employees/login', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function createEmployee(payload: CreateEmployeeRequest) {
  return request<{ id: number }>('/api/employees', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function getEmployees() {
  return request<EmployeeListItem[]>('/api/employees')
}

export function getCurrentUser() {
  return request<LoginUser>('/api/employees/me')
}
