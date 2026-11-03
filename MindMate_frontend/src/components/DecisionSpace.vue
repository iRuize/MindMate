<script setup>
import {computed, nextTick, onBeforeUnmount, ref, watch} from 'vue'
import MarkdownText from '@/components/MarkdownText.vue'
import {
  clearDecisionPreferences,
  createDecision,
  deleteDecision,
  fetchDecisionConfig,
  fetchDecisionMessages,
  fetchDecisionPreferences,
  fetchDecisions,
  saveDecisionPreferences,
  sendDecisionMessage,
} from '@/api/decisions'

const props = defineProps({active: {type: Boolean, default: false}})
const emit = defineEmits(['changed'])
const preferenceFields = [
  {key: 'personality', label: '性格', placeholder: '你通常如何思考和行动？'},
  {key: 'interests', label: '兴趣', placeholder: '哪些事情让你投入其中？'},
  {key: 'values', label: '价值取舍', placeholder: '做选择时，你最看重什么？'},
  {key: 'goals', label: '近期目标', placeholder: '你最近希望达成什么？'},
  {key: 'decisionStyle', label: '决策风格', placeholder: '你倾向谨慎比较，还是先尝试再调整？'},
]
const emptyPreferences = () => ({
  personality: '',
  interests: '',
  values: '',
  goals: '',
  decisionStyle: '',
  memoryEnabled: false
})
const preferences = ref(emptyPreferences())
const savedPreferences = ref(emptyPreferences())
const activeTab = ref('chat')
const preferencesLoaded = ref(false)
const preferencesLoading = ref(false)
const preferencesBusy = ref(false)
const preferencesError = ref('')
const preferencesNotice = ref('')
const config = ref(null)
const configLoading = ref(false)
const configError = ref('')
const sessions = ref([])
const sessionsLoaded = ref(false)
const sessionsLoading = ref(false)
const sessionsError = ref('')
const actionBusy = ref(false)
const actionError = ref('')
const selectedId = ref(null)
const messages = ref([])
const messagesLoading = ref(false)
const messagesLoaded = ref(false)
const messagesError = ref('')
const sending = ref(false)
const thinking = ref(false)
const optimisticUser = ref(null)
const drafts = ref({})
const sendErrors = ref({})
const needsVerification = ref({})
const conversationNotice = ref('')
const prefillNotice = ref('')
const messageList = ref(null)
const input = ref(null)
let messageSequence = 0
let disposed = false
let initializationPromise
let sendController = null
const busy = computed(() => sending.value || actionBusy.value || preferencesBusy.value)
const selectedSession = computed(() => sessions.value.find((item) => item.id === selectedId.value))
const draftKey = computed(() => selectedId.value === null ? 'new' : `session:${selectedId.value}`)
const draft = computed({
  get: () => drafts.value[draftKey.value] || '',
  set: (value) => {
    drafts.value[draftKey.value] = value
  },
})
const currentSendError = computed(() => sendErrors.value[selectedId.value] || '')
const requiresVerification = computed(() => Boolean(needsVerification.value[selectedId.value]))
const canSend = computed(() => config.value?.available === true && !configLoading.value && !configError.value && preferencesLoaded.value
    && !preferencesLoading.value && selectedId.value !== null && messagesLoaded.value
    && !messagesLoading.value && !sessionsLoading.value && !busy.value && !requiresVerification.value
    && Boolean(draft.value.trim()) && draft.value.length <= 4000)
const errorText = (error) => error?.message || '请稍后重试'
const formatTime = (value) => value ? String(value).replace('T', ' ').slice(0, 16) : ''
const scrollToBottom = async () => {
  await nextTick()
  if (disposed || !props.active || !messageList.value) return
  messageList.value.scrollTop = messageList.value.scrollHeight
}

