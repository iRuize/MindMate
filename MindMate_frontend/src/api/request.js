import axios from 'axios'

/** 登录凭证的本地存储键，路由守卫与请求拦截共用 */
export const TOKEN_KEY = 'mindmate_token'

const request = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 10000,
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body.code === 200) {
      // 统一拆包，业务层直接拿 data
      return body.data
    }
    if (body.code === 401) {
      localStorage.removeItem(TOKEN_KEY)
      window.location.replace('/login')
      return Promise.reject(new Error(body.message))
    }
    return Promise.reject(new Error(body.message || '请求失败'))
  },
  (error) => {
    const message = error.response?.data?.message || '网络异常，请稍后重试'
    return Promise.reject(new Error(message))
  },
)

export default request
