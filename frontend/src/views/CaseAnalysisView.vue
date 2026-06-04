<template>
  <section class="case-analysis-layout">
    <section class="panel case-input-panel">
      <div class="panel-header">
        <h2>案例分析</h2>
        <el-button :icon="Refresh" @click="loadRegulations">刷新制度</el-button>
      </div>

      <el-steps :active="activeStep" finish-status="success" simple class="flow-steps">
        <el-step title="导入事实" />
        <el-step title="确认制度来源" />
        <el-step title="寻租分析" />
      </el-steps>

      <el-form label-width="104px">
        <el-form-item label="规章集">
          <el-select v-model="form.regulationSetId" filterable placeholder="选择检索范围" @change="handleSetChange">
            <el-option v-for="item in regulationSets" :key="item.id" :label="setLabel(item)" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="案例标题">
          <el-input v-model="form.caseTitle" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="案例事实">
          <el-input v-model="form.caseDescription" type="textarea" :rows="9" maxlength="5000" show-word-limit />
        </el-form-item>
        <el-form-item label="已导入制度">
          <el-select
            v-model="form.relatedRegulationIds"
            multiple
            filterable
            collapse-tags
            collapse-tags-tooltip
            placeholder="选择可作为初始线索的制度"
          >
            <el-option v-for="item in regulations" :key="item.id" :label="regulationLabel(item)" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="补充 URL">
          <el-input v-model="form.sourceUrls" type="textarea" :rows="4" placeholder="每行一个官方公文或法规 URL" />
        </el-form-item>
        <el-form-item>
          <div class="analysis-action-row">
            <el-button type="primary" plain :icon="Search" :loading="discovering" @click="discoverSources">
              定向发现制度正文
            </el-button>
            <el-button :disabled="!canConfirmSources" @click="confirmSources">确认来源完整</el-button>
            <el-button type="primary" :icon="Search" :loading="analyzing" :disabled="!form.sourcesConfirmed" @click="analyze">
              开始寻租分析
            </el-button>
            <el-button :icon="Refresh" @click="resetExample">还原案例</el-button>
          </div>
        </el-form-item>
      </el-form>

      <section v-if="webCandidates.length" class="source-review-panel">
        <div class="source-review-title">
          <strong>候选制度正文</strong>
          <el-tag :type="form.sourcesConfirmed ? 'success' : 'warning'">
            {{ form.sourcesConfirmed ? '已确认' : '待确认' }}
          </el-tag>
        </div>
        <el-table
          ref="candidateTableRef"
          :data="webCandidates"
          size="small"
          border
          max-height="340"
          @selection-change="handleCandidateSelection"
        >
          <el-table-column type="selection" width="42" />
          <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
          <el-table-column prop="domain" label="域名" width="140" show-overflow-tooltip />
          <el-table-column label="来源" width="84">
            <template #default="{ row }">
              <el-tag :type="row.officialSource ? 'success' : 'warning'" size="small">
                {{ row.officialSource ? '官方' : '核验' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="fetchStatus" label="正文" width="92">
            <template #default="{ row }">
              <el-tag :type="fetchStatusType(row.fetchStatus)" size="small">{{ row.fetchStatus }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="contentLength" label="字数" width="78" />
          <el-table-column label="操作" width="138" fixed="right">
            <template #default="{ row }">
              <el-link :href="row.url" target="_blank" type="primary">打开</el-link>
              <el-button link type="primary" @click="reserveImport(row)">导入</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>
    </section>

    <section class="panel case-result-panel">
      <div class="panel-header">
        <h2>案例寻租分析结果</h2>
        <el-tag v-if="result?.recordId" type="info">记录ID：{{ result.recordId }}</el-tag>
      </div>

      <el-empty v-if="!result" description="暂无分析结果" />
      <template v-else>
        <section class="risk-score-band risk-score-band-wide">
          <div>
            <span>风险等级</span>
            <strong><el-tag :type="riskTagType(result.riskLevel)" size="large">{{ result.riskLevel }}</el-tag></strong>
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
        </section>

        <section class="risk-section">
          <h3>案例摘要</h3>
          <p>{{ result.summary }}</p>
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
            <el-table-column label="操作" width="138" fixed="right">
              <template #default="{ row }">
                <el-link v-if="row.sourceUrl" :href="row.sourceUrl" target="_blank" type="primary">打开</el-link>
                <span v-else>-</span>
                <el-button v-if="row.sourceUrl" link type="primary" @click="reserveImport(row)">导入</el-button>
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
            <el-table-column label="操作" width="138" fixed="right">
              <template #default="{ row }">
                <el-link :href="row.url" target="_blank" type="primary">打开</el-link>
                <el-button link type="primary" @click="reserveImport(row)">导入</el-button>
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
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { analyzeCase as analyzeCaseApi, discoverWebSources } from '../api/ai'
import { getRegulations } from '../api/regulations'
import { getRegulationSets } from '../api/regulationSets'

const example = {
  caseTitle: '郴州北湖区勇鑫烟花零售店燃爆事件',
  caseDescription:
    '2025年郴州中心城区烟花爆竹销售与禁燃限放政策调整期间，涉事零售店处于新划定禁燃区内。公开材料显示，政策涉及停止办理相关行政许可、存量网点退出、批发公司原价回购、禁燃区外异地选址重新申办等事项；许可证有效期、春节备货销售窗口、行政救济周期和政策切换窗口高度重叠。',
  sourceUrls: [
    'https://www.gov.cn/gongbao/content/2006/content_219931.htm',
    'https://www.mem.gov.cn/gk/gwgg/agwzlfl/zjl_01/201310/t20131023_233708.shtml'
  ].join('\n')
}

const regulations = ref([])
const regulationSets = ref([])
const result = ref(null)
const analyzing = ref(false)
const discovering = ref(false)
const webCandidates = ref([])
const selectedCandidates = ref([])
const sourceDiscoveryDone = ref(false)
const candidateTableRef = ref()
const form = reactive({
  regulationSetId: null,
  caseTitle: example.caseTitle,
  caseDescription: example.caseDescription,
  relatedRegulationIds: [],
  sourceUrls: example.sourceUrls,
  sourcesConfirmed: false
})

const activeStep = computed(() => {
  if (result.value) return 3
  if (form.sourcesConfirmed) return 2
  if (webCandidates.value.length) return 1
  return 0
})
const canConfirmSources = computed(() => sourceDiscoveryDone.value && (webCandidates.value.length === 0 || selectedCandidates.value.length > 0))

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
  clearSourceConfirmation()
  await loadRegulations()
}

async function resetExample() {
  form.regulationSetId = defaultSetId()
  form.caseTitle = example.caseTitle
  form.caseDescription = example.caseDescription
  form.sourceUrls = example.sourceUrls
  form.relatedRegulationIds = []
  result.value = null
  clearSourceConfirmation()
  await loadRegulations()
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
      sourceUrls: sourceUrlList(),
      maxResults: 10
    })
    webCandidates.value = data.candidates || []
    selectedCandidates.value = []
    sourceDiscoveryDone.value = true
    form.sourcesConfirmed = false
    await nextTick()
    const defaults = webCandidates.value
      .filter((item) => item.fetchStatus === 'FETCHED' && item.officialSource && (item.contentLength || 0) >= 500)
      .slice(0, 6)
    defaults.forEach((item) => candidateTableRef.value?.toggleRowSelection(item, true))
    ElMessage.success(`发现 ${webCandidates.value.length} 条候选制度正文`)
  } finally {
    discovering.value = false
  }
}

