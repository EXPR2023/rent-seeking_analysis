import request from './request'

export const getRegulations = (params) => request.get('/regulations', { params })
export const getRegulation = (id) => request.get(`/regulations/${id}`)
export const createRegulation = (payload) => request.post('/regulations', payload)
export const updateRegulation = (id, payload) => request.put(`/regulations/${id}`, payload)
export const saveRegulationContent = (id, content) => request.put(`/regulations/${id}/content`, { content })
export const deleteRegulation = (id) => request.delete(`/regulations/${id}`)
