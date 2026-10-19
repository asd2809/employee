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

async function request<T>(url: string, options?: RequestInit): Promise<T> {
  let response: Response

  try {
    response = await fetch(url, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
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
    throw new Error(result.message || '请求失败，请稍后重试')
  }

  return result.data
}

export function login(payload: LoginRequest) {
  return request<LoginUser>('/api/employees/login', {
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
