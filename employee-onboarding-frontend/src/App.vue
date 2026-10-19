<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { createEmployee, getEmployees, login, type CreateEmployeeRequest, type EmployeeListItem, type LoginUser } from './api/employee'

type FieldErrors = Partial<Record<keyof CreateEmployeeRequest, string>>
const phonePattern = /^1[3-9]\d{9}$/
const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const accountPattern = /^[A-Za-z0-9_]{4,20}$/
const savedUser = sessionStorage.getItem('onboarding-admin')
const currentUser = ref<LoginUser | null>(savedUser ? JSON.parse(savedUser) : null)
const activeMenu = ref<'employees' | 'create'>('employees')
const employees = ref<EmployeeListItem[]>([])
const listLoading = ref(false), loginLoading = ref(false), createLoading = ref(false)
const loginError = ref(''), createMessage = ref(''), createError = ref('')
const fieldErrors = ref<FieldErrors>({})
const loginForm = reactive({ account: '', password: '' })
const employeeForm = reactive<CreateEmployeeRequest>({ account: '', password: '', name: '', gender: 1, phone: '', email: '' })
const employeeCount = computed(() => employees.value.length)

async function handleLogin() {
  loginError.value = ''
  if (!loginForm.account.trim() || !loginForm.password) { loginError.value = '请输入账号和密码'; return }
  loginLoading.value = true
  try {
    const user = await login({ account: loginForm.account.trim(), password: loginForm.password })
    if (user.role !== 1) throw new Error('该账号没有管理员权限')
    currentUser.value = user
    sessionStorage.setItem('onboarding-admin', JSON.stringify(user))
    await loadEmployees()
  } catch (error) { loginError.value = error instanceof Error ? error.message : '登录失败' }
  finally { loginLoading.value = false }
}

async function loadEmployees() {
  listLoading.value = true
  try { employees.value = await getEmployees() }
  catch (error) { createError.value = error instanceof Error ? error.message : '员工列表加载失败' }
  finally { listLoading.value = false }
}

function validateEmployeeForm() {
  const errors: FieldErrors = {}
  const account = employeeForm.account.trim(), name = employeeForm.name.trim(), phone = employeeForm.phone.trim(), email = employeeForm.email.trim()
  if (!account) errors.account = '请输入登录账号'
  else if (!accountPattern.test(account)) errors.account = '账号需为 4-20 位字母、数字或下划线'
  else if (employees.value.some((item) => item.account === account)) errors.account = '该账号已存在'
  if (!employeeForm.password) errors.password = '请输入初始密码'
  else if (employeeForm.password.length < 6) errors.password = '密码至少需要 6 位'
  if (!name) errors.name = '请输入员工姓名'
  if (!phone) errors.phone = '请输入手机号'
  else if (!phonePattern.test(phone)) errors.phone = '请输入正确的 11 位手机号'
  if (!email) errors.email = '请输入邮箱'
  else if (!emailPattern.test(email)) errors.email = '请输入正确的邮箱地址'
  fieldErrors.value = errors
  return Object.keys(errors).length === 0
}

async function handleCreateEmployee() {
  createMessage.value = ''; createError.value = ''
  if (!validateEmployeeForm()) return
  createLoading.value = true
  try {
    const result = await createEmployee({ ...employeeForm, account: employeeForm.account.trim(), name: employeeForm.name.trim(), phone: employeeForm.phone.trim(), email: employeeForm.email.trim() })
    createMessage.value = '员工账号创建成功，员工 ID：' + result.id
    Object.assign(employeeForm, { account: '', password: '', name: '', gender: 1, phone: '', email: '' })
    fieldErrors.value = {}
    await loadEmployees()
  } catch (error) { createError.value = error instanceof Error ? error.message : '账号创建失败' }
  finally { createLoading.value = false }
}

function logout() {
  currentUser.value = null
  sessionStorage.removeItem('onboarding-admin')
  loginForm.password = ''
  activeMenu.value = 'employees'
}
onMounted(() => { if (currentUser.value) loadEmployees() })
</script>

