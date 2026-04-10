<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getNotifySettings, updateNotifySettings, bindEmail, sendEmailCaptcha } from '@/api/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('profile')
const saving = ref(false)
const bindEmailVisible = ref(false)
const countdown = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null

const notifySettings = ref({
  wechatNotifyPermission: 0,
  emailNotifyPermission: 0,
  phoneNotifyPermission: 0,
})

const bindForm = ref({
  email: '',
  code: '',
})

async function loadSettings() {
  try {
    const res: any = await getNotifySettings()
    const data = res.data
    notifySettings.value = {
      wechatNotifyPermission: data.wechatNotifyPermission || 0,
      emailNotifyPermission: data.emailNotifyPermission || 0,
      phoneNotifyPermission: data.phoneNotifyPermission || 0,
    }
  } catch {
    // handled by interceptor
  }
}

async function saveSettings() {
  saving.value = true
  try {
    await updateNotifySettings(notifySettings.value)
    ElMessage.success('设置已保存')
  } catch {
    // handled by interceptor
  } finally {
    saving.value = false
  }
}

async function handleSendBindCode() {
  if (!bindForm.value.email) {
    ElMessage.warning('请输入邮箱')
    return
  }
  try {
    await sendEmailCaptcha(bindForm.value.email)
    ElMessage.success('验证码已发送')
    countdown.value = 60
    countdownTimer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0 && countdownTimer) {
        clearInterval(countdownTimer)
        countdownTimer = null
      }
    }, 1000)
  } catch {
    // handled by interceptor
  }
}

async function handleBindEmail() {
  if (!bindForm.value.email || !bindForm.value.code) {
    ElMessage.warning('请填写完整信息')
    return
  }
  try {
    await bindEmail({ email: bindForm.value.email, code: bindForm.value.code })
    ElMessage.success('邮箱绑定成功')
    bindEmailVisible.value = false
    bindForm.value = { email: '', code: '' }
    userStore.fetchUserInfo()
  } catch {
    // handled by interceptor
  }
}

onMounted(() => {
  loadSettings()
})
</script>

