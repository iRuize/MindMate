<script setup>
import { onBeforeUnmount, ref } from 'vue'
import { useRouter } from 'vue-router'
import { login, sendSmsCode } from '@/api/auth'
import { TOKEN_KEY } from '@/api/request'

const router = useRouter()

const phoneNumber = ref('')
const smsCode = ref('')
const mockCode = ref('')
const errorMessage = ref('')
const sending = ref(false)
const logging = ref(false)
const countdown = ref(0)

const PHONE_PATTERN = /^1[3-9]\d{9}$/

let timer = null

const startCountdown = () => {
  countdown.value = 60
  timer = window.setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) {
      window.clearInterval(timer)
      timer = null
    }
  }, 1000)
}

const handleSend = async () => {
  errorMessage.value = ''
  mockCode.value = ''
  if (!PHONE_PATTERN.test(phoneNumber.value)) {
    errorMessage.value = '请输入正确的 11 位手机号'
    return
  }
  sending.value = true
  try {
    mockCode.value = (await sendSmsCode(phoneNumber.value)) || ''
    startCountdown()
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    sending.value = false
  }
}

const handleLogin = async () => {
  errorMessage.value = ''
  if (!PHONE_PATTERN.test(phoneNumber.value)) {
    errorMessage.value = '请输入正确的 11 位手机号'
    return
  }
  if (!/^\d{6}$/.test(smsCode.value)) {
    errorMessage.value = '请输入 6 位验证码'
    return
  }
  logging.value = true
  try {
    const data = await login(phoneNumber.value, smsCode.value)
    localStorage.setItem(TOKEN_KEY, data.token)
    router.replace({ name: 'home' })
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    logging.value = false
  }
}

const fillMockCode = () => {
  smsCode.value = mockCode.value
}

onBeforeUnmount(() => {
  if (timer) {
    window.clearInterval(timer)
  }
})
</script>

<template>
  <div class="login-page">
    <div class="login-panel">
      <div class="brand">
        <h1>MindMate</h1>
        <p>让清单自己长出优先级</p>
      </div>

      <form class="login-form" @submit.prevent="handleLogin">
        <label class="field">
          <span>手机号</span>
          <input
            v-model.trim="phoneNumber"
            type="tel"
            maxlength="11"
            placeholder="请输入 11 位手机号"
            autocomplete="tel"
          />
        </label>

        <label class="field">
          <span>验证码</span>
          <div class="code-row">
            <input
              v-model.trim="smsCode"
              type="text"
              inputmode="numeric"
              maxlength="6"
              placeholder="6 位数字"
              autocomplete="one-time-code"
            />
            <button
              type="button"
              class="ghost-btn"
              :disabled="countdown > 0 || sending"
              @click="handleSend"
            >
              {{ countdown > 0 ? `${countdown}s 后重发` : '获取验证码' }}
            </button>
          </div>
        </label>

        <p v-if="mockCode" class="mock-hint">
          当前为伪装短信，验证码：
          <strong>{{ mockCode }}</strong>
          <button type="button" class="link-btn" @click="fillMockCode">填入</button>
        </p>

        <p v-if="errorMessage" class="error-hint">{{ errorMessage }}</p>

        <button type="submit" class="primary-btn" :disabled="logging">
          {{ logging ? '登录中…' : '登录' }}
        </button>
      </form>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 24px;
  background-color: #e493d0;
  background-image:
    radial-gradient(closest-side, rgba(235, 105, 78, 1), rgba(235, 105, 78, 0)),
    radial-gradient(closest-side, rgba(243, 11, 164, 1), rgba(243, 11, 164, 0)),
    radial-gradient(closest-side, rgba(254, 234, 131, 1), rgba(254, 234, 131, 0)),
    radial-gradient(closest-side, rgba(170, 142, 245, 1), rgba(170, 142, 245, 0)),
    radial-gradient(closest-side, rgba(248, 192, 147, 1), rgba(248, 192, 147, 0));
  background-size: 130vmax 130vmax, 80vmax 80vmax, 90vmax 90vmax, 110vmax 110vmax, 90vmax 90vmax;
  background-position: -80vmax -80vmax, 60vmax -30vmax, 10vmax 10vmax, -30vmax -10vmax, 50vmax 50vmax;
  background-repeat: no-repeat;
  animation: movement 8s linear infinite;
}

