import request from './request'

export const login = (payload) => request.post('/auth/login', payload)
export const logout = () => request.post('/auth/logout')
export const getCurrentUser = () => request.get('/auth/me')
export const getCurrentMenus = () => request.get('/menus/current')
