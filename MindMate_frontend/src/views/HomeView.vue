<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { avatarAddress, fetchMe, logout, updateMe, uploadAvatar } from '@/api/auth'
import { TOKEN_KEY } from '@/api/request'
import { createTask, deleteTask, fetchTasks, updateTask, updateTaskStatus } from '@/api/tasks'
import TaskList from '@/components/TaskList.vue'

const router = useRouter()
const user = ref(null)
const loading = ref(true)
const errorMessage = ref('')
const loggingOut = ref(false)
const activeView = ref('overview')
const notice = ref('')
const tasks = ref([])
const taskList = ref(null)
const openTodayTask = (task) => {
  taskList.value?.showTask(task)
  activeView.value = 'tasks'
}
const tasksLoading = ref(false)
const tasksLoaded = ref(false)
const taskLoadError = ref('')
const taskActionError = ref('')
const taskBusy = ref('')
const taskMessage = ref('')
const now = ref(new Date())
const localDate = (date) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
const today = computed(() => localDate(now.value))
const isOverdue = (task) => task.status !== 2 && task.dueAt && new Date(task.dueAt).getTime() < now.value.getTime()
const todayTasks = computed(() => tasks.value.filter((task) => task.status !== 2 && (
  task.plannedDate === today.value || task.dueAt?.slice(0, 10) === today.value || isOverdue(task)
)).sort((a, b) => Number(Boolean(isOverdue(b))) - Number(Boolean(isOverdue(a))) || b.priority - a.priority || (a.dueAt || '9999').localeCompare(b.dueAt || '9999')))
const weeklyCompleted = computed(() => {
  const monday = new Date(now.value)
  monday.setHours(0, 0, 0, 0)
  monday.setDate(monday.getDate() - (monday.getDay() + 6) % 7)
  return tasks.value.filter((task) => {
    if (task.status !== 2 || !task.completedAt) return false
    const completed = new Date(task.completedAt)
    return completed >= monday && completed <= now.value
  }).length
})
const reloadTasks = async () => {
  if (tasksLoading.value || taskBusy.value) return
  tasksLoading.value = true
  taskLoadError.value = ''
  now.value = new Date()
  try {
    tasks.value = await fetchTasks()
    tasksLoaded.value = true
  } catch (error) {
    taskLoadError.value = `${tasksLoaded.value ? '任务刷新失败，当前保留上次成功的数据' : '任务加载失败'}：${error.message || '请稍后重试'}`
  } finally { tasksLoading.value = false }
}
const runTaskAction = async (key, action, message) => {
  if (taskBusy.value || tasksLoading.value) return false
  taskBusy.value = key
  taskActionError.value = ''
  taskMessage.value = ''
  let succeeded = false
  try {
    await action()
    now.value = new Date()
    taskMessage.value = message
    succeeded = true
  } catch (error) {
    taskActionError.value = `操作失败：${error.message || '请稍后重试'}。请在原操作处重试。`
  } finally { taskBusy.value = '' }
  // 先同步成功响应，再刷新；刷新失败也保留已保存的结果。
  if (succeeded) await reloadTasks()
  return succeeded
}
const saveTask = (id, data) => runTaskAction(id === null ? 'create' : `save:${id}`, async () => {
  const saved = id === null ? await createTask(data) : await updateTask(id, data)
  const index = tasks.value.findIndex((task) => task.id === saved.id)
  if (index === -1) tasks.value.unshift(saved)
  else tasks.value.splice(index, 1, saved)
}, '任务已保存')
const changeTaskStatus = (task) => runTaskAction(`status:${task.id}`, async () => {
  const saved = await updateTaskStatus(task.id, task.status === 2 ? 0 : 2)
  const index = tasks.value.findIndex((item) => item.id === saved.id)
  if (index !== -1) tasks.value.splice(index, 1, saved)
}, task.status === 2 ? '任务已重开' : '任务已完成')
const removeTask = (id) => runTaskAction(`delete:${id}`, async () => {
  await deleteTask(id)
  tasks.value = tasks.value.filter((task) => task.id !== id)
}, '任务已删除')
const refreshOnFocus = () => {
  now.value = new Date()
  if (activeView.value === 'overview' || activeView.value === 'tasks') void reloadTasks()
}
watch(activeView, (view) => {
  if (view === 'overview' || view === 'tasks') void reloadTasks()
})
let clockTimer
const views = [
  { id: 'overview', icon: '◫', label: '我的工作台' },
  { id: 'tasks', icon: '☷', label: '任务清单' },
  { id: 'decisions', icon: '◇', label: '决策空间' },
  { id: 'review', icon: '↺', label: '回顾与洞察' },
]
const currentTitle = computed(() => views.find((item) => item.id === activeView.value)?.label || '个人账号')
const name = computed(() => user.value?.username || '朋友')
const form = ref({ username: '', email: '', personalizedSignature: '' })
const saving = ref(false)
const uploading = ref(false)
const avatarFailed = ref(false)
const fileInput = ref(null)
const profileError = ref('')
const profileMessage = ref('')
const avatarUrl = computed(() => avatarFailed.value ? '' : avatarAddress(user.value?.avatarUrl))
const syncForm = () => {
  form.value = {
    username: user.value?.username || '',
    email: user.value?.email || '',
    personalizedSignature: user.value?.personalizedSignature || '',
  }
}
const saveProfile = async () => {
  if (saving.value || uploading.value) return
  saving.value = true
  profileError.value = ''
  profileMessage.value = ''
  try {
    user.value = await updateMe({ ...form.value })
    syncForm()
    profileMessage.value = '个人信息已保存'
  } catch (error) { profileError.value = error.message }
  finally { saving.value = false }
}
const handleAvatar = async (event) => {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file || uploading.value || saving.value) return
  profileError.value = ''
  profileMessage.value = ''
  if (!['image/jpeg', 'image/png'].includes(file.type) || file.size > 2 * 1024 * 1024) {
    profileError.value = '请选择不超过 2MB 的 JPG 或 PNG 图片'
    return
  }
  uploading.value = true
  try {
    const updatedUser = await uploadAvatar(file)
    user.value = { ...user.value, avatarUrl: updatedUser.avatarUrl }
    avatarFailed.value = false
    profileMessage.value = '头像已更新'
  } catch (error) { profileError.value = error.message }
  finally { uploading.value = false }
}
const maskedPhone = computed(() => user.value?.phoneNumber?.replace(/^(\d{3})\d{4}(\d{4})$/, '$1 **** $2') || '个人空间')
const dateLabel = computed(() => new Intl.DateTimeFormat('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' }).format(now.value))
const greeting = computed(() => now.value.getHours() < 12 ? '上午好' : now.value.getHours() < 18 ? '下午好' : '晚上好')
const formatDateTime = (value) => value ? String(value).replace('T', ' ').slice(0, 16) : '未设置'
const showNotice = (message) => { notice.value = message }
const loadUser = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    user.value = await fetchMe()
    avatarFailed.value = false
    syncForm()
  }
  catch (error) { errorMessage.value = error.message }
  finally { loading.value = false }
}
const handleLogout = async () => {
  if (loggingOut.value) return
  loggingOut.value = true
  errorMessage.value = ''
  try {
    await logout()
    localStorage.removeItem(TOKEN_KEY)
    router.replace({ name: 'login' })
  } catch (error) { errorMessage.value = error.message || '退出失败，请重试' }
  finally { loggingOut.value = false }
}
onMounted(() => {
  void loadUser()
  void reloadTasks()
  window.addEventListener('focus', refreshOnFocus)
  clockTimer = window.setInterval(() => { now.value = new Date() }, 60000)
})
onUnmounted(() => {
  window.removeEventListener('focus', refreshOnFocus)
  window.clearInterval(clockTimer)
})
</script>

