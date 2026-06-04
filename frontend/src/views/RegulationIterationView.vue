<template>
  <section class="case-analysis-layout">
    <section class="panel case-input-panel">
      <div class="panel-header">
        <h2>规章迭代分析</h2>
        <el-button :icon="Refresh" @click="refreshAll">刷新规章集</el-button>
      </div>

      <el-steps :active="activeStep" finish-status="success" simple class="flow-steps">
        <el-step title="导入规章" />
        <el-step title="内部分析" />
        <el-step title="确认措施" />
        <el-step title="外部来源" />
        <el-step title="二轮匹配" />
      </el-steps>

      <el-form label-width="112px">
        <el-form-item label="主规章来源">
          <el-radio-group v-model="form.primarySourceMode" @change="handlePrimaryModeChange">
            <el-radio-button value="SET">已有规章集</el-radio-button>
            <el-radio-button value="URL">可信法规 URL</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <template v-if="form.primarySourceMode === 'SET'">
          <el-form-item label="主规章集">
            <el-select v-model="form.regulationSetId" filterable placeholder="选择第一轮分析对象" @change="handleSetChange">
              <el-option v-for="item in regulationSets" :key="item.id" :label="setLabel(item)" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="规章正文">
            <el-select
              v-model="form.relatedRegulationIds"
              multiple
              filterable
              collapse-tags
              collapse-tags-tooltip
              placeholder="选择重点规章，可留空使用规章集概览"
            >
              <el-option v-for="item in regulations" :key="item.id" :label="regulationLabel(item)" :value="item.id" />
            </el-select>
          </el-form-item>
        </template>

        <template v-else>
          <el-form-item label="可信 URL">
            <el-input v-model="form.trustedRegulationUrl" placeholder="粘贴有置信来源的法规 URL" />
          </el-form-item>
          <el-form-item>
            <div class="analysis-action-row">
              <el-button type="primary" plain :icon="Search" :loading="primaryDiscovering" @click="verifyPrimaryUrl">
                核验 URL 正文
              </el-button>
              <el-button :disabled="!canConfirmPrimaryUrl" @click="confirmPrimaryUrl">确认主规章来源</el-button>
            </div>
          </el-form-item>
          <section v-if="primaryCandidates.length" class="source-review-panel">
            <div class="source-review-title">
              <strong>主规章 URL 核验</strong>
              <el-tag :type="form.primaryUrlConfirmed ? 'success' : 'warning'">
                {{ form.primaryUrlConfirmed ? '已确认' : '待确认' }}
              </el-tag>
            </div>
            <el-table
              ref="primaryTableRef"
              :data="primaryCandidates"
              size="small"
              border
              max-height="240"
              @selection-change="handlePrimarySelection"
            >
              <el-table-column type="selection" width="42" />
              <el-table-column prop="title" label="标题" min-width="210" show-overflow-tooltip />
              <el-table-column prop="domain" label="域名" width="132" show-overflow-tooltip />
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
        </template>

        <el-form-item label="分析说明">
          <el-input v-model="form.analysisNote" type="textarea" :rows="4" maxlength="1200" show-word-limit />
        </el-form-item>

        <el-form-item>
          <div class="analysis-action-row">
            <el-button type="primary" :icon="Search" :loading="internalAnalyzing" :disabled="!canRunInternal" @click="runInternalAnalysis">
              开始一轮内部分析
            </el-button>
            <el-button :disabled="!internalResult || internalConfirmed" @click="confirmInternalResult">确认一轮结果</el-button>
            <el-button :icon="Refresh" @click="resetFlow">重置流程</el-button>
          </div>
        </el-form-item>

        <template v-if="internalConfirmed">
          <el-divider />
          <el-form-item label="外部来源">
            <el-radio-group v-model="form.externalSourceMode" @change="handleExternalModeChange">
              <el-radio-button value="SET">指定规章集</el-radio-button>
              <el-radio-button value="WEB">联网检索公文</el-radio-button>
            </el-radio-group>
          </el-form-item>

          <template v-if="form.externalSourceMode === 'SET'">
            <el-form-item label="外部规章集">
              <el-select v-model="form.externalRegulationSetId" filterable placeholder="选择二轮排查来源">
                <el-option
                  v-for="item in externalSetOptions"
                  :key="item.id"
                  :label="setLabel(item)"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </template>

          <template v-else>
            <el-form-item label="检索词">
              <el-input v-model="form.externalQuery" placeholder="默认使用一轮制约措施生成检索线索" />
            </el-form-item>
            <el-form-item label="追加 URL">
              <el-input v-model="form.externalUrls" type="textarea" :rows="4" placeholder="每行一个外部公文或法律规章 URL" />
            </el-form-item>
            <el-form-item label="导入目标">
              <div class="import-target-row">
                <el-select v-model="form.externalImportRegulationSetId" filterable placeholder="导入到外部约束集（自动）">
                  <el-option label="联网发现外部约束集（自动）" :value="0" />
                  <el-option v-for="item in regulationSets" :key="item.id" :label="setLabel(item)" :value="item.id" />
                </el-select>
                <el-button :loading="importingExternal" :disabled="!externalSelected.length" @click="importSelectedExternalSources">
                  导入选中
                </el-button>
              </div>
            </el-form-item>
            <el-form-item>
              <div class="analysis-action-row">
                <el-button type="primary" plain :icon="Search" :loading="externalDiscovering" @click="discoverExternalSources">
                  检索外部公文
                </el-button>
                <el-button :disabled="!canConfirmExternalSources" @click="confirmExternalSources">确认外部来源</el-button>
              </div>
            </el-form-item>
            <section v-if="externalCandidates.length" class="source-review-panel">
              <div class="source-review-title">
                <div>
                  <strong>外部候选公文</strong>
                  <span v-if="rejectedExternalCandidates.length" class="muted">
                    已驳回 {{ rejectedExternalCandidates.length }} 条
                  </span>
                </div>
                <div class="source-review-actions">
                  <el-tag :type="form.externalSourcesConfirmed ? 'success' : 'warning'">
                    {{ form.externalSourcesConfirmed ? '已确认' : '待确认' }}
                  </el-tag>
                  <el-button size="small" plain type="danger" @click="rejectAllExternalCandidates">
                    驳回本次结果
                  </el-button>
                </div>
              </div>
              <el-table
                ref="externalTableRef"
                :data="externalCandidates"
                size="small"
                border
                max-height="300"
                @selection-change="handleExternalSelection"
              >
                <el-table-column type="selection" width="42" />
                <el-table-column prop="title" label="标题" min-width="210" show-overflow-tooltip />
                <el-table-column prop="domain" label="域名" width="132" show-overflow-tooltip />
                <el-table-column prop="fetchStatus" label="正文" width="92">
                  <template #default="{ row }">
                    <el-tag :type="fetchStatusType(row.fetchStatus)" size="small">{{ row.fetchStatus }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="contentLength" label="字数" width="78" />
                <el-table-column label="操作" width="178" fixed="right">
                  <template #default="{ row }">
                    <el-link :href="row.url" target="_blank" type="primary">打开</el-link>
                    <el-button
                      link
                      type="primary"
                      :loading="importingExternal"
                      :disabled="isImportedExternal(row)"
                      @click="importExternalCandidate(row)"
                    >
                      {{ isImportedExternal(row) ? '已导入' : '导入' }}
                    </el-button>
                    <el-button link type="danger" @click="rejectExternalCandidate(row)">驳回</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </section>
            <section v-else-if="externalDiscoveryDone" class="source-review-panel">
              <div class="source-review-title">
                <strong>外部候选公文</strong>
                <el-tag type="info">暂无可确认候选</el-tag>
              </div>
              <p class="source-review-empty">
                {{ rejectedExternalCandidates.length ? `本次检索结果已全部驳回（${rejectedExternalCandidates.length} 条）。` : '本次检索未发现候选公文。' }}
                可调整检索词或追加 URL 后重新检索。
              </p>
            </section>
          </template>

          <el-form-item>
            <el-button type="primary" :icon="Search" :loading="finalAnalyzing" :disabled="!canRunFinal" @click="runFinalAnalysis">
              开始二轮条文匹配
            </el-button>
          </el-form-item>
        </template>
      </el-form>
    </section>

    <section class="panel case-result-panel">
      <div class="panel-header">
        <h2>迭代分析结果</h2>
        <el-tag v-if="finalResult?.recordId || internalResult?.recordId" type="info">
          记录ID：{{ finalResult?.recordId || internalResult?.recordId }}
        </el-tag>
      </div>

      <el-empty v-if="!internalResult" description="暂无一轮分析结果" />
      <template v-else>
        <section class="risk-score-band risk-score-band-wide">
          <div>
            <span>风险等级</span>
            <strong><el-tag :type="riskTagType(currentResult.riskLevel)" size="large">{{ currentResult.riskLevel }}</el-tag></strong>
          </div>
          <div>
            <span>风险分数</span>
            <strong>{{ currentResult.riskScore ?? '-' }}</strong>
          </div>
          <div>
            <span>分析模式</span>
            <strong>{{ modeLabel(currentResult.analysisMode) }}</strong>
          </div>
          <div>
            <span>内部风险</span>
            <strong>{{ internalResult.internalRisks?.length || 0 }}</strong>
          </div>
          <div>
            <span>外部约束</span>
            <strong>{{ finalResult?.externalConstraints?.length || 0 }}</strong>
          </div>
        </section>

        <section class="risk-section">
          <h3>一轮摘要</h3>
          <p>{{ internalResult.summary }}</p>
        </section>

        <section v-if="internalResult.internalRisks?.length" class="risk-section">
          <h3>内部寻租风险</h3>
          <el-table :data="internalResult.internalRisks" size="small" border>
            <el-table-column prop="source" label="来源" width="110" />
            <el-table-column prop="riskType" label="类型" width="160" show-overflow-tooltip />
            <el-table-column prop="riskTitle" label="风险" min-width="220" show-overflow-tooltip />
            <el-table-column prop="mechanism" label="形成机制" min-width="320" show-overflow-tooltip />
            <el-table-column prop="relatedRegulations" label="关联规章" min-width="220" show-overflow-tooltip />
            <el-table-column prop="riskLevel" label="等级" width="96">
              <template #default="{ row }">
                <el-tag :type="riskTagType(row.riskLevel)">{{ row.riskLevel }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section v-if="internalResult.controlMeasures?.length" class="risk-section">
          <h3>制约措施</h3>
          <el-table :data="internalResult.controlMeasures" size="small" border>
            <el-table-column prop="riskTitle" label="对应风险" min-width="220" show-overflow-tooltip />
            <el-table-column prop="controlObjective" label="制约目标" min-width="220" show-overflow-tooltip />
            <el-table-column prop="measure" label="措施" min-width="320" show-overflow-tooltip />
            <el-table-column prop="evidenceNeed" label="需外部证明" min-width="240" show-overflow-tooltip />
            <el-table-column prop="externalSearchHint" label="检索线索" min-width="220" show-overflow-tooltip />
            <el-table-column prop="priority" label="优先级" width="96">
              <template #default="{ row }">
                <el-tag :type="riskTagType(row.priority)">{{ row.priority }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <template v-if="finalResult">
          <section class="risk-section">
            <h3>二轮摘要</h3>
            <p>{{ finalResult.jointSummary || finalResult.summary }}</p>
          </section>

          <section v-if="finalResult.externalConstraints?.length" class="risk-section">
            <h3>外部条文匹配</h3>
            <el-table :data="finalResult.externalConstraints" size="small" border>
              <el-table-column prop="documentTitle" label="外部制度" min-width="220" show-overflow-tooltip />
              <el-table-column prop="constraintType" label="约束类型" width="140" show-overflow-tooltip />
              <el-table-column prop="constrainsRisk" label="约束风险/措施" min-width="220" show-overflow-tooltip />
              <el-table-column prop="constraintMechanism" label="匹配机制" min-width="320" show-overflow-tooltip />
              <el-table-column prop="sufficiency" label="充分性" width="120" />
              <el-table-column prop="importAdvice" label="处理建议" min-width="260" show-overflow-tooltip />
            </el-table>
          </section>

          <section v-if="finalResult.closureGaps?.length" class="risk-section">
            <h3>仍未闭合缺口</h3>
            <el-table :data="finalResult.closureGaps" size="small" border>
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

          <section v-if="finalResult.fetchedSources?.length" class="risk-section">
            <h3>公开来源抓取</h3>
            <el-table :data="finalResult.fetchedSources" size="small" border>
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
                  <el-button
                    link
                    type="primary"
                    :loading="importingExternal"
                    :disabled="isImportedExternal(row)"
                    @click="importExternalCandidate(row)"
                  >
                    {{ isImportedExternal(row) ? '已导入' : '导入' }}
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </section>
        </template>

        <section v-if="currentResult.iterationTraces?.length" class="risk-section">
          <h3>迭代轨迹</h3>
          <el-table :data="currentResult.iterationTraces" size="small" border>
            <el-table-column prop="round" label="轮次" width="80" />
            <el-table-column prop="objective" label="目标" width="180" />
            <el-table-column prop="query" label="输入/查询" min-width="320" show-overflow-tooltip />
            <el-table-column prop="evidenceCount" label="证据数" width="90" />
            <el-table-column prop="stopReason" label="停止原因" min-width="260" show-overflow-tooltip />
          </el-table>
        </section>
      </template>
    </section>
  </section>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { analyzeCase as analyzeCaseApi, discoverWebSources, importWebSources } from '../api/ai'
import { getRegulations } from '../api/regulations'
import { getRegulationSets } from '../api/regulationSets'

const regulationSets = ref([])
const regulations = ref([])
const internalResult = ref(null)
const finalResult = ref(null)
const internalConfirmed = ref(false)
const internalAnalyzing = ref(false)
const finalAnalyzing = ref(false)
const primaryDiscovering = ref(false)
const externalDiscovering = ref(false)
const importingExternal = ref(false)
const primaryCandidates = ref([])
const externalCandidates = ref([])
const primarySelected = ref([])
const externalSelected = ref([])
const rejectedExternalCandidates = ref([])
const importedExternalUrls = ref([])
const primaryDiscoveryDone = ref(false)
const externalDiscoveryDone = ref(false)
const primaryTableRef = ref()
const externalTableRef = ref()

const form = reactive({
  primarySourceMode: 'SET',
  regulationSetId: null,
  relatedRegulationIds: [],
  trustedRegulationUrl: '',
  primaryUrlConfirmed: false,
  analysisNote: '先对主规章内部的授权、流程、裁量、公开、监督和利益分配条款进行寻租风险识别，再把风险转化为可被外部规章验证的制约措施。',
  externalSourceMode: 'SET',
  externalRegulationSetId: null,
  externalImportRegulationSetId: 0,
  externalQuery: '',
  externalUrls: '',
  externalSourcesConfirmed: false
})

const currentResult = computed(() => finalResult.value || internalResult.value || {})
const externalSetOptions = computed(() => regulationSets.value.filter((item) => item.id !== form.regulationSetId))
const activeStep = computed(() => {
  if (finalResult.value) return 5
  if (form.externalSourcesConfirmed || (form.externalSourceMode === 'SET' && form.externalRegulationSetId && internalConfirmed.value)) return 4
  if (internalConfirmed.value) return 3
  if (internalResult.value) return 2
  if (form.primarySourceMode === 'URL' && form.primaryUrlConfirmed) return 1
  return 0
})
const canConfirmPrimaryUrl = computed(() => primaryDiscoveryDone.value && primarySelected.value.length > 0)
const canRunInternal = computed(() => {
  if (form.primarySourceMode === 'SET') return Boolean(form.regulationSetId)
  return form.primaryUrlConfirmed && primarySelected.value.length > 0
})
const canConfirmExternalSources = computed(() => externalDiscoveryDone.value && externalCandidates.value.length > 0 && externalSelected.value.length > 0)
const canRunFinal = computed(() => {
  if (!internalConfirmed.value) return false
  if (form.externalSourceMode === 'SET') return Boolean(form.externalRegulationSetId)
  return form.externalSourcesConfirmed
})

async function loadSets() {
  regulationSets.value = await getRegulationSets({ enabledOnly: true })
  if (!form.regulationSetId) {
    form.regulationSetId = defaultSetId()
  }
  if (!form.externalRegulationSetId) {
    form.externalRegulationSetId = externalSetOptions.value[0]?.id || null
  }
}

async function loadRegulations() {
  if (!regulationSets.value.length) {
    await loadSets()
  }
  if (!form.regulationSetId) {
    regulations.value = []
    return
  }
  const data = await getRegulations({ pageNo: 1, pageSize: 100, status: 'ACTIVE', regulationSetId: form.regulationSetId })
  regulations.value = data.records
  if (!form.relatedRegulationIds.length) {
    form.relatedRegulationIds = data.records.slice(0, 12).map((item) => item.id)
  }
}

async function refreshAll() {
  await loadSets()
  await loadRegulations()
}

async function handleSetChange() {
  form.relatedRegulationIds = []
  resetResults()
  await loadRegulations()
  if (form.externalRegulationSetId === form.regulationSetId) {
    form.externalRegulationSetId = externalSetOptions.value[0]?.id || null
  }
}

function handlePrimaryModeChange() {
  resetResults()
  clearPrimaryUrlConfirmation()
}

function handleExternalModeChange() {
  finalResult.value = null
  clearExternalConfirmation()
}

async function verifyPrimaryUrl() {
  const urls = urlList(form.trustedRegulationUrl)
  if (!urls.length) {
    ElMessage.warning('请输入可信法规 URL')
    return
  }
  primaryDiscovering.value = true
  try {
    const data = await discoverWebSources({
      query: '用户提供法规 URL 核验',
      sourceUrls: urls,
      maxResults: Math.max(4, urls.length)
    })
    primaryCandidates.value = data.candidates || []
    primarySelected.value = []
    primaryDiscoveryDone.value = true
    form.primaryUrlConfirmed = false
    await nextTick()
    primaryCandidates.value
      .filter((item) => item.fetchStatus === 'FETCHED' && (item.contentLength || 0) >= 500)
      .slice(0, 2)
      .forEach((item) => primaryTableRef.value?.toggleRowSelection(item, true))
    ElMessage.success(`已核验 ${primaryCandidates.value.length} 条主规章来源`)
  } finally {
    primaryDiscovering.value = false
  }
}

function handlePrimarySelection(rows) {
  primarySelected.value = rows
  form.primaryUrlConfirmed = false
}

function confirmPrimaryUrl() {
  if (!primarySelected.value.length) {
    ElMessage.warning('请选择已核验的主规章来源')
    return
  }
  form.primaryUrlConfirmed = true
  ElMessage.success('主规章来源已确认')
}

async function runInternalAnalysis() {
  if (!canRunInternal.value) {
    ElMessage.warning('请先完成主规章来源确认')
    return
  }
  internalAnalyzing.value = true
  try {
    internalResult.value = await analyzeCaseApi(baseAnalysisPayload(false))
    finalResult.value = null
    internalConfirmed.value = false
    form.externalQuery = buildExternalQuery()
    ElMessage.success('一轮内部分析已完成')
  } finally {
    internalAnalyzing.value = false
  }
}

function confirmInternalResult() {
  if (!internalResult.value) {
    return
  }
  internalConfirmed.value = true
  form.externalQuery = buildExternalQuery()
  ElMessage.success('一轮结果已确认，可以选择外部来源')
}

async function discoverExternalSources() {
  if (!internalConfirmed.value) {
    ElMessage.warning('请先确认一轮结果')
    return
  }
  externalDiscovering.value = true
  try {
    const data = await discoverWebSources({
      query: form.externalQuery.trim() || buildExternalQuery(),
      caseTitle: primaryTitle(),
      caseDescription: controlMeasureText(),
      regulationSetId: form.regulationSetId,
      sourceUrls: urlList(form.externalUrls),
      maxResults: 30
    })
    externalCandidates.value = data.candidates || []
    externalSelected.value = []
    rejectedExternalCandidates.value = []
    importedExternalUrls.value = []
    externalDiscoveryDone.value = true
    form.externalSourcesConfirmed = false
    await nextTick()
    externalCandidates.value
      .filter((item) => item.fetchStatus === 'FETCHED' && item.officialSource && (item.contentLength || 0) >= 500)
      .slice(0, 6)
      .forEach((item) => externalTableRef.value?.toggleRowSelection(item, true))
    ElMessage.success(`发现 ${externalCandidates.value.length} 条外部候选公文`)
  } finally {
    externalDiscovering.value = false
  }
}

function handleExternalSelection(rows) {
  externalSelected.value = rows
  form.externalSourcesConfirmed = false
}

async function importExternalCandidate(row) {
  await importExternalSources([row])
}

async function importSelectedExternalSources() {
  if (!externalSelected.value.length) {
    ElMessage.warning('请选择需要导入的外部公文')
    return
  }
  await importExternalSources(externalSelected.value)
}

async function importExternalSources(rows) {
  const sources = (rows || []).filter((item) => item?.url)
  if (!sources.length) {
    ElMessage.warning('没有可导入的外部公文')
    return
  }
  importingExternal.value = true
  try {
    const data = await importWebSources({
      regulationSetId: form.externalImportRegulationSetId || null,
      sources: sources.map((item) => ({
        url: item.url,
        title: item.title,
        typeCode: item.suggestedTypeCode,
        publishDepartment: item.suggestedPublishDepartment
      }))
    })
    const successfulUrls = (data.results || [])
      .filter((item) => ['IMPORTED', 'SKIPPED'].includes(item.status))
      .map((item) => item.url)
      .filter(Boolean)
    importedExternalUrls.value = Array.from(new Set([...importedExternalUrls.value, ...successfulUrls]))
    await loadSets()
    ElMessage.success(`已导入 ${data.importedCount || 0} 条，跳过 ${data.skippedCount || 0} 条，失败 ${data.failedCount || 0} 条`)
  } finally {
    importingExternal.value = false
  }
}

function isImportedExternal(row) {
  return Boolean(row?.url && importedExternalUrls.value.includes(row.url))
}

function rejectExternalCandidate(row) {
  const url = row?.url
  if (!url) {
    return
  }
  rejectedExternalCandidates.value.push(row)
  externalCandidates.value = externalCandidates.value.filter((item) => item.url !== url)
  externalSelected.value = externalSelected.value.filter((item) => item.url !== url)
  form.externalSourcesConfirmed = false
  finalResult.value = null
  ElMessage.info('已驳回该候选公文')
}

function rejectAllExternalCandidates() {
  if (!externalCandidates.value.length) {
    return
  }
  rejectedExternalCandidates.value.push(...externalCandidates.value)
  externalCandidates.value = []
  externalSelected.value = []
  form.externalSourcesConfirmed = false
  finalResult.value = null
  ElMessage.info('已驳回本次外部检索结果')
}

function confirmExternalSources() {
  if (!externalCandidates.value.length || !externalSelected.value.length) {
    ElMessage.warning('请选择已核验的外部来源')
    return
  }
  form.externalSourcesConfirmed = true
  ElMessage.success('外部来源已确认')
}

async function runFinalAnalysis() {
  if (!canRunFinal.value) {
    ElMessage.warning('请先选择并确认外部来源')
    return
  }
  finalAnalyzing.value = true
  try {
    finalResult.value = await analyzeCaseApi(baseAnalysisPayload(true))
    ElMessage.success('二轮条文匹配已完成')
  } finally {
    finalAnalyzing.value = false
  }
}

function baseAnalysisPayload(externalDiscovery) {
  return {
    analysisType: 'REGULATION_ITERATION',
    caseTitle: primaryTitle(),
    caseDescription: iterationDescription(externalDiscovery),
    regulationSetId: form.primarySourceMode === 'SET' ? form.regulationSetId : null,
    externalRegulationSetId: externalDiscovery && form.externalSourceMode === 'SET' ? form.externalRegulationSetId : null,
    jointAnalysis: true,
    externalDiscovery,
    relatedRegulationIds: form.primarySourceMode === 'SET' ? form.relatedRegulationIds : [],
    sourceUrls: allConfirmedUrls(externalDiscovery)
  }
}

function iterationDescription(externalDiscovery) {
  const roundText = externalDiscovery
    ? `请进入第二轮：以以下一轮制约措施为线索，排查外部规章是否存在可匹配条文。\n${controlMeasureText()}`
    : '请只完成第一轮：先分析主规章内部寻租风险，并推理制约措施；完成后在一轮结果处停止。'
  return `${form.analysisNote}\n\n${roundText}`
}

function primaryTitle() {
  if (form.primarySourceMode === 'SET') {
    return `规章迭代分析：${setName(form.regulationSetId) || '主规章集'}`
  }
  return `规章迭代分析：${primarySelected.value[0]?.title || form.trustedRegulationUrl || '可信法规 URL'}`
}

function allConfirmedUrls(includeExternal) {
  const urls = new Set()
  if (form.primarySourceMode === 'URL') {
    primarySelected.value.map((item) => item.url).filter(Boolean).forEach((url) => urls.add(url))
  }
  if (includeExternal && form.externalSourceMode === 'WEB') {
    externalSelected.value.map((item) => item.url).filter(Boolean).forEach((url) => urls.add(url))
  }
  return Array.from(urls)
}

function buildExternalQuery() {
  const hints = internalResult.value?.controlMeasures?.map((item) => item.externalSearchHint).filter(Boolean) || []
  const risks = internalResult.value?.internalRisks?.map((item) => item.riskTitle).filter(Boolean) || []
  return [...hints, ...risks].join(' ')
}

function controlMeasureText() {
  const measures = internalResult.value?.controlMeasures || []
  if (measures.length) {
    return measures
      .map((item, index) => `${index + 1}. ${item.riskTitle}：${item.measure}；检索线索：${item.externalSearchHint}`)
      .join('\n')
  }
  const risks = internalResult.value?.riskItems || []
  return risks.map((item, index) => `${index + 1}. ${item.title}：${item.suggestion}`).join('\n')
}

function resetFlow() {
  resetResults()
  clearExternalConfirmation()
}

function resetResults() {
  internalResult.value = null
  finalResult.value = null
  internalConfirmed.value = false
}

function clearPrimaryUrlConfirmation() {
  primaryCandidates.value = []
  primarySelected.value = []
  primaryDiscoveryDone.value = false
  form.primaryUrlConfirmed = false
}

function clearExternalConfirmation() {
  externalCandidates.value = []
  externalSelected.value = []
  rejectedExternalCandidates.value = []
  importedExternalUrls.value = []
  externalDiscoveryDone.value = false
  form.externalSourcesConfirmed = false
}

function urlList(value) {
  return (value || '')
    .split(/\s+/)
    .map((item) => item.trim())
    .filter(Boolean)
}

function reserveImport() {
  ElMessage.info('导入入口已预留，下一步接入正式导入流程')
}

function regulationLabel(item) {
  return item.code ? `${item.code} ${item.title}` : item.title
}

function defaultSetId() {
  return regulationSets.value.find((item) => item.defaultSet && item.enabled)?.id || regulationSets.value.find((item) => item.enabled)?.id || null
}

function setName(id) {
  return regulationSets.value.find((item) => item.id === id)?.setName || ''
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

function fetchStatusType(status) {
  if (status === 'FETCHED') return 'success'
  if (status === 'FAILED') return 'danger'
  return 'warning'
}

function modeLabel(mode) {
  const labels = {
    ITERATION_RAG_ENHANCED: '迭代 RAG 增强',
    ITERATION_LLM_ENHANCED: '迭代 LLM 增强',
    ITERATION_RULE_ONLY: '迭代规则兜底',
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
.flow-steps {
  margin-bottom: 18px;
}

.analysis-action-row {
  width: 100%;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.import-target-row {
  width: 100%;
  display: grid;
  grid-template-columns: minmax(180px, 1fr) auto;
  gap: 8px;
  align-items: center;
}

.source-review-panel {
  margin: 0 0 18px 112px;
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

.source-review-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.source-review-empty {
  margin: 0;
  color: #475569;
  line-height: 1.7;
}

@media (max-width: 900px) {
  .source-review-panel {
    margin-left: 0;
  }

  .source-review-title {
    align-items: flex-start;
    flex-direction: column;
  }

  .import-target-row {
    grid-template-columns: 1fr;
  }
}
</style>
