<script setup>
import { computed, nextTick, ref } from 'vue'

const props = defineProps({
  tasks: { type: Array, required: true },
  loading: Boolean,
  loaded: Boolean,
  busy: { type: String, default: '' },
  saveTask: { type: Function, required: true },
  changeStatus: { type: Function, required: true },
  removeTask: { type: Function, required: true },
  now: { type: Date, required: true },
})
const emit = defineEmits(['refresh'])
const filter = ref('all')
const search = ref('')
const panelOpen = ref(false)
const editingId = ref(null)
const titleInput = ref(null)
const formError = ref('')
const submitting = ref(false)
const deleteId = ref(null)
const priorities = ['低', '中', '高', '紧急']
const blankForm = () => ({ title: '', description: '', priority: 1, plannedDate: '', dueAt: '', estimatedMinutes: '' })
const form = ref(blankForm())
const disabled = computed(() => props.loading || Boolean(props.busy) || submitting.value)
const counts = computed(() => ({
  all: props.tasks.length,
  pending: props.tasks.filter((task) => task.status !== 2).length,
  completed: props.tasks.filter((task) => task.status === 2).length,
}))
const filters = computed(() => [
  { value: 'all', label: '全部', count: counts.value.all },
  { value: 'pending', label: '待办', count: counts.value.pending },
  { value: 'completed', label: '已完成', count: counts.value.completed },
])
const visibleTasks = computed(() => props.tasks.filter((task) => {
  const matchesStatus = filter.value === 'all' || (filter.value === 'completed' ? task.status === 2 : task.status !== 2)
  return matchesStatus && task.title.toLocaleLowerCase().includes(search.value.trim().toLocaleLowerCase())
}))
const isOverdue = (task) => task.status !== 2 && task.dueAt && new Date(task.dueAt).getTime() < props.now.getTime()
const displayTime = (value) => value ? value.replace('T', ' ').slice(0, 16) : ''
const openForm = async (task = null) => {
  if (disabled.value) return
  editingId.value = task?.id ?? null
  form.value = task ? {
    title: task.title,
    description: task.description || '',
    priority: task.priority,
    plannedDate: task.plannedDate || '',
    dueAt: task.dueAt || '',
    estimatedMinutes: task.estimatedMinutes ?? '',
  } : blankForm()
  formError.value = ''
  deleteId.value = null
  panelOpen.value = true
  await nextTick()
  titleInput.value?.focus()
}
const closeForm = () => {
  if (disabled.value) return
  panelOpen.value = false
  formError.value = ''
}
const submit = async () => {
  if (disabled.value) return
  formError.value = ''
  const title = form.value.title.trim()
  const minutes = form.value.estimatedMinutes === '' ? null : Number(form.value.estimatedMinutes)
  if (!title) {
    formError.value = '请填写任务标题，不能只输入空格。'
    titleInput.value?.focus()
    return
  }
  if (minutes !== null && (!Number.isInteger(minutes) || minutes < 1 || minutes > 525600)) {
    formError.value = '预计耗时请填写 1 至 525600 之间的整数分钟，或留空。'
    return
  }
  const data = {
    title,
    description: form.value.description.trim(),
    priority: Number(form.value.priority),
    plannedDate: form.value.plannedDate || null,
    // 保留本地时间，不通过 UTC 转换改变用户选择的时刻。
    dueAt: form.value.dueAt ? (form.value.dueAt.length === 16 ? `${form.value.dueAt}:00` : form.value.dueAt.slice(0, 19)) : null,
    estimatedMinutes: minutes,
  }
  submitting.value = true
  try {
    if (await props.saveTask(editingId.value, data)) panelOpen.value = false
  } finally { submitting.value = false }
}
const showTask = (task) => {
  filter.value = 'pending'
  search.value = task.title
  deleteId.value = null
}
defineExpose({ showTask })
const confirmDelete = async (task) => {
  if (disabled.value) return
  if (await props.removeTask(task.id)) {
    deleteId.value = null
    if (editingId.value === task.id) panelOpen.value = false
  }
}
</script>

