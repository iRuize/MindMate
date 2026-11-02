<script setup>
import {computed, onBeforeUnmount, reactive, ref, watch} from 'vue'
import {createQuote, deleteQuote, fetchQuotes, mediaAddress} from '@/api/quotes'

const props = defineProps({active: {type: Boolean, default: false}})
const quotes = ref([])
const loading = ref(false)
const loaded = ref(false)
const loadError = ref('')
const form = reactive({content: '', source: '', file: null})
const previewUrl = ref('')
const formError = ref('')
const notice = ref('')
const submitting = ref(false)
const progress = ref(0)
const deletingId = ref(null)
const fileInput = ref(null)

const mediaKind = (file) => !file ? '' : file.type.startsWith('video/') ? 'video' : 'image'
const previewKind = ref('')
const canSubmit = computed(() => (form.content.trim() || form.file) && !submitting.value)
const hasDraft = computed(() => Boolean(form.content.trim() || form.source.trim() || form.file))

const releasePreview = () => {
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = ''
  previewKind.value = ''
}

const loadQuotes = async () => {
  if (loading.value) return
  loading.value = true
  loadError.value = ''
  try {
    quotes.value = await fetchQuotes()
    loaded.value = true
  } catch (error) {
    loadError.value = `语录加载失败：${error.message || '请稍后重试'}`
  } finally {
    loading.value = false
  }
}

const pickFile = (event) => {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  formError.value = ''
  const kind = mediaKind(file)
  const limit = kind === 'video' ? 50 * 1024 * 1024 : 5 * 1024 * 1024
  if (!['image/jpeg', 'image/png', 'image/webp', 'video/mp4', 'video/webm'].includes(file.type)) {
    formError.value = '仅支持 JPG、PNG、WebP 图片，或 MP4、WebM 视频'
    return
  }
  if (file.size > limit) {
    formError.value = kind === 'video' ? '视频请控制在 50MB 以内' : '图片请控制在 5MB 以内'
    return
  }
  releasePreview()
  form.file = file
  previewKind.value = kind
  previewUrl.value = URL.createObjectURL(file)
}

const clearFile = () => {
  releasePreview()
  form.file = null
}

const resetForm = () => {
  form.content = ''
  form.source = ''
  form.file = null
  releasePreview()
  formError.value = ''
  progress.value = 0
}

const submit = async () => {
  if (!canSubmit.value) {
    formError.value = '写一句台词，或者上传一张图片、一段视频'
    return
  }
  submitting.value = true
  formError.value = ''
  notice.value = ''
  progress.value = 0
  try {
    const saved = await createQuote({
      content: form.content.trim(),
      source: form.source.trim(),
      file: form.file,
    }, (event) => {
      if (event.total) progress.value = Math.round(event.loaded / event.total * 100)
    })
    quotes.value.unshift(saved)
    loaded.value = true
    resetForm()
    notice.value = '已收进你的语录'
  } catch (error) {
    formError.value = `保存失败：${error.message || '请稍后重试'}`
  } finally {
    submitting.value = false
  }
}

const remove = async (quote) => {
  if (deletingId.value) return
  deletingId.value = quote.id
  loadError.value = ''
  try {
    await deleteQuote(quote.id)
    quotes.value = quotes.value.filter((item) => item.id !== quote.id)
    notice.value = '已删除'
  } catch (error) {
    loadError.value = `删除失败：${error.message || '请稍后重试'}`
  } finally {
    deletingId.value = null
  }
}

watch(() => props.active, (active) => {
  if (active && !loaded.value && !loading.value) loadQuotes()
}, {immediate: true})

onBeforeUnmount(releasePreview)
</script>

