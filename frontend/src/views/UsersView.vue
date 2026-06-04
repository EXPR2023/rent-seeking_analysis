<template>
  <section class="panel">
    <div class="panel-header">
      <h2>用户管理</h2>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增用户</el-button>
    </div>

    <el-form class="toolbar" :inline="true" :model="query">
      <el-form-item label="关键词">
        <el-input v-model="query.keyword" clearable placeholder="账号 / 姓名 / 邮箱" @keyup.enter="loadUsers" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable placeholder="全部" style="width: 140px">
          <el-option label="启用" value="ENABLED" />
          <el-option label="停用" value="DISABLED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button :icon="Search" type="primary" @click="loadUsers">查询</el-button>
        <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="users" row-key="id">
      <el-table-column prop="username" label="账号" min-width="130" />
      <el-table-column prop="realName" label="姓名" min-width="130" />
      <el-table-column prop="email" label="邮箱" min-width="190" show-overflow-tooltip />
      <el-table-column prop="phone" label="手机号" min-width="140" />
      <el-table-column label="角色" min-width="220">
        <template #default="{ row }">
          <el-tag v-for="role in row.roleCodes" :key="role" class="role-tag" type="info">{{ role }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'">
            {{ row.status === 'ENABLED' ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="270" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="primary" @click="openRoles(row)">角色</el-button>
          <el-button link :type="row.status === 'ENABLED' ? 'warning' : 'success'" @click="toggleStatus(row)">
            {{ row.status === 'ENABLED' ? '停用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-row">
      <el-pagination
        v-model:current-page="query.pageNo"
        v-model:page-size="query.pageSize"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @change="loadUsers"
      />
    </div>
  </section>

  <el-dialog v-model="userDialog.visible" :title="userDialog.mode === 'create' ? '新增用户' : '编辑用户'" width="520px">
    <el-form ref="userFormRef" :model="userForm" :rules="userRules" label-width="84px">
      <el-form-item v-if="userDialog.mode === 'create'" label="账号" prop="username">
        <el-input v-model="userForm.username" />
      </el-form-item>
      <el-form-item label="姓名" prop="realName">
        <el-input v-model="userForm.realName" />
      </el-form-item>
      <el-form-item label="邮箱" prop="email">
        <el-input v-model="userForm.email" />
      </el-form-item>
      <el-form-item label="手机号" prop="phone">
        <el-input v-model="userForm.phone" />
      </el-form-item>
      <el-form-item :label="userDialog.mode === 'create' ? '密码' : '新密码'" prop="password">
        <el-input v-model="userForm.password" type="password" show-password />
      </el-form-item>
      <el-form-item v-if="userDialog.mode === 'create'" label="角色">
        <el-select v-model="userForm.roleIds" multiple collapse-tags collapse-tags-tooltip>
          <el-option v-for="role in roles" :key="role.id" :label="role.roleName" :value="role.id" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="userDialog.visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="saveUser">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="roleDialog.visible" title="分配角色" width="460px">
    <el-checkbox-group v-model="roleDialog.roleIds" class="role-checks">
      <el-checkbox v-for="role in roles" :key="role.id" :label="role.id">
        {{ role.roleName }}
      </el-checkbox>
    </el-checkbox-group>
    <template #footer>
      <el-button @click="roleDialog.visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="saveRoles">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { assignUserRoles, createUser, getRoles, getUsers, updateUser, updateUserStatus } from '../api/users'

const loading = ref(false)
const saving = ref(false)
const users = ref([])
const roles = ref([])
const total = ref(0)
const userFormRef = ref()

const query = reactive({
  keyword: '',
  status: '',
  pageNo: 1,
  pageSize: 10
})

const userDialog = reactive({
  visible: false,
  mode: 'create',
  id: null
})

const userForm = reactive({
  username: '',
  password: '',
  realName: '',
  email: '',
  phone: '',
  roleIds: []
})

const roleDialog = reactive({
  visible: false,
  user: null,
  roleIds: []
})

const userRules = computed(() => ({
  username: userDialog.mode === 'create' ? [{ required: true, message: '请输入账号', trigger: 'blur' }] : [],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  password:
    userDialog.mode === 'create'
      ? [{ required: true, min: 6, message: '至少 6 位', trigger: 'blur' }]
      : [{ min: 6, message: '至少 6 位', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}))

async function loadUsers() {
  loading.value = true
  try {
    const data = await getUsers(query)
    users.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function loadRoles() {
  roles.value = await getRoles({ enabled: true })
}

function resetQuery() {
  query.keyword = ''
  query.status = ''
  query.pageNo = 1
  loadUsers()
}

function resetUserForm() {
  Object.assign(userForm, {
    username: '',
    password: '',
    realName: '',
    email: '',
    phone: '',
    roleIds: []
  })
}

function openCreate() {
  resetUserForm()
  userDialog.mode = 'create'
  userDialog.id = null
  userDialog.visible = true
}

function openEdit(row) {
  resetUserForm()
  userDialog.mode = 'edit'
  userDialog.id = row.id
  Object.assign(userForm, {
    realName: row.realName,
    email: row.email,
    phone: row.phone,
    password: ''
  })
  userDialog.visible = true
}

async function saveUser() {
  await userFormRef.value.validate()
  saving.value = true
  try {
    if (userDialog.mode === 'create') {
      await createUser(userForm)
    } else {
      await updateUser(userDialog.id, {
        realName: userForm.realName,
        email: userForm.email,
        phone: userForm.phone,
        password: userForm.password || null
      })
    }
    ElMessage.success('保存成功')
    userDialog.visible = false
    await loadUsers()
  } finally {
    saving.value = false
  }
}

function openRoles(row) {
  roleDialog.user = row
  roleDialog.roleIds = roles.value.filter((role) => row.roleCodes?.includes(role.roleCode)).map((role) => role.id)
  roleDialog.visible = true
}

async function saveRoles() {
  saving.value = true
  try {
    await assignUserRoles(roleDialog.user.id, roleDialog.roleIds)
    ElMessage.success('角色已更新')
    roleDialog.visible = false
    await loadUsers()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row) {
  const nextStatus = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  await ElMessageBox.confirm(`确认${nextStatus === 'ENABLED' ? '启用' : '停用'}该用户？`, '状态变更')
  await updateUserStatus(row.id, nextStatus)
  ElMessage.success('状态已更新')
  await loadUsers()
}

onMounted(async () => {
  await Promise.all([loadRoles(), loadUsers()])
})
</script>