<template>
  <div class="workspace">
    <aside class="sidebar">
      <a class="brand-lockup" href="/home"><span class="brand-mark">m<span>·</span></span>MindMate</a>
      <div class="space-label">PERSONAL WORKSPACE</div>
      <nav class="navigation" aria-label="主导航"><button v-for="view in views" :key="view.id" :class="{ active: activeView === view.id }" :aria-current="activeView === view.id ? 'page' : undefined" @click="activeView = view.id; notice = ''"><span class="nav-icon" aria-hidden="true">{{ view.icon }}</span>{{ view.label }}<span v-if="view.id === 'decisions'" class="ai-tag">AI</span></button></nav>
      <div class="sidebar-bottom"><div class="sidebar-quote">每一次决定，<br />都会有帮助。</div><button class="account-button" :class="{ selected: activeView === 'account' }" @click="activeView = 'account'; notice = ''"><span class="avatar"><img v-if="avatarUrl" :src="avatarUrl" alt="个人头像" @error="avatarFailed = true" /><template v-else>{{ name.slice(0, 1) }}</template></span><span><strong>{{ loading ? '加载中…' : name }}</strong><small>{{ maskedPhone }}</small></span><span class="account-arrow">↗</span></button></div>
    </aside>
    <div class="main-shell">
      <header class="topbar"><div><span class="breadcrumb">个人空间</span><span class="slash">/</span><span>{{ currentTitle }}</span></div><div class="topbar-right"><span class="workspace-label"><i></i> 专注当下</span><button class="text-action" :disabled="loggingOut" @click="handleLogout">{{ loggingOut ? '退出中…' : '退出登录' }}</button></div></header>
      <main class="content">
        <div v-if="errorMessage" class="feedback error" role="alert">{{ errorMessage }}<button @click="loadUser">重新加载</button></div>
        <div v-if="notice" class="feedback" role="status">{{ notice }}<button aria-label="关闭提示" @click="notice = ''">×</button></div>
        <template v-if="activeView === 'overview' || activeView === 'tasks'">
          <div v-if="taskLoadError" class="feedback error" role="alert"><span>{{ taskLoadError }}</span><button :disabled="tasksLoading || Boolean(taskBusy)" @click="reloadTasks">{{ tasksLoading ? '重试中…' : '重新加载' }}</button></div>
          <div v-if="taskActionError" class="feedback error" role="alert"><span>{{ taskActionError }}</span><button aria-label="关闭任务操作错误" @click="taskActionError = ''">×</button></div>
          <div v-if="taskMessage" class="feedback" role="status"><span>{{ taskMessage }}</span><button aria-label="关闭任务提示" @click="taskMessage = ''">×</button></div>
        </template>
        <template v-if="activeView === 'overview'">
          <section class="welcome"><div><span class="eyebrow">{{ dateLabel }}</span><h1>{{ greeting }}，{{ name }}<span class="greeting-dot">。</span></h1><p>把纷繁留在外面，在这里找到今天的方向。</p></div><span class="welcome-stamp">ONE THING AT A TIME</span></section>
          <section class="focus-banner"><div><span class="eyebrow">A LITTLE CLARITY, EVERY DAY</span><h2>先理清思路，再迈出下一步。</h2><p>记录值得做的事，留意真正重要的选择。</p><button class="button-primary" @click="activeView = 'tasks'">整理我的清单 <span aria-hidden="true">↗</span></button></div><div class="focus-art" aria-hidden="true"><div class="art-orbit orbit-one"></div><div class="art-orbit orbit-two"></div><div class="art-center">m<span>·</span></div><span class="art-point point-one"></span><span class="art-point point-two"></span><span class="art-caption">THINK CLEAR. MOVE FORWARD.</span></div></section>
          <section class="summary-grid" aria-label="工作台概览"><div class="summary-card"><span>今日待办</span><strong>{{ tasksLoaded ? todayTasks.length : '—' }}<small>件</small></strong><p>今日计划与截止，含逾期待办</p></div><div class="summary-card"><span>等待思考的决定</span><strong>—<small>个</small></strong><p>为重要的选择留出空间</p></div><div class="summary-card"><span>本周已完成</span><strong>{{ tasksLoaded ? weeklyCompleted : '—' }}<small>件</small></strong><p>从本地周一零点起，记录每一点进展</p></div></section>
          <div class="dashboard-grid"><section class="panel today-panel"><div class="section-heading"><h2>今日清单</h2><button class="text-action" @click="activeView = 'tasks'">查看全部 <span aria-hidden="true">↗</span></button></div><p v-if="tasksLoading" class="today-loading" role="status">{{ tasksLoaded ? '正在刷新今日任务…' : '正在加载今日任务…' }}</p><ul v-if="tasksLoaded && todayTasks.length" class="today-list"><li v-for="task in todayTasks.slice(0, 5)" :key="task.id"><button class="today-task-link" @click="openTodayTask(task)"><strong>{{ task.title }}</strong><span :class="{ overdue: isOverdue(task) }">{{ isOverdue(task) ? `已逾期 · ${task.dueAt.slice(0, 10)}` : task.dueAt ? `截止 ${task.dueAt.slice(0, 10)}` : '计划今天' }}</span></button><button class="text-action" @click="openTodayTask(task)">前往处理 ↗</button></li></ul><div v-else-if="!tasksLoading && tasksLoaded" class="empty-state"><div class="empty-symbol" aria-hidden="true">☷</div><h3>今天的清单很轻盈</h3><p>没有今日计划、今日截止或逾期的待办。<br />去清单里，安排下一件值得做的事。</p><button class="button-secondary" @click="activeView = 'tasks'">安排任务</button></div><p v-else-if="!tasksLoading" class="today-loading">今日任务暂不可用，请点击上方“重新加载”。</p><p v-if="tasksLoaded && todayTasks.length > 5" class="today-more">还有 {{ todayTasks.length - 5 }} 项待办，可前往任务清单查看。</p><div class="panel-footer"><span class="tiny-dot"></span>不必填满每一天，重要的是知道先做什么。</div></section><section class="panel companion-panel"><div class="section-heading"><h2>你的决策伙伴</h2><span class="ai-tag">AI · 筹备中</span></div><p class="companion-intro">当选择变得复杂，<br />试着把问题拆得简单一点。</p><div class="prompt-list"><button @click="showNotice('AI 决策功能正在筹备中，后续将结合你的任务与目标提供建议。')"><span>01</span>今天，我应该先做什么？<span>↗</span></button><button @click="showNotice('决策空间即将开放，后续可以在这里梳理选项、收益与代价。')"><span>02</span>这两个选择，该如何取舍？<span>↗</span></button><button @click="showNotice('行动拆解功能正在筹备中，后续将帮助你把目标变成可执行的步骤。')"><span>03</span>如何把目标变成下一步？<span>↗</span></button></div><p class="companion-note">帮你梳理思路，决定始终由你做。</p></section></div>
          <footer class="content-footer"><span>MindMate · 每一次决定，都会有帮助</span><span>留一点空间，给更好的答案。</span></footer>
        </template>
        <TaskList ref="taskList" v-show="activeView === 'tasks'" :tasks="tasks" :loading="tasksLoading" :loaded="tasksLoaded" :busy="taskBusy" :now="now" :save-task="saveTask" :change-status="changeTaskStatus" :remove-task="removeTask" @refresh="reloadTasks" />
        <section v-if="activeView === 'account'" class="account-view">
          <span class="eyebrow">YOUR PERSONAL SPACE</span><h1>个人信息</h1><p class="page-description">让你的个人空间，多一点自己的样子。</p>
          <div class="panel profile-panel">
            <p v-if="loading">正在加载账号信息…</p>
            <template v-else-if="user">
              <div class="avatar-editor">
                <span class="avatar profile-avatar"><img v-if="avatarUrl" :src="avatarUrl" alt="个人头像" @error="avatarFailed = true" /><template v-else>{{ name.slice(0, 1) }}</template></span>
                <div><h2>个人头像</h2><p>支持 JPG、PNG，大小不超过 2MB</p><input ref="fileInput" class="file-input" type="file" accept="image/jpeg,image/png" aria-label="选择头像图片" :disabled="uploading || saving" @change="handleAvatar" /><button type="button" class="button-secondary" :disabled="uploading || saving" @click="fileInput?.click()">{{ uploading ? '正在上传…' : '更换头像' }}</button></div>
              </div>
              <p v-if="profileError" class="profile-error" role="alert">{{ profileError }}</p><p v-if="profileMessage" class="profile-success" role="status">{{ profileMessage }}</p>
              <form class="profile-form" @submit.prevent="saveProfile">
                <fieldset :disabled="saving || uploading">
                  <label class="profile-field">手机号<input :value="user.phoneNumber" readonly /><small>手机号作为登录账号，暂不支持修改</small></label>
                  <label class="profile-field">用户名<input v-model.trim="form.username" maxlength="50" placeholder="为自己取一个名字" autocomplete="nickname" /></label>
                  <label class="profile-field">邮箱<input v-model.trim="form.email" type="email" maxlength="100" placeholder="请输入邮箱（选填）" autocomplete="email" /></label>
                  <label class="profile-field full-width">个性签名<textarea v-model.trim="form.personalizedSignature" maxlength="255" rows="3" placeholder="写下一句此刻想对自己说的话" /><small>{{ form.personalizedSignature.length }} / 255</small></label>
                </fieldset>
                <div class="profile-actions"><span>最近登录：{{ formatDateTime(user.lastLoginAt) }}</span><div><button type="button" class="button-secondary" :disabled="saving || uploading" @click="syncForm(); profileError = ''; profileMessage = ''">重置</button><button class="button-primary" type="submit" :disabled="saving || uploading">{{ saving ? '保存中…' : '保存修改' }}</button></div></div>
              </form>
            </template>
            <p v-else class="page-description">账号信息暂时无法加载，请点击上方“重新加载”。</p>
          </div>
        </section>
        <section v-else-if="activeView === 'decisions' || activeView === 'review'" class="module-view"><span class="eyebrow">MAKE ROOM FOR WHAT MATTERS</span><h1>{{ currentTitle }}</h1><p class="page-description">{{ activeView === 'decisions' ? '把选项、顾虑与期待放在一起，让选择更清晰。' : '回头看看走过的路，为下一次选择积累经验。' }}</p><div class="panel module-placeholder"><span class="empty-symbol" aria-hidden="true">{{ activeView === 'decisions' ? '◇' : '↺' }}</span><h2>{{ currentTitle }}，即将开放</h2><p>这个空间已为你预留，更多能力正在准备中。</p><button class="button-secondary" @click="activeView = 'overview'">返回工作台</button></div></section>
      </main>
    </div>
  </div>
