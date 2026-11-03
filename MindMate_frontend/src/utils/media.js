import request from '@/api/request'

/**
 * 将服务端返回的相对路径转换为可访问地址。
 * 生产环境 baseURL 为空时直接使用相对路径，交给同源 Nginx 处理。
 */
export const resolveMediaUrl = (path) => {
  if (!path) return ''
  const base = request.defaults.baseURL
  return base ? new URL(path, base).href : path
}
