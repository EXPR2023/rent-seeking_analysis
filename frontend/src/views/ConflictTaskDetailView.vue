<template>
  <section class="panel">
    <div class="panel-header">
      <h2>冲突详情 #{{ detail?.id }}</h2>
      <el-button :icon="ArrowLeft" @click="router.push('/conflicts/tasks')">返回</el-button>
    </div>

    <el-skeleton v-if="loading || !detail" :rows="8" animated />
    <template v-else>
      <el-descriptions :column="3" border class="detail-descriptions">
        <el-descriptions-item label="主制度">{{ detail.mainRegulationTitle }}</el-descriptions-item>
        <el-descriptions-item label="任务状态">
          <el-tag :type="tagTypeOf(taskStatuses, detail.status)">{{ labelOf(taskStatuses, detail.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="冲突项">{{ detail.totalItems }}</el-descriptions-item>
        <el-descriptions-item label="创建人">{{ detail.createdByName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detail.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="完成时间">{{ detail.finishedAt || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-table :data="detail.items" row-key="id">
        <el-table-column label="类型" width="130">
          <template #default="{ row }">{{ labelOf(conflictTypes, row.conflictType) }}</template>
        </el-table-column>
        <el-table-column label="等级" width="90">
          <template #default="{ row }">
            <el-tag :type="tagTypeOf(conflictLevels, row.conflictLevel)">
              {{ labelOf(conflictLevels, row.conflictLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="compareRegulationTitle" label="对比制度" min-width="180" show-overflow-tooltip />
        <el-table-column prop="explanation" label="冲突说明" min-width="260" show-overflow-tooltip />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="tagTypeOf(conflictStatuses, row.status)">
              {{ labelOf(conflictStatuses, row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="row.status === 'CONFIRMED'" @click="review(row, 'confirm')">
              确认
            </el-button>
            <el-button link type="warning" :disabled="row.status === 'IGNORED'" @click="review(row, 'ignore')">
              忽略
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { confirmConflictItem, getConflictTask, ignoreConflictItem } from '../api/conflicts'
import {
  conflictLevels,
  conflictStatuses,
  conflictTypes,
  labelOf,
  tagTypeOf,
  taskStatuses
} from '../constants/conflicts'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const detail = ref(null)

async function loadDetail() {
  loading.value = true
  try {
    detail.value = await getConflictTask(route.params.id)
  } finally {
    loading.value = false
  }
}

async function review(row, action) {
  const verb = action === 'confirm' ? '确认' : '忽略'
  const { value } = await ElMessageBox.prompt(`填写${verb}备注`, `${verb}冲突项`, {
    inputType: 'textarea',
    inputPlaceholder: '可留空'
  })
  if (action === 'confirm') {
    await confirmConflictItem(row.id, value || '')
  } else {
    await ignoreConflictItem(row.id, value || '')
  }
  ElMessage.success(`已${verb}`)
  await loadDetail()
}

onMounted(loadDetail)
</script>