</template>

<style scoped>
.workspace { min-height: 100dvh; display: flex; background: #f7f8f6; }
.sidebar { position: fixed; inset: 0 auto 0 0; width: 230px; display: flex; flex-direction: column; padding: 32px 22px 20px; background: #fff; border-right: 1px solid var(--border); z-index: 10; }.sidebar .brand-lockup { margin-left: 8px; }
.space-label { font-size: 9px; letter-spacing: 1.5px; color: #9aaca1; margin: 48px 12px 15px; }
.navigation { display: flex; flex-direction: column; gap: 7px; }.navigation button { display: flex; align-items: center; gap: 12px; border: 0; background: transparent; color: var(--text-secondary); text-align: left; padding: 13px; border-radius: 7px; font-size: 13px; }.navigation button:hover { background: #f7f9f7; }.navigation button.active { background: var(--accent-light); color: var(--accent); font-weight: 600; }.nav-icon { font-size: 20px; width: 22px; font-weight: 400; }.ai-tag { background: #edf3ee; color: #557b65; padding: 3px 6px; border-radius: 4px; font-size: 9px; font-weight: 500; white-space: nowrap; }.navigation .ai-tag { margin-left: auto; }
.sidebar-bottom { margin-top: auto; }.sidebar-quote { margin: 40px 12px 25px; font-size: 15px; line-height: 1.9; color: #83988a; }.account-button { display: flex; align-items: center; gap: 10px; width: 100%; padding: 14px 4px 0; border: 0; border-top: 1px solid var(--border); background: transparent; text-align: left; }.avatar { display: grid; place-items: center; width: 35px; height: 35px; background: #edf2ed; border-radius: 50%; color: var(--accent); font-size: 13px; }.account-button strong { display: block; font-size: 12px; font-weight: 500; max-width: 108px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.account-button small { display: block; font-size: 10px; color: var(--text-muted); margin-top: 4px; }.account-arrow { margin-left: auto; color: #8a978e; }.account-button.selected strong { color: var(--accent); }
.main-shell { margin-left: 230px; flex: 1; min-width: 0; }.topbar { min-height: 73px; padding: 0 38px; display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid var(--border); font-size: 11px; background: #ffffffb3; }.breadcrumb { color: var(--text-muted); }.slash { margin: 0 15px; color: #bec6bf; }.topbar-right { display: flex; align-items: center; gap: 25px; }.workspace-label { color: var(--text-secondary); display: flex; align-items: center; gap: 7px; }.workspace-label i, .tiny-dot { width: 5px; height: 5px; background: #769a7f; border-radius: 50%; display: inline-block; }.text-action { border: 0; background: transparent; color: var(--text-secondary); padding: 5px 0; font-size: 11px; }.text-action:hover { color: var(--accent); }
.content { max-width: 1280px; margin: 0 auto; padding: 36px 40px 22px; }.welcome { display: flex; align-items: center; justify-content: space-between; margin-bottom: 28px; }.welcome h1, .account-view h1, .module-view h1 { font-size: 28px; font-weight: 600; letter-spacing: -1px; margin: 12px 0; }.greeting-dot { color: var(--accent); }.welcome p, .page-description { color: var(--text-secondary); font-size: 12px; line-height: 1.8; margin: 0; }.welcome-stamp { font-size: 9px; letter-spacing: 2px; color: #94a398; }
.focus-banner { min-height: 230px; background: #edf3ec; border: 1px solid #e2eadf; border-radius: 12px; display: flex; align-items: center; justify-content: space-between; padding: 30px 36px; overflow: hidden; }.focus-banner .eyebrow { color: #73917c; font-size: 9px; }.focus-banner h2 { font-size: clamp(20px, 2vw, 27px); margin: 14px 0 12px; font-weight: 500; letter-spacing: -.6px; }.focus-banner p { font-size: 12px; color: #7a8c7e; margin: 0 0 23px; }.focus-banner .button-primary { padding: 10px 15px; font-size: 11px; }.focus-banner .button-primary span { margin-left: 28px; }
.focus-art { width: 240px; height: 185px; flex-shrink: 0; position: relative; display: grid; place-items: center; }.art-orbit { border: 1px solid #cbdacb; border-radius: 50%; position: absolute; width: 180px; height: 180px; }.orbit-one { transform: rotate(-25deg) scaleX(.65); }.orbit-two { transform: rotate(45deg) scaleX(.65); }.art-center { font-size: 66px; font-weight: 600; color: #52765e; letter-spacing: -7px; padding-bottom: 12px; }.art-center span { color: #9cb09c; }.art-point { position: absolute; width: 8px; height: 8px; border-radius: 50%; background: #739479; }.point-one { top: 25px; left: 84px; }.point-two { bottom: 32px; right: 63px; width: 5px; height: 5px; background: #a6b8a3; }.art-caption { position: absolute; bottom: -2px; font-size: 7px; color: #8da08f; letter-spacing: 1.8px; }
.summary-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin: 20px 0; }.summary-card { border: 1px solid var(--border); border-radius: 10px; padding: 20px 23px; background: #fff; }.summary-card > span { font-size: 11px; color: var(--text-secondary); }.summary-card strong { display: block; font-size: 29px; font-weight: 500; margin: 10px 0; color: #526456; }.summary-card small { font-size: 10px; margin-left: 9px; font-weight: 400; color: var(--text-muted); }.summary-card p { color: var(--text-muted); font-size: 10px; margin: 0; }
.dashboard-grid { display: grid; grid-template-columns: 1.2fr 1fr; gap: 20px; }.panel { border: 1px solid var(--border); border-radius: 10px; background: #fff; }.section-heading { display: flex; align-items: center; justify-content: space-between; gap: 10px; }.section-heading h2 { font-size: 14px; font-weight: 600; margin: 0; }.today-panel { padding: 23px 24px 0; }.empty-state { text-align: center; padding: 28px 0 24px; }.empty-symbol { font-size: 26px; color: #8ca592; background: #f2f6f1; width: 50px; height: 50px; border-radius: 13px; display: inline-grid; place-items: center; }.empty-state h3 { font-size: 13px; font-weight: 500; margin: 17px 0 10px; }.empty-state p, .module-placeholder p { font-size: 11px; color: var(--text-muted); line-height: 1.9; }.status-pill { display: inline-block; font-size: 9px; background: #f5f6f3; color: #8b9789; border-radius: 4px; padding: 5px 10px; margin-top: 7px; }.panel-footer { border-top: 1px solid #eff2ed; padding: 14px 0; font-size: 9px; color: var(--text-muted); display: flex; align-items: center; gap: 8px; }.companion-panel { padding: 23px 24px; }.companion-intro { font-size: 19px; line-height: 1.7; color: #526b59; margin: 24px 0 17px; letter-spacing: -.4px; }.prompt-list { display: flex; flex-direction: column; }.prompt-list button { display: flex; align-items: center; gap: 10px; padding: 12px 0; background: transparent; border: 0; border-bottom: 1px solid #eff2ed; color: var(--text-secondary); font-size: 11px; text-align: left; }.prompt-list button:hover { color: var(--accent); }.prompt-list button span:first-child { font-size: 9px; color: #a4b2a5; }.prompt-list button span:last-child { margin-left: auto; color: #9aaa9b; }.companion-note { font-size: 9px; color: var(--text-muted); margin: 18px 0 0; }.content-footer { display: flex; justify-content: space-between; gap: 15px; color: #9aa69b; font-size: 9px; padding-top: 25px; }
.feedback { display: flex; justify-content: space-between; align-items: center; gap: 12px; padding: 13px 16px; margin-bottom: 20px; border: 1px solid #d7e5d9; background: #eff7f0; border-radius: 7px; color: #41624a; font-size: 12px; line-height: 1.7; }.feedback button { border: 0; background: transparent; color: inherit; white-space: nowrap; }.feedback.error { color: #a63a3a; background: #fff5f3; border-color: #f1ddd7; }.profile-panel { padding: 28px; margin-top: 28px; }.profile { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 28px; margin: 0; }.profile dt { font-size: 11px; color: var(--text-muted); margin-bottom: 9px; }.profile dd { font-size: 14px; margin: 0; overflow-wrap: anywhere; }.module-placeholder { margin-top: 28px; min-height: 360px; display: flex; flex-direction: column; align-items: center; justify-content: center; text-align: center; padding: 30px; }.module-placeholder h2 { font-size: 19px; font-weight: 500; margin: 22px 0 0; }.module-placeholder .button-secondary { margin-top: 18px; }
.today-loading, .today-more { color: var(--text-secondary); font-size: 11px; line-height: 1.8; padding: 16px 0; margin: 0; }
.today-list { list-style: none; padding: 8px 0; margin: 0; }
.today-list li { display: flex; align-items: center; gap: 14px; padding: 15px 0; border-bottom: 1px solid #eff2ed; }
.today-list li:last-child { border-bottom: 0; }
.today-task-link { flex: 1; min-width: 0; border: 0; background: transparent; text-align: left; padding: 0; color: var(--text); }
.today-task-link strong { display: block; font-size: 13px; line-height: 1.7; font-weight: 500; overflow-wrap: anywhere; }
.today-task-link span { display: block; font-size: 10px; color: var(--text-secondary); margin-top: 5px; }
.today-task-link .overdue { color: #ae5141; }
.today-list .text-action { flex-shrink: 0; color: var(--accent); }
.avatar { flex-shrink: 0; overflow: hidden; }
.avatar img { width: 100%; height: 100%; object-fit: cover; display: block; }
.avatar-editor { display: flex; gap: 24px; align-items: center; padding-bottom: 26px; border-bottom: 1px solid var(--border); }
.profile-avatar { width: 88px; height: 88px; font-size: 30px; }
.avatar-editor h2 { margin: 0 0 8px; font-size: 15px; font-weight: 500; }
.avatar-editor p { color: var(--text-muted); font-size: 11px; margin: 0 0 12px; }
.file-input { display: none; }
.profile-form fieldset { border: 0; padding: 26px 0; margin: 0; display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 24px; min-width: 0; }
.profile-field { font-size: 12px; display: flex; flex-direction: column; gap: 10px; min-width: 0; }
.profile-field input, .profile-field textarea { width: 100%; border: 1px solid var(--border); border-radius: 7px; padding: 12px; font: inherit; color: var(--text); background: #fff; }
.profile-field input[readonly] { background: #f6f8f5; color: var(--text-muted); }
.profile-field textarea { resize: vertical; min-height: 85px; }
.profile-field small { font-size: 10px; color: var(--text-muted); }
.full-width { grid-column: 1 / -1; }
.profile-actions { border-top: 1px solid var(--border); padding-top: 22px; display: flex; align-items: center; justify-content: space-between; gap: 16px; flex-wrap: wrap; }
.profile-actions > span { color: var(--text-muted); font-size: 11px; }
.profile-actions > div { display: flex; gap: 10px; }
.profile-actions .button-primary { padding: 10px 18px; }
.profile-error, .profile-success { font-size: 12px; padding: 12px; border-radius: 6px; line-height: 1.7; }
.profile-error { color: #a63a3a; background: #fff5f3; }
.profile-success { color: var(--accent); background: var(--accent-light); }
@media (max-width: 700px) { .profile-form fieldset { grid-template-columns: 1fr; }.avatar-editor { gap: 16px; }.profile-avatar { width: 72px; height: 72px; }.profile-panel { padding: 20px; } }
@media (min-width: 1500px) { .content { padding-top: 48px; }.focus-banner { min-height: 255px; }.summary-card { padding: 24px; } }
@media (max-width: 1100px) { .sidebar { width: 200px; padding-inline: 16px; }.main-shell { margin-left: 200px; }.content { padding: 28px 24px 20px; }.topbar { padding: 0 24px; }.focus-art { width: 170px; }.welcome-stamp { display: none; }.dashboard-grid { grid-template-columns: 1fr; }.companion-intro br { display: none; } }
@media (max-width: 700px) { .workspace { flex-direction: column; }.sidebar { position: static; width: 100%; padding: 18px 18px 10px; border-right: 0; border-bottom: 1px solid var(--border); }.sidebar .brand-lockup { margin: 0 0 17px; }.space-label, .sidebar-quote { display: none; }.navigation { flex-direction: row; gap: 2px; overflow-x: auto; }.navigation button { padding: 9px 10px; font-size: 11px; white-space: nowrap; gap: 5px; }.nav-icon { font-size: 16px; width: 17px; }.navigation .ai-tag { display: none; }.sidebar-bottom { position: absolute; top: 18px; right: 18px; }.account-button { padding: 0; border: 0; }.account-button > span:not(.avatar) { display: none; }.avatar { width: 30px; height: 30px; }.main-shell { margin-left: 0; }.topbar { min-height: 52px; padding: 0 20px; }.workspace-label { display: none; }.content { padding: 26px 18px 18px; }.welcome h1 { font-size: 24px; }.welcome p { font-size: 11px; }.focus-banner { padding: 25px 22px; min-height: 210px; }.focus-banner h2 { font-size: 20px; }.focus-art { display: none; }.summary-grid { gap: 8px; margin: 14px 0; }.summary-card { padding: 15px 12px; }.summary-card p { display: none; }.summary-card > span { font-size: 10px; }.summary-card strong { font-size: 25px; margin-bottom: 0; }.today-panel, .companion-panel { padding-inline: 18px; }.content-footer span:last-child { display: none; }.profile { grid-template-columns: 1fr; }.eyebrow { font-size: 9px; } }
</style>
