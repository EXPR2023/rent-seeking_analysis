<template>
  <section class="panel">
    <div class="panel-header">
      <h2>操作日志</h2>
      <el-button :icon="Refresh" @click="loadLogs">刷新</el-button>
    </div>
    <el-form class="toolbar" :inline="true" :model="query">
      <el-form-item label="模块">
        <el-input v-model="query.moduleName" clearable placeholder="模块名" />
      </el-form-item>
      <el-form-item label="账号">
        <el-input v-model="query.username" clearable placeholder="操作账号" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadLogs">查询</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="logs" row-key="id">
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column prop="username" label="账号" min-width="120" />
      <el-table-column prop="moduleName" label="模块" min-width="150" />
      <el-table-column prop="operationType" label="操作" min-width="150" />
      <el-table-column prop="requestMethod" label="方法" width="90" />
      <el-table-column prop="requestUri" label="URI" min-width="260" show-overflow-tooltip />
      <el-table-column prop="resultCode" label="结果" width="120" />
      <el-table-column prop="createdAt" label="时间" min-width="170" />
    </el-table>
    <div class="pagination-row">
      <el-pagination
        v-model:current-page="query.pageNo"
        v-model:page-size="query.pageSize"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @change="loadLogs"
      />
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { getAuditLogs } from '../api/system'

const loading = ref(false)
const logs = ref([])
const total = ref(0)
const query = reactive({ moduleName: '', username: '', pageNo: 1, pageSize: 10 })

async function loadLogs() {
  loading.value = true
  try {
    const data = await getAuditLogs(query)
    logs.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

onMounted(loadLogs)
</script>
