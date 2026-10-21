<script setup lang="ts">
import AccountSettings from '@/components/AccountSettings.vue'
import type { LoginUser } from '@/api/employee'

defineProps<{ user: LoginUser }>()
const emit = defineEmits<{
  (e: 'updated', user: LoginUser): void
  (e: 'logout'): void
}>()
</script>

<template>
  <div class="employee-page">
    <header class="employee-topbar">
      <div class="employee-brand">
        <span class="brand-mark small">EO</span>
        <div>
          <strong>员工自助中心</strong>
          <small>EMPLOYEE SELF SERVICE</small>
        </div>
      </div>
      <div class="employee-user">
        <div class="avatar">{{ (user.name || user.account).slice(0, 1) }}</div>
        <div>
          <strong>{{ user.name || user.account }}</strong>
          <small>{{ user.account }} · 员工</small>
        </div>
        <button type="button" @click="emit('logout')">退出登录</button>
      </div>
    </header>

    <main class="employee-main">
      <div class="page-heading">
        <p class="eyebrow accent">MY PROFILE</p>
        <h1>我的资料</h1>
        <p class="muted">在这里维护个人信息，修改密码为独立操作。</p>
      </div>
      <AccountSettings :user="user" @updated="(updated) => emit('updated', updated)" />
    </main>
  </div>
</template>

<style scoped>
.employee-page { min-height: 100vh; background: var(--wash); }
.employee-topbar { display: flex; justify-content: space-between; align-items: center; gap: 20px; padding: 18px clamp(20px, 5vw, 64px); background: var(--navy); color: #fff; }
.employee-brand, .employee-user { display: flex; align-items: center; gap: 12px; }
.employee-brand strong, .employee-user strong { display: block; font-size: 14px; }
.employee-brand small, .employee-user small { display: block; margin-top: 2px; color: #8ea5ac; font-size: 11px; letter-spacing: 1.2px; }
.employee-user button { border: 1px solid #ffffff2e; border-radius: 8px; padding: 8px 14px; color: #d9e6e8; background: transparent; font-size: 12px; }
.employee-user button:hover { background: #ffffff14; }
.employee-main { max-width: 960px; margin: 0 auto; padding: 44px clamp(20px, 5vw, 48px) 64px; }
.page-heading h1 { margin: 6px 0 0; font-family: Georgia, "Songti SC", serif; font-size: 34px; }
.page-heading .muted { margin: 8px 0 28px; }
@media (max-width: 640px) {
  .employee-topbar { align-items: flex-start; }
  .employee-brand small { display: none; }
  .employee-user > div:nth-child(2) { display: none; }
}
</style>
