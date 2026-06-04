<template>
  <section class="panel">
    <div class="panel-header">
      <h2>AI 分析记录</h2>
      <el-button :icon="Refresh" @click="loadRecords">刷新</el-button>
    </div>

    <el-form class="toolbar" :inline="true" :model="query">
      <el-form-item label="场景">
          <el-select v-model="query.sceneCode" clearable placeholder="全部" style="width: 190px">
            <el-option label="冲突说明" value="CONFLICT_EXPLANATION" />
          <el-option label="案例扩展分析" value="CASE_RENT_SEEKING_ANALYSIS" />
          <el-option label="制度问答" value="REGULATION_QA" />
          <el-option label="寻租风险识别" value="RENT_SEEKING_RISK" />
            <el-option label="制度摘要" value="REGULATION_SUMMARY" />
          </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable placeholder="全部" style="width: 120px">
          <el-option label="成功" value="SUCCESS" />
          <el-option label="失败" value="FAILED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="loadRecords">查询</el-button>
        <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="records" row-key="id">
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column prop="sceneCode" label="场景" min-width="170" />
      <el-table-column prop="businessType" label="业务类型" min-width="130" />
      <el-table-column prop="businessId" label="业务ID" width="110" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="responseText" label="响应摘要" min-width="300" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="创建时间" min-width="170" />
    </el-table>

    <div class="pagination-row">
      <el-pagination
        v-model:current-page="query.pageNo"
        v-model:page-size="query.pageSize"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @change="loadRecords"
      />
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { getAiRecords } from '../api/ai'

const loading = ref(false)
const records = ref([])
const total = ref(0)
const query = reactive({ sceneCode: '', status: '', pageNo: 1, pageSize: 10 })

async function loadRecords() {
  loading.value = true
  try {
    const data = await getAiRecords(query)
    records.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.sceneCode = ''
  query.status = ''
  query.pageNo = 1
  loadRecords()
}

onMounted(loadRecords)
</script>
