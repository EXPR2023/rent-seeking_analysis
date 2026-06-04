import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import BasicLayout from '../layouts/BasicLayout.vue'
import LoginView from '../views/LoginView.vue'
import DashboardView from '../views/DashboardView.vue'
import UsersView from '../views/UsersView.vue'
import RolesView from '../views/RolesView.vue'
import RegulationsView from '../views/RegulationsView.vue'
import RegulationDetailView from '../views/RegulationDetailView.vue'
import ConflictTasksView from '../views/ConflictTasksView.vue'
import ConflictTaskDetailView from '../views/ConflictTaskDetailView.vue'
import AiRecordsView from '../views/AiRecordsView.vue'
import CaseAnalysisView from '../views/CaseAnalysisView.vue'
import RegulationIterationView from '../views/RegulationIterationView.vue'
import RisksView from '../views/RisksView.vue'
import RiskAnalysisDetailView from '../views/RiskAnalysisDetailView.vue'
import ReportsView from '../views/ReportsView.vue'
import SystemConfigsView from '../views/SystemConfigsView.vue'
import SystemDictsView from '../views/SystemDictsView.vue'
import SystemLogsView from '../views/SystemLogsView.vue'
import SystemHealthView from '../views/SystemHealthView.vue'
import PlaceholderView from '../views/PlaceholderView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: LoginView },
    {
      path: '/',
      component: BasicLayout,
      redirect: '/dashboard',
      meta: { requiresAuth: true },
      children: [
        { path: 'dashboard', component: DashboardView, meta: { title: '工作台' } },
        { path: 'users', component: UsersView, meta: { title: '用户管理' } },
        { path: 'roles', component: RolesView, meta: { title: '角色权限' } },
        { path: 'regulations', component: RegulationsView, meta: { title: '制度管理' } },
        { path: 'regulations/:id', component: RegulationDetailView, meta: { title: '制度详情' } },
        { path: 'conflicts/tasks', component: ConflictTasksView, meta: { title: '冲突检测' } },
        { path: 'conflicts/tasks/:id', component: ConflictTaskDetailView, meta: { title: '冲突详情' } },
        { path: 'case-analysis', component: CaseAnalysisView, meta: { title: '案例分析' } },
        { path: 'regulation-iteration-analysis', component: RegulationIterationView, meta: { title: '规章迭代分析' } },
        { path: 'rent-seeking-analysis', redirect: '/case-analysis' },
        { path: 'risks', component: RisksView, meta: { title: '制度风险库' } },
        { path: 'risks/:id', component: RiskAnalysisDetailView, meta: { title: '寻租分析工作台' } },
        { path: 'ai/records', component: AiRecordsView, meta: { title: 'AI 分析记录' } },
        { path: 'ai/regulation-qa', redirect: '/case-analysis' },
        { path: 'reports', component: ReportsView, meta: { title: '报告预览' } },
        { path: 'system/configs', component: SystemConfigsView, meta: { title: '系统参数' } },
        { path: 'system/dicts', component: SystemDictsView, meta: { title: '字典管理' } },
        { path: 'system/logs', component: SystemLogsView, meta: { title: '操作日志' } },
        { path: 'system/health', component: SystemHealthView, meta: { title: '健康检查' } },
        { path: ':pathMatch(.*)*', component: PlaceholderView, meta: { title: '模块建设中' } }
      ]
    }
  ]
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (to.path === '/login' && auth.isLoggedIn) {
    return '/dashboard'
  }
  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    return '/login'
  }
  if (to.meta.requiresAuth && auth.isLoggedIn && !auth.user) {
    try {
      await auth.loadSession()
    } catch {
      auth.clearSession()
      return '/login'
    }
  }
})

export default router
