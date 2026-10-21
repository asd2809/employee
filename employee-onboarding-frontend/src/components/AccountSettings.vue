<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { changePassword, updateEmployee, type LoginUser } from '@/api/employee'

const props = defineProps<{ user: LoginUser }>()
const emit = defineEmits<{ (e: 'updated', user: LoginUser): void }>()

const phonePattern = /^1[3-9]\d{9}$/
const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

const profile = reactive({
  name: props.user.name ?? '',
  gender: props.user.gender ?? 1,
  phone: props.user.phone ?? '',
  email: props.user.email ?? '',
})
const profileErrors = ref<{ name?: string; phone?: string; email?: string }>({})
const profileSaving = ref(false)
const profileMessage = ref('')
const profileError = ref('')

const pwd = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdError = ref('')
const pwdMessage = ref('')
const pwdSaving = ref(false)

watch(
  () => props.user,
  (user) => {
    Object.assign(profile, {
      name: user.name ?? '',
      gender: user.gender ?? 1,
      phone: user.phone ?? '',
      email: user.email ?? '',
    })
  },
)

function validateProfile() {
  const errors: { name?: string; phone?: string; email?: string } = {}
  if (!profile.name.trim()) errors.name = '请输入姓名'
  if (!profile.phone.trim()) errors.phone = '请输入手机号'
  else if (!phonePattern.test(profile.phone.trim())) errors.phone = '请输入正确的 11 位手机号'
  if (!profile.email.trim()) errors.email = '请输入邮箱'
  else if (!emailPattern.test(profile.email.trim())) errors.email = '请输入正确的邮箱地址'
  profileErrors.value = errors
  return Object.keys(errors).length === 0
}

async function saveProfile() {
  profileMessage.value = ''
  profileError.value = ''
  if (!validateProfile()) return
  profileSaving.value = true
  try {
    const updated = await updateEmployee(props.user.id, {
      name: profile.name.trim(),
      gender: profile.gender,
      phone: profile.phone.trim(),
      email: profile.email.trim(),
    })
    emit('updated', { ...props.user, ...updated })
    profileMessage.value = '资料已保存'
  } catch (error) {
    profileError.value = error instanceof Error ? error.message : '保存失败，请稍后重试'
  } finally {
    profileSaving.value = false
  }
}

function validatePassword() {
  pwdError.value = ''
  if (!pwd.oldPassword) {
    pwdError.value = '请输入原密码'
    return false
  }
  if (!pwd.newPassword || pwd.newPassword.length < 6) {
    pwdError.value = '新密码至少需要 6 位'
    return false
  }
  if (pwd.newPassword !== pwd.confirmPassword) {
    pwdError.value = '两次输入的新密码不一致'
    return false
  }
  return true
}

async function savePassword() {
  pwdMessage.value = ''
  if (!validatePassword()) return
  pwdSaving.value = true
  try {
    await changePassword(props.user.id, {
      oldPassword: pwd.oldPassword,
      newPassword: pwd.newPassword,
    })
    pwdMessage.value = '密码修改成功'
    Object.assign(pwd, { oldPassword: '', newPassword: '', confirmPassword: '' })
  } catch (error) {
    pwdError.value = error instanceof Error ? error.message : '密码修改失败，请稍后重试'
  } finally {
    pwdSaving.value = false
  }
}
</script>

<template>
  <div class="account-settings">
    <section class="content-card settings-card">
      <div class="card-heading">
        <div>
          <h2>基本资料</h2>
          <p>登录账号由系统分配不可修改，其余资料可自行维护</p>
        </div>
        <span class="role-badge">{{ user.role === 1 ? '管理员' : '员工' }}</span>
      </div>
      <form class="employee-form" @submit.prevent="saveProfile">
        <label class="field">
          <span>登录账号</span>
          <input :value="user.account" disabled>
        </label>
        <label class="field">
          <span>姓名 <b>*</b></span>
          <input v-model="profile.name" placeholder="请输入真实姓名">
          <small v-if="profileErrors.name">{{ profileErrors.name }}</small>
        </label>
        <fieldset class="field gender-field">
          <legend>性别 <b>*</b></legend>
          <div class="radio-group">
            <label><input v-model="profile.gender" type="radio" :value="1"><span>男</span></label>
            <label><input v-model="profile.gender" type="radio" :value="0"><span>女</span></label>
          </div>
        </fieldset>
        <label class="field">
          <span>手机号 <b>*</b></span>
          <input v-model="profile.phone" inputmode="numeric" maxlength="11" placeholder="请输入 11 位手机号">
          <small v-if="profileErrors.phone">{{ profileErrors.phone }}</small>
        </label>
        <label class="field">
          <span>Email <b>*</b></span>
          <input v-model="profile.email" type="email" placeholder="name@example.com">
          <small v-if="profileErrors.email">{{ profileErrors.email }}</small>
        </label>
        <div v-if="profileMessage || profileError" class="form-feedback" :class="{ error: profileError }">
          {{ profileError || profileMessage }}
        </div>
        <div class="form-actions">
          <button class="primary-button" type="submit" :disabled="profileSaving">
            {{ profileSaving ? '正在保存…' : '保存资料' }}
          </button>
        </div>
      </form>
    </section>

    <section class="content-card settings-card">
      <div class="card-heading">
        <div>
          <h2>修改密码</h2>
          <p>修改密码为独立操作，需要验证原密码</p>
        </div>
      </div>
      <form class="employee-form" @submit.prevent="savePassword">
        <label class="field">
          <span>原密码 <b>*</b></span>
          <input v-model="pwd.oldPassword" type="password" autocomplete="current-password" placeholder="请输入当前密码">
        </label>
        <label class="field">
          <span>新密码 <b>*</b></span>
          <input v-model="pwd.newPassword" type="password" autocomplete="new-password" placeholder="至少 6 位">
        </label>
        <label class="field">
          <span>确认新密码 <b>*</b></span>
          <input v-model="pwd.confirmPassword" type="password" autocomplete="new-password" placeholder="再次输入新密码">
        </label>
        <div v-if="pwdMessage || pwdError" class="form-feedback" :class="{ error: pwdError }">
          {{ pwdError || pwdMessage }}
        </div>
        <div class="form-actions">
          <button class="primary-button" type="submit" :disabled="pwdSaving">
            {{ pwdSaving ? '正在提交…' : '更新密码' }}
          </button>
        </div>
      </form>
    </section>
  </div>
</template>

<style scoped>
.account-settings { display: grid; gap: 22px; }
.settings-card { max-width: 900px; }
.role-badge { display: inline-flex; border-radius: 20px; padding: 5px 12px; color: #27816f; background: #e8f6f1; font-size: 12px; }
</style>
