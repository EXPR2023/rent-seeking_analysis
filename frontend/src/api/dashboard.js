import request from './request'

export const getDashboardSummary = () => request.get('/dashboard/summary')
export const getRiskLevelChart = () => request.get('/dashboard/risk-levels')
export const getConflictStatusChart = () => request.get('/dashboard/conflict-status')
