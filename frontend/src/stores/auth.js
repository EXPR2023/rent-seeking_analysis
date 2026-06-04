import { defineStore } from 'pinia'
import { getCurrentMenus, getCurrentUser, login as loginApi, logout as logoutApi } from '../api/auth'

const TOKEN_KEY = 'rent_analysis_token'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: null,
    menus: []
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token)
  },
  actions: {
    async login(credentials) {
      const data = await loginApi(credentials)
      this.token = data.accessToken
      localStorage.setItem(TOKEN_KEY, data.accessToken)
      this.user = data.user
      this.user.roleCodes = data.roleCodes
      await this.loadSession()
    },
    async loadSession() {
      if (!this.token) return
      const [user, menus] = await Promise.all([getCurrentUser(), getCurrentMenus()])
      this.user = user
      this.menus = menus || []
    },
    async logout() {
      try {
        if (this.token) await logoutApi()
      } finally {
        this.clearSession()
      }
    },
    clearSession() {
      this.token = ''
      this.user = null
      this.menus = []
      localStorage.removeItem(TOKEN_KEY)
    }
  }
})
