<template>
  <section class="panel">
    <div class="panel-header">
      <h2>角色权限</h2>
      <el-button :icon="Refresh" @click="loadRoles">刷新</el-button>
    </div>
    <el-table v-loading="loading" :data="roles" row-key="id">
      <el-table-column prop="roleName" label="角色名称" min-width="150" />
      <el-table-column prop="roleCode" label="角色编码" min-width="180" />
      <el-table-column prop="description" label="说明" min-width="260" show-overflow-tooltip />
      <el-table-column label="状态" width="110">
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
import { getRoles } from '../api/users'

const roles = ref([])
const loading = ref(false)

async function loadRoles() {
  loading.value = true
  try {
    roles.value = await getRoles()
  } finally {
    loading.value = false
  }
}

onMounted(loadRoles)
</script>
