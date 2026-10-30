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
  if (sending.value || countdown.value > 0) return
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
  if (logging.value) return
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

onBeforeUnmount(() => {
  if (timer) window.clearInterval(timer)
})
</script>

<template>
  <div class="login-page">
    <header class="login-header"><a class="brand-lockup" href="/login"><span class="brand-mark">m<span>·</span></span>MindMate</a><span class="header-note">让思考更清晰，让行动更从容</span></header>
    <main class="login-layout">
      <section class="intro">
        <span class="eyebrow">YOUR PARTNER IN EVERY DECISION</span>
        <h1>每一次决定，<br />都会有帮助<span class="period">。</span></h1>
        <p class="intro-copy">从整理眼前的待办开始，<br />为每一个值得认真思考的选择，留一点空间。</p>
        <div class="thought-card">
          <span class="thought-label">给今天的一个小提醒</span>
          <p>不必一次想清所有事情。<br />先找到，当下最重要的那一件。</p>
          <div class="thought-line"><span></span><span></span><span></span></div>
        </div>
        <div class="intro-caption"><span class="small-dot"></span> 清晰思考 · 从容行动</div>
      </section>
      <section class="login-panel" aria-labelledby="login-title">
        <span class="eyebrow">WELCOME TO MINDMATE</span>
        <h2 id="login-title">从这里，开始理清思路</h2>
        <p class="form-description">使用手机号登录，进入你的个人工作台。</p>
        <form @submit.prevent="handleLogin">
          <label class="field"><span>手机号</span><input v-model.trim="phoneNumber" type="tel" inputmode="numeric" maxlength="11" placeholder="请输入手机号" autocomplete="tel" required :disabled="logging || sending" /></label>
          <label class="field"><span>验证码</span><div class="code-row"><input v-model.trim="smsCode" type="text" inputmode="numeric" maxlength="6" placeholder="请输入 6 位验证码" autocomplete="one-time-code" required :disabled="logging" /><button type="button" class="code-button" :disabled="countdown > 0 || sending || logging" @click="handleSend">{{ sending ? '获取中…' : countdown > 0 ? `${countdown}s 后重发` : '获取验证码' }}</button></div></label>
          <p v-if="mockCode" class="mock-hint" role="status">体验验证码 <strong>{{ mockCode }}</strong><button type="button" class="text-button" @click="smsCode = mockCode">一键填入</button></p>
          <p v-if="errorMessage" class="error-hint" role="alert">{{ errorMessage }}</p>
          <button type="submit" class="button-primary login-submit" :disabled="logging || sending">{{ logging ? '正在进入…' : '进入 MindMate' }}<span aria-hidden="true">↗</span></button>
          <p class="form-footnote">首次登录将自动创建账号</p>
        </form>
      </section>
    </main>
    <footer class="login-footer"><span>MindMate · 你的思考伙伴</span><span>每一步，都更有方向。</span></footer>
  </div>
</template>

<style scoped>
.login-page { min-height: 100dvh; display: flex; flex-direction: column; background: #fafbf9; }
.login-header, .login-footer { display: flex; align-items: center; justify-content: space-between; padding: 30px 5%; }
.header-note, .login-footer { color: var(--text-muted); font-size: 12px; }
.login-layout { width: min(1080px, 90%); margin: auto; display: grid; grid-template-columns: 1.15fr 1fr; align-items: center; gap: 100px; padding: 58px 0 70px; }
.intro h1 { font-size: clamp(36px, 4.3vw, 58px); font-weight: 600; line-height: 1.45; letter-spacing: -2px; margin: 24px 0 20px; }
.period { color: var(--accent); }
.intro-copy { font-size: 15px; color: var(--text-secondary); line-height: 1.95; }
.thought-card { border-left: 2px solid #bdd2c6; padding: 3px 0 3px 22px; margin-top: 44px; }
.thought-label { color: var(--text-muted); font-size: 11px; }
.thought-card p { font-size: 14px; line-height: 1.9; color: #4c6256; margin: 10px 0 15px; }
.thought-line { display: flex; gap: 5px; }.thought-line span { height: 3px; width: 17px; background: #dce5df; border-radius: 3px; }.thought-line span:first-child { background: var(--accent); width: 28px; }
.intro-caption { font-size: 11px; color: var(--text-muted); display: flex; align-items: center; gap: 8px; margin-top: 34px; }
.small-dot { width: 6px; height: 6px; border-radius: 50%; background: var(--accent); }
.login-panel { padding: 40px; background: #fff; border: 1px solid var(--border); border-radius: 16px; box-shadow: 0 12px 50px #253e2e05; }
.login-panel h2 { font-size: 23px; margin: 18px 0 10px; font-weight: 600; letter-spacing: -.6px; }
.form-description { color: var(--text-secondary); font-size: 13px; margin: 0 0 32px; line-height: 1.8; }
.field { display: block; margin-bottom: 22px; }.field > span { display: block; margin-bottom: 10px; font-size: 12px; font-weight: 500; }
.field input { width: 100%; padding: 13px 14px; border: 1px solid var(--border); border-radius: 7px; background: #fdfefd; color: var(--text); min-width: 0; font-size: 13px; }
.code-row { display: flex; border: 1px solid var(--border); border-radius: 7px; align-items: center; background: #fdfefd; }.code-row input { border: 0; background: transparent; }.code-row:focus-within { outline: 2px solid #bed4c6; outline-offset: 2px; }
.code-button { flex-shrink: 0; padding: 3px 14px; margin-right: 3px; border: 0; border-left: 1px solid var(--border); background: transparent; font-size: 12px; color: var(--accent); }
.mock-hint { padding: 10px; background: var(--accent-light); border-radius: 6px; color: var(--accent); font-size: 11px; line-height: 1.8; }.mock-hint strong { letter-spacing: 2px; margin-left: 8px; }.text-button { border: 0; background: none; color: var(--accent); font-size: 11px; margin-left: 8px; text-decoration: underline; }
.error-hint { color: #ad3a3a; font-size: 12px; line-height: 1.6; }
.login-submit { width: 100%; display: flex; justify-content: space-between; align-items: center; padding: 14px 18px; margin-top: 30px; }
.form-footnote { font-size: 11px; color: var(--text-muted); text-align: center; margin: 18px 0 0; }
@media (max-width: 850px) { .login-layout { gap: 40px; }.login-panel { padding: 28px; }.header-note { display: none; } }
@media (max-width: 640px) { .login-header { padding: 22px 6%; }.login-layout { grid-template-columns: 1fr; width: 88%; padding: 20px 0 40px; gap: 30px; }.intro h1 { font-size: 34px; margin: 15px 0 12px; letter-spacing: -1px; }.intro-copy { font-size: 13px; }.thought-card, .intro-caption { display: none; }.intro .eyebrow { font-size: 9px; }.login-panel { padding: 26px 22px; }.login-panel h2 { font-size: 21px; }.login-footer { padding: 18px 6%; font-size: 10px; } }
</style>
