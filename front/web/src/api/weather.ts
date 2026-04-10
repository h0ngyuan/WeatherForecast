import request from '@/utils/request'

export interface WeatherAskParams {
  question: string
  sessionId?: number
}

export interface ChatHistory {
  id: number
  sessionId: number
  role: 'user' | 'assistant'
  content: string
  messageType: string
  createTime: string
}

export function getChatHistory(sessionId: number) {
  return request.get('/weather/chat-history', { params: { sessionId } })
}

export function getCurrentSession() {
  return request.get('/weather/current-session')
}

export function createNewSession(title?: string) {
  return request.post('/weather/new-session', null, { params: { title } })
}

export interface WeatherAskResult {
  answer: string
  relevant: boolean
  relevanceScore: number | null
  finalQualityScore: number | null
  loopCount: number | null
}

export function askWeather(data: WeatherAskParams) {
  return request.post('/weather/query', data)
}

export function grantPermission(data: {
  threadId: string
  grantPhonePermission?: boolean
  grantEmailPermission?: boolean
  grantWechatPermission?: boolean
  phone?: string
  email?: string
}) {
  return request.post('/weather/grant-permission', data)
}

export function resumeProcess(threadId: string) {
  return request.post('/weather/resume', null, { params: { threadId } })
}

export function rejectPermission(threadId: string) {
  return request.post('/weather/reject-permission', null, { params: { threadId } })
}

export interface SubscribeParams {
  subscribeName: string
  location: string
  weatherCodes: number[]
  taskType?: number
  notifyCondition?: string
  disasterLevel?: number
}

export function subscribeWeather(data: SubscribeParams) {
  return request.post('/weather/subscribe', data)
}

// 获取当前IP定位城市
export function getCurrentLocation() {
  return request.get('/weather/current-location')
}
