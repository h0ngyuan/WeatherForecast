import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 120000,
})

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('satoken')
    if (token) {
      config.headers['satoken'] = token
    }
    return config
  },
  (error) => Promise.reject(error),
)

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.success === false) {
      // 处理未登录情况（包括400错误中的"未提供token"）
      if (res.code === 401 || res.msg?.includes('token') || res.msg?.includes('登录')) {
        ElMessage.error('登录已过期，请重新登录')
        localStorage.removeItem('satoken')
        router.push('/login')
      } else {
        ElMessage.error(res.msg || '请求失败')
      }
      return Promise.reject(new Error(res.msg || '请求失败'))
    }
    return res
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('satoken')
      router.push('/login')
    }
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  },
)

export default request
