<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchMe, logout } from '@/api/auth'
import { TOKEN_KEY } from '@/api/request'

const router = useRouter()

const user = ref(null)
const loading = ref(true)
const errorMessage = ref('')
const loggingOut = ref(false)

const formatDateTime = (value) => (value ? String(value).replace('T', ' ').slice(0, 16) : '—')

const display = (value) => (value ? value : '未设置')

const loadUser = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    user.value = await fetchMe()
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    loading.value = false
  }
}

const handleLogout = async () => {
  loggingOut.value = true
  try {
    await logout()
  } finally {
    localStorage.removeItem(TOKEN_KEY)
    loggingOut.value = false
    router.replace({ name: 'login' })
  }
}

onMounted(loadUser)
</script>

<template>
  <div class="home-page">
    <header class="topbar">
      <div class="logo">MindMate</div>
      <button class="logout-btn" :disabled="loggingOut" @click="handleLogout">
        {{ loggingOut ? '退出中…' : '退出登录' }}
      </button>
    </header>

    <main class="content">
      <section class="card">
        <h2>当前账号</h2>

        <p v-if="loading" class="hint">加载中…</p>
        <p v-else-if="errorMessage" class="hint error">{{ errorMessage }}</p>

        <dl v-else-if="user" class="profile">
          <div>
            <dt>手机号</dt>
            <dd>{{ user.phoneNumber }}</dd>
          </div>
          <div>
            <dt>用户名</dt>
            <dd>{{ display(user.username) }}</dd>
          </div>
          <div>
            <dt>真实姓名</dt>
            <dd>{{ display(user.realName) }}</dd>
          </div>
          <div>
            <dt>个性签名</dt>
            <dd>{{ display(user.personalizedSignature) }}</dd>
          </div>
          <div>
            <dt>邮箱</dt>
            <dd>{{ display(user.email) }}</dd>
          </div>
          <div>
            <dt>最近登录</dt>
            <dd>{{ formatDateTime(user.lastLoginAt) }}</dd>
          </div>
        </dl>
      </section>

      <section class="card placeholder">
        <h2>今日清单</h2>
        <p>任务模块尚未接入。数据表 TASK 与 TASK_EVENT 已就绪，等待接口层实现。</p>
      </section>
    </main>
  </div>
</template>

<style scoped>
.home-page {
  min-height: 100vh;
  background-color: #f5f6fa;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 32px;
  background-color: #fff;
  border-bottom: 1px solid #eceef3;
}

.logo {
  font-size: 1.1rem;
  font-weight: 500;
  letter-spacing: 0.08em;
  color: #4a2a52;
}

.logout-btn {
  padding: 7px 16px;
  border: 1px solid #e2e4eb;
  border-radius: 8px;
  background-color: #fff;
  color: #5f6472;
  font-size: 0.82rem;
  transition: border-color 0.2s, color 0.2s;
}

.logout-btn:not(:disabled):hover {
  border-color: #b9bcc7;
  color: #1f2430;
}

.content {
  max-width: 720px;
  margin: 0 auto;
  padding: 32px 24px 48px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.card {
  padding: 24px 26px;
  border-radius: 14px;
  background-color: #fff;
  border: 1px solid #eceef3;
}

.card h2 {
  margin: 0 0 18px;
  font-size: 0.95rem;
  font-weight: 500;
  color: #2c3140;
}

.hint {
  margin: 0;
  font-size: 0.85rem;
  color: #7a8093;
}

.hint.error {
  color: #b83a3a;
}

.profile {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px 28px;
  margin: 0;
}

.profile dt {
  margin-bottom: 4px;
  font-size: 0.75rem;
  color: #8b90a0;
}

.profile dd {
  margin: 0;
  font-size: 0.9rem;
  color: #1f2430;
  word-break: break-all;
}

.placeholder p {
  margin: 0;
  font-size: 0.85rem;
  line-height: 1.7;
  color: #7a8093;
}
</style>
