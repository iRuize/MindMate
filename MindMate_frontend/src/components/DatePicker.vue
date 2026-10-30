<script>
let closeActivePicker = null
</script>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, useId, watch } from 'vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  id: { type: String, default: '' },
  label: { type: String, default: '日期' },
  ariaDescribedby: { type: String, default: undefined },
  disabled: Boolean,
})
const emit = defineEmits(['update:modelValue'])
const instanceId = useId()
const pickerId = computed(() => props.id || `date-picker-${instanceId}`)
const trigger = ref(null)
const popover = ref(null)
const opened = ref(false)
const popoverStyle = ref({ visibility: 'hidden' })
const today = ref(new Date())
const viewYear = ref(today.value.getFullYear())
const viewMonth = ref(today.value.getMonth() + 1)
const weekdays = ['一', '二', '三', '四', '五', '六', '日']
const pad = (value) => String(value).padStart(2, '0')
const formatDate = (year, month, day) => `${String(year).padStart(4, '0')}-${pad(month)}-${pad(day)}`
const localDate = (year, month, day) => {
  const date = new Date()
  date.setHours(0, 0, 0, 0)
  date.setFullYear(year, month - 1, day)
  return date
}
const selected = computed(() => {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(props.modelValue)) return null
  const [year, month, day] = props.modelValue.split('-').map(Number)
  const date = localDate(year, month, day)
  return year >= 1 && year <= 9999 && date.getFullYear() === year && date.getMonth() + 1 === month && date.getDate() === day
    ? { year, month, day }
    : null
})
const displayValue = computed(() => selected.value
  ? formatDate(selected.value.year, selected.value.month, selected.value.day).replaceAll('-', '/')
  : '请选择日期')