const loadConfig = async () => {
  if (configLoading.value || busy.value) return
  configLoading.value = true
  configError.value = ''
  try {
    config.value = await fetchDecisionConfig()
  } catch (error) {
    configError.value = `模型配置读取失败：${errorText(error)}`
  } finally {
    configLoading.value = false
  }
}
const loadPreferences = async () => {
  if (preferencesLoading.value || busy.value) return
  preferencesLoading.value = true
  preferencesError.value = ''
  preferencesNotice.value = ''
  // 读取失败时禁止保存，避免用默认值覆盖服务端的已有偏好。
  preferencesLoaded.value = false
  try {
    const data = await fetchDecisionPreferences()
    const loaded = {...emptyPreferences(), ...data, memoryEnabled: data.memoryEnabled === true}
    preferences.value = {...loaded}
    savedPreferences.value = {...loaded}
    preferencesLoaded.value = true
  } catch (error) {
    preferencesError.value = `个人偏好读取失败：${errorText(error)}。加载成功后才能保存或清空。`
  } finally {
    preferencesLoading.value = false
  }
}
const loadMessages = async (id = selectedId.value) => {
  if (id === null || busy.value) return
  const sequence = ++messageSequence
  messagesLoading.value = true
  messagesLoaded.value = false
  messagesError.value = ''
  conversationNotice.value = ''
  try {
    const data = await fetchDecisionMessages(id)
    if (disposed || sequence !== messageSequence || selectedId.value !== id) return
    messages.value = data
    messagesLoaded.value = true
    if (needsVerification.value[id]) {
      delete needsVerification.value[id]
      delete sendErrors.value[id]
      conversationNotice.value = '已刷新会话。请先核对消息是否已保存、助手是否已回复；若仍在处理中，请稍后再刷新。确认需要再次发送时再手动操作，可能再次产生费用。原输入已保留。'
    }
    void scrollToBottom()
  } catch (error) {
    if (disposed || sequence !== messageSequence || selectedId.value !== id) return
    messagesError.value = `会话消息加载失败：${errorText(error)}`
  } finally {
    if (!disposed && sequence === messageSequence && selectedId.value === id) messagesLoading.value = false
  }
}
const selectSession = (id) => {
  if (busy.value || sessionsLoading.value || selectedId.value === id) return
  ++messageSequence
  selectedId.value = id
  messages.value = []
  messagesLoaded.value = false
  messagesLoading.value = false
  messagesError.value = ''
  conversationNotice.value = ''
  prefillNotice.value = ''
  if (id !== null) void loadMessages(id)
}
const loadSessions = async () => {
  if (sessionsLoading.value || busy.value) return
  sessionsLoading.value = true
  sessionsError.value = ''
  try {
    sessions.value = await fetchDecisions()
    sessionsLoaded.value = true
    const nextId = sessions.value.some((item) => item.id === selectedId.value)
        ? selectedId.value : sessions.value[0]?.id ?? null
    if (nextId !== selectedId.value) {
      // 列表刷新期间由本次读取统一决定选中项。
      sessionsLoading.value = false
      selectSession(nextId)
    }
  } catch (error) {
    sessionsError.value = `会话列表加载失败：${errorText(error)}${sessionsLoaded.value ? '，保留上次列表。' : ''}`
  } finally {
    sessionsLoading.value = false
  }
}
const addSession = async () => {
  if (busy.value || sessionsLoading.value || !sessionsLoaded.value) return
  actionBusy.value = true
  actionError.value = ''
  try {
    const session = await createDecision({})
    sessions.value.unshift(session)
    if (selectedId.value === null && draft.value) drafts.value[`session:${session.id}`] = draft.value
    if (selectedId.value === null) delete drafts.value.new
    ++messageSequence
    selectedId.value = session.id
    messages.value = []
    messagesLoaded.value = true
    messagesLoading.value = false
    messagesError.value = ''
    conversationNotice.value = ''
    prefillNotice.value = ''
    void scrollToBottom()
    emit('changed')
  } catch (error) {
    actionError.value = `新增失败：${errorText(error)}。请先刷新列表确认是否已创建，再手动重试。`
  } finally {
    actionBusy.value = false
  }
}
const removeSession = async (session) => {
  if (busy.value || sessionsLoading.value) return
  if (!window.confirm(`删除会话“${session.title || '未命名会话'}”及其全部消息？此操作无法撤销。`)) return
  actionBusy.value = true
  actionError.value = ''
  let removed = false
  try {
    await deleteDecision(session.id)
    sessions.value = sessions.value.filter((item) => item.id !== session.id)
    delete drafts.value[`session:${session.id}`]
    delete sendErrors.value[session.id]
    delete needsVerification.value[session.id]
    removed = true
    emit('changed')
  } catch (error) {
    actionError.value = `删除失败：${errorText(error)}。请刷新列表确认后再手动重试。`
  } finally {
    actionBusy.value = false
  }
  if (removed && selectedId.value === session.id) selectSession(sessions.value[0]?.id ?? null)
}
const savePreferences = async () => {
  if (busy.value || sessionsLoading.value || preferencesLoading.value || !preferencesLoaded.value) return
  if (preferenceFields.some(({key}) => preferences.value[key].length > 1000)) return
  preferencesBusy.value = true
  preferencesError.value = ''
  preferencesNotice.value = ''
  try {
    const data = await saveDecisionPreferences({...preferences.value})
    const saved = {...emptyPreferences(), ...data, memoryEnabled: data.memoryEnabled === true}
    preferences.value = {...saved}
    savedPreferences.value = {...saved}
    preferencesNotice.value = '个人偏好已保存，开关从下一次发送开始生效。'
  } catch (error) {
    preferencesLoaded.value = false
    preferencesError.value = `保存失败：${errorText(error)}。请重新加载确认服务端状态后再编辑和发送。`
  } finally {
    preferencesBusy.value = false
  }
}
const clearPreferences = async () => {
  if (busy.value || sessionsLoading.value || preferencesLoading.value || !preferencesLoaded.value) return
  if (!window.confirm('清空已保存的五项个人偏好并关闭记忆开关？会话和消息仍会保留，此操作无法撤销。')) return
  preferencesBusy.value = true
  preferencesError.value = ''
  preferencesNotice.value = ''
  try {
    await clearDecisionPreferences()
    preferences.value = emptyPreferences()
    savedPreferences.value = emptyPreferences()
    preferencesNotice.value = '个人偏好已清空，记忆开关已关闭。会话和消息仍保留。'
  } catch (error) {
    preferencesLoaded.value = false
    preferencesError.value = `清空失败：${errorText(error)}。请重新加载确认服务端状态。`
  } finally {
    preferencesBusy.value = false
  }
}
const sendMessage = async () => {
  if (!canSend.value) return
  const id = selectedId.value
  const sequence = messageSequence
  const key = draftKey.value
  const content = draft.value.trim()
  const controller = new AbortController()
  sendController = controller
  sending.value = true
  thinking.value = true
  // 先上屏用户消息并立即清空输入框，等服务端返回后再用真实记录替换。
  optimisticUser.value = {content, createdAt: new Date().toISOString()}
  drafts.value[key] = ''
  delete sendErrors.value[id]
  conversationNotice.value = ''
  prefillNotice.value = ''
  await scrollToBottom()
  try {
    const result = await sendDecisionMessage(id, content, controller.signal)
    if (disposed || selectedId.value !== id || sequence !== messageSequence) return
    optimisticUser.value = null
    thinking.value = false
    messages.value.push(result.userMessage, result.assistantMessage)
    void scrollToBottom()
  } catch (error) {
    if (disposed || selectedId.value !== id || sequence !== messageSequence) return
    needsVerification.value[id] = true
    // 仅在没有新输入时回填，避免覆盖用户在失败后重新输入的内容。
    if (!drafts.value[key]) drafts.value[key] = content
    sendErrors.value[id] = error.canceled
      ? '已停止等待回复，原输入已保留。服务端可能仍在处理并产生费用，请先刷新当前会话确认结果，确认后再决定是否重新发送。'
      : `发送未确认：${errorText(error)}。若超时或连接中断，服务端仍可能完成并产生费用。请先刷新当前会话确认结果，再决定是否手动发送；不会自动重发，原输入已保留。`
  } finally {
    if (sendController === controller) sendController = null
    sending.value = false
    thinking.value = false
    optimisticUser.value = null
    if (!disposed) {
      await nextTick()
      if (props.active) input.value?.focus()
    }
  }
}
const stopSending = () => {
  if (!sending.value || !sendController) return
  sendController.abort()
}
const handleKeydown = (event) => {
  if (event.key !== 'Enter' || event.shiftKey || event.isComposing || event.keyCode === 229) return
  event.preventDefault()
  void sendMessage()
}
const prefill = async (question) => {
  await initializationPromise
  if (disposed) return
  if (busy.value) {
    prefillNotice.value = '当前操作尚未完成，请完成后再选择问题。已有输入已保留。'
    return
  }
  if (draft.value && draft.value !== question && !window.confirm('用所选问题替换当前未发送的草稿？')) return
  draft.value = question.slice(0, 4000)
  prefillNotice.value = '问题已填入，可补充背景后手动发送。尚未调用模型。'
  await nextTick()
  input.value?.focus()
}
defineExpose({prefill})
watch(() => props.active, (active) => {
  if (!active) return
  if (!initializationPromise) {
    initializationPromise = Promise.allSettled([loadConfig(), loadPreferences(), loadSessions()])
  }
  void scrollToBottom()
}, {immediate: true})
onBeforeUnmount(() => {
  disposed = true;
  ++messageSequence
})
</script>

