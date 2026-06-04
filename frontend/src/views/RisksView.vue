<template>
  <section class="risk-center">
    <section class="risk-hero-panel">
      <div>
        <p class="eyebrow">旗舰功能</p>
        <h2>寻租分析中心</h2>
        <p>聚合规则兜底、LLM 增强和证据链结果，优先处理高风险制度与待复核事项。</p>
      </div>
      <div class="risk-hero-stats">
        <div>
          <span>分析记录</span>
          <strong>{{ total }}</strong>
        </div>
        <div>
          <span>高风险</span>
          <strong>{{ highRiskCount }}</strong>
        </div>
        <div>
          <span>LLM 增强</span>
          <strong>{{ llmCount }}</strong>
        </div>
        <div>
          <span>证据引用</span>
          <strong>{{ evidenceCount }}</strong>
        </div>
      </div>
    </section>

    <section class="panel">
      <div class="panel-header">
        <h2>分析任务与结果</h2>
        <el-button :icon="Refresh" @click="loadAnalyses">刷新</el-button>
      </div>

      <el-form class="toolbar" :inline="true" :model="query">
        <el-form-item label="风险等级">
          <el-select v-model="query.riskLevel" clearable placeholder="全部" style="width: 140px">
            <el-option v-for="item in riskLevels" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="复核状态">
          <el-select v-model="query.reviewStatus" clearable placeholder="全部" style="width: 140px">
            <el-option v-for="item in reviewStatuses" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="loadAnalyses">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="records" row-key="id">
        <el-table-column prop="id" label="分析ID" width="90" />
        <el-table-column prop="regulationTitle" label="制度标题" min-width="240" show-overflow-tooltip />
        <el-table-column label="分析模式" width="120">
          <template #default="{ row }">
            <el-tag :type="tagTypeOf(analysisModes, row.analysisMode)">
              {{ labelOf(analysisModes, row.analysisMode) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="风险等级" width="110">
          <template #default="{ row }">
            <el-tag :type="tagTypeOf(riskLevels, row.riskLevel)">{{ labelOf(riskLevels, row.riskLevel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="riskScore" label="分数" width="80" />
        <el-table-column label="证据" width="90">
          <template #default="{ row }">{{ row.evidenceCount || 0 }} 条</template>
        </el-table-column>
        <el-table-column label="置信度" width="100">
          <template #default="{ row }">{{ formatPercent(row.confidence) }}</template>
        </el-table-column>
        <el-table-column label="复核状态" width="110">
          <template #default="{ row }">
            <el-tag :type="tagTypeOf(reviewStatuses, row.reviewStatus)">
              {{ labelOf(reviewStatuses, row.reviewStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="170" />
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push(`/risks/${row.id}`)">进入工作台</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-row">
        <el-pagination
          v-model:current-page="query.pageNo"
          v-model:page-size="query.pageSize"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @change="loadAnalyses"
        />
      </div>
    </section>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh, Search } from '@element-plus/icons-vue'
import { getRiskAnalyses } from '../api/risks'
import { analysisModes, labelOf, reviewStatuses, riskLevels, tagTypeOf } from '../constants/risks'

const router = useRouter()
const loading = ref(false)
const records = ref([])
const total = ref(0)
const query = reactive({ riskLevel: '', reviewStatus: '', pageNo: 1, pageSize: 10 })
const highRiskCount = computed(() => records.value.filter((item) => ['HIGH', 'CRITICAL'].includes(item.riskLevel)).length)
const llmCount = computed(() => records.value.filter((item) => item.analysisMode === 'LLM_ENHANCED').length)
const evidenceCount = computed(() => records.value.reduce((sum, item) => sum + (item.evidenceCount || 0), 0))

async function loadAnalyses() {
  loading.value = true
  try {
    const data = await getRiskAnalyses(query)
    records.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.riskLevel = ''
  query.reviewStatus = ''
  query.pageNo = 1
  loadAnalyses()
}

function formatPercent(value) {
  if (value === null || value === undefined) return '-'
  return `${Math.round(Number(value) * 100)}%`
}

onMounted(loadAnalyses)
</script>