<template>
  <section class="quote-wall">
    <header class="quote-heading">
      <div><span class="eyebrow">LINES THAT STAY WITH YOU</span>
        <h1>经典语录</h1>
        <p>把打动过你的台词留下来。可以是一句话，也可以是一张截图、一段片段。</p></div>
      <slot name="actions"/>
    </header>

    <div v-if="loadError" class="quote-feedback error" role="alert"><span>{{ loadError }}</span>
      <button :disabled="loading" type="button" @click="loadQuotes">{{ loading ? '重试中…' : '重新加载' }}</button>
    </div>
    <div v-if="notice" class="quote-feedback" role="status"><span>{{ notice }}</span>
      <button aria-label="关闭提示" type="button" @click="notice = ''">×</button>
    </div>

    <section class="quote-composer">
      <label class="composer-field quote-text">
        <span>台词或感受</span>
        <textarea v-model="form.content" :disabled="submitting" maxlength="2000" placeholder="写下让你记住的那句话…"
                  rows="3"/>
      </label>
      <div class="composer-row">
        <label class="composer-field source-field"><span>出自（选填）</span>
          <input v-model="form.source" :disabled="submitting" maxlength="200" placeholder="剧名、角色或集数"/>
        </label>
        <div class="upload-area">
          <input ref="fileInput" accept="image/jpeg,image/png,image/webp,video/mp4,video/webm" class="visually-hidden"
                 type="file" @change="pickFile"/>
          <button :disabled="submitting" class="ghost-button" type="button" @click="fileInput?.click()">
            {{ form.file ? '更换附件' : '图片 / 视频' }}
          </button>
          <button v-if="form.file" :disabled="submitting" class="ghost-button subtle" type="button" @click="clearFile">移除</button>
        </div>
      </div>

      <div v-if="previewUrl" class="upload-preview">
        <video v-if="previewKind === 'video'" :src="previewUrl" controls preload="metadata"/>
        <img v-else :src="previewUrl" alt="待上传的图片预览"/>
        <span>{{ form.file.name }}</span>
      </div>

      <p v-if="formError" class="composer-error" role="alert">{{ formError }}</p>
      <div class="composer-actions">
        <span class="composer-hint">图片不超过 5MB，视频不超过 50MB</span>
        <button :disabled="!canSubmit" class="button-primary" type="button" @click="submit">
          {{ submitting ? (progress ? `上传中 ${progress}%` : '保存中…') : '收进语录' }}
        </button>
      </div>
    </section>

    <p v-if="loading && !loaded" class="quote-status" role="status">正在读取你的语录…</p>
    <div v-else-if="loaded && !quotes.length" class="quote-empty">
      <span aria-hidden="true">❝</span>
      <h2>还没有留下的句子</h2>
      <p>下次看到让你停下来的台词，把它放在这里。</p>
    </div>
    <div v-else class="quote-grid">
      <article v-for="quote in quotes" :key="quote.id" class="quote-card">
        <div v-if="quote.mediaType === 1" class="card-media">
          <img :alt="quote.content || '语录图片'" :src="mediaAddress(quote.mediaUrl)" loading="lazy"/>
        </div>
        <div v-else-if="quote.mediaType === 2" class="card-media">
          <video :src="mediaAddress(quote.mediaUrl)" controls preload="metadata"/>
        </div>
        <blockquote v-if="quote.content">{{ quote.content }}</blockquote>
        <footer>
          <span class="quote-source">{{ quote.source || '未注明出处' }}</span>
          <button :disabled="deletingId === quote.id" aria-label="删除这条语录" class="card-delete" type="button"
                  @click="remove(quote)">{{ deletingId === quote.id ? '删除中…' : '删除' }}
          </button>
        </footer>
      </article>
    </div>
    <footer v-if="loaded && quotes.length" class="quote-footer">共 {{ quotes.length }} 条 · 只对你可见</footer>
  </section>
</template>

<style scoped>
.quote-wall {
  padding-bottom: 8px;
}

.quote-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 18px;
}

.quote-heading h1 {
  margin: 8px 0;
  font-size: 26px;
  font-weight: 600;
  letter-spacing: -1px;
}

.quote-heading p {
  margin: 0;
  font-size: 12px;
  color: var(--text-secondary);
  line-height: 1.8;
}

.quote-feedback {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 11px 14px;
  margin-bottom: 12px;
  border: 1px solid #d7e5d9;
  border-radius: 8px;
  background: #eff7f0;
  color: #41624a;
  font-size: 12px;
}