<template>
  <section class="decision-space">
    <header class="decision-heading">
      <div><span class="eyebrow">MAKE ROOM FOR WHAT MATTERS</span>
        <h1>决策空间</h1>
        <p>把选项、顾虑与期待放在一起，让选择更清晰。</p></div>
      <slot name="actions"/>
    </header>

    <div v-if="configError" class="decision-feedback error" role="alert"><span>{{ configError }}</span>
      <button :disabled="configLoading || busy" @click="loadConfig">重新读取</button>
    </div>
    <div v-else-if="config && !config.available" class="decision-feedback" role="status"><span>模型尚未配置。请在后端配置 DEEPSEEK_API_KEY；配置就绪后可发送问题。</span>
      <button :disabled="configLoading || busy" @click="loadConfig">刷新配置</button>
    </div>

    <nav class="decision-tabs">
      <div aria-label="决策空间视图" class="tab-list" role="tablist">
        <button :aria-selected="activeTab === 'chat'" :class="{ active: activeTab === 'chat' }" role="tab" type="button"
                @click="activeTab = 'chat'">对话</button>
        <button :aria-selected="activeTab === 'memory'" :class="{ active: activeTab === 'memory' }" role="tab" type="button"
                @click="activeTab = 'memory'">记忆</button>
      </div>
      <span class="model-label">{{
          config?.model || 'DeepSeek'
        }} · {{
          configLoading ? '读取配置中' : configError ? '配置状态未知' : config?.available === true ? '密钥已配置' : '未配置密钥'
        }}</span>
    </nav>

    <div v-show="activeTab === 'chat'" class="decision-layout">
      <aside aria-label="决策会话列表" class="sessions-panel">
        <div class="sessions-heading"><h2>最近</h2>
          <button :disabled="busy || sessionsLoading || !sessionsLoaded" aria-label="新增会话" class="add-session"
                  title="新增会话" @click="addSession">
            <svg aria-hidden="true" viewBox="0 0 24 24" width="15" height="15"><path d="M12 5v14M5 12h14" fill="none"
                                                                                   stroke="currentColor"
                                                                                   stroke-linecap="round"
                                                                                   stroke-width="2"/></svg>
          </button>
        </div>
        <div v-if="sessionsError" class="decision-feedback error" role="alert"><span>{{ sessionsError }}</span>
          <button :disabled="sessionsLoading || busy" @click="loadSessions">重试</button>
        </div>
        <div v-if="actionError" class="decision-feedback error" role="alert"><span>{{ actionError }}</span>
          <button :disabled="sessionsLoading || busy" @click="loadSessions">刷新列表</button>
        </div>
        <p v-if="sessionsLoading" class="empty-copy" role="status">正在读取会话…</p>
        <p v-else-if="sessionsLoaded && !sessions.length" class="empty-copy">
          还没有会话。点右上角 + 新建一个，留一点空间给重要的选择。</p>
        <ul class="session-list">
          <li v-for="session in sessions" :key="session.id" :class="{ selected: selectedId === session.id }">
            <button :aria-current="selectedId === session.id ? 'true' : undefined" :disabled="busy || sessionsLoading"
                    class="session-select" @click="selectSession(session.id)"><strong>{{
                session.title || '未命名会话'
              }}</strong><small>{{ formatTime(session.updatedAt || session.createdAt) }}</small></button>
            <button :aria-label="`删除会话：${session.title || '未命名会话'}`" :disabled="busy || sessionsLoading"
                    class="session-delete" @click="removeSession(session)">删除
            </button>
          </li>
        </ul>
      </aside>

      <section :aria-busy="sending || messagesLoading" aria-label="决策对话" class="conversation-panel">
        <header class="conversation-heading">
          <div><h2>{{ selectedSession?.title || (selectedId === null ? '从一个问题开始' : '未命名会话') }}</h2>
            <p>帮你梳理思路，决定始终由你做。</p></div>
          <button v-if="selectedId !== null" :disabled="busy || messagesLoading || sessionsLoading" class="link-button"
                  @click="loadMessages()">{{ messagesLoading ? '读取中…' : '刷新会话' }}
          </button>
        </header>
        <div ref="messageList" aria-label="消息记录" class="message-list">
          <p v-if="messagesLoading" class="empty-copy" role="status">正在加载消息…</p>
          <div v-if="messagesError" class="decision-feedback error" role="alert"><span>{{ messagesError }}</span>
            <button :disabled="busy || messagesLoading" @click="loadMessages()">重新加载</button>
          </div>
          <div v-if="selectedId === null" class="conversation-empty"><span aria-hidden="true">◇</span>
            <h3>为重要的选择留出空间</h3>
            <p>先在左侧新增会话，再写下你的问题。<br/>可以描述选项、顾虑，以及你期待的结果。</p></div>
          <div v-else-if="messagesLoaded && !messages.length" class="conversation-empty"><span
              aria-hidden="true">◇</span>
            <h3>这次，你想理清什么？</h3>
            <p>写下困扰你的选择，我们从这里开始。</p></div>
          <article v-for="message in messages" :key="message.id" :class="message.role === 'user' ? 'user-message' : 'assistant-message'"
                   class="message">
            <div class="message-meta"><strong>{{ message.role === 'user' ? '你' : '决策助手' }}</strong>
              <time>{{ formatTime(message.createdAt) }}</time>
            </div>
            <div v-if="message.role === 'user'" class="message-content">{{ message.content }}</div>
            <MarkdownText v-else :content="message.content" />
          </article>
          <article v-if="optimisticUser" class="message user-message pending-message">
            <div class="message-meta"><strong>你</strong><time>刚刚</time></div>
            <div class="message-content">{{ optimisticUser.content }}</div>
          </article>
          <article v-if="thinking" class="message assistant-message thinking-message" role="status" aria-live="polite">
            <span class="thinking-dot" aria-hidden="true"></span>
            <span class="sr-only">决策助手正在思考，可点击方形按钮停止等待</span>
          </article>
        </div>
        <div v-if="currentSendError" class="decision-feedback error" role="alert"><span>{{ currentSendError }}</span>
          <button :disabled="busy || messagesLoading || sessionsLoading" @click="loadMessages()">刷新当前会话</button>
        </div>
        <div v-if="conversationNotice" class="decision-feedback" role="status">{{ conversationNotice }}</div>
        <div v-if="prefillNotice" class="decision-feedback" role="status">{{ prefillNotice }}</div>
        <form class="composer" @submit.prevent="sendMessage">
          <label class="composer-label" for="decision-question">你的问题</label>
          <div class="composer-box">
            <textarea id="decision-question" ref="input" v-model="draft" :disabled="busy"
                      maxlength="4000" placeholder="描述你面临的选择，也可以补充背景与顾虑…"
                      rows="3" @keydown="handleKeydown"/>
            <div class="composer-bar">
              <span class="composer-hint">Enter 发送 · Shift + Enter 换行 · {{ draft.length }}/4000</span>
              <button v-if="!sending" class="send-button" type="submit" :disabled="!canSend" aria-label="发送问题" title="发送">
                <svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true"><path d="M12 6v12M12 6l-5.5 5.5M12 6l5.5 5.5" fill="none" stroke="currentColor" stroke-width="2.1" stroke-linecap="round" stroke-linejoin="round"/></svg>
              </button>
              <button v-else class="send-button stop" type="button" aria-label="停止等待回复" title="停止" @click="stopSending">
                <span class="stop-square" aria-hidden="true"></span>
              </button>
            </div>
          </div>
          <p v-if="preferencesLoaded && (preferences.memoryEnabled !== savedPreferences.memoryEnabled || preferenceFields.some(({ key }) => preferences[key] !== savedPreferences[key]))"
             class="draft-note">记忆中有未保存的修改，本次仍按已保存的状态发送。</p>
        </form>
      </section>
    </div>

    <section v-show="activeTab === 'memory'" class="memory-panel">
      <div class="memory-head">
        <h2>记忆</h2>
        <p>写下你希望我记住的部分。只有开启记忆并保存后，这些内容才会随对话发送给模型；手机号和邮箱不会发送，对话也不会自动转成长期记忆。</p>
      </div>
      <div v-if="preferencesLoading" class="preferences-status" role="status">正在加载记忆…</div>
      <div v-if="preferencesError" class="decision-feedback error" role="alert"><span>{{ preferencesError }}</span>
        <button :disabled="preferencesLoading || busy" @click="loadPreferences">重新加载</button>
      </div>
      <div v-if="preferencesNotice" class="decision-feedback" role="status">{{ preferencesNotice }}</div>
      <form @submit.prevent="savePreferences">
        <fieldset :disabled="!preferencesLoaded || preferencesLoading || sessionsLoading || busy">
          <div class="preference-grid"><label v-for="field in preferenceFields" :key="field.key"
                                              class="preference-field"><span>{{ field.label }}</span><textarea
              v-model="preferences[field.key]" :placeholder="field.placeholder" maxlength="1000"
              rows="2"/><small>{{ preferences[field.key].length }} / 1000</small></label></div>
          <label class="memory-switch"><input v-model="preferences.memoryEnabled" role="switch" type="checkbox"/>启用记忆（保存后生效）</label>
          <div class="preferences-actions">
            <button class="danger-button" type="button" @click="clearPreferences">清空记忆</button>
            <button class="button-primary" type="submit">{{ preferencesBusy ? '处理中…' : '保存记忆' }}</button>
          </div>
        </fieldset>
      </form>
    </section>
  </section>