function handleCandidateSelection(rows) {
  selectedCandidates.value = rows
  form.sourcesConfirmed = false
}

function confirmSources() {
  if (webCandidates.value.length && !selectedCandidates.value.length) {
    ElMessage.warning('请选择已核验的制度正文来源')
    return
  }
  form.sourcesConfirmed = true
  ElMessage.success('来源已确认，可以开始寻租分析')
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
  if (!form.sourcesConfirmed) {
    ElMessage.warning('请先确认制度正文来源')
    return
  }
  analyzing.value = true
  try {
    result.value = await analyzeCaseApi({
      analysisType: 'CASE_ANALYSIS',
      caseTitle: form.caseTitle.trim(),
      caseDescription: form.caseDescription.trim(),
      regulationSetId: form.regulationSetId,
      jointAnalysis: false,
      externalDiscovery: false,
      relatedRegulationIds: form.relatedRegulationIds,
      sourceUrls: confirmedSourceUrls()
    })
  } finally {
    analyzing.value = false
  }
}

function clearSourceConfirmation() {
  webCandidates.value = []
  selectedCandidates.value = []
  sourceDiscoveryDone.value = false
  form.sourcesConfirmed = false
}

function sourceUrlList() {
  return form.sourceUrls
    .split(/\s+/)
    .map((item) => item.trim())
    .filter(Boolean)
}

function confirmedSourceUrls() {
  const urls = new Set(selectedCandidates.value.map((item) => item.url).filter(Boolean))
  if (!webCandidates.value.length) {
    sourceUrlList().forEach((url) => urls.add(url))
  }
  return Array.from(urls)
}

function reserveImport() {
  ElMessage.info('导入入口已预留，下一步接入正式导入流程')
}

function regulationLabel(item) {
  return item.code ? `${item.code} ${item.title}` : item.title
}

function defaultSelectedRegulationIds(items) {
  const preferred = items.filter((item) => item.code?.startsWith('CZ-FW-REG-'))
  return (preferred.length ? preferred : items).slice(0, 8).map((item) => item.id)
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
    RULE_ONLY: '规则兜底'
  }
  return labels[mode] || mode || '-'
}

onMounted(async () => {
  await loadSets()
  await loadRegulations()
})
</script>

<style scoped>
.flow-steps {
  margin-bottom: 18px;
}

.analysis-action-row {
  width: 100%;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.source-review-panel {
  margin-top: 16px;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f8fafc;
}

.source-review-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}
</style>
