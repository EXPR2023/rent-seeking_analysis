export const riskLevels = [
  { label: '低风险', value: 'LOW', type: 'success' },
  { label: '中风险', value: 'MEDIUM', type: 'warning' },
  { label: '高风险', value: 'HIGH', type: 'danger' },
  { label: '严重风险', value: 'CRITICAL', type: 'danger' }
]

export const reviewStatuses = [
  { label: '待复核', value: 'PENDING', type: 'warning' },
  { label: '已确认', value: 'CONFIRMED', type: 'success' },
  { label: '已调整', value: 'ADJUSTED', type: 'info' }
]

export const analysisModes = [
  { label: '规则兜底', value: 'RULE_ONLY', type: 'info' },
  { label: 'LLM 增强', value: 'LLM_ENHANCED', type: 'success' },
  { label: 'RAG 增强', value: 'RAG_ENHANCED', type: 'warning' }
]

export const sourceTraces = [
  { label: '规则命中', value: 'RULE', type: 'info' },
  { label: 'LLM 识别', value: 'LLM', type: 'success' },
  { label: '人工调整', value: 'MANUAL', type: 'warning' }
]

export const rectificationStatuses = [
  { label: '待整改', value: 'PENDING', type: 'warning' },
  { label: '处理中', value: 'PROCESSING', type: 'primary' },
  { label: '已完成', value: 'COMPLETED', type: 'success' }
]

export const riskIndicators = [
  { label: '自由裁量权', value: 'DISCRETIONARY_POWER' },
  { label: '审批集中度', value: 'APPROVAL_CONCENTRATION' },
  { label: '流程透明度', value: 'PROCESS_TRANSPARENCY' },
  { label: '监督约束', value: 'SUPERVISION_CONSTRAINT' },
  { label: '利益关联', value: 'BENEFIT_RELATED' },
  { label: '处罚弹性', value: 'PENALTY_FLEXIBILITY' },
  { label: '时间窗口寻租', value: 'TIME_WINDOW_RENT' }
]

export function labelOf(options, value) {
  return options.find((item) => item.value === value)?.label || value || '-'
}

export function tagTypeOf(options, value) {
  return options.find((item) => item.value === value)?.type || 'info'
}
