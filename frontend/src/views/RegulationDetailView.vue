<template>
  <section class="detail-layout">
    <section class="panel">
      <div class="panel-header">
        <h2>{{ detail?.title || '制度详情' }}</h2>
        <div class="header-actions">
          <el-button :icon="ArrowLeft" @click="router.push('/regulations')">返回</el-button>
          <el-button type="primary" :loading="saving" @click="saveAll">保存</el-button>
        </div>
      </div>

      <el-skeleton v-if="loading" :rows="8" animated />
      <template v-else>
        <el-descriptions :column="2" border class="detail-descriptions">
          <el-descriptions-item label="规章集">{{ detail?.regulationSetName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="制度编号">{{ form.code || '-' }}</el-descriptions-item>
          <el-descriptions-item label="制度类型">{{ labelOf(regulationTypes, form.typeCode) }}</el-descriptions-item>
          <el-descriptions-item label="发布部门">{{ form.publishDepartment || '-' }}</el-descriptions-item>
          <el-descriptions-item label="制度状态">
            <el-tag :type="tagTypeOf(regulationStatuses, form.status)">
              {{ labelOf(regulationStatuses, form.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="生效日期">{{ form.effectiveDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="失效日期">{{ form.expiryDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="分析状态">
            <el-tag :type="tagTypeOf(analysisStatuses, detail?.analysisStatus)">
              {{ labelOf(analysisStatuses, detail?.analysisStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="最近更新">{{ detail?.updatedAt || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-form ref="formRef" :model="form" :rules="rules" label-width="92px" class="detail-form">
          <el-form-item label="规章集" prop="regulationSetId">
            <el-select v-model="form.regulationSetId" filterable>
              <el-option
                v-for="item in regulationSets"
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
              <el-form-item label="制度编号">
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
            <el-input v-model="form.content" type="textarea" :rows="16" />
          </el-form-item>
        </el-form>
      </template>
    </section>

    <aside class="panel side-panel">
      <h2>后续入口</h2>
      <el-button :icon="Operation" @click="router.push('/conflicts/tasks')">发起冲突检测</el-button>
      <el-button :icon="DataAnalysis" :loading="analyzing" @click="runRiskAnalysis">发起风险分析</el-button>
      <el-button :icon="Document" disabled>报告预览</el-button>
    </aside>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, DataAnalysis, Document, Operation } from '@element-plus/icons-vue'
import { getRegulation, updateRegulation } from '../api/regulations'
import { getRegulationSets } from '../api/regulationSets'
import { analyzeRegulation } from '../api/risks'
import {
  analysisStatuses,
  labelOf,
  regulationStatuses,
  regulationTypes,
  tagTypeOf
} from '../constants/regulations'

const route = useRoute()
const router = useRouter()
const formRef = ref()
const loading = ref(false)
const saving = ref(false)
const analyzing = ref(false)
const detail = ref(null)
const regulationSets = ref([])
const form = reactive(defaultForm())

const rules = {
  regulationSetId: [{ required: true, message: '请选择规章集', trigger: 'change' }],
  title: [{ required: true, message: '请输入制度标题', trigger: 'blur' }],
  typeCode: [{ required: true, message: '请选择制度类型', trigger: 'change' }],
  status: [{ required: true, message: '请选择制度状态', trigger: 'change' }]
}

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

function fillForm(data) {
  Object.assign(form, {
    regulationSetId: data.regulationSetId || null,
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
  regulationSets.value = await getRegulationSets({ enabledOnly: true })
}

async function loadDetail() {
  loading.value = true
  try {
    detail.value = await getRegulation(route.params.id)
    fillForm(detail.value)
  } finally {
    loading.value = false
  }
}

async function saveAll() {
  await formRef.value.validate()
  saving.value = true
  try {
    detail.value = await updateRegulation(route.params.id, form)
    fillForm(detail.value)
    ElMessage.success('制度已保存')
  } finally {
    saving.value = false
  }
}

async function runRiskAnalysis() {
  analyzing.value = true
  try {
    const analysisId = await analyzeRegulation(route.params.id)
    ElMessage.success('风险分析已完成')
    router.push(`/risks/${analysisId}`)
  } finally {
    analyzing.value = false
  }
}

function setLabel(item) {
  return item.defaultSet ? `${item.setName}（默认）` : item.setName
}

onMounted(async () => {
  await loadSets()
  await loadDetail()
})
</script>
