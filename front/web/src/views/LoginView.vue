<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { login, generateImageCaptcha, sendEmailCaptcha, verifyImageCaptcha } from '@/api/auth'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const loginType = ref<'email'>('email')
const loading = ref(false)
const captchaLoading = ref(false)
const countdown = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null

const formRef = ref<FormInstance>()
const form = ref({
  email: '',
  verifyCode: '',
  captchaKey: '',
  captchaCode: '',
})

const captchaImage = ref('')

const rules = ref<FormRules>({
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' },
  ],
  captchaCode: [{ required: true, message: '请输入图形验证码', trigger: 'blur' }],
  verifyCode: [{ required: true, message: '请输入邮箱验证码', trigger: 'blur' }],
})

async function fetchCaptcha() {
  try {
    captchaLoading.value = true
    const res: any = await generateImageCaptcha()
    // 后端已经返回完整的 data:image/png;base64,xxx 格式
    captchaImage.value = res.data.image
    form.value.captchaKey = res.data.key
  } catch {
    ElMessage.error('获取验证码失败')
  } finally {
    captchaLoading.value = false
  }
}

async function handleSendCode() {
  if (!form.value.email) {
    ElMessage.warning('请先输入邮箱')
    return
  }
  if (!form.value.captchaCode) {
    ElMessage.warning('请先输入图形验证码')
    return
  }
  // 立即开始倒计时
  countdown.value = 60
  countdownTimer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0 && countdownTimer) {
      clearInterval(countdownTimer)
      countdownTimer = null
    }
  }, 1000)
  try {
    // 先验证图形验证码
    const verifyRes: any = await verifyImageCaptcha({
      key: form.value.captchaKey,
      code: form.value.captchaCode
    })
    // 只有验证通过(data为true)才发送邮箱验证码
    if (verifyRes.data === true) {
      await sendEmailCaptcha(form.value.email)
      ElMessage.success('验证码已发送')
    } else {
      ElMessage.error('图形验证码错误')
      fetchCaptcha()
    }
  } catch {
    // 验证失败，刷新图形验证码
    ElMessage.error('图形验证码验证失败')
    fetchCaptcha()
  }
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res: any = await login({
      email: form.value.email,
      captchaKey: form.value.captchaKey,
      captchaCode: form.value.captchaCode,
      verifyCode: form.value.verifyCode,
      type: 'email',
    })
    userStore.setToken(res.data)
    ElMessage.success('登录成功')
    router.push('/chat')
  } catch {
    fetchCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchCaptcha()
})
</script>

<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <div class="logo-icon">
          <el-icon :size="48" color="#409eff"><PartlyCloudy /></el-icon>
        </div>
        <h1>天气预报系统</h1>
        <p class="subtitle">智能天气查询与灾害预警平台</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large">
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" prefix-icon="Message" />
        </el-form-item>

        <el-form-item label="图形验证码" prop="captchaCode">
          <div class="captcha-row">
            <el-input v-model="form.captchaCode" placeholder="请输入图形验证码" />
            <div class="captcha-img" @click="fetchCaptcha">
              <img v-if="captchaImage" :src="captchaImage" alt="验证码" />
              <el-icon v-else :size="24" class="captcha-loading"><Loading /></el-icon>
            </div>
          </div>
        </el-form-item>

        <el-form-item label="邮箱验证码" prop="verifyCode">
          <div class="code-row">
            <el-input v-model="form.verifyCode" placeholder="请输入邮箱验证码" />
            <el-button
              type="primary"
              :disabled="countdown > 0"
              :loading="countdown > 0"
              @click="handleSendCode"
            >
              {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="loading" class="login-btn" @click="handleLogin">
            登 录
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 420px;
  padding: 40px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.logo-icon {
  margin-bottom: 12px;
}

.login-header h1 {
  margin: 0;
  font-size: 24px;
  color: #303133;
}

.subtitle {
  margin: 8px 0 0;
  color: #909399;
  font-size: 14px;
}

.captcha-row {
  display: flex;
  gap: 12px;
  width: 100%;
}

.captcha-row .el-input {
  flex: 1;
}

.captcha-img {
  width: 120px;
  height: 40px;
  cursor: pointer;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.captcha-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.captcha-loading {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.code-row {
  display: flex;
  gap: 12px;
  width: 100%;
}

.code-row .el-input {
  flex: 1;
}

.login-btn {
  width: 100%;
}
</style>
