<template>
  <section class="panel">
    <div class="panel-header">
      <h2>制度管理</h2>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增制度</el-button>
    </div>

    <el-form class="toolbar" :inline="true" :model="query">
      <el-form-item label="规章集">
        <el-select v-model="query.regulationSetId" clearable placeholder="全部集合" style="width: 220px" @change="loadRegulations">
          <el-option
            v-for="item in regulationSets"
            :key="item.id"
            :label="setLabel(item)"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="关键词">
        <el-input v-model="query.keyword" clearable placeholder="标题 / 编号 / 部门" @keyup.enter="loadRegulations" />
      </el-form-item>
      <el-form-item label="类型">
        <el-select v-model="query.typeCode" clearable placeholder="全部" style="width: 140px">
          <el-option v-for="item in regulationTypes" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable placeholder="全部" style="width: 140px">
          <el-option v-for="item in regulationStatuses" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button :icon="Search" type="primary" @click="loadRegulations">查询</el-button>
        <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        <el-button :icon="Plus" @click="openSetDialog">新增规章集</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="records" row-key="id">
      <el-table-column prop="regulationSetName" label="规章集" min-width="160" show-overflow-tooltip />
      <el-table-column prop="title" label="制度标题" min-width="240" show-overflow-tooltip />
      <el-table-column prop="code" label="编号" min-width="140" show-overflow-tooltip />
      <el-table-column label="类型" width="110">
        <template #default="{ row }">{{ labelOf(regulationTypes, row.typeCode) }}</template>
      </el-table-column>
      <el-table-column prop="publishDepartment" label="发布部门" min-width="150" show-overflow-tooltip />
      <el-table-column label="制度状态" width="110">
        <template #default="{ row }">
          <el-tag :type="tagTypeOf(regulationStatuses, row.status)">
            {{ labelOf(regulationStatuses, row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="分析状态" width="110">
        <template #default="{ row }">
          <el-tag :type="tagTypeOf(analysisStatuses, row.analysisStatus)">
            {{ labelOf(analysisStatuses, row.analysisStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="updatedAt" label="更新时间" min-width="170" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="viewDetail(row)">详情</el-button>
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-row">
      <el-pagination
        v-model:current-page="query.pageNo"
        v-model:page-size="query.pageSize"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @change="loadRegulations"
      />
    </div>
  </section>

  <el-dialog v-model="dialog.visible" :title="dialog.mode === 'create' ? '新增制度' : '编辑制度'" width="760px">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
      <el-form-item label="规章集" prop="regulationSetId">
        <el-select v-model="form.regulationSetId" filterable placeholder="选择规章集">
          <el-option
            v-for="item in enabledRegulationSets"
            :key="item.id"
            :label="setLabel(item)"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="制度标题" prop="title">
        <el-input v-model="form.title" />
      </el-form-item>
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="制度编号" prop="code">
            <el-input v-model="form.code" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="制度类型" prop="typeCode">
            <el-select v-model="form.typeCode">
              <el-option v-for="item in regulationTypes" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="发布部门">
            <el-input v-model="form.publishDepartment" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="制度状态" prop="status">
            <el-select v-model="form.status">
              <el-option v-for="item in regulationStatuses" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="生效日期">
            <el-date-picker v-model="form.effectiveDate" value-format="YYYY-MM-DD" type="date" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="失效日期">
            <el-date-picker v-model="form.expiryDate" value-format="YYYY-MM-DD" type="date" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="适用范围">
        <el-input v-model="form.applicableScope" type="textarea" :rows="2" maxlength="500" show-word-limit />
      </el-form-item>
      <el-form-item label="制度正文">
        <el-input v-model="form.content" type="textarea" :rows="8" placeholder="可在新增时录入正文，也可进入详情页继续维护" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialog.visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="setDialog.visible" title="新增规章集" width="560px">
    <el-form ref="setFormRef" :model="setForm" :rules="setRules" label-width="92px">
      <el-form-item label="名称" prop="setName">
        <el-input v-model="setForm.setName" />
      </el-form-item>
      <el-form-item label="编码" prop="setCode">
        <el-input v-model="setForm.setCode" placeholder="例如 FOOD_SAFETY" />
      </el-form-item>
      <el-form-item label="说明">
        <el-input v-model="setForm.description" type="textarea" :rows="3" maxlength="500" show-word-limit />
      </el-form-item>
      <el-form-item label="设为默认">
        <el-switch v-model="setForm.defaultSet" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="setDialog.visible = false">取消</el-button>
      <el-button type="primary" :loading="savingSet" @click="createSet">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { createRegulation, deleteRegulation, getRegulation, getRegulations, updateRegulation } from '../api/regulations'
import { createRegulationSet, getRegulationSets } from '../api/regulationSets'
import {
  analysisStatuses,
  labelOf,
  regulationStatuses,
  regulationTypes,
  tagTypeOf
} from '../constants/regulations'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const saving = ref(false)
const savingSet = ref(false)
const records = ref([])
const regulationSets = ref([])
const total = ref(0)
const setFormRef = ref()

const query = reactive({
  regulationSetId: null,
  keyword: '',
  typeCode: '',
  status: '',
  pageNo: 1,
  pageSize: 10
})

const dialog = reactive({
  visible: false,
  mode: 'create',
  id: null
})

const setDialog = reactive({ visible: false })
const form = reactive(defaultForm())
const setForm = reactive(defaultSetForm())

const rules = {
  regulationSetId: [{ required: true, message: '请选择规章集', trigger: 'change' }],
  title: [{ required: true, message: '请输入制度标题', trigger: 'blur' }],
  typeCode: [{ required: true, message: '请选择制度类型', trigger: 'change' }],
  status: [{ required: true, message: '请选择制度状态', trigger: 'change' }]
}

const setRules = {
  setName: [{ required: true, message: '请输入规章集名称', trigger: 'blur' }],
  setCode: [{ required: true, message: '请输入规章集编码', trigger: 'blur' }]
}

const enabledRegulationSets = computed(() => regulationSets.value.filter((item) => item.enabled))

function defaultForm() {
  return {
    regulationSetId: null,
    title: '',
    code: '',
    typeCode: 'POLICY',
    publishDepartment: '',
    effectiveDate: '',
    expiryDate: '',
    applicableScope: '',
    status: 'DRAFT',
    content: ''
  }
}

function defaultSetForm() {
  return {
    setName: '',
    setCode: '',
    description: '',
    enabled: true,
    defaultSet: false
  }
}

function fillForm(data = {}) {
  Object.assign(form, defaultForm(), {
    regulationSetId: data.regulationSetId || query.regulationSetId || defaultSetId(),
    title: data.title || '',
    code: data.code || '',
    typeCode: data.typeCode || 'POLICY',
    publishDepartment: data.publishDepartment || '',
    effectiveDate: data.effectiveDate || '',
    expiryDate: data.expiryDate || '',
    applicableScope: data.applicableScope || '',
    status: data.status || 'DRAFT',
    content: data.content || ''
  })
}

async function loadSets() {
  regulationSets.value = await getRegulationSets()
  if (!query.regulationSetId) {
    query.regulationSetId = defaultSetId()
  }
}

async function loadRegulations() {
  if (!regulationSets.value.length) {
    await loadSets()
  }
  loading.value = true
  try {
    const data = await getRegulations(query)
    records.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.regulationSetId = defaultSetId()
  query.keyword = ''
  query.typeCode = ''
  query.status = ''
  query.pageNo = 1
  loadRegulations()
}

function openCreate() {
  fillForm()
  dialog.mode = 'create'
  dialog.id = null
  dialog.visible = true
}

function openSetDialog() {
  Object.assign(setForm, defaultSetForm())
  setDialog.visible = true
}

async function createSet() {
  await setFormRef.value.validate()
  savingSet.value = true
  try {
    const created = await createRegulationSet(setForm)
    ElMessage.success('规章集已新增')
    setDialog.visible = false
    await loadSets()
    query.regulationSetId = created.id
    await loadRegulations()
  } finally {
    savingSet.value = false
  }
}

async function openEdit(row) {
  const detail = await getRegulation(row.id)
  fillForm(detail)
  dialog.mode = 'edit'
  dialog.id = row.id
  dialog.visible = true
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (dialog.mode === 'create') {
      const detail = await createRegulation(form)
      ElMessage.success('制度已新增')
      dialog.visible = false
      await loadRegulations()
      router.push(`/regulations/${detail.id}`)
    } else {
      await updateRegulation(dialog.id, form)
      ElMessage.success('制度已保存')
      dialog.visible = false
      await loadRegulations()
    }
  } finally {
    saving.value = false
  }
}

function viewDetail(row) {
  router.push(`/regulations/${row.id}`)
}

function defaultSetId() {
  return regulationSets.value.find((item) => item.defaultSet && item.enabled)?.id || regulationSets.value.find((item) => item.enabled)?.id || null
}

function setLabel(item) {
  const count = item.regulationCount ?? 0
  return item.defaultSet ? `${item.setName}（默认，${count}项）` : `${item.setName}（${count}项）`
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除制度“${row.title}”？`, '删除制度', { type: 'warning' })
  await deleteRegulation(row.id)
  ElMessage.success('制度已删除')
  await loadRegulations()
}

onMounted(async () => {
  await loadSets()
  await loadRegulations()
})
</script>