</template>

<style scoped>
.decision-space {
  display: flex;
  flex-direction: column;
  height: calc(100dvh - 24px);
  min-height: 380px;
}

.decision-heading, .decision-tabs {
  flex-shrink: 0;
}

.decision-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 10px;
}

.decision-heading h1 {
  margin: 6px 0;
  font-size: 23px;
  font-weight: 600;
  letter-spacing: -1px;
}

.decision-heading p {
  margin: 0;
  font-size: 11px;
  color: var(--text-secondary);
  line-height: 1.8;
}

.model-label {
  flex-shrink: 0;
  padding: 5px 8px;
  border-radius: 6px;
  color: var(--accent);
  background: var(--accent-light);
  font-size: 10px;
}

.decision-feedback {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
  padding: 12px 14px;
  margin: 12px 0;
  background: #eff7f0;
  border: 1px solid #d7e5d9;
  border-radius: 7px;
  color: #41624a;
  font-size: 12px;
  line-height: 1.8;
  overflow-wrap: anywhere;
}

.decision-feedback.error {
  background: #fff5f3;
  border-color: #f1ddd7;
  color: #a63a3a;
}

.decision-feedback button, .link-button {
  flex-shrink: 0;
  border: 0;
  background: transparent;
  color: inherit;
  padding: 3px 0;
  font-size: 12px;
  text-decoration: underline;
  text-underline-offset: 3px;
}

