<template>
  <section class="dashboard-grid">
    <section class="risk-hero-panel">
      <div>
        <p class="eyebrow">寻租分析旗舰工作区</p>
        <h2>优先发现制度设计中的寻租风险</h2>
        <p>从制度正文、冲突线索、规则命中和 LLM/RAG 增强结果中汇聚风险结论，推动复核与整改闭环。</p>
        <div class="risk-hero-actions">
          <el-button type="primary" @click="router.push('/case-analysis')">进入案例分析</el-button>
          <el-button @click="router.push('/regulation-iteration-analysis')">进入规章迭代分析</el-button>
          <el-button @click="router.push('/regulations')">管理规章集</el-button>
        </div>
      </div>
      <div class="risk-hero-stats">
        <div>
          <span>已分析制度</span>
          <strong>{{ summary.analyzedRegulationTotal ?? '-' }}</strong>
        </div>
        <div>
          <span>高风险制度</span>
          <strong>{{ summary.highRiskTotal ?? '-' }}</strong>
        </div>
        <div>
          <span>整改建议</span>
          <strong>{{ summary.suggestionTotal ?? '-' }}</strong>
        </div>
        <div>
          <span>待确认冲突</span>
          <strong>{{ summary.pendingConflictTotal ?? '-' }}</strong>
        </div>
      </div>
    </section>
    <div class="metric-band">
      <div v-for="item in metrics" :key="item.label" class="metric-item">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </div>
    </div>
    <div class="chart-grid">
      <section class="panel">
        <div class="panel-header">
          <h2>风险等级分布</h2>
        </div>
        <div ref="riskChartRef" class="chart-box"></div>
      </section>
      <section class="panel">
        <div class="panel-header">
          <h2>冲突状态分布</h2>
        </div>
        <div ref="conflictChartRef" class="chart-box"></div>
      </section>
    </div>
    <section class="panel">
      <div class="panel-header">
        <h2>当前菜单</h2>
      </div>
      <el-table :data="menus" row-key="id" default-expand-all>
        <el-table-column prop="menuName" label="菜单" min-width="160" />
        <el-table-column prop="menuCode" label="编码" min-width="160" />
        <el-table-column prop="path" label="路径" min-width="180" />
      </el-table>
    </section>
  </section>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { getConflictStatusChart, getDashboardSummary, getRiskLevelChart } from '../api/dashboard'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const summary = ref({})
const riskChartRef = ref()
const conflictChartRef = ref()
const menus = computed(() => auth.menus || [])
const metrics = computed(() => [
  { label: '制度总数', value: summary.value.regulationTotal ?? '-' },
  { label: '已分析制度', value: summary.value.analyzedRegulationTotal ?? '-' },
  { label: '冲突总数', value: summary.value.conflictTotal ?? '-' },
  { label: '待确认冲突', value: summary.value.pendingConflictTotal ?? '-' },
  { label: '高风险制度', value: summary.value.highRiskTotal ?? '-' },
  { label: '整改建议', value: summary.value.suggestionTotal ?? '-' }
])

function countMenus(menus = []) {
  return menus.reduce((sum, menu) => sum + 1 + countMenus(menu.children), 0)
}

function renderPie(target, title, data) {
  const chart = echarts.init(target)
  chart.setOption({
    tooltip: { trigger: 'item' },
    series: [
      {
        name: title,
        type: 'pie',
        radius: ['42%', '70%'],
        data: data.map((item) => ({ name: item.name, value: item.value }))
      }
    ]
  })
}

onMounted(async () => {
  const [summaryData, riskLevels, conflictStatus] = await Promise.all([
    getDashboardSummary(),
    getRiskLevelChart(),
    getConflictStatusChart()
  ])
  summary.value = summaryData
  await nextTick()
  renderPie(riskChartRef.value, '风险等级', riskLevels)
  renderPie(conflictChartRef.value, '冲突状态', conflictStatus)
})
</script>
