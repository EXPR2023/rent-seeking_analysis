<template>
  <el-container class="app-shell">
    <el-aside class="sidebar" width="232px">
      <div class="brand">
        <div class="brand-mark">规</div>
        <div>
          <div class="brand-title">寻租分析平台</div>
          <div class="brand-subtitle">制度与监管政策</div>
        </div>
      </div>
      <el-menu :default-active="activeMenuPath" router class="side-menu">
        <el-menu-item v-for="menu in flatMenus" :key="menu.id" :index="menu.path">
          <el-icon><component :is="iconFor(menu.menuCode)" /></el-icon>
          <span>{{ menu.menuName }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="topbar">
        <div>
          <h1>{{ pageTitle }}</h1>
          <p>{{ roleText }}</p>
        </div>
        <el-dropdown trigger="click" @command="handleCommand">
          <button class="user-button" type="button">
            <el-icon><User /></el-icon>
            <span>{{ auth.user?.realName || auth.user?.username }}</span>
            <el-icon><ArrowDown /></el-icon>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <el-main class="content">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowDown,
  Cpu,
  DataAnalysis,
  Document,
  Files,
  House,
  Lock,
  Notebook,
  Operation,
  Reading,
  Setting,
  Tickets,
  User,
  UserFilled
} from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const pageTitle = computed(() => route.meta.title || '工作台')
const roleText = computed(() => (auth.user?.roleCodes || []).join(' / ') || '未分配角色')
const flatMenus = computed(() => expandAnalysisMenus(flatten(auth.menus)))
const activeMenuPath = computed(() => {
  if (route.path.startsWith('/regulations')) return '/regulations'
  if (route.path.startsWith('/conflicts/tasks')) return '/conflicts/tasks'
  if (route.path.startsWith('/case-analysis') || route.path.startsWith('/rent-seeking-analysis') || route.path.startsWith('/ai/regulation-qa')) return '/case-analysis'
  if (route.path.startsWith('/regulation-iteration-analysis')) return '/regulation-iteration-analysis'
  if (route.path.startsWith('/risks')) return '/risks'
  return route.path
})

function flatten(menus) {
  return (menus || []).flatMap((menu) => [menu, ...flatten(menu.children)])
}

function expandAnalysisMenus(menus) {
  const hasCase = menus.some((menu) => menu.menuCode === 'CASE_ANALYSIS')
  const hasIteration = menus.some((menu) => menu.menuCode === 'REGULATION_ITERATION_ANALYSIS')
  const expanded = []
  for (const menu of menus) {
    if (menu.menuCode === 'AI_QA') {
      if (!hasCase) {
        expanded.push({ ...menu, id: `${menu.id}-case`, menuName: '案例分析', menuCode: 'CASE_ANALYSIS', path: '/case-analysis' })
      }
      if (!hasIteration) {
        expanded.push({
          ...menu,
          id: `${menu.id}-iteration`,
          menuName: '规章迭代分析',
          menuCode: 'REGULATION_ITERATION_ANALYSIS',
          path: '/regulation-iteration-analysis'
        })
      }
      continue
    }
    expanded.push(menu)
  }
  return expanded
}

function iconFor(code) {
  const icons = {
    DASHBOARD: House,
    USERS: UserFilled,
    ROLES: Lock,
    REGULATIONS: Notebook,
    CONFLICT_TASKS: Operation,
    RISKS: DataAnalysis,
    AI_RECORDS: Cpu,
    AI_QA: Reading,
    CASE_ANALYSIS: Reading,
    REGULATION_ITERATION_ANALYSIS: DataAnalysis,
    REPORTS: Document,
    SYSTEM_CONFIGS: Setting,
    SYSTEM_DICTS: Tickets,
    SYSTEM_LOGS: Files,
    SYSTEM_HEALTH: DataAnalysis
  }
  return icons[code] || Document
}

async function handleCommand(command) {
  if (command === 'logout') {
    await auth.logout()
    router.replace('/login')
  }
}
</script>