.quote-feedback.error {
  background: #fff5f3;
  border-color: #f1ddd7;
  color: #a63a3a;
}

.quote-feedback button {
  border: 0;
  background: transparent;
  color: inherit;
  font-size: 12px;
  flex-shrink: 0;
}

.quote-composer {
  border: 1px solid var(--border);
  border-radius: 12px;
  background: #fff;
  padding: 16px;
  margin-bottom: 20px;
}

.composer-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 11px;
  color: var(--text-secondary);
}

.quote-text textarea {
  border: 0;
  padding: 2px 0;
  font-size: 13px;
  line-height: 1.8;
  resize: vertical;
  min-height: 64px;
}

.quote-text textarea:focus-visible {
  outline: none;
}

.composer-row {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f4ee;
}

.source-field {
  flex: 1;
  min-width: 0;
}

.source-field input {
  border: 0;
  border-bottom: 1px solid #e6ebe4;
  border-radius: 0;
  padding: 6px 0;
  font-size: 12px;
  background: transparent;
}

.source-field input:focus-visible {
  outline: none;
  border-bottom-color: #a7c5b0;
}

.upload-area {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

.ghost-button {
  border: 1px solid #dde6da;
  border-radius: 7px;
  background: #fff;
  color: var(--accent);
  font-size: 11px;
  padding: 7px 12px;
}

.ghost-button.subtle {
  border-color: transparent;
  color: #a65346;
}

.visually-hidden {
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

.upload-preview {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
  padding: 10px;
  border-radius: 9px;
  background: #f7f9f6;
  font-size: 11px;
  color: var(--text-secondary);
  overflow-wrap: anywhere;
}

.upload-preview img,
.upload-preview video {
  width: 96px;
  height: 64px;
  object-fit: cover;
  border-radius: 6px;
  background: #000;
}

.composer-error {
  margin: 12px 0 0;
  padding: 9px 12px;
  border-radius: 7px;
  background: #fff5f3;
  color: #a63a3a;
  font-size: 12px;
}

.composer-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-top: 14px;
}

.composer-hint {
  font-size: 10px;
  color: var(--text-muted);
}

.composer-actions .button-primary {
  padding: 9px 18px;
}

.quote-status {
  font-size: 12px;
  color: var(--text-secondary);
}

.quote-empty {
  text-align: center;
  padding: 56px 20px;
  border: 1px dashed #dfe8dc;
  border-radius: 12px;
  background: #fbfcfa;
}

.quote-empty > span {
  font-size: 30px;
  color: #9cb3a0;
}

.quote-empty h2 {
  margin: 14px 0 8px;
  font-size: 15px;
  font-weight: 500;
}

.quote-empty p {
  margin: 0;
  font-size: 12px;
  color: var(--text-secondary);
}

.quote-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(238px, 1fr));
  gap: 16px;
}

.quote-card {
  display: flex;
  flex-direction: column;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: #fff;
  overflow: hidden;
}

.card-media img,
.card-media video {
  display: block;
  width: 100%;
  max-height: 320px;
  object-fit: cover;
  background: #0f1512;
}

.quote-card blockquote {
  margin: 0;
  padding: 16px 16px 10px;
  font-size: 13px;
  line-height: 1.9;
  color: #33473a;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.quote-card footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 0 16px 13px;
  margin-top: auto;
}

.quote-source {
  font-size: 10px;
  color: var(--text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-delete {
  flex-shrink: 0;
  border: 0;
  background: transparent;
  color: #a65346;
  font-size: 10px;
  padding: 2px 0;
}

.quote-footer {
  margin-top: 18px;
  font-size: 10px;
  color: var(--text-muted);
}

@media (max-width: 700px) {
  .quote-heading {
    align-items: flex-start;
    flex-wrap: wrap;
    gap: 12px;
  }

  .quote-heading h1 {
    font-size: 23px;
  }

  .quote-grid {
    grid-template-columns: 1fr;
  }

  .composer-row {
    align-items: stretch;
    flex-direction: column;
    gap: 12px;
  }

  .upload-area {
    justify-content: flex-end;
  }
}
</style>
