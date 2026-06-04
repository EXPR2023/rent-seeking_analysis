import request from './request'

export const getAiRecords = (params) => request.get('/ai/records', { params })
export const askRegulation = (id, question) => request.post(`/ai/regulations/${id}/chat`, { question })
export const analyzeCase = (payload) => request.post('/ai/cases/analyze', payload)
export const discoverWebSources = (payload) => request.post('/ai/web-sources/discover', payload)
export const importWebSources = (payload) => request.post('/ai/web-sources/import', payload)
