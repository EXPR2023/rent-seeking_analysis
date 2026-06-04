import request from './request'

export const getRegulationSets = (params) => request.get('/regulation-sets', { params })
export const createRegulationSet = (payload) => request.post('/regulation-sets', payload)
export const updateRegulationSet = (id, payload) => request.put(`/regulation-sets/${id}`, payload)
export const deleteRegulationSet = (id) => request.delete(`/regulation-sets/${id}`)
