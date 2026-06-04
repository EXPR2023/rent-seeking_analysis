import request from './request'

export const analyzeRegulation = (id) => request.post(`/regulations/${id}/analyze`, { force: true })
export const getRiskAnalyses = (params) => request.get('/risk-analyses', { params })
export const getRiskAnalysis = (id) => request.get(`/risk-analyses/${id}`)
export const reviewRiskAnalysis = (id, payload) => request.put(`/risk-analyses/${id}/review`, payload)
