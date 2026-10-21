<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { createEmployee, deleteEmployee, getEmployees, login, setToken, clearSession, type CreateEmployeeRequest, type EmployeeListItem, type LoginUser } from './api/employee'
import AccountSettings from './components/AccountSettings.vue'
import EmployeeManageDialog from './components/EmployeeManageDialog.vue'
import EmployeeWorkspace from './views/EmployeeWorkspace.vue'

type FieldErrors = Partial<Record<keyof CreateEmployeeRequest, string>>
const phonePattern = /^1[3-9]\d{9}$/
const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const accountPattern = /^[A-Za-z0-9_]{4,20}$/
const USER_KEY = 'onboarding-admin'
const savedUser = sessionStorage.getItem(USER_KEY)
const currentUser = ref<LoginUser | null>(savedUser ? JSON.parse(savedUser) : null)
const loginRole = ref<'admin' | 'employee'>('admin')
const activeMenu = ref<'employees' | 'create' | 'account'>('employees')
const managedEmployee = ref<EmployeeListItem | null>(null)
const employees = ref<EmployeeListItem[]>([])
const listLoading = ref(false), loginLoading = ref(false), createLoading = ref(false)
const loginError = ref(''), createMessage = ref(''), createError = ref(''), listError = ref('')
const fieldErrors = ref<FieldErrors>({})
const loginForm = reactive({ account: '', password: '' })
const employeeForm = reactive<CreateEmployeeRequest>({ account: '', password: '', name: '', gender: 1, phone: '', email: '' })
const employeeCount = computed(() => employees.value.length)
const pageTitle = computed(() => {
  if (activeMenu.value === 'create') return '创建员工账号'
  if (activeMenu.value === 'account') return '我的账号'
  return '新员工列表'
})

async function handleLogin() {
  loginError.value = ''
  if (!loginForm.account.trim() || !loginForm.password) { loginError.value = '请输入账号和密码'; return }
  loginLoading.value = true
  try {
    const res = await login({ account: loginForm.account.trim(), password: loginForm.password })
    // 登录入口区分管理员与员工，避免进错工作台
    if (loginRole.value === 'admin' && res.user.role !== 1) throw new Error('该账号不是管理员，请切换到「员工登录」')
    if (loginRole.value === 'employee' && res.user.role === 1) throw new Error('管理员账号请使用「管理员登录」入口')
    setToken(res.token)
    currentUser.value = res.user
    sessionStorage.setItem(USER_KEY, JSON.stringify(res.user))
    if (res.user.role === 1) await loadEmployees()
  } catch (error) { loginError.value = error instanceof Error ? error.message : '登录失败' }
  finally { loginLoading.value = false }
}

