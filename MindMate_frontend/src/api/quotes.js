import request from './request'
import {resolveMediaUrl} from '@/utils/media'

/** 当前用户的经典语录，按新增倒序 */
export const fetchQuotes = () => request.get('/quotes')

/** 新增语录：文字与媒体至少一项，媒体可为图片或视频 */
export const createQuote = ({content, source, file}, onUploadProgress) => {
  const data = new FormData()
  if (content) data.append('content', content)
  if (source) data.append('source', source)
  if (file) data.append('file', file)
  return request.post('/quotes', data, {timeout: 180000, onUploadProgress})
}

/** 删除语录 */
export const deleteQuote = (id) => request.delete(`/quotes/${encodeURIComponent(id)}`)

/** 将服务端媒体路径转换为可展示地址 */
export const mediaAddress = (path) => resolveMediaUrl(path)