const todayValue = computed(() => formatDate(today.value.getFullYear(), today.value.getMonth() + 1, today.value.getDate()))
const monthTitle = computed(() => `${viewYear.value}年${viewMonth.value}月`)
const atFirstMonth = computed(() => viewYear.value === 1 && viewMonth.value === 1)
const atLastMonth = computed(() => viewYear.value === 9999 && viewMonth.value === 12)
const days = computed(() => {
  const offset = (localDate(viewYear.value, viewMonth.value, 1).getDay() + 6) % 7
  const count = localDate(viewYear.value, viewMonth.value + 1, 0).getDate()
  return Array.from({ length: 42 }, (_, index) => {
    const day = index - offset + 1
    return day >= 1 && day <= count
      ? { day, value: formatDate(viewYear.value, viewMonth.value, day) }
      : null
  })
})
const isDisabled = () => props.disabled || trigger.value?.matches(':disabled')
const close = (restoreFocus = false) => {
  opened.value = false
  if (closeActivePicker === close) closeActivePicker = null
  if (restoreFocus && !isDisabled()) trigger.value?.focus({ preventScroll: true })
}
const positionPopover = () => {
  if (!trigger.value || !popover.value) return
  const viewport = window.visualViewport
  const leftEdge = (viewport?.offsetLeft || 0) + 8
  const topEdge = (viewport?.offsetTop || 0) + 8
  const viewportWidth = viewport?.width || document.documentElement.clientWidth
  const viewportHeight = viewport?.height || window.innerHeight
  const rightEdge = leftEdge + viewportWidth - 16
  const bottomEdge = topEdge + viewportHeight - 16
  const rect = trigger.value.getBoundingClientRect()
  if (rect.bottom < topEdge || rect.top > bottomEdge) {
    close()
    return
  }
  const width = Math.min(304, Math.max(0, viewportWidth - 16))
  const height = popover.value.scrollHeight + 2
  const below = Math.max(0, bottomEdge - rect.bottom - 8)
  const above = Math.max(0, rect.top - topEdge - 8)
  const openAbove = below < height && above > below
  const availableHeight = openAbove ? above : below
  const maxHeight = availableHeight < 120 ? Math.max(0, viewportHeight - 16) : availableHeight
  const visibleHeight = Math.min(height, maxHeight)
  const top = openAbove ? rect.top - visibleHeight - 8 : rect.bottom + 8
  popoverStyle.value = {
    width: `${width}px`,
    left: `${Math.max(leftEdge, Math.min(rect.left, rightEdge - width))}px`,
    top: `${Math.max(topEdge, Math.min(top, bottomEdge - visibleHeight))}px`,
    maxHeight: `${maxHeight}px`,
    visibility: 'visible',
  }
}
const toggle = async () => {
  if (isDisabled()) return
  if (opened.value) {
    close()
    return
  }
  closeActivePicker?.()
  closeActivePicker = close
  today.value = new Date()
  viewYear.value = selected.value?.year ?? today.value.getFullYear()
  viewMonth.value = selected.value?.month ?? today.value.getMonth() + 1
  const viewportWidth = window.visualViewport?.width || document.documentElement.clientWidth
  popoverStyle.value = { width: `${Math.min(304, Math.max(0, viewportWidth - 16))}px`, visibility: 'hidden' }
  opened.value = true
  await nextTick()
  if (!opened.value) return
  if (isDisabled()) {
    close()
    return
  }
  positionPopover()
  await nextTick()
  if (!opened.value) return
  const initialValue = selected.value ? props.modelValue : todayValue.value
  const initialButton = popover.value?.querySelector(`[data-date="${initialValue}"]`) || popover.value?.querySelector('.date-picker__day')
  initialButton?.focus({ preventScroll: true })
}
const changeMonth = (direction) => {
  if (isDisabled() || (direction < 0 && atFirstMonth.value) || (direction > 0 && atLastMonth.value)) return
  const monthIndex = (viewYear.value - 1) * 12 + viewMonth.value - 1 + direction
  viewYear.value = Math.floor(monthIndex / 12) + 1
  viewMonth.value = monthIndex % 12 + 1
}
const choose = (value) => {
  if (isDisabled()) {
    close()
    return
  }
  emit('update:modelValue', value)
  close(true)
}
const isInside = (target) => target instanceof Node && (trigger.value?.contains(target) || popover.value?.contains(target))
const handleOutside = (event) => {
  if (opened.value && !isInside(event.target)) close()
}
const handleKeydown = (event) => {
  if (!opened.value || event.key !== 'Escape') return
  event.preventDefault()
  event.stopPropagation()
  close(true)
}
const handleScroll = (event) => {
  if (opened.value && !(event.target instanceof Node && popover.value?.contains(event.target))) close()
}
const handleResize = () => close()
watch(() => props.disabled, (value) => {
  if (value) close()
})
onMounted(() => {
  document.addEventListener('pointerdown', handleOutside, true)
  document.addEventListener('focusin', handleOutside)
  document.addEventListener('keydown', handleKeydown, true)
  window.addEventListener('scroll', handleScroll, true)
  window.addEventListener('resize', handleResize)
  window.visualViewport?.addEventListener('scroll', handleResize)
  window.visualViewport?.addEventListener('resize', handleResize)
})
onBeforeUnmount(() => {
  close()
  document.removeEventListener('pointerdown', handleOutside, true)
  document.removeEventListener('focusin', handleOutside)
  document.removeEventListener('keydown', handleKeydown, true)
  window.removeEventListener('scroll', handleScroll, true)
  window.removeEventListener('resize', handleResize)
  window.visualViewport?.removeEventListener('scroll', handleResize)
  window.visualViewport?.removeEventListener('resize', handleResize)
})
</script>

<template>
  <div class="date-picker">
    <button
      :id="pickerId"
      ref="trigger"
      type="button"
      class="date-picker__trigger"
      :class="{ 'is-empty': !selected, 'is-open': opened }"
      :disabled="disabled"
      :aria-label="`${label}，${displayValue}`"
      :aria-describedby="ariaDescribedby"
      aria-haspopup="dialog"
      :aria-expanded="opened"
      :aria-controls="opened ? `${pickerId}-popover` : undefined"
      @click="toggle"
    >
      <span>{{ displayValue }}</span>
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" aria-hidden="true" focusable="false"><rect x="3" y="5" width="18" height="16" rx="3" /><path d="M7 3v4m10-4v4M3 11h18M7 15h2m3 0h2" /></svg>
    </button>
    <Teleport to="body">
      <div
        v-if="opened"
        :id="`${pickerId}-popover`"
        ref="popover"
        class="date-picker__popover"
        :style="popoverStyle"
        role="dialog"
        :aria-label="`${label}选择日历`"
      >
        <div class="date-picker__header">
          <button type="button" class="date-picker__nav" aria-label="上个月" :disabled="disabled || atFirstMonth" @click="changeMonth(-1)"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="m14 6-6 6 6 6" /></svg></button>
          <h3 aria-live="polite" aria-atomic="true">{{ monthTitle }}</h3>
          <button type="button" class="date-picker__nav" aria-label="下个月" :disabled="disabled || atLastMonth" @click="changeMonth(1)"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="m10 6 6 6-6 6" /></svg></button>
        </div>
        <div class="date-picker__weekdays"><span v-for="weekday in weekdays" :key="weekday" :aria-label="`星期${weekday}`">{{ weekday }}</span></div>
        <div class="date-picker__days">
          <template v-for="(item, index) in days" :key="index">
            <button
              v-if="item"
              type="button"
              class="date-picker__day"
              :class="{ 'is-today': item.value === todayValue, 'is-selected': item.value === modelValue }"
              :data-date="item.value"
              :aria-label="`${viewYear}年${viewMonth}月${item.day}日${item.value === todayValue ? '，今天' : ''}`"
              :aria-pressed="item.value === modelValue"
              :aria-current="item.value === todayValue ? 'date' : undefined"
              :disabled="disabled"
              @click="choose(item.value)"
            >{{ item.day }}<span v-if="item.value === todayValue" class="date-picker__today-dot" aria-hidden="true" /></button>
            <span v-else aria-hidden="true" />
          </template>
        </div>
        <div class="date-picker__footer"><button type="button" :disabled="disabled" @click="choose(todayValue)">今天</button><button type="button" :disabled="disabled" @click="choose('')">清空</button></div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.date-picker { width: 100%; min-width: 0; }