@keyframes movement {
  0%,
  100% {
    background-size: 130vmax 130vmax, 80vmax 80vmax, 90vmax 90vmax, 110vmax 110vmax, 90vmax 90vmax;
    background-position: -80vmax -80vmax, 60vmax -30vmax, 10vmax 10vmax, -30vmax -10vmax, 50vmax 50vmax;
  }
  50% {
    background-size: 80vmax 80vmax, 110vmax 110vmax, 80vmax 80vmax, 60vmax 60vmax, 80vmax 80vmax;
    background-position: -50vmax -70vmax, 40vmax -30vmax, 10vmax 0vmax, 20vmax 10vmax, 30vmax 70vmax;
  }
}

.login-panel {
  width: 380px;
  max-width: 100%;
  padding: 40px 36px 36px;
  border-radius: 20px;
  background-color: rgba(255, 255, 255, 0.22);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.16);
}

.brand {
  margin-bottom: 28px;
  text-align: center;
  color: #fff;
}

.brand h1 {
  margin: 0;
  font-size: 2.4rem;
  font-weight: 500;
  letter-spacing: 0.14em;
  text-shadow: 0 4px 8px rgba(0, 0, 0, 0.12);
}

.brand p {
  margin: 8px 0 0;
  font-size: 0.85rem;
  opacity: 0.82;
}

.field {
  display: block;
  margin-bottom: 16px;
}

.field > span {
  display: block;
  margin-bottom: 6px;
  font-size: 0.8rem;
  color: rgba(255, 255, 255, 0.9);
}

.field input {
  width: 100%;
  padding: 11px 13px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 9px;
  background-color: rgba(255, 255, 255, 0.85);
  color: #1f2430;
  outline: none;
  transition: border-color 0.2s, background-color 0.2s;
}

.field input:focus {
  border-color: #fff;
  background-color: #fff;
}

.code-row {
  display: flex;
  gap: 10px;
}

.code-row input {
  flex: 1;
  min-width: 0;
}

.ghost-btn {
  flex: none;
  padding: 0 14px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 9px;
  background-color: rgba(255, 255, 255, 0.18);
  color: #fff;
  font-size: 0.8rem;
  white-space: nowrap;
  transition: background-color 0.2s;
}

.ghost-btn:not(:disabled):hover {
  background-color: rgba(255, 255, 255, 0.34);
}

.mock-hint,
.error-hint {
  margin: 0 0 12px;
  padding: 8px 11px;
  border-radius: 8px;
  font-size: 0.78rem;
  line-height: 1.5;
}

.mock-hint {
  background-color: rgba(255, 255, 255, 0.24);
  color: #fff;
}

.mock-hint strong {
  font-weight: 500;
  letter-spacing: 0.16em;
}

.link-btn {
  margin-left: 8px;
  padding: 2px 8px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 6px;
  background: transparent;
  color: #fff;
  font-size: 0.72rem;
}

.error-hint {
  background-color: rgba(180, 30, 30, 0.26);
  color: #fff3f3;
}

.primary-btn {
  width: 100%;
  margin-top: 6px;
  padding: 12px;
  border: none;
  border-radius: 10px;
  background-color: rgba(255, 255, 255, 0.9);
  color: #4a2a52;
  font-size: 0.95rem;
  font-weight: 500;
  letter-spacing: 0.1em;
  transition: background-color 0.2s;
}

.primary-btn:not(:disabled):hover {
  background-color: #fff;
}
</style>
