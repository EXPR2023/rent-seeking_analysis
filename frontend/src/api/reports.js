import request from './request'

export const getRegulationReport = (id) => request.get(`/reports/regulation/${id}`)