<template>
  <div v-if="!currentUser" class="login-page">
    <section class="brand-panel"><div class="brand-mark">EO</div><div><p class="eyebrow">EMPLOYEE ONBOARDING</p><h1>让每一次入职，<br>都从容有序。</h1><p class="brand-copy">集中管理员工账号，让新同事顺利开启工作的第一天。</p></div><div class="brand-decoration"><span></span><span></span><span></span></div></section>
    <main class="login-main"><form class="login-card" @submit.prevent="handleLogin"><div class="mobile-logo">EO</div><p class="eyebrow accent">管理员工作台</p><h2>欢迎回来</h2><p class="muted">请使用管理员账号登录系统</p><label class="field"><span>账号</span><div class="input-wrap"><span class="input-icon">@</span><input v-model="loginForm.account" autocomplete="username" placeholder="请输入管理员账号"></div></label><label class="field"><span>密码</span><div class="input-wrap"><span class="input-icon">●</span><input v-model="loginForm.password" type="password" autocomplete="current-password" placeholder="请输入密码"></div></label><p v-if="loginError" class="feedback error">{{ loginError }}</p><button class="primary-button full" type="submit" :disabled="loginLoading">{{ loginLoading ? '正在登录…' : '登录管理后台' }}</button><p class="login-hint">账号由系统管理员统一分配</p></form></main>
  </div>
  <div v-else class="admin-layout">
    <aside class="sidebar"><div class="sidebar-brand"><span class="brand-mark small">EO</span><strong>入职管理</strong></div><nav><button :class="{ active: activeMenu === 'employees' }" @click="activeMenu = 'employees'"><span>▦</span> 员工列表</button><button :class="{ active: activeMenu === 'create' }" @click="activeMenu = 'create'"><span>＋</span> 新建账号</button></nav><div class="sidebar-user"><div class="avatar">{{ currentUser.name?.slice(0, 1) || '管' }}</div><div><strong>{{ currentUser.name || currentUser.account }}</strong><small>管理员</small></div><button title="退出登录" @click="logout">↪</button></div></aside>
    <main class="workspace"><header class="topbar"><div><p class="eyebrow accent">EMPLOYEE ONBOARDING</p><h1>{{ activeMenu === 'employees' ? '新员工列表' : '创建员工账号' }}</h1></div><button v-if="activeMenu === 'employees'" class="primary-button" @click="activeMenu = 'create'">＋ 新建员工</button></header>
      <section v-if="activeMenu === 'employees'"><div class="stats-card"><div class="stats-icon">♟</div><div><strong>{{ employeeCount }}</strong><span>当前员工账号</span></div><p>账号信息已与后端实时同步</p></div><div class="content-card table-card"><div class="card-heading"><div><h2>账号列表</h2><p>查看已经完成账号创建的新员工</p></div><button class="ghost-button" :disabled="listLoading" @click="loadEmployees">↻ 刷新</button></div><div v-if="listLoading" class="empty-state">正在加载员工数据…</div><div v-else-if="!employees.length" class="empty-state"><div>＋</div><h3>还没有员工账号</h3><p>点击“新建员工”创建第一个账号</p></div><div v-else class="table-scroll"><table><thead><tr><th>序号</th><th>登录账号</th><th>员工姓名</th><th>账号状态</th></tr></thead><tbody><tr v-for="(employee, index) in employees" :key="employee.account"><td>{{ String(index + 1).padStart(2, '0') }}</td><td><span class="account-cell"><span class="mini-avatar">{{ employee.name.slice(0, 1) }}</span>{{ employee.account }}</span></td><td>{{ employee.name }}</td><td><span class="status">正常</span></td></tr></tbody></table></div></div></section>
      <section v-else class="content-card form-card"><div class="card-heading"><div><h2>填写员工信息</h2><p>创建后员工可以使用账号和初始密码登录</p></div></div><form class="employee-form" @submit.prevent="handleCreateEmployee"><label class="field"><span>登录账号 <b>*</b></span><input v-model="employeeForm.account" placeholder="4-20 位字母、数字或下划线"><small v-if="fieldErrors.account">{{ fieldErrors.account }}</small></label><label class="field"><span>初始密码 <b>*</b></span><input v-model="employeeForm.password" type="password" autocomplete="new-password" placeholder="至少 6 位"><small v-if="fieldErrors.password">{{ fieldErrors.password }}</small></label><label class="field"><span>员工姓名 <b>*</b></span><input v-model="employeeForm.name" placeholder="请输入真实姓名"><small v-if="fieldErrors.name">{{ fieldErrors.name }}</small></label><fieldset class="field gender-field"><legend>性别 <b>*</b></legend><div class="radio-group"><label><input v-model="employeeForm.gender" type="radio" :value="1"><span>男</span></label><label><input v-model="employeeForm.gender" type="radio" :value="0"><span>女</span></label></div></fieldset><label class="field"><span>手机号 <b>*</b></span><input v-model="employeeForm.phone" inputmode="numeric" maxlength="11" placeholder="请输入 11 位手机号"><small v-if="fieldErrors.phone">{{ fieldErrors.phone }}</small></label><label class="field"><span>Email <b>*</b></span><input v-model="employeeForm.email" type="email" placeholder="name@example.com"><small v-if="fieldErrors.email">{{ fieldErrors.email }}</small></label><div v-if="createMessage || createError" class="form-feedback" :class="{ error: createError }">{{ createError || createMessage }}</div><div class="form-actions"><button type="button" class="ghost-button" @click="activeMenu = 'employees'">取消</button><button class="primary-button" type="submit" :disabled="createLoading">{{ createLoading ? '正在创建…' : '创建员工账号' }}</button></div></form></section>
    </main>
  </div>
</template>