.decision-tabs {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-shrink: 0;
  border-bottom: 1px solid var(--border);
  margin-bottom: 14px;
}

.tab-list {
  display: flex;
  gap: 22px;
}

.tab-list button {
  position: relative;
  border: 0;
  background: transparent;
  padding: 0 0 10px;
  font-size: 12px;
  color: var(--text-secondary);
}

.tab-list button.active {
  color: var(--accent);
  font-weight: 600;
}

.tab-list button.active::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -1px;
  height: 2px;
  border-radius: 2px 2px 0 0;
  background: var(--accent);
}

.memory-panel {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding-right: 6px;
}

.memory-head h2 {
  margin: 0 0 6px;
  font-size: 14px;
  font-weight: 600;
}

.memory-head p {
  margin: 0 0 18px;
  font-size: 11px;
  color: var(--text-secondary);
  line-height: 1.9;
}

.memory-panel form {
  max-width: 820px;
}

.preferences-status {
  padding: 4px 0 12px;
  font-size: 12px;
  color: var(--text-secondary);
}

.privacy-note {
  margin: 0 0 15px;
  color: var(--text-secondary);
  font-size: 11px;
  line-height: 1.9;
}

.memory-panel fieldset {
  min-width: 0;
  border: 0;
  margin: 0;
  padding: 0;
}

