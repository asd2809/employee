<script setup lang="ts">
import { reactive, ref } from 'vue'
import { changePassword, deleteEmployee, updateEmployee, type EmployeeListItem } from '@/api/employee'

const props = defineProps<{ employee: EmployeeListItem; currentUserId: number }>()
const emit = defineEmits<{
  (e: 'close'): void
  (e: 'changed', employee?: EmployeeListItem): void
}>()

const phonePattern = /^1[3-9]\d{9}$/
const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

const form = reactive({
  name: props.employee.name ?? '',
  gender: props.employee.gender ?? 1,
  phone: props.employee.phone ?? '',
  email: props.employee.email ?? '',
})
const formErrors = ref<{ name?: string; phone?: string; email?: string }>({})
const saving = ref(false)
const message = ref('')
const errorMessage = ref('')

const newPassword = ref('')
const resetting = ref(false)
const resetMessage = ref('')
const resetError = ref('')
const deleting = ref(false)

const isAdminAccount = props.employee.role === 1
const isSelf = props.employee.id === props.currentUserId

function validate() {
  const errors: { name?: string; phone?: string; email?: string } = {}
  if (!form.name.trim()) errors.name = '请输入姓名'
  if (!form.phone.trim()) errors.phone = '请输入手机号'
  else if (!phonePattern.test(form.phone.trim())) errors.phone = '请输入正确的 11 位手机号'
  if (!form.email.trim()) errors.email = '请输入邮箱'
  else if (!emailPattern.test(form.email.trim())) errors.email = '请输入正确的邮箱地址'
  formErrors.value = errors
  return Object.keys(errors).length === 0
}

async function saveProfile() {
  message.value = ''
  errorMessage.value = ''
  if (!validate()) return
  saving.value = true
  try {
    const updated = await updateEmployee(props.employee.id, {
      name: form.name.trim(),
      gender: form.gender,
      phone: form.phone.trim(),
      email: form.email.trim(),
    })
    message.value = '资料已保存'
    emit('changed', updated)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '保存失败，请稍后重试'
  } finally {
    saving.value = false
  }
}

async function resetPassword() {
  resetMessage.value = ''
  resetError.value = ''
  if (!newPassword.value || newPassword.value.length < 6) {
    resetError.value = '新密码至少需要 6 位'
    return
  }
  resetting.value = true
  try {
    await changePassword(props.employee.id, { newPassword: newPassword.value })
    resetMessage.value = '密码已重置，请通知员工使用新密码登录'
    newPassword.value = ''
  } catch (error) {
    resetError.value = error instanceof Error ? error.message : '重置失败，请稍后重试'
  } finally {
    resetting.value = false
  }
}

async function removeEmployee() {
  if (!window.confirm(`确定删除员工「${props.employee.name}」的账号吗？删除后该账号将无法登录。`)) return
  deleting.value = true
  try {
    await deleteEmployee(props.employee.id)
    emit('changed')
    emit('close')
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '删除失败，请稍后重试'
  } finally {
    deleting.value = false
  }
}
</script>