<template>
  <section class="task-view" aria-labelledby="task-list-title">
    <span class="eyebrow">ONE STEP AT A TIME</span>
    <div class="page-heading"><div><h1 id="task-list-title">任务清单</h1><p>把想做的事，整理成可以迈出的下一步。</p></div><button class="button-primary" :disabled="disabled" @click="openForm()">＋ 新增任务</button></div>

    <form v-if="panelOpen" class="task-form" :aria-busy="submitting" @submit.prevent="submit">
      <h2>{{ editingId === null ? '新增任务' : '编辑任务' }}</h2>
      <fieldset :disabled="disabled">
        <label class="field full">任务标题 <span class="required">必填</span><input ref="titleInput" v-model="form.title" maxlength="200" required placeholder="下一步，想完成什么？" /></label>
        <label class="field full">任务描述<textarea v-model="form.description" maxlength="5000" rows="3" placeholder="补充说明（选填）" /></label>
        <label class="field">优先级<select v-model.number="form.priority"><option v-for="(label, value) in priorities" :key="value" :value="value">{{ label }}</option></select></label>
        <label class="field">计划日期<input v-model="form.plannedDate" type="date" min="0001-01-01" max="9999-12-31" /></label>
        <label class="field">截止时间<input v-model="form.dueAt" type="datetime-local" step="1" min="0001-01-01T00:00:00" max="9999-12-31T23:59:59" /><small>按本地时间保存</small></label>
        <label class="field">预计耗时（分钟）<input v-model="form.estimatedMinutes" type="number" min="1" max="525600" step="1" placeholder="选填" /></label>
      </fieldset>
      <p v-if="formError" class="form-error" role="alert">{{ formError }}</p>
      <div class="form-actions"><button type="button" class="button-secondary" :disabled="disabled" @click="closeForm">取消</button><button type="submit" class="button-primary" :disabled="disabled">{{ submitting ? '保存中…' : '保存任务' }}</button></div>
    </form>

    <div class="task-panel" :aria-busy="loading">
      <div class="toolbar">
        <div class="filters" role="group" aria-label="任务状态筛选"><button v-for="item in filters" :key="item.value" :class="{ selected: filter === item.value }" :aria-pressed="filter === item.value" @click="filter = item.value; deleteId = null">{{ item.label }} <span>{{ loaded ? item.count : '—' }}</span></button></div>
        <div class="search-actions"><input v-model="search" type="search" aria-label="搜索任务标题" placeholder="搜索任务标题" /><button class="refresh-button" :disabled="disabled" @click="emit('refresh')">{{ loading ? '刷新中…' : '刷新' }}</button></div>
      </div>
      <p v-if="loading" class="loading-message" role="status">{{ loaded ? '正在刷新任务…' : '正在加载任务…' }}</p>
      <ul v-if="visibleTasks.length" class="task-list">
        <li v-for="task in visibleTasks" :key="task.id" class="task-row" :class="{ completed: task.status === 2 }">
          <div class="task-main">
            <button class="status-toggle" :class="{ checked: task.status === 2 }" :disabled="disabled" :aria-label="`${task.status === 2 ? '重开' : '完成'}任务：${task.title}`" :aria-pressed="task.status === 2" @click="changeStatus(task)">{{ busy === `status:${task.id}` ? '…' : task.status === 2 ? '✓' : '' }}</button>
            <div class="task-detail"><h2>{{ task.title }}</h2><p v-if="task.description" class="description">{{ task.description }}</p><div class="task-meta"><span class="priority" :class="`priority-${task.priority}`">{{ priorities[task.priority] }}优先级</span><span>{{ task.status === 2 ? '已完成' : '待办' }}</span><span v-if="task.plannedDate">计划 {{ task.plannedDate }}</span><span v-if="task.dueAt" :class="{ overdue: isOverdue(task) }">{{ isOverdue(task) ? '已逾期 · ' : '截止 ' }}{{ displayTime(task.dueAt) }}</span><span v-if="task.estimatedMinutes != null">预计 {{ task.estimatedMinutes }} 分钟</span><span v-if="task.status === 2 && task.completedAt">完成于 {{ displayTime(task.completedAt) }}</span></div></div>
          </div>
          <div class="row-actions"><button :disabled="disabled" @click="openForm(task)">编辑</button><button class="delete-action" :disabled="disabled" @click="deleteId = task.id">删除</button></div>
          <div v-if="deleteId === task.id" class="delete-confirm" role="group" :aria-label="`确认删除任务：${task.title}`"><span>确定删除这项任务？删除后无法恢复。</span><div><button :disabled="disabled" @click="deleteId = null">取消</button><button class="delete-action" :disabled="disabled" @click="confirmDelete(task)">{{ busy === `delete:${task.id}` ? '删除中…' : '确认删除' }}</button></div></div>
        </li>
      </ul>
      <div v-else-if="loaded && !loading" class="empty-state"><h2>{{ tasks.length ? '没有符合条件的任务' : '从一件小事开始' }}</h2><p>{{ tasks.length ? '试试其他筛选条件，或换一个标题关键词。' : '记下想做的事，给今天一个清晰的方向。' }}</p><button v-if="tasks.length" class="button-secondary" @click="filter = 'all'; search = ''">清除筛选</button><button v-else class="button-secondary" :disabled="disabled" @click="openForm()">新增第一项任务</button></div>
      <p v-else-if="!loading && !loaded" class="loading-message">任务尚未加载成功，请点击上方重试。</p>
      <div v-if="loaded" class="list-footer">显示 {{ visibleTasks.length }} 项 / 共 {{ tasks.length }} 项任务</div>
    </div>
  </section>
</template>