.preference-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.preference-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
  font-size: 12px;
}

.preference-field:last-child {
  grid-column: 1 / -1;
}

.preference-field small {
  color: var(--text-muted);
  font-size: 10px;
  text-align: right;
}

textarea {
  width: 100%;
  border: 1px solid var(--border);
  border-radius: 7px;
  padding: 11px;
  color: var(--text);
  background: #fff;
  font: inherit;
  font-size: 12px;
  line-height: 1.8;
}

textarea {
  resize: vertical;
}

textarea:focus-visible {
  outline: 2px solid #a7c5b0;
  outline-offset: 2px;
}

textarea:disabled {
  background: #f7f9f6;
}

.memory-switch {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  margin: 18px 0;
  line-height: 1.8;
}

.memory-switch input {
  accent-color: var(--accent);
}

.preferences-actions {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.button-primary {
  padding: 10px 18px;
}

.danger-button {
  padding: 8px 0;
  border: 0;
  background: transparent;
  color: #a65346;
  font-size: 12px;
}

.decision-layout {
  display: grid;
  grid-template-columns: 210px minmax(0, 1fr);
  gap: 30px;
  flex: 1;
  min-height: 0;
  align-items: stretch;
}

.sessions-panel, .conversation-panel {
  background: transparent;
  border: 0;
  border-radius: 0;
  min-width: 0;
}

.sessions-panel {
  padding: 0 24px 0 0;
  border-right: 1px solid var(--border);
  min-height: 0;
  overflow-y: auto;
}

.sessions-heading, .conversation-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.sessions-heading h2, .conversation-heading h2 {
  font-size: 14px;
  font-weight: 600;
  margin: 0;
  overflow-wrap: anywhere;
}

.link-button {
  color: var(--accent);
}

.add-session {
  width: 24px;
  height: 24px;
  display: grid;
  place-items: center;
  flex-shrink: 0;
  padding: 0;
  line-height: 0;
  border: 1px solid #dde6da;
  border-radius: 7px;
  background: #fff;
  color: var(--accent);
  cursor: pointer;
  transition: background .15s ease, border-color .15s ease;
}

.add-session svg {
  display: block;
}

.add-session:hover:not(:disabled) {
  background: var(--accent-light);
  border-color: #c9ddca;
}

.add-session:disabled {
  color: #b3c0b1;
  cursor: not-allowed;
  background: #f7f9f6;
}

.session-list {
  list-style: none;
  padding: 0;
  margin: 10px 0 0;
}

.session-list li {
  display: flex;
  align-items: center;
  margin-top: 5px;
  border-radius: 7px;
}

.session-list li.selected {
  background: var(--accent-light);
}

.session-select {
  flex: 1;
  min-width: 0;
  border: 0;
  text-align: left;
  padding: 12px 9px;
  background: transparent;
  color: var(--text);
}

.session-select strong {
  display: block;
  font-size: 12px;
  line-height: 1.7;
  font-weight: 500;
  overflow-wrap: anywhere;
}

.session-select small {
  display: block;
  margin-top: 5px;
  font-size: 10px;
  color: var(--text-muted);
}

.session-delete {
  border: 0;
  background: transparent;
  color: #a65346;
  font-size: 10px;
  padding: 10px 7px;
  flex-shrink: 0;
}

.conversation-panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.conversation-heading {
  padding: 0 0 10px;
  border-bottom: 1px solid var(--border);
}

.conversation-heading p {
  margin: 4px 0 0;
  font-size: 10px;
  color: var(--text-muted);
  line-height: 1.8;
}

.message-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 12px 0 2px;
}

