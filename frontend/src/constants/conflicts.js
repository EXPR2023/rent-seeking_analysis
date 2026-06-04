export const taskStatuses = [
  { label: '待处理', value: 'PENDING', type: 'info' },
  { label: '执行中', value: 'RUNNING', type: 'warning' },
  { label: '已完成', value: 'COMPLETED', type: 'success' },
  { label: '失败', value: 'FAILED', type: 'danger' }
]

export const conflictTypes = [
  { label: '重复', value: 'DUPLICATE' },
  { label: '不一致', value: 'INCONSISTENT' },
  { label: '适用范围冲突', value: 'SCOPE_CONFLICT' },
  { label: '主体冲突', value: 'SUBJECT_CONFLICT' },
  { label: '时间冲突', value: 'TIME_CONFLICT' }
]

export const conflictLevels = [
  { label: '低', value: 'LOW', type: 'info' },
  { label: '中', value: 'MEDIUM', type: 'warning' },
  { label: '高', value: 'HIGH', type: 'danger' },
  { label: '严重', value: 'CRITICAL', type: 'danger' }
]

export const conflictStatuses = [
  { label: '待确认', value: 'PENDING', type: 'warning' },
  { label: '已确认', value: 'CONFIRMED', type: 'success' },
  { label: '已忽略', value: 'IGNORED', type: 'info' }
]

export function labelOf(options, value) {
  return options.find((item) => item.value === value)?.label || value || '-'
}

export function tagTypeOf(options, value) {
  return options.find((item) => item.value === value)?.type || 'info'
}
