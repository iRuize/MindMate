<script setup>
defineProps({loggingOut: {type: Boolean, default: false}})
defineEmits(['logout'])
</script>

<template>
  <div class="page-actions">
    <span class="focus-state"><i aria-hidden="true"></i>专注当下</span>
    <button :disabled="loggingOut" class="logout-button" type="button" @click="$emit('logout')">
      {{ loggingOut ? '退出中…' : '退出登录' }}
    </button>
  </div>
</template>

<style scoped>
.page-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
  font-size: 11px;
  color: var(--text-secondary);
}

.focus-state {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
}

.focus-state i {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #769a7f;
}

.logout-button {
  border: 0;
  background: transparent;
  color: var(--text-secondary);
  font-size: 11px;
  padding: 0;
  white-space: nowrap;
}

.logout-button:hover:not(:disabled) {
  color: var(--accent);
}

@media (max-width: 700px) {
  .focus-state {
    display: none;
  }
}
</style>
