<template>
  <section class="panel">
    <div class="panel-header">
      <h2>报告预览</h2>
      <el-button type="primary" :loading="loading" @click="loadReport">生成预览</el-button>
    </div>
    <el-form class="toolbar" :inline="true">
      <el-form-item label="制度">
        <el-select v-model="regulationId" filterable placeholder="选择制度" style="width: 360px">
          <el-option v-for="item in regulations" :key="item.id" :label="item.title" :value="item.id" />
        </el-select>
      </el-form-item>
    </el-form>

    <el-empty v-if="!report" description="请选择制度生成报告" />
    <section v-else class="report-preview">
      <h2>{{ report.reportName }}</h2>
      <p class="muted">报告记录：#{{ report.reportId }} ｜ 生成时间：{{ report.generatedAt }}</p>
      <p>{{ report.reportSummary }}</p>
      <h3>制度信息</h3>
      <p>{{ report.regulation.title }}，状态 {{ report.regulation.status }}，分析状态 {{ report.regulation.analysisStatus }}。</p>
      <h3>风险分析</h3>
      <p v-if="report.latestRiskAnalysis">
        风险等级 {{ report.latestRiskAnalysis.riskLevel }}，分数 {{ report.latestRiskAnalysis.riskScore }}。
        {{ report.latestRiskAnalysis.overallSuggestion }}
      </p>
      <p v-else>暂无风险分析结果。</p>
      <h3>冲突检测</h3>
      <el-table :data="report.conflictItems" size="small">
        <el-table-column prop="conflictType" label="类型" width="150" />
        <el-table-column prop="conflictLevel" label="等级" width="100" />
        <el-table-column prop="status" label="状态" width="110" />
        <el-table-column prop="explanation" label="说明" min-width="260" show-overflow-tooltip />
      </el-table>
    </section>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getRegulations } from '../api/regulations'
import { getRegulationReport } from '../api/reports'

const regulations = ref([])
const regulationId = ref(null)
const report = ref(null)
const loading = ref(false)

async function loadRegulations() {
  const data = await getRegulations({ pageNo: 1, pageSize: 100 })
  regulations.value = data.records
  regulationId.value = data.records[0]?.id || null
}

async function loadReport() {
  if (!regulationId.value) {
    ElMessage.warning('请选择制度')
    return
  }
  loading.value = true
  try {
    report.value = await getRegulationReport(regulationId.value)
  } finally {
    loading.value = false
  }
}

onMounted(loadRegulations)
</script>
