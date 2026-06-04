<template>
  <section class="detail-layout">
    <section class="panel">
      <div class="panel-header">
        <h2>寻租分析工作台 #{{ detail?.id }}</h2>
        <div class="header-actions">
          <el-button :icon="ArrowLeft" @click="router.push('/risks')">返回</el-button>
          <el-button type="primary" :loading="saving" @click="openReview">复核</el-button>
        </div>
      </div>

      <el-skeleton v-if="loading || !detail" :rows="8" animated />
      <template v-else>
        <div class="risk-score-band risk-score-band-wide">
          <div>
            <span>分析模式</span>
            <strong>
              <el-tag size="large" :type="tagTypeOf(analysisModes, detail.analysisMode)">
                {{ labelOf(analysisModes, detail.analysisMode) }}
              </el-tag>
            </strong>
          </div>
          <div>
            <span>风险等级</span>
            <strong>
              <el-tag size="large" :type="tagTypeOf(riskLevels, detail.riskLevel)">
                {{ labelOf(riskLevels, detail.riskLevel) }}
              </el-tag>
            </strong>
          </div>
          <div>
            <span>风险分数</span>
            <strong>{{ detail.riskScore }}</strong>
          </div>
          <div>
            <span>复核状态</span>
            <strong>
              <el-tag size="large" :type="tagTypeOf(reviewStatuses, detail.reviewStatus)">
                {{ labelOf(reviewStatuses, detail.reviewStatus) }}
              </el-tag>
            </strong>
          </div>
          <div>
            <span>证据引用</span>
            <strong>{{ detail.evidenceCount || 0 }}</strong>
          </div>
          <div>
            <span>置信度</span>
            <strong>{{ formatPercent(detail.confidence) }}</strong>
          </div>
        </div>

        <el-descriptions :column="2" border class="detail-descriptions">
          <el-descriptions-item label="制度标题">{{ detail.regulationTitle }}</el-descriptions-item>
          <el-descriptions-item label="模型">{{ detail.modelName || 'local-rule-engine' }}</el-descriptions-item>
          <el-descriptions-item label="提示词版本">{{ detail.promptVersion || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建人">{{ detail.createdByName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ detail.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ detail.updatedAt }}</el-descriptions-item>
          <el-descriptions-item label="复核备注" :span="2">{{ detail.reviewComment || '-' }}</el-descriptions-item>
        </el-descriptions>

        <section class="risk-section">
          <h3>制度摘要</h3>
          <p>{{ detail.summary }}</p>
        </section>
        <section class="risk-section">
          <h3>总体建议</h3>
          <p>{{ detail.overallSuggestion }}</p>
        </section>

        <el-table :data="detail.items" row-key="id">
          <el-table-column label="来源" width="110">
            <template #default="{ row }">
              <el-tag :type="tagTypeOf(sourceTraces, row.sourceTrace)">
                {{ labelOf(sourceTraces, row.sourceTrace) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="指标" min-width="150">
            <template #default="{ row }">{{ labelOf(riskIndicators, row.indicatorCode) }}</template>
          </el-table-column>
          <el-table-column prop="title" label="风险点" min-width="180" />
          <el-table-column label="等级" width="110">
            <template #default="{ row }">
              <el-tag :type="tagTypeOf(riskLevels, row.riskLevel)">{{ labelOf(riskLevels, row.riskLevel) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="reason" label="原因" min-width="230" show-overflow-tooltip />
          <el-table-column prop="suggestion" label="建议" min-width="260" show-overflow-tooltip />
          <el-table-column prop="relatedClause" label="相关条款" min-width="240" show-overflow-tooltip />
          <el-table-column label="证据" width="90">
            <template #default="{ row }">{{ row.evidenceCount || 0 }} 条</template>
          </el-table-column>
          <el-table-column label="整改" width="110">
            <template #default="{ row }">
              <el-tag :type="tagTypeOf(rectificationStatuses, row.rectificationStatus)">
                {{ labelOf(rectificationStatuses, row.rectificationStatus) }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>

        <section class="risk-section evidence-section">
          <h3>RAG 证据链</h3>
          <el-empty v-if="!detail.evidences?.length" description="暂无证据引用" />
          <div v-else class="evidence-list">
            <article v-for="evidence in detail.evidences" :key="evidence.id" class="evidence-card">
              <div class="evidence-title">
                <el-tag size="small">#{{ evidence.citationNo }}</el-tag>
                <strong>{{ evidence.title }}</strong>
                <span>{{ evidence.sourceType }}</span>
              </div>
              <p>{{ evidence.snippet }}</p>
            </article>
          </div>
        </section>
      </template>
    </section>

    <aside class="panel side-panel">
      <h2>关联入口</h2>
      <el-button :icon="Document" @click="router.push(`/regulations/${detail?.regulationId}`)" :disabled="!detail">
        查看制度
      </el-button>
    </aside>
  </section>

  <el-dialog v-model="dialogVisible" title="风险复核" width="460px">
    <el-form :model="reviewForm" label-width="92px">
      <el-form-item label="复核状态">
        <el-select v-model="reviewForm.reviewStatus">
          <el-option v-for="item in reviewStatuses" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="复核备注">
        <el-input v-model="reviewForm.reviewComment" type="textarea" :rows="4" maxlength="500" show-word-limit />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="saveReview">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Document } from '@element-plus/icons-vue'
import { getRiskAnalysis, reviewRiskAnalysis } from '../api/risks'
import {
  analysisModes,
  labelOf,
  rectificationStatuses,
  reviewStatuses,
  riskIndicators,
  riskLevels,
  sourceTraces,
  tagTypeOf
} from '../constants/risks'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const detail = ref(null)
const reviewForm = reactive({ reviewStatus: 'CONFIRMED', reviewComment: '' })

async function loadDetail() {
  loading.value = true
  try {
    detail.value = await getRiskAnalysis(route.params.id)
  } finally {
    loading.value = false
  }
}

function openReview() {
  reviewForm.reviewStatus = detail.value?.reviewStatus || 'CONFIRMED'
  reviewForm.reviewComment = detail.value?.reviewComment || ''
  dialogVisible.value = true
}

async function saveReview() {
  saving.value = true
  try {
    detail.value = await reviewRiskAnalysis(route.params.id, reviewForm)
    ElMessage.success('复核已保存')
    dialogVisible.value = false
  } finally {
    saving.value = false
  }
}

function formatPercent(value) {
  if (value === null || value === undefined) return '-'
  return `${Math.round(Number(value) * 100)}%`
}

onMounted(loadDetail)
</script>