.date-picker__trigger { display: flex; align-items: center; justify-content: space-between; gap: 10px; width: 100%; min-width: 0; min-height: 44px; padding: 11px 12px; border: 1px solid #dbe5da; border-radius: 7px; background: #fff; color: #273c30; font: inherit; text-align: left; }
.date-picker__trigger span { overflow-wrap: anywhere; }
.date-picker__trigger svg { flex-shrink: 0; color: #6e9276; }
.date-picker__trigger.is-empty { color: #929d94; }
.date-picker__trigger:not(:disabled):hover, .date-picker__trigger.is-open { border-color: #9abca1; background: #f6faf4; }
.date-picker__trigger:disabled { cursor: not-allowed; opacity: .55; }
.date-picker__trigger:focus-visible, .date-picker__popover button:focus-visible { outline: 2px solid #8caf95; outline-offset: 2px; }
.date-picker__popover { position: fixed; z-index: 1000; box-sizing: border-box; padding: 14px; overflow-y: auto; overscroll-behavior: contain; border: 1px solid #dbe7d8; border-radius: 14px; background: #fcfefb; color: #273c30; box-shadow: 0 12px 36px #284a3226, 0 2px 8px #284a320d; font-family: inherit; font-size: 12px; color-scheme: light; }
.date-picker__popover button { box-sizing: border-box; min-width: 0; border: 0; font: inherit; cursor: pointer; }
.date-picker__popover button:disabled { cursor: not-allowed; opacity: .55; }
.date-picker__header { display: flex; align-items: center; justify-content: space-between; gap: 8px; margin-bottom: 12px; }
.date-picker__header h3 { margin: 0; font-size: 14px; font-weight: 600; }
.date-picker__nav { display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0; width: 30px; height: 30px; padding: 0; border-radius: 8px; background: #edf4ec; color: #426a50; }
.date-picker__nav:not(:disabled):hover { background: #dcebd9; }
.date-picker__weekdays, .date-picker__days { display: grid; grid-template-columns: repeat(7, minmax(0, 1fr)); gap: 4px; }
.date-picker__weekdays { margin-bottom: 6px; color: #829284; text-align: center; font-size: 11px; line-height: 24px; }
.date-picker__days > span { width: 100%; aspect-ratio: 1; }
.date-picker__day { position: relative; width: 100%; aspect-ratio: 1; padding: 0; border-radius: 9px; background: transparent; color: #3c5643; }
.date-picker__day:not(:disabled):hover { background: #e5f0e1; color: #315a3d; }
.date-picker__day.is-today { box-shadow: inset 0 0 0 1px #a9c4a6; font-weight: 600; }
.date-picker__day.is-selected { background: #426a50; color: #fff; font-weight: 600; }
.date-picker__day.is-selected:not(:disabled):hover { background: #34553f; color: #fff; }
.date-picker__today-dot { position: absolute; bottom: 3px; left: calc(50% - 2px); width: 4px; height: 4px; border-radius: 50%; background: currentColor; }
.date-picker__footer { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 4px; margin-top: 10px; padding-top: 10px; border-top: 1px solid #e6eee2; }
.date-picker__footer button { padding: 7px 9px; border-radius: 6px; background: #edf4ec; color: #426a50; }
.date-picker__footer button:not(:disabled):hover { background: #dcebd9; }
@media (max-width: 360px) {
  .date-picker__popover { padding: 10px; }
}
</style>
