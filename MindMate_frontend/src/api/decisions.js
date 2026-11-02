import request from './request'

export const fetchDecisionConfig = () => request.get('/decisions/config')
export const fetchDecisionPreferences = () => request.get('/decisions/preferences')
export const saveDecisionPreferences = (data) => request.put('/decisions/preferences', data)
export const clearDecisionPreferences = () => request.delete('/decisions/preferences')
export const fetchDecisions = () => request.get('/decisions')
export const createDecision = (data = {}) => request.post('/decisions', data)
export const fetchDecisionMessages = (id) => request.get(`/decisions/${encodeURIComponent(id)}/messages`)
export const sendDecisionMessage = (id, content, signal) => request.post(`/decisions/${encodeURIComponent(id)}/messages`, {content}, {timeout: 120000, signal})
export const deleteDecision = (id) => request.delete(`/decisions/${encodeURIComponent(id)}`)