<style scoped>
.page-heading { display: flex; align-items: center; justify-content: space-between; gap: 20px; margin-bottom: 26px; }
h1 { font-size: 28px; font-weight: 600; letter-spacing: -1px; margin: 12px 0; }
.page-heading p { color: var(--text-secondary); font-size: 12px; line-height: 1.8; margin: 0; }
.button-primary { padding: 11px 18px; white-space: nowrap; }
.task-panel, .task-form { background: #fff; border: 1px solid var(--border); border-radius: 10px; }
.task-form { padding: 24px; margin-bottom: 22px; }
.task-form h2 { font-size: 16px; font-weight: 600; margin: 0 0 20px; }
.task-form fieldset { border: 0; padding: 0; margin: 0; min-width: 0; display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 18px; }
.field { display: flex; flex-direction: column; gap: 9px; font-size: 12px; min-width: 0; }
.field.full { grid-column: 1 / -1; }
.required, .field small { font-size: 10px; color: var(--text-muted); }
.field input, .field textarea, .field select, .search-actions input { width: 100%; min-width: 0; padding: 11px 12px; border: 1px solid var(--border); border-radius: 7px; background: #fff; color: var(--text); font: inherit; }
.field textarea { resize: vertical; min-height: 80px; }
.field textarea:focus-visible, .field select:focus-visible { outline: 2px solid #a7c5b0; outline-offset: 3px; }
.form-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 22px; }
.form-error { color: #a63a3a; background: #fff5f3; border-radius: 6px; padding: 12px; font-size: 12px; }
.toolbar { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 16px; padding: 18px 20px; border-bottom: 1px solid var(--border); }
.filters, .search-actions { display: flex; gap: 5px; align-items: center; }
.filters button { border: 0; background: transparent; color: var(--text-secondary); font-size: 12px; border-radius: 6px; padding: 9px 10px; white-space: nowrap; }
.filters button.selected { color: var(--accent); background: var(--accent-light); font-weight: 600; }
.filters span { margin-left: 4px; font-size: 10px; }
.search-actions { gap: 12px; }
.search-actions input { width: 190px; font-size: 12px; }
.refresh-button { border: 0; background: transparent; color: var(--accent); font-size: 12px; white-space: nowrap; }
.loading-message { padding: 15px 22px; color: var(--text-secondary); font-size: 12px; line-height: 1.8; margin: 0; }
.task-list { padding: 0; margin: 0; list-style: none; }
.task-row { display: flex; align-items: flex-start; flex-wrap: wrap; gap: 14px; padding: 22px; border-bottom: 1px solid #eff2ed; }
.task-main { display: flex; align-items: flex-start; gap: 13px; flex: 1; min-width: 0; }
.status-toggle { width: 23px; height: 23px; flex-shrink: 0; margin-top: 1px; border: 1px solid #c5d4c6; border-radius: 50%; background: #fff; color: var(--accent); line-height: 1; padding: 0; }
.status-toggle.checked { background: var(--accent-light); border-color: #a4bda8; }
.task-detail { min-width: 0; }
.task-detail h2 { font-size: 14px; font-weight: 500; line-height: 1.7; margin: 0; overflow-wrap: anywhere; }
.completed .task-detail h2 { text-decoration: line-through; color: var(--text-muted); }
.description { color: var(--text-secondary); font-size: 12px; line-height: 1.8; white-space: pre-wrap; overflow-wrap: anywhere; margin: 7px 0; }
.task-meta { display: flex; flex-wrap: wrap; gap: 7px 12px; align-items: center; margin-top: 9px; font-size: 10px; color: var(--text-secondary); }
.priority { border-radius: 4px; padding: 3px 6px; background: #f1f4f0; color: #70836c; }
.priority-1 { background: #edf4ec; color: #52765e; }
.priority-2 { background: #fbf4e7; color: #976d2d; }
.priority-3 { background: #fff0ed; color: #ae5141; }
.overdue { color: #ae5141; }
.row-actions { display: flex; gap: 14px; }
.row-actions button, .delete-confirm button { border: 0; background: transparent; color: var(--accent); font-size: 11px; padding: 5px 0; }
.row-actions .delete-action, .delete-confirm .delete-action { color: #a65347; }
.delete-confirm { flex-basis: 100%; display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 10px; background: #fff5f3; color: #995045; padding: 10px 12px; border-radius: 6px; font-size: 12px; line-height: 1.7; }
.delete-confirm > div { display: flex; gap: 16px; }
.empty-state { padding: 48px 20px; text-align: center; }
.empty-state h2 { font-size: 16px; font-weight: 500; margin: 0 0 12px; }
.empty-state p { font-size: 12px; color: var(--text-secondary); line-height: 1.8; margin-bottom: 20px; }
.list-footer { padding: 14px 22px; font-size: 10px; color: var(--text-muted); }
@media (max-width: 700px) {
  h1 { font-size: 24px; }
  .page-heading { align-items: flex-start; flex-wrap: wrap; gap: 14px; }
  .task-form { padding: 18px; }
  .task-form fieldset { grid-template-columns: 1fr; }
  .toolbar { padding: 14px; }
  .search-actions { width: 100%; }
  .search-actions input { flex: 1; width: 0; }
  .task-row { padding: 18px 14px; }
  .row-actions { margin-left: 36px; }
  .task-main { flex-basis: 100%; }
}
</style>
