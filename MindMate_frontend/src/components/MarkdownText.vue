<script setup>
import { computed } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

const props = defineProps({ content: { type: String, default: '' } })

marked.setOptions({ gfm: true, breaks: true })
const html = computed(() => DOMPurify.sanitize(marked.parse(props.content || ''), {
  USE_PROFILES: { html: true },
  FORBID_TAGS: ['style', 'iframe', 'form', 'input', 'button'],
}))
</script>

<template>
  <div class="markdown" v-html="html" />
</template>

<style scoped>
.markdown { font-size: 13px; line-height: 1.85; color: var(--text); overflow-wrap: anywhere; }
.markdown :deep(> *:first-child) { margin-top: 0; }
.markdown :deep(> *:last-child) { margin-bottom: 0; }
.markdown :deep(p) { margin: 0 0 12px; }
.markdown :deep(h1), .markdown :deep(h2), .markdown :deep(h3), .markdown :deep(h4) {
  margin: 20px 0 10px; line-height: 1.5; font-weight: 600; color: #34473b;
}
.markdown :deep(h1) { font-size: 18px; }
.markdown :deep(h2) { font-size: 16px; }
.markdown :deep(h3) { font-size: 14px; }
.markdown :deep(h4) { font-size: 13px; }
.markdown :deep(ul), .markdown :deep(ol) { margin: 0 0 12px; padding-left: 22px; }
.markdown :deep(li) { margin: 4px 0; }
.markdown :deep(li > p) { margin: 0 0 6px; }
.markdown :deep(strong) { font-weight: 600; color: #2f4336; }
.markdown :deep(em) { font-style: italic; color: #4c6256; }
.markdown :deep(a) { color: var(--accent); text-decoration: underline; text-underline-offset: 3px; }
.markdown :deep(code) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 12px; background: #f1f5f0; border-radius: 4px; padding: 2px 5px; color: #3f5648;
}
.markdown :deep(pre) {
  margin: 0 0 14px; padding: 14px 16px; background: #f7f9f6; border: 1px solid #e6ece4;
  border-radius: 8px; overflow-x: auto;
}
.markdown :deep(pre code) { background: transparent; padding: 0; font-size: 12px; line-height: 1.7; }
.markdown :deep(blockquote) {
  margin: 0 0 14px; padding: 8px 14px; border-left: 3px solid #cfe0d0;
  background: #f7faf7; color: #50685a; border-radius: 0 6px 6px 0;
}
.markdown :deep(blockquote p:last-child) { margin-bottom: 0; }
.markdown :deep(table) {
  width: 100%; border-collapse: collapse; margin: 0 0 14px; font-size: 12px;
  display: block; overflow-x: auto;
}
.markdown :deep(th), .markdown :deep(td) { border: 1px solid #e3ebe1; padding: 8px 10px; text-align: left; vertical-align: top; }
.markdown :deep(th) { background: #f4f8f3; font-weight: 600; white-space: nowrap; }
.markdown :deep(hr) { border: 0; border-top: 1px solid var(--border); margin: 18px 0; }
.markdown :deep(img) { max-width: 100%; height: auto; border-radius: 6px; }
</style>