.empty-copy {
  margin: 18px 0;
  font-size: 12px;
  line-height: 1.9;
  color: var(--text-secondary);
}

.conversation-empty {
  text-align: center;
  padding: 46px 10px;
}

.conversation-empty > span {
  font-size: 32px;
  color: #8ca592;
}

.conversation-empty h3 {
  margin: 18px 0 10px;
  font-size: 15px;
  font-weight: 500;
}

.conversation-empty p {
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.9;
}

.message {
  width: fit-content;
  max-width: 88%;
  margin-bottom: 10px;
  padding: 11px 14px;
  border-radius: 9px;
}

.user-message {
  margin-left: auto;
  background: #eaf3e8;
  border: 1px solid #dce9d8;
}

.assistant-message {
  margin-right: auto;
  background: #fff;
  border: 1px solid #e7eee4;
  box-shadow: 0 1px 2px rgba(54, 78, 60, .03);
}

.message-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 9px;
  font-size: 10px;
  color: var(--text-secondary);
}

.message-meta strong {
  font-weight: 500;
  color: var(--accent);
}

.message-content {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  font-size: 13px;
  line-height: 1.9;
}

.pending-message {
  opacity: .72;
}

.thinking-message {
  display: flex;
  align-items: center;
  min-height: 44px;
  padding: 15px 18px;
}

