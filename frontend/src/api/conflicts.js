import request from './request'

export const getConflictTasks = (params) => request.get('/conflict-tasks', { params })
export const createConflictTask = (payload) => request.post('/conflict-tasks', payload)
export const getConflictTask = (id) => request.get(`/conflict-tasks/${id}`)
export const confirmConflictItem = (id, reviewComment = '') =>
  request.put(`/conflict-items/${id}/confirm`, { reviewComment })
export const ignoreConflictItem = (id, reviewComment = '') =>
  request.put(`/conflict-items/${id}/ignore`, { reviewComment })