<template>
  <div class="settings-container">
    <el-container class="settings-layout">
      <el-header class="page-header">
        <div class="header-left">
          <el-button text @click="router.push('/')">
            <el-icon><ArrowLeft /></el-icon>
            返回
          </el-button>
        </div>
        <h2 class="header-title">个人设置</h2>
        <div style="width: 80px"></div>
      </el-header>

      <el-main class="page-main">
        <div class="settings-card">
          <el-tabs v-model="activeTab">
            <el-tab-pane label="个人信息" name="profile">
              <div class="profile-section">
                <div class="avatar-section">
                  <el-avatar :size="80" icon="UserFilled" />
                  <div class="user-detail">
                    <h3>{{ userStore.userInfo?.nickname || '用户' }}</h3>
                    <p>ID: {{ userStore.userInfo?.id }}</p>
                  </div>
                </div>

                <el-descriptions :column="1" border class="info-table">
                  <el-descriptions-item label="邮箱">
                    {{ userStore.userInfo?.email || '未绑定' }}
                    <el-button
                      v-if="!userStore.userInfo?.email"
                      type="primary"
                      link
                      @click="bindEmailVisible = true"
                    >
                      绑定
                    </el-button>
                  </el-descriptions-item>
                  <el-descriptions-item label="手机号">
                    {{ userStore.userInfo?.phone || '未绑定' }}
                  </el-descriptions-item>
                  <el-descriptions-item label="账户来源">
                    <el-tag v-if="userStore.userInfo?.accountSource === 0" type="info">本地注册</el-tag>
                    <el-tag v-else-if="userStore.userInfo?.accountSource === 1" type="success">微信</el-tag>
                    <el-tag v-else type="warning">其他</el-tag>
                  </el-descriptions-item>
                  <el-descriptions-item label="注册时间">
                    {{ userStore.userInfo?.createTime }}
                  </el-descriptions-item>
                </el-descriptions>
              </div>
            </el-tab-pane>

            <el-tab-pane label="通知设置" name="notify">
              <div class="notify-section">
                <h3>通知权限</h3>
                <p class="section-desc">选择您希望接收天气预警通知的方式</p>

                <div class="notify-items">
                  <div class="notify-item">
                    <div class="notify-info">
                      <el-icon :size="24" color="#409eff"><Message /></el-icon>
                      <div>
                        <div class="notify-label">邮箱通知</div>
                        <div class="notify-desc">通过邮件接收天气预警</div>
                      </div>
                    </div>
                    <el-switch
                      v-model="notifySettings.emailNotifyPermission"
                      :active-value="1"
                      :inactive-value="0"
                    />
                  </div>

                  <div class="notify-item">
                    <div class="notify-info">
                      <el-icon :size="24" color="#67c23a"><Iphone /></el-icon>
                      <div>
                        <div class="notify-label">短信通知</div>
                        <div class="notify-desc">通过短信接收天气预警</div>
                      </div>
                    </div>
                    <el-switch
                      v-model="notifySettings.phoneNotifyPermission"
                      :active-value="1"
                      :inactive-value="0"
                    />
                  </div>

                  <div class="notify-item">
                    <div class="notify-info">
                      <el-icon :size="24" color="#07c160"><ChatDotRound /></el-icon>
                      <div>
                        <div class="notify-label">微信通知</div>
                        <div class="notify-desc">通过微信接收天气预警</div>
                      </div>
                    </div>
                    <el-switch
                      v-model="notifySettings.wechatNotifyPermission"
                      :active-value="1"
                      :inactive-value="0"
                    />
                  </div>
                </div>

                <el-button type="primary" :loading="saving" @click="saveSettings">
                  保存设置
                </el-button>
              </div>
            </el-tab-pane>

            <el-tab-pane label="账户安全" name="security">
              <div class="security-section">
                <h3>账户操作</h3>
                <div class="security-actions">
                  <div class="security-item">
                    <div>
                      <div class="security-label">退出登录</div>
                      <div class="security-desc">退出当前账户</div>
                    </div>
                    <el-button type="danger" plain @click="userStore.logout()">退出</el-button>
                  </div>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </el-main>
    </el-container>

    <el-dialog v-model="bindEmailVisible" title="绑定邮箱" width="400px">
      <el-form label-position="top">
        <el-form-item label="邮箱">
          <el-input v-model="bindForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="验证码">
          <div class="code-row">
            <el-input v-model="bindForm.code" placeholder="请输入验证码" />
            <el-button :disabled="countdown > 0" @click="handleSendBindCode">
              {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bindEmailVisible = false">取消</el-button>
        <el-button type="primary" @click="handleBindEmail">确认绑定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.settings-container {
  height: 100vh;
  overflow: hidden;
  background: #f5f7fa;
}

.settings-layout {
  height: 100%;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  height: 60px;
}

.header-title {
  margin: 0;
  font-size: 18px;
  color: #303133;
}

.page-main {
  display: flex;
  justify-content: center;
  padding: 32px 24px;
  overflow-y: auto;
}

.settings-card {
  width: 700px;
  background: #fff;
  border-radius: 16px;
  padding: 32px 40px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.profile-section {
  padding: 16px 0;
}

.avatar-section {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 24px;
}

.user-detail h3 {
  margin: 0 0 4px;
  font-size: 18px;
}

.user-detail p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.info-table {
  margin-top: 16px;
}

.notify-section {
  padding: 16px 0;
}

.notify-section h3 {
  margin: 0 0 8px;
  font-size: 18px;
}

.section-desc {
  color: #909399;
  font-size: 14px;
  margin: 0 0 24px;
}

.notify-items {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 24px;
}

.notify-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border: 1px solid #e4e7ed;
  border-radius: 12px;
}

.notify-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.notify-label {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
}

.notify-desc {
  font-size: 13px;
  color: #909399;
  margin-top: 2px;
}

.security-section {
  padding: 16px 0;
}

.security-section h3 {
  margin: 0 0 16px;
  font-size: 18px;
}

.security-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.security-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border: 1px solid #e4e7ed;
  border-radius: 12px;
}

.security-label {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
}

.security-desc {
  font-size: 13px;
  color: #909399;
  margin-top: 2px;
}

.code-row {
  display: flex;
  gap: 12px;
  width: 100%;
}

.code-row .el-input {
  flex: 1;
}
</style>
