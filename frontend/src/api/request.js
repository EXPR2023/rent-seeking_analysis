import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'
import { useAuthStore } from '../stores/auth'

const request = axios.create({
  baseURL: '/api',
  timeout: 60000
})

request.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body?.code && body.code !== 'SUCCESS') {
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body?.data
  },
  (error) => {
    const status = error.response?.status
    const message = error.response?.data?.message || error.message || '网络异常'
    if (status === 401) {
      const auth = useAuthStore()
      auth.clearSession()
      router.replace('/login')
    } else {
      ElMessage.error(message)
    }
    return Promise.reject(error)
  }
)

export default request
