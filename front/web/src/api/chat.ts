import request from '@/utils/request'

export function createSession(sessionTitle?: string) {
  return request.post('/chat/session/create', null, { params: { sessionTitle } })
}

export function getSession(sessionId: number) {
  return request.get('/chat/session/get', { params: { sessionId } })
}

export function updateSessionTitle(sessionId: number, title: string) {
  return request.post('/chat/session/updateTitle', null, { params: { sessionId, title } })
}

export function endSession(sessionId: number) {
  return request.post('/chat/session/end', null, { params: { sessionId } })
}