.thinking-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: var(--accent);
  animation: thinking-breathe 1.35s ease-in-out infinite;
}

@keyframes thinking-breathe {
  0%, 100% {
    opacity: .28;
    transform: scale(.82);
  }
  50% {
    opacity: 1;
    transform: scale(1.08);
  }
}

@media (prefers-reduced-motion: reduce) {
  .thinking-dot {
    animation: none;
    opacity: .8;
  }
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.conversation-panel > .decision-feedback {
  margin: 12px 0;
}

.composer {
  flex-shrink: 0;
  border-top: 0;
  padding: 10px 0 0;
}

.composer-label {
  display: block;
  font-size: 11px;
  margin-bottom: 8px;
}

.composer-box {
  border: 1px solid #dde6da;
  border-radius: 12px;
  background: #fff;
  padding: 8px 10px 6px;
  transition: border-color .15s ease, box-shadow .15s ease;
}

.composer-box:focus-within {
  border-color: #a9c6b1;
  box-shadow: 0 0 0 3px #eef5ed;
}

.composer textarea {
  display: block;
  min-height: 52px;
  max-height: 200px;
  border: 0;
  padding: 4px 0;
  background: transparent;
  resize: none;
}

.composer textarea:focus-visible {
  outline: none;
}

.draft-note {
  margin: 8px 0;
  font-size: 11px;
  color: #876b38;
  line-height: 1.8;
}

.composer-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 4px;
}

.composer-hint {
  font-size: 9px;
  line-height: 1.8;
  color: var(--text-muted);
}

.send-button {
  width: 30px;
  height: 30px;
  flex-shrink: 0;
  display: grid;
  place-items: center;
  padding: 0;
  line-height: 0;
  border: 0;
  border-radius: 9px;
  background: var(--accent);
  color: #fff;
  cursor: pointer;
  transition: opacity .15s ease, background .15s ease;
}

.send-button svg {
  display: block;
}

.send-button:hover:not(:disabled) {
  background: #3f6b4d;
}

.send-button:disabled {
  background: #dfe7dc;
  color: #a9b6a7;
  cursor: not-allowed;
}

.send-button.stop {
  background: #eef3ed;
  color: #3f6b4d;
  border: 1px solid #d9e4d7;
}

.send-button.stop:hover {
  background: #e4ece1;
}

.stop-square {
  width: 9px;
  height: 9px;
  border-radius: 2px;
  background: currentColor;
  display: block;
}

.message-list, .sessions-panel, .memory-panel {
  scrollbar-width: thin;
  scrollbar-color: #d8e3d7 transparent;
}

.message-list::-webkit-scrollbar,
.sessions-panel::-webkit-scrollbar,
.memory-panel::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

.message-list::-webkit-scrollbar-track,
.sessions-panel::-webkit-scrollbar-track,
.memory-panel::-webkit-scrollbar-track {
  background: transparent;
}

.message-list::-webkit-scrollbar-thumb,
.sessions-panel::-webkit-scrollbar-thumb,
.memory-panel::-webkit-scrollbar-thumb {
  background: #dde7dc;
  border-radius: 3px;
}

.message-list::-webkit-scrollbar-thumb:hover,
.sessions-panel::-webkit-scrollbar-thumb:hover,
.memory-panel::-webkit-scrollbar-thumb:hover {
  background: #cad8c8;
}

@media (max-width: 1000px) {
  .decision-layout {
    grid-template-columns: 180px minmax(0, 1fr);
    gap: 20px;
  }

  .decision-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .sessions-panel {
    padding-right: 18px;
  }

  .message-list {
    padding: 18px 0 6px;
  }
}

@media (max-width: 700px) {
  .decision-layout {
    grid-template-columns: 1fr;
    gap: 0;
  }

  .sessions-panel {
    padding: 0 0 20px;
    border-right: 0;
    border-bottom: 1px solid var(--border);
    margin-bottom: 20px;
  }

  .preference-grid {
    grid-template-columns: 1fr;
  }

  .decision-heading h1 {
    font-size: 24px;
  }

  .composer-hint {
    display: none;
  }

  .composer-bar {
    justify-content: flex-end;
  }

  .message {
    max-width: 97%;
  }

  .decision-space {
    height: auto;
    min-height: 0;
  }

  .message-list, .sessions-panel, .memory-panel {
    overflow: visible;
  }
}
</style>
