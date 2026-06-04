import request from './request'

export const getUsers = (params) => request.get('/users', { params })
export const createUser = (payload) => request.post('/users', payload)
export const updateUser = (id, payload) => request.put(`/users/${id}`, payload)
export const updateUserStatus = (id, status) => request.patch(`/users/${id}/status`, { status })
export const assignUserRoles = (id, roleIds) => request.put(`/users/${id}/roles`, { roleIds })
export const getRoles = (params = {}) => request.get('/roles', { params })
