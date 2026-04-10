import request from '@/utils/request'

export interface LoginParams {
  openId?: string
  phone?: string
  email?: string
  password?: string
  captchaKey?: string
  captchaCode?: string
  verifyCode?: string
  type: 'wx' | 'phone' | 'email'
}

export function login(data: LoginParams) {
  return request.post('/wx/login', data)
}

export function logout() {
  return request.post('/wx/logout')
}

export function getUserInfo() {
  return request.get('/wx/getUserInfo')
}

export function generateImageCaptcha() {
  return request.get('/wx/generateImageCaptcha')
}

export function verifyImageCaptcha(data: { key: string; code: string }) {
  return request.post('/wx/verifyImageCaptcha', data)
}

export function sendEmailCaptcha(email: string) {
  return request.post('/wx/sendEmailCaptcha', { email })
}

export function checkEmailBound() {
  return request.get('/wx/checkEmailBound')
}

export function bindEmail(data: { email: string; code: string }) {
  return request.post('/wx/bindEmail', data)
}

export function getNotifySettings() {
  return request.get('/wx/getNotifySettings')
}

export function updateNotifySettings(data: {
  wechatNotifyPermission?: number
  emailNotifyPermission?: number
  phoneNotifyPermission?: number
}) {
  return request.post('/wx/updateNotifySettings', data)
}