async function loadEmployees() {
  listLoading.value = true
  listError.value = ''
  try { employees.value = await getEmployees() }
  catch (error) { listError.value = error instanceof Error ? error.message : '员工列表加载失败' }
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

async function handleDeleteEmployee(employee: EmployeeListItem) {
  if (!window.confirm(`确定删除员工「${employee.name}」的账号吗？删除后该账号将无法登录。`)) return
  listError.value = ''
  try {
    await deleteEmployee(employee.id)
    await loadEmployees()
  } catch (error) { listError.value = error instanceof Error ? error.message : '删除失败，请稍后重试' }
}

async function handleEmployeeChanged(updated?: EmployeeListItem) {
  await loadEmployees()
  // 管理员修改的是自己的资料时，同步侧边栏与本地登录态
  if (updated && currentUser.value && updated.id === currentUser.value.id) {
    handleUserUpdated({ ...currentUser.value, ...updated })
  }
}

function handleUserUpdated(user: LoginUser) {
  currentUser.value = user
  sessionStorage.setItem(USER_KEY, JSON.stringify(user))
}

function switchRole(role: 'admin' | 'employee') {
  loginRole.value = role
  loginError.value = ''
}

function logout() {
  currentUser.value = null
  managedEmployee.value = null
  clearSession()
  loginForm.password = ''
  activeMenu.value = 'employees'
}
onMounted(() => { if (currentUser.value?.role === 1) loadEmployees() })
</script>

<template>
  <div v-if="!currentUser" class="login-page">
    <section class="brand-panel"><div class="brand-mark">EO</div><div><p class="eyebrow">EMPLOYEE ONBOARDING</p><h1>让每一次入职，<br>都从容有序。</h1><p class="brand-copy">集中管理员工账号，让新同事顺利开启工作的第一天。</p></div><div class="brand-decoration"><span></span><span></span><span></span></div></section>
    <main class="login-main"><form class="login-card" @submit.prevent="handleLogin">
      <div class="mobile-logo">EO</div>
      <div class="role-switch">
        <button type="button" :class="{ active: loginRole === 'admin' }" @click="switchRole('admin')">管理员登录</button>
        <button type="button" :class="{ active: loginRole === 'employee' }" @click="switchRole('employee')">员工登录</button>
      </div>
      <p class="eyebrow accent">{{ loginRole === 'admin' ? '管理员工作台' : '员工自助中心' }}</p>
      <h2>欢迎回来</h2>
      <p class="muted">{{ loginRole === 'admin' ? '请使用管理员账号登录系统' : '请使用员工账号登录个人中心' }}</p>
      <label class="field"><span>账号</span><div class="input-wrap"><span class="input-icon">@</span><input v-model="loginForm.account" autocomplete="username" :placeholder="loginRole === 'admin' ? '请输入管理员账号' : '请输入员工账号'"></div></label>
      <label class="field"><span>密码</span><div class="input-wrap"><span class="input-icon">●</span><input v-model="loginForm.password" type="password" autocomplete="current-password" placeholder="请输入密码"></div></label>
      <p v-if="loginError" class="feedback error">{{ loginError }}</p>
      <button class="primary-button full" type="submit" :disabled="loginLoading">{{ loginLoading ? '正在登录…' : (loginRole === 'admin' ? '登录管理后台' : '登录员工中心') }}</button>
      <p class="login-hint">{{ loginRole === 'admin' ? '账号由系统管理员统一分配' : '如需开通账号请联系管理员' }}</p>
    </form></main>
  </div>
  <EmployeeWorkspace v-else-if="currentUser.role !== 1" :user="currentUser" @updated="handleUserUpdated" @logout="logout" />
  <div v-else class="admin-layout">
    <aside class="sidebar"><div class="sidebar-brand"><span class="brand-mark small">EO</span><strong>入职管理</strong></div><nav><button :class="{ active: activeMenu === 'employees' }" @click="activeMenu = 'employees'"><span>▦</span> 员工列表</button><button :class="{ active: activeMenu === 'create' }" @click="activeMenu = 'create'"><span>＋</span> 新建账号</button><button :class="{ active: activeMenu === 'account' }" @click="activeMenu = 'account'"><span>◎</span> 我的账号</button></nav><div class="sidebar-user"><div class="avatar">{{ currentUser.name?.slice(0, 1) || '管' }}</div><div><strong>{{ currentUser.name || currentUser.account }}</strong><small>管理员</small></div><button title="退出登录" @click="logout">↪</button></div></aside>
    <main class="workspace"><header class="topbar"><div><p class="eyebrow accent">EMPLOYEE ONBOARDING</p><h1>{{ pageTitle }}</h1></div><button v-if="activeMenu === 'employees'" class="primary-button" @click="activeMenu = 'create'">＋ 新建员工</button></header>
      <section v-if="activeMenu === 'employees'"><div class="stats-card"><div class="stats-icon">♟</div><div><strong>{{ employeeCount }}</strong><span>当前员工账号</span></div><p>账号信息已与后端实时同步</p></div><div class="content-card table-card"><div class="card-heading"><div><h2>账号列表</h2><p>查看并管理已创建的员工账号</p></div><button class="ghost-button" :disabled="listLoading" @click="loadEmployees">↻ 刷新</button></div><p v-if="listError" class="list-error">{{ listError }}</p><div v-if="listLoading" class="empty-state">正在加载员工数据…</div><div v-else-if="!employees.length" class="empty-state"><div>＋</div><h3>还没有员工账号</h3><p>点击“新建员工”创建第一个账号</p></div><div v-else class="table-scroll"><table><thead><tr><th>序号</th><th>登录账号</th><th>员工姓名</th><th>手机号</th><th>Email</th><th>角色</th><th>操作</th></tr></thead><tbody><tr v-for="(employee, index) in employees" :key="employee.id"><td>{{ String(index + 1).padStart(2, '0') }}</td><td><span class="account-cell"><span class="mini-avatar">{{ employee.name.slice(0, 1) }}</span>{{ employee.account }}</span></td><td>{{ employee.name }}</td><td>{{ employee.phone || '—' }}</td><td>{{ employee.email || '—' }}</td><td><span class="status">{{ employee.role === 1 ? '管理员' : '员工' }}</span></td><td><div class="row-actions"><button class="ghost-button small" @click="managedEmployee = employee">编辑</button><button v-if="employee.role !== 1 && employee.id !== currentUser.id" class="danger-button small" @click="handleDeleteEmployee(employee)">删除</button></div></td></tr></tbody></table></div></div></section>
      <section v-else-if="activeMenu === 'create'" class="content-card form-card"><div class="card-heading"><div><h2>填写员工信息</h2><p>创建后员工可以使用账号和初始密码登录</p></div></div><form class="employee-form" @submit.prevent="handleCreateEmployee"><label class="field"><span>登录账号 <b>*</b></span><input v-model="employeeForm.account" placeholder="4-20 位字母、数字或下划线"><small v-if="fieldErrors.account">{{ fieldErrors.account }}</small></label><label class="field"><span>初始密码 <b>*</b></span><input v-model="employeeForm.password" type="password" autocomplete="new-password" placeholder="至少 6 位"><small v-if="fieldErrors.password">{{ fieldErrors.password }}</small></label><label class="field"><span>员工姓名 <b>*</b></span><input v-model="employeeForm.name" placeholder="请输入真实姓名"><small v-if="fieldErrors.name">{{ fieldErrors.name }}</small></label><fieldset class="field gender-field"><legend>性别 <b>*</b></legend><div class="radio-group"><label><input v-model="employeeForm.gender" type="radio" :value="1"><span>男</span></label><label><input v-model="employeeForm.gender" type="radio" :value="0"><span>女</span></label></div></fieldset><label class="field"><span>手机号 <b>*</b></span><input v-model="employeeForm.phone" inputmode="numeric" maxlength="11" placeholder="请输入 11 位手机号"><small v-if="fieldErrors.phone">{{ fieldErrors.phone }}</small></label><label class="field"><span>Email <b>*</b></span><input v-model="employeeForm.email" type="email" placeholder="name@example.com"><small v-if="fieldErrors.email">{{ fieldErrors.email }}</small></label><div v-if="createMessage || createError" class="form-feedback" :class="{ error: createError }">{{ createError || createMessage }}</div><div class="form-actions"><button type="button" class="ghost-button" @click="activeMenu = 'employees'">取消</button><button class="primary-button" type="submit" :disabled="createLoading">{{ createLoading ? '正在创建…' : '创建员工账号' }}</button></div></form></section>
      <section v-else><AccountSettings :user="currentUser" @updated="handleUserUpdated" /></section>
    </main>
    <EmployeeManageDialog v-if="managedEmployee" :employee="managedEmployee" :current-user-id="currentUser.id" @close="managedEmployee = null" @changed="handleEmployeeChanged" />
  </div>
</template>

<style scoped>
.role-switch { display: grid; grid-template-columns: 1fr 1fr; gap: 6px; margin-bottom: 24px; padding: 4px; border: 1px solid var(--line); border-radius: 10px; background: #f5f9f9; }
.role-switch button { height: 40px; border: 0; border-radius: 7px; color: var(--muted); background: transparent; font-weight: 650; }
.role-switch button.active { color: var(--teal-dark); background: #fff; box-shadow: 0 1px 4px #152c3a14; }
.list-error { margin: 16px 28px 0; border-radius: 8px; padding: 10px 14px; color: var(--danger); background: #fff1f1; font-size: 13px; }
.row-actions { display: flex; gap: 8px; white-space: nowrap; }
.ghost-button.small, .danger-button.small { min-height: 32px; padding: 0 12px; border-radius: 7px; font-size: 12px; }
.danger-button { border: 1px solid #f0cccc; background: #fff; color: var(--danger); }
.danger-button:hover { background: #fff5f5; }
</style>
