import request from './request'

/** 获取验证码。伪装实现，返回值为验证码本身，不真实下发短信 */
export const sendSmsCode = (phoneNumber) =>
  request.get('/auth/sendSmsCode', { params: { phoneNumber } })

/** 验证码登录，返回 { token, user } */
export const login = (phoneNumber, smsCode) =>
  request.post('/auth/login', { phoneNumber, smsCode })

/** 查询当前登录用户 */
export const fetchMe = () => request.get('/auth/me')

/** 退出登录，服务端销毁凭证 */
export const logout = () => request.post('/auth/logout')
