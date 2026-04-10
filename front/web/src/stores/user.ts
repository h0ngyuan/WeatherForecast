import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getUserInfo, logout as logoutApi } from '@/api/auth'
import router from '@/router'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('satoken') || '')
  const userInfo = ref<any>(null)
  const isLoggedIn = ref(!!token.value)

  async function fetchUserInfo() {
    try {
      const res: any = await getUserInfo()
      userInfo.value = res.data
      return res.data
    } catch {
      return null
    }
  }

  function setToken(newToken: string) {
    token.value = newToken
    localStorage.setItem('satoken', newToken)
    isLoggedIn.value = true
  }

  async function logout() {
    try {
      await logoutApi()
    } finally {
      token.value = ''
      userInfo.value = null
      isLoggedIn.value = false
      localStorage.removeItem('satoken')
      router.push('/login')
    }
  }

  return { token, userInfo, isLoggedIn, setToken, fetchUserInfo, logout }
})
