<template>
  <section class="panel">
    <div class="panel-header">
      <h2>冲突检测任务</h2>
      <el-button type="primary" :icon="Plus" @click="openCreate">创建任务</el-button>
    </div>

    <el-form class="toolbar" :inline="true" :model="query">
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable placeholder="全部" style="width: 140px">
          <el-option v-for="item in taskStatuses" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button :icon="Search" type="primary" @click="loadTasks">查询</el-button>
        <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tasks" row-key="id">
      <el-table-column prop="id" label="任务ID" width="90" />
      <el-table-column prop="mainRegulationTitle" label="主制度" min-width="240" show-overflow-tooltip />
      <el-table-column prop="compareRegulationIds" label="对比制度ID" min-width="140" />
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="tagTypeOf(taskStatuses, row.status)">{{ labelOf(taskStatuses, row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="totalItems" label="冲突项" width="100" />
      <el-table-column prop="createdAt" label="创建时间" min-width="170" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/conflicts/tasks/${row.id}`)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-row">
      <el-pagination
        v-model:current-page="query.pageNo"
        v-model:page-size="query.pageSize"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @change="loadTasks"
      />
    </div>
  </section>

  <el-dialog v-model="dialogVisible" title="创建冲突检测任务" width="620px">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
      <el-form-item label="规章集" prop="regulationSetId">
        <el-select v-model="form.regulationSetId" filterable placeholder="选择规章集" @change="handleSetChange">
          <el-option
            v-for="item in regulationSets"
            :key="item.id"
            :label="setLabel(item)"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="主制度" prop="mainRegulationId">
        <el-select v-model="form.mainRegulationId" filterable placeholder="选择主制度">
          <el-option v-for="item in regulations" :key="item.id" :label="item.title" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="对比制度" prop="compareRegulationIds">
        <el-select v-model="form.compareRegulationIds" multiple filterable collapse-tags collapse-tags-tooltip>
          <el-option
            v-for="item in compareOptions"
            :key="item.id"
            :label="item.title"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="creating" @click="createTask">开始检测</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { createConflictTask, getConflictTasks } from '../api/conflicts'
import { getRegulations } from '../api/regulations'
import { getRegulationSets } from '../api/regulationSets'
import { labelOf, tagTypeOf, taskStatuses } from '../constants/conflicts'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const creating = ref(false)
const dialogVisible = ref(false)
const tasks = ref([])
const regulations = ref([])
const regulationSets = ref([])
const total = ref(0)

const query = reactive({ status: '', pageNo: 1, pageSize: 10 })
const form = reactive({ regulationSetId: null, mainRegulationId: null, compareRegulationIds: [] })
const rules = {
  regulationSetId: [{ required: true, message: '请选择规章集', trigger: 'change' }],
  mainRegulationId: [{ required: true, message: '请选择主制度', trigger: 'change' }],
  compareRegulationIds: [{ required: true, message: '请选择对比制度', trigger: 'change' }]
}

const compareOptions = computed(() => regulations.value.filter((item) => item.id !== form.mainRegulationId))

async function loadTasks() {
  loading.value = true
  try {
    const data = await getConflictTasks(query)
    tasks.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function loadRegulations() {
  const data = await getRegulations({ pageNo: 1, pageSize: 100, regulationSetId: form.regulationSetId })
  regulations.value = data.records
}

async function loadSets() {
  regulationSets.value = await getRegulationSets({ enabledOnly: true })
  if (!form.regulationSetId) {
    form.regulationSetId = defaultSetId()
  }
}

function resetQuery() {
  query.status = ''
  query.pageNo = 1
  loadTasks()
}

async function openCreate() {
  await loadSets()
  form.regulationSetId = form.regulationSetId || defaultSetId()
  form.mainRegulationId = null
  form.compareRegulationIds = []
  await loadRegulations()
  dialogVisible.value = true
}

async function handleSetChange() {
  form.mainRegulationId = null
  form.compareRegulationIds = []
  await loadRegulations()
}

async function createTask() {
  await formRef.value.validate()
  creating.value = true
  try {
    const detail = await createConflictTask(form)
    ElMessage.success(`检测完成，生成 ${detail.totalItems} 个冲突项`)
    dialogVisible.value = false
    await loadTasks()
    router.push(`/conflicts/tasks/${detail.id}`)
  } finally {
    creating.value = false
  }
}

onMounted(loadTasks)

function defaultSetId() {
  return regulationSets.value.find((item) => item.defaultSet && item.enabled)?.id || regulationSets.value.find((item) => item.enabled)?.id || null
}

function setLabel(item) {
  const count = item.regulationCount ?? 0
  return item.defaultSet ? `${item.setName}（默认，${count}项）` : `${item.setName}（${count}项）`
}
</script>
