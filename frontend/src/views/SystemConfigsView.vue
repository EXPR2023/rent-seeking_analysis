<template>
  <section class="panel">
    <div class="panel-header">
      <h2>LLM 配置</h2>
      <el-button :icon="Refresh" @click="loadConfigs">刷新</el-button>
    </div>

    <el-table v-loading="loading" :data="aiConfigs" row-key="configKey">
      <el-table-column prop="configKey" label="参数键" min-width="220" />
      <el-table-column label="参数值" min-width="300" show-overflow-tooltip>
        <template #default="{ row }">
          {{ displayValue(row) }}
        </template>
      </el-table-column>
      <el-table-column prop="description" label="说明" min-width="300" />
      <el-table-column prop="updatedAt" label="更新时间" min-width="170" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
  </section>

  <section class="panel">
    <div class="panel-header">
      <h2>Embedding / RAG 配置</h2>
    </div>
    <el-table v-loading="loading" :data="embeddingConfigs" row-key="configKey">
      <el-table-column prop="configKey" label="参数键" min-width="240" />
      <el-table-column label="参数值" min-width="300" show-overflow-tooltip>
        <template #default="{ row }">
          {{ displayValue(row) }}
        </template>
      </el-table-column>
      <el-table-column prop="description" label="说明" min-width="320" />
      <el-table-column prop="updatedAt" label="更新时间" min-width="170" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
  </section>

  <section class="panel">
    <div class="panel-header">
      <h2>系统参数</h2>
    </div>
    <el-table v-loading="loading" :data="generalConfigs" row-key="configKey">
      <el-table-column prop="configKey" label="参数键" min-width="220" />
      <el-table-column prop="configValue" label="参数值" min-width="260" show-overflow-tooltip />
      <el-table-column prop="description" label="说明" min-width="260" />
      <el-table-column prop="updatedAt" label="更新时间" min-width="170" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
  </section>

  <el-dialog v-model="dialogVisible" title="编辑系统参数" width="520px">
    <el-form label-width="84px">
      <el-form-item label="参数键">
        <el-input :model-value="editing?.configKey" disabled />
      </el-form-item>
      <el-form-item label="参数值">
        <el-input
          v-model="configValue"
          :type="editing?.sensitive ? 'password' : 'textarea'"
          :rows="editing?.sensitive ? undefined : 4"
          :placeholder="editing?.sensitive ? '留空保持原 API Key 不变' : ''"
          maxlength="1000"
          show-word-limit
          show-password
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { getSystemConfigs, updateSystemConfig } from '../api/system'

const loading = ref(false)
const saving = ref(false)
const configs = ref([])
const editing = ref(null)
const configValue = ref('')
const dialogVisible = ref(false)
const aiConfigKeys = ['ai.enabled', 'ai.provider', 'ai.base-url', 'ai.api-key', 'ai.model', 'ai.timeout']
const embeddingConfigKeys = [
  'embedding.provider',
  'embedding.base-url',
  'embedding.api-key',
  'embedding.model',
  'embedding.dimension',
  'embedding.timeout',
  'rag.enabled',
  'rag.top-k',
  'rag.similarity.threshold',
  'rag.chunk-size'
]
const aiConfigs = computed(() => configs.value.filter((item) => aiConfigKeys.includes(item.configKey)))
const embeddingConfigs = computed(() => configs.value.filter((item) => embeddingConfigKeys.includes(item.configKey)))
const generalConfigs = computed(() =>
  configs.value.filter((item) => !aiConfigKeys.includes(item.configKey) && !embeddingConfigKeys.includes(item.configKey))
)

async function loadConfigs() {
  loading.value = true
  try {
    configs.value = await getSystemConfigs()
  } finally {
    loading.value = false
  }
}

function openEdit(row) {
  editing.value = row
  configValue.value = row.sensitive ? '' : row.configValue || ''
  dialogVisible.value = true
}

function displayValue(row) {
  if (row.sensitive) {
    return row.configValue || '未配置'
  }
  return row.configValue || '-'
}

async function save() {
  saving.value = true
  try {
    await updateSystemConfig(editing.value.configKey, configValue.value)
    ElMessage.success('参数已更新')
    dialogVisible.value = false
    await loadConfigs()
  } finally {
    saving.value = false
  }
}

onMounted(loadConfigs)
</script>
