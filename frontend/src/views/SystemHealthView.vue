<template>
  <section class="panel">
    <div class="panel-header">
      <h2>健康检查</h2>
      <el-button :icon="Refresh" @click="loadHealth">刷新</el-button>
    </div>
    <el-skeleton v-if="loading || !health" :rows="5" animated />
    <template v-else>
      <div class="metric-band">
        <div v-for="item in statusItems" :key="item.label" class="metric-item">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </div>
      <el-descriptions :column="1" border class="detail-descriptions">
        <el-descriptions-item v-for="(value, key) in health.details" :key="key" :label="key">
          {{ value }}
        </el-descriptions-item>
      </el-descriptions>
    </template>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { getHealth } from '../api/system'

const loading = ref(false)
const health = ref(null)
const statusItems = computed(() => [
  { label: 'Backend', value: health.value.backend },
  { label: 'MySQL', value: health.value.mysql },
  { label: 'Redis', value: health.value.redis },
  { label: 'AI', value: health.value.ai }
])

async function loadHealth() {
  loading.value = true
  try {
    health.value = await getHealth()
  } finally {
    loading.value = false
  }
}

onMounted(loadHealth)
</script>
