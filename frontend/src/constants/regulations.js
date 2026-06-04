export const regulationTypes = [
  { label: '监管政策', value: 'POLICY' },
  { label: '流程类', value: 'PROCEDURE' },
  { label: '审批类', value: 'APPROVAL' },
  { label: '监督类', value: 'SUPERVISION' }
]

export const regulationStatuses = [
  { label: '草稿', value: 'DRAFT', type: 'info' },
  { label: '已生效', value: 'ACTIVE', type: 'success' },
  { label: '已失效', value: 'INACTIVE', type: 'warning' },
  { label: '已归档', value: 'ARCHIVED', type: 'info' }
]

export const analysisStatuses = [
  { label: '未分析', value: 'NOT_ANALYZED', type: 'info' },
  { label: '分析中', value: 'ANALYZING', type: 'warning' },
  { label: '已完成', value: 'COMPLETED', type: 'success' },
  { label: '分析失败', value: 'FAILED', type: 'danger' }
]

export function labelOf(options, value) {
  return options.find((item) => item.value === value)?.label || value || '-'
}

export function tagTypeOf(options, value) {
  return options.find((item) => item.value === value)?.type || 'info'
}
