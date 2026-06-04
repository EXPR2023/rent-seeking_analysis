<template>
  <section class="case-analysis-layout">
    <section class="panel case-input-panel">
      <div class="panel-header">
        <h2>寻租联合分析</h2>
        <el-button :icon="Refresh" @click="loadRegulations">刷新制度</el-button>
      </div>

      <el-form label-width="96px">
        <el-form-item label="分析方式">
          <div class="analysis-switch-row">
            <el-switch
              v-model="form.jointAnalysis"
              active-text="联合分析"
              inactive-text="仅案例扩展"
            />
            <el-switch
              v-if="form.jointAnalysis"
              v-model="form.externalDiscovery"
              active-text="召回外部约束 B"
              inactive-text="只分析 A 内部"
            />
          </div>
        </el-form-item>
        <el-form-item label="规章集">
          <el-select v-model="form.regulationSetId" filterable placeholder="选择分析集合" @change="handleSetChange">
            <el-option
              v-for="item in regulationSets"
              :key="item.id"
              :label="setLabel(item)"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="案例标题">
          <el-input v-model="form.caseTitle" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="案例事实">
          <el-input v-model="form.caseDescription" type="textarea" :rows="9" maxlength="5000" show-word-limit />
        </el-form-item>
        <el-form-item label="关联制度">
          <el-select
            v-model="form.relatedRegulationIds"
            multiple
            filterable
            collapse-tags
            collapse-tags-tooltip
            placeholder="选择已导入制度"
          >
            <el-option
              v-for="item in regulations"
              :key="item.id"
              :label="regulationLabel(item)"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="官方链接">
          <el-input v-model="form.sourceUrls" type="textarea" :rows="5" />
        </el-form-item>
        <el-form-item label="联网发现">
          <div class="web-discovery-actions">
            <el-button type="primary" plain :icon="Search" :loading="discovering" @click="discoverSources">
              发现候选公文
            </el-button>
            <el-select v-model="form.importRegulationSetId" filterable placeholder="导入到外部约束集（自动）">
              <el-option label="联网发现外部约束集（自动）" :value="0" />
              <el-option
                v-for="item in regulationSets"
                :key="item.id"
                :label="setLabel(item)"
                :value="item.id"
              />
            </el-select>
            <el-button :loading="importing" :disabled="!selectedCandidates.length" @click="importSelectedSources">
              导入选中
            </el-button>
          </div>
        </el-form-item>
        <section v-if="webCandidates.length" class="web-candidate-panel">
          <div class="web-candidate-title">
            <strong>候选来源</strong>
            <span>{{ webCandidates.length }} 条</span>
          </div>
          <el-table
            ref="candidateTableRef"
            :data="webCandidates"
            size="small"
            border
            max-height="280"
            @selection-change="handleCandidateSelection"
          >
            <el-table-column type="selection" width="42" />
            <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
            <el-table-column prop="domain" label="域名" width="132" show-overflow-tooltip />
            <el-table-column prop="fetchStatus" label="状态" width="86">
              <template #default="{ row }">
                <el-tag :type="fetchStatusType(row.fetchStatus)" size="small">{{ row.fetchStatus }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="contentLength" label="字数" width="72" />
            <el-table-column label="链接" width="64">
              <template #default="{ row }">
                <el-link :href="row.url" target="_blank" type="primary">打开</el-link>
              </template>
            </el-table-column>
          </el-table>
        </section>
        <el-form-item>
          <el-button type="primary" :icon="Search" :loading="analyzing" @click="analyze">开始分析</el-button>
          <el-button :icon="Refresh" @click="resetExample">还原案例</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="panel case-result-panel">
      <div class="panel-header">
        <h2>分析结果</h2>
        <el-tag v-if="result?.recordId" type="info">记录ID：{{ result.recordId }}</el-tag>
      </div>

      <el-empty v-if="!result" description="暂无分析结果" />
      <template v-else>
        <section class="risk-score-band risk-score-band-wide">
          <div>
            <span>风险等级</span>
            <strong>
              <el-tag :type="riskTagType(result.riskLevel)" size="large">{{ result.riskLevel }}</el-tag>
            </strong>
          </div>
          <div>
            <span>风险分数</span>
            <strong>{{ result.riskScore ?? '-' }}</strong>
          </div>
          <div>
            <span>分析模式</span>
            <strong>{{ modeLabel(result.analysisMode) }}</strong>
          </div>
          <div>
            <span>制度证据</span>
            <strong>{{ result.evidenceSnippets?.length || 0 }}</strong>
          </div>
          <div>
            <span>抓取来源</span>
            <strong>{{ result.fetchedSources?.length || 0 }}</strong>
          </div>
          <div v-if="result.jointAnalysis">
            <span>主规章集 A</span>
            <strong>{{ result.primaryRegulationSetName || '-' }}</strong>
          </div>
        </section>

        <section class="risk-section">
          <h3>案例摘要</h3>
          <p>{{ result.summary }}</p>
        </section>

        <section v-if="result.jointAnalysis" class="risk-section">
          <h3>联合推理摘要</h3>
          <p>{{ result.jointSummary }}</p>
        </section>

        <section v-if="result.internalRisks?.length" class="risk-section">
          <h3>A 内部风险图谱</h3>
          <el-table :data="result.internalRisks" size="small" border>
            <el-table-column prop="source" label="来源" width="110" />
            <el-table-column prop="riskType" label="类型" width="160" show-overflow-tooltip />
            <el-table-column prop="riskTitle" label="风险" min-width="220" show-overflow-tooltip />
            <el-table-column prop="mechanism" label="形成机制" min-width="320" show-overflow-tooltip />
            <el-table-column prop="relatedRegulations" label="关联制度" min-width="220" show-overflow-tooltip />
            <el-table-column prop="riskLevel" label="等级" width="96">
              <template #default="{ row }">
                <el-tag :type="riskTagType(row.riskLevel)">{{ row.riskLevel }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section v-if="result.caseRegulationMappings?.length" class="risk-section">
          <h3>案例-A 映射</h3>
          <el-table :data="result.caseRegulationMappings" size="small" border>
            <el-table-column prop="caseFact" label="案例事实" min-width="240" show-overflow-tooltip />
            <el-table-column prop="matchedRegulation" label="匹配制度" min-width="220" show-overflow-tooltip />
            <el-table-column prop="matchedRisk" label="对应风险" min-width="180" show-overflow-tooltip />
            <el-table-column prop="gapType" label="缺口类型" width="120" />
            <el-table-column prop="explanation" label="解释" min-width="320" show-overflow-tooltip />
          </el-table>
        </section>

        <section v-if="result.externalConstraints?.length" class="risk-section">
          <h3>外部约束证据 B</h3>
          <el-table :data="result.externalConstraints" size="small" border>
            <el-table-column prop="documentTitle" label="外部制度" min-width="220" show-overflow-tooltip />
            <el-table-column prop="constraintType" label="约束类型" width="140" show-overflow-tooltip />
            <el-table-column prop="constrainsRisk" label="约束风险" min-width="220" show-overflow-tooltip />
            <el-table-column prop="constraintMechanism" label="约束机制" min-width="320" show-overflow-tooltip />
            <el-table-column prop="sufficiency" label="充分性" width="120" />
            <el-table-column prop="importAdvice" label="处理建议" min-width="260" show-overflow-tooltip />
          </el-table>
        </section>

        <section v-if="result.closureGaps?.length" class="risk-section">
          <h3>制度闭环缺口</h3>
          <el-table :data="result.closureGaps" size="small" border>
            <el-table-column prop="gapTitle" label="缺口" min-width="220" show-overflow-tooltip />
            <el-table-column prop="missingLink" label="缺少衔接" min-width="240" show-overflow-tooltip />
            <el-table-column prop="remainingRisk" label="剩余风险" min-width="280" show-overflow-tooltip />
            <el-table-column prop="recommendedAction" label="建议动作" min-width="300" show-overflow-tooltip />
            <el-table-column prop="priority" label="优先级" width="96">
              <template #default="{ row }">
                <el-tag :type="riskTagType(row.priority)">{{ row.priority }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section v-if="result.iterationTraces?.length" class="risk-section">
          <h3>有限迭代轨迹</h3>
          <el-table :data="result.iterationTraces" size="small" border>
            <el-table-column prop="round" label="轮次" width="80" />
            <el-table-column prop="objective" label="目标" width="160" />
            <el-table-column prop="query" label="输入/查询" min-width="320" show-overflow-tooltip />
            <el-table-column prop="evidenceCount" label="证据数" width="90" />
            <el-table-column prop="stopReason" label="停止原因" min-width="260" show-overflow-tooltip />
          </el-table>
        </section>

        <section class="risk-section">
          <h3>相关法律公文</h3>
          <el-table :data="result.relatedDocuments" size="small" border>
            <el-table-column prop="title" label="名称" min-width="220" show-overflow-tooltip />
            <el-table-column prop="issuingBody" label="发布机关" width="160" show-overflow-tooltip />
            <el-table-column prop="importStatus" label="状态" width="110">
              <template #default="{ row }">
                <el-tag :type="importStatusType(row.importStatus)">{{ row.importStatus }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="relevance" label="关联说明" min-width="320" show-overflow-tooltip />
            <el-table-column label="来源" width="88">
              <template #default="{ row }">
                <el-link v-if="row.sourceUrl" :href="row.sourceUrl" target="_blank" type="primary">打开</el-link>
                <span v-else>-</span>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section class="risk-section">
          <h3>组织责任链</h3>
          <el-table :data="result.organizationNodes" size="small" border>
            <el-table-column prop="organization" label="组织" min-width="180" show-overflow-tooltip />
            <el-table-column prop="level" label="层级" width="100" />
            <el-table-column prop="role" label="职责" min-width="260" show-overflow-tooltip />
            <el-table-column prop="riskResponsibility" label="风险责任" min-width="300" show-overflow-tooltip />
            <el-table-column prop="evidenceNeed" label="需补证据" min-width="220" show-overflow-tooltip />
          </el-table>
        </section>

        <section class="risk-section">
          <h3>寻租风险点</h3>
          <el-collapse>
            <el-collapse-item v-for="(item, index) in result.riskItems" :key="`${item.title}-${index}`">
              <template #title>
                <div class="case-risk-title">
                  <el-tag :type="riskTagType(item.riskLevel)" size="small">{{ item.riskLevel }}</el-tag>
                  <span>{{ item.title }}</span>
                </div>
              </template>
              <div class="case-risk-grid">
                <div>
                  <span>指标</span>
                  <p>{{ item.indicatorCode }}</p>
                </div>
                <div>
                  <span>机制</span>
                  <p>{{ item.mechanism }}</p>
                </div>
                <div>
                  <span>原因</span>
                  <p>{{ item.reason }}</p>
                </div>
                <div>
                  <span>证据</span>
                  <p>{{ item.evidence }}</p>
                </div>
                <div>
                  <span>整改动作</span>
                  <p>{{ item.suggestion }}</p>
                </div>
              </div>
            </el-collapse-item>
          </el-collapse>
        </section>

        <section class="risk-section">
          <h3>待导入材料</h3>
          <el-table :data="result.manualImportSuggestions" size="small" border>
            <el-table-column prop="category" label="类别" width="110" />
            <el-table-column prop="title" label="材料" min-width="260" show-overflow-tooltip />
            <el-table-column prop="suggestedSource" label="建议来源" min-width="220" show-overflow-tooltip />
            <el-table-column prop="reason" label="用途" min-width="320" show-overflow-tooltip />
          </el-table>
        </section>

        <section v-if="result.fetchedSources?.length" class="risk-section">
          <h3>公开来源抓取</h3>
          <el-table :data="result.fetchedSources" size="small" border>
            <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="150">
              <template #default="{ row }">
                <el-tag :type="fetchStatusType(row.status)">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="message" label="说明" min-width="260" show-overflow-tooltip />
            <el-table-column label="链接" width="88">
              <template #default="{ row }">
                <el-link :href="row.url" target="_blank" type="primary">打开</el-link>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section v-if="result.evidenceSnippets?.length" class="risk-section evidence-section">
          <h3>RAG 证据片段</h3>
          <div class="evidence-list">
            <article v-for="(item, index) in result.evidenceSnippets" :key="index" class="evidence-card">
              <p>{{ item }}</p>
            </article>
          </div>
        </section>
      </template>
    </section>
  </section>
</template>

<script setup>
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { analyzeCase as analyzeCaseApi, discoverWebSources, importWebSources } from '../api/ai'
import { getRegulations } from '../api/regulations'
import { getRegulationSets } from '../api/regulationSets'

const example = {
  caseTitle: '郴州北湖区勇鑫烟花零售店燃爆事件',
  caseDescription:
    '2025年郴州中心城区烟花爆竹销售与禁燃限放政策调整期间，涉事零售店处于新划定禁燃区内。公开材料显示，政策涉及停止办理相关行政许可、存量网点退出、批发公司原价回购、禁燃区外异地选址重新申办等事项；许可证有效期、春节备货销售窗口、行政救济周期和政策切换窗口高度重叠。',
  sourceUrls: [
    'https://www.gov.cn/gongbao/content/2006/content_219931.htm',
    'https://www.mem.gov.cn/gk/gwgg/agwzlfl/zjl_01/201310/t20131023_233708.shtml',
    'https://www.chinanews.com.cn/sh/2026/03-27/10593646.shtml'
  ].join('\n')
}

const regulations = ref([])
const regulationSets = ref([])
const result = ref(null)
const analyzing = ref(false)
const discovering = ref(false)
const importing = ref(false)
const webCandidates = ref([])
const selectedCandidates = ref([])
const candidateTableRef = ref()
const form = reactive({
  regulationSetId: null,
  importRegulationSetId: 0,
  jointAnalysis: true,
  externalDiscovery: true,
  caseTitle: example.caseTitle,
  caseDescription: example.caseDescription,
  relatedRegulationIds: [],
  sourceUrls: example.sourceUrls
})

async function loadSets() {
  regulationSets.value = await getRegulationSets({ enabledOnly: true })
  if (!form.regulationSetId) {
    form.regulationSetId = defaultSetId()
  }
}

async function loadRegulations() {
  if (!regulationSets.value.length) {
    await loadSets()
  }
  const data = await getRegulations({ pageNo: 1, pageSize: 100, status: 'ACTIVE', regulationSetId: form.regulationSetId })
  regulations.value = data.records
  if (!form.relatedRegulationIds.length) {
    form.relatedRegulationIds = defaultSelectedRegulationIds(data.records)
  }
}

async function handleSetChange() {
  form.relatedRegulationIds = []
  webCandidates.value = []
  selectedCandidates.value = []
  result.value = null
  await loadRegulations()
}

async function resetExample() {
  form.regulationSetId = defaultSetId()
  form.importRegulationSetId = 0
  form.jointAnalysis = true
  form.externalDiscovery = true
  form.caseTitle = example.caseTitle
  form.caseDescription = example.caseDescription
  form.sourceUrls = example.sourceUrls
  form.relatedRegulationIds = []
  webCandidates.value = []
  selectedCandidates.value = []
  await loadRegulations()
  result.value = null
}

async function discoverSources() {
  if (!form.caseTitle.trim() && !form.caseDescription.trim()) {
    ElMessage.warning('请输入案例标题或案例事实')
    return
  }
  discovering.value = true
  try {
    const data = await discoverWebSources({
      caseTitle: form.caseTitle.trim(),
      caseDescription: form.caseDescription.trim(),
      regulationSetId: form.regulationSetId,
      relatedRegulationIds: form.relatedRegulationIds,
      maxResults: 8
    })
    webCandidates.value = data.candidates || []
    selectedCandidates.value = []
    await nextTick()
    const defaults = webCandidates.value
      .filter((item) => item.fetchStatus === 'FETCHED' && item.officialSource && (item.contentLength || 0) >= 2000)
      .slice(0, 5)
    defaults.forEach((item) => candidateTableRef.value?.toggleRowSelection(item, true))
    ElMessage.success(`发现 ${webCandidates.value.length} 条候选来源`)
  } finally {
    discovering.value = false
  }
}

function handleCandidateSelection(rows) {
  selectedCandidates.value = rows
}

async function importSelectedSources() {
  if (!selectedCandidates.value.length) {
    ElMessage.warning('请选择需要导入的候选来源')
    return
  }
  importing.value = true
  try {
    const data = await importWebSources({
      regulationSetId: form.importRegulationSetId || null,
      sources: selectedCandidates.value.map((item) => ({
        url: item.url,
        title: item.title,
        typeCode: item.suggestedTypeCode,
        publishDepartment: item.suggestedPublishDepartment
      }))
    })
    appendSourceUrls(selectedCandidates.value.map((item) => item.url))
    await loadSets()
    if (form.importRegulationSetId && form.importRegulationSetId === form.regulationSetId) {
      await loadRegulations()
    }
    ElMessage.success(`已导入 ${data.importedCount || 0} 条，跳过 ${data.skippedCount || 0} 条`)
  } finally {
    importing.value = false
  }
}

async function analyze() {
  if (!form.caseTitle.trim()) {
    ElMessage.warning('请输入案例标题')
    return
  }
  if (!form.caseDescription.trim()) {
    ElMessage.warning('请输入案例事实')
    return
  }
  analyzing.value = true
  try {
    result.value = await analyzeCaseApi({
      caseTitle: form.caseTitle.trim(),
      caseDescription: form.caseDescription.trim(),
      regulationSetId: form.regulationSetId,
      jointAnalysis: form.jointAnalysis,
      externalDiscovery: form.externalDiscovery,
      relatedRegulationIds: form.relatedRegulationIds,
      sourceUrls: form.sourceUrls
        .split(/\s+/)
        .map((item) => item.trim())
        .filter(Boolean)
    })
  } finally {
    analyzing.value = false
  }
}

function regulationLabel(item) {
  return item.code ? `${item.code} ${item.title}` : item.title
}

function appendSourceUrls(urls) {
  const merged = new Set(
    form.sourceUrls
      .split(/\s+/)
      .map((item) => item.trim())
      .filter(Boolean)
  )
  urls.filter(Boolean).forEach((url) => merged.add(url))
  form.sourceUrls = Array.from(merged).join('\n')
}

function defaultSelectedRegulationIds(items) {
  const fireworks = items.filter((item) => item.code?.startsWith('CZ-FW-REG-'))
  const preferred = fireworks.length ? fireworks : items
  return preferred.slice(0, 8).map((item) => item.id)
}

function defaultSetId() {
  return regulationSets.value.find((item) => item.defaultSet && item.enabled)?.id || regulationSets.value.find((item) => item.enabled)?.id || null
}

function setLabel(item) {
  const count = item.regulationCount ?? 0
  return item.defaultSet ? `${item.setName}（默认，${count}项）` : `${item.setName}（${count}项）`
}

function riskTagType(level) {
  const types = {
    LOW: 'success',
    MEDIUM: 'warning',
    HIGH: 'danger',
    CRITICAL: 'danger'
  }
  return types[level] || 'info'
}

function importStatusType(status) {
  if (status === '已导入' || status === '已抓取') return 'success'
  if (status === '建议导入') return 'warning'
  return 'info'
}

function fetchStatusType(status) {
  if (status === 'FETCHED') return 'success'
  if (status === 'FAILED') return 'danger'
  return 'warning'
}

function modeLabel(mode) {
  const labels = {
    RAG_ENHANCED: 'RAG 增强',
    LLM_ENHANCED: 'LLM 增强',
    RULE_ONLY: '规则兜底',
    JOINT_RAG_ENHANCED: '联合 RAG 增强',
    JOINT_LLM_ENHANCED: '联合 LLM 增强',
    JOINT_RULE_ONLY: '联合规则兜底'
  }
  return labels[mode] || mode || '-'
}

onMounted(async () => {
  await loadSets()
  await loadRegulations()
})
</script>

<style scoped>
.analysis-switch-row {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: center;
}

.web-discovery-actions {
  width: 100%;
  display: grid;
  grid-template-columns: auto minmax(180px, 1fr) auto;
  gap: 8px;
  align-items: center;
}

.web-candidate-panel {
  margin: 0 0 18px 96px;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f8fafc;
}

.web-candidate-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.web-candidate-title span {
  color: #64748b;
  font-size: 13px;
}

@media (max-width: 900px) {
  .web-discovery-actions {
    grid-template-columns: 1fr;
  }

  .web-candidate-panel {
    margin-left: 0;
  }
}
</style>