<template>
  <div class="dialog-mask" @click.self="emit('close')">
    <div class="dialog-card" role="dialog" aria-modal="true">
      <header class="dialog-head">
        <div>
          <h2>管理员工资料</h2>
          <p>{{ employee.account }} · {{ isAdminAccount ? '管理员' : '员工' }}</p>
        </div>
        <button class="close-button" type="button" @click="emit('close')">×</button>
      </header>

      <div class="dialog-body">
        <form class="dialog-form" @submit.prevent="saveProfile">
          <label class="field">
            <span>姓名 <b>*</b></span>
            <input v-model="form.name" placeholder="请输入真实姓名">
            <small v-if="formErrors.name">{{ formErrors.name }}</small>
          </label>
          <fieldset class="field gender-field">
            <legend>性别 <b>*</b></legend>
            <div class="radio-group">
              <label><input v-model="form.gender" type="radio" :value="1"><span>男</span></label>
              <label><input v-model="form.gender" type="radio" :value="0"><span>女</span></label>
            </div>
          </fieldset>
          <label class="field">
            <span>手机号 <b>*</b></span>
            <input v-model="form.phone" inputmode="numeric" maxlength="11" placeholder="请输入 11 位手机号">
            <small v-if="formErrors.phone">{{ formErrors.phone }}</small>
          </label>
          <label class="field">
            <span>Email <b>*</b></span>
            <input v-model="form.email" type="email" placeholder="name@example.com">
            <small v-if="formErrors.email">{{ formErrors.email }}</small>
          </label>
          <div v-if="message || errorMessage" class="form-feedback" :class="{ error: errorMessage }">
            {{ errorMessage || message }}
          </div>
          <div class="dialog-actions">
            <button class="primary-button" type="submit" :disabled="saving">
              {{ saving ? '正在保存…' : '保存资料' }}
            </button>
          </div>
        </form>

        <section v-if="!isAdminAccount" class="dialog-section">
          <h3>重置密码</h3>
          <p class="section-hint">管理员重置密码无需原密码，重置后请及时通知员工</p>
          <div class="inline-form">
            <input v-model="newPassword" type="password" autocomplete="new-password" placeholder="新密码，至少 6 位">
            <button class="ghost-button" type="button" :disabled="resetting" @click="resetPassword">
              {{ resetting ? '正在重置…' : '重置密码' }}
            </button>
          </div>
          <p v-if="resetMessage || resetError" class="feedback" :class="{ error: resetError }">
            {{ resetError || resetMessage }}
          </p>
        </section>

        <section v-if="!isAdminAccount && !isSelf" class="dialog-section danger-section">
          <h3>删除账号</h3>
          <p class="section-hint">逻辑删除（离职标记），删除后账号立即无法登录，历史记录保留</p>
          <button class="danger-button" type="button" :disabled="deleting" @click="removeEmployee">
            {{ deleting ? '正在删除…' : '删除该员工账号' }}
          </button>
        </section>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dialog-mask { position: fixed; inset: 0; z-index: 40; display: grid; place-items: center; padding: 24px; background: #152c3a66; }
.dialog-card { width: min(560px, 100%); max-height: 90vh; overflow-y: auto; border-radius: 14px; background: #fff; box-shadow: 0 24px 60px #152c3a33; }
.dialog-head { display: flex; justify-content: space-between; align-items: flex-start; padding: 24px 26px 18px; border-bottom: 1px solid #e8eeee; }
.dialog-head h2 { margin: 0 0 4px; font-size: 18px; }
.dialog-head p { margin: 0; color: var(--muted); font-size: 13px; }
.close-button { border: 0; background: transparent; color: #8ba0a8; font-size: 24px; line-height: 1; }
.dialog-body { padding: 22px 26px 26px; }
.dialog-form { display: grid; grid-template-columns: 1fr 1fr; gap: 0 18px; }
.dialog-form .field { margin-bottom: 16px; }
.dialog-form .form-feedback, .dialog-actions { grid-column: 1 / -1; }
.dialog-actions { display: flex; justify-content: flex-end; padding-top: 14px; border-top: 1px solid #edf1f2; }
.dialog-section { margin-top: 20px; padding-top: 18px; border-top: 1px solid #edf1f2; }
.dialog-section h3 { margin: 0 0 5px; font-size: 15px; }
.section-hint { margin: 0 0 12px; color: var(--muted); font-size: 12px; }
.inline-form { display: flex; gap: 10px; }
.inline-form input { flex: 1; height: 44px; border: 1px solid var(--line); border-radius: 10px; padding: 0 14px; outline: none; }
.inline-form input:focus { border-color: var(--teal); box-shadow: 0 0 0 3px #008b8315; }
.danger-section h3 { color: var(--danger); }
.danger-button { min-height: 42px; padding: 0 18px; border: 1px solid #f0cccc; border-radius: 9px; background: #fff; color: var(--danger); font-weight: 650; }
.danger-button:hover { background: #fff5f5; }
.danger-button:disabled { opacity: .6; cursor: not-allowed; }
@media (max-width: 640px) { .dialog-form { grid-template-columns: 1fr; } }
</style>
