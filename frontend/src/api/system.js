import request from './request'

export const getSystemConfigs = () => request.get('/system/configs')
export const updateSystemConfig = (configKey, configValue) =>
  request.put(`/system/configs/${encodeURIComponent(configKey)}`, { configValue })
export const getDicts = (params) => request.get('/system/dicts', { params })
export const getAuditLogs = (params) => request.get('/audit/logs', { params })
export const getHealth = () => request.get('/system/health')
