<template>
  <section class="panel">
    <div class="panel-header">
      <h2>字典管理</h2>
      <el-button :icon="Refresh" @click="loadDicts">刷新</el-button>
    </div>
    <el-form class="toolbar" :inline="true">
      <el-form-item label="字典类型">
        <el-input v-model="dictType" clearable placeholder="例如 RISK_LEVEL" @keyup.enter="loadDicts" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadDicts">查询</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="dicts" row-key="id">
      <el-table-column prop="dictType" label="类型" min-width="180" />
      <el-table-column prop="itemCode" label="编码" min-width="180" />
      <el-table-column prop="itemName" label="名称" min-width="180" />
      <el-table-column prop="sortOrder" label="排序" width="90" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { getDicts } from '../api/system'

const loading = ref(false)
const dicts = ref([])
const dictType = ref('')

async function loadDicts() {
  loading.value = true
  try {
    dicts.value = await getDicts({ dictType: dictType.value })
  } finally {
    loading.value = false
  }
}

onMounted(loadDicts)
</script>
