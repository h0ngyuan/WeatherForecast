<script setup lang="ts">
import { ref, nextTick, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useChatStore } from '@/stores/chat'
import { askWeather, subscribeWeather, type SubscribeParams } from '@/api/weather'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const chatStore = useChatStore()

const inputMessage = ref('')
const sending = ref(false)
const messageListRef = ref<HTMLDivElement>()

const subscribeDialogVisible = ref(false)
const subscribeFormRef = ref<FormInstance>()
const subscribeLoading = ref(false)
const subscribeForm = ref<SubscribeParams>({
  subscribeName: '',
  location: '',
  weatherCodes: [],
  taskType: 1,
  notifyCondition: '',
  disasterLevel: 3,
})

const weatherOptions = [
  { label: '晴', value: 1, icon: '☀️' },
  { label: '多云', value: 7, icon: '⛅' },
  { label: '阴', value: 8, icon: '☁️' },
  { label: '小雨', value: 46, icon: '🌦️' },
  { label: '中雨', value: 47, icon: '🌧️' },
  { label: '大雨', value: 48, icon: '⛈️' },
  { label: '暴雨', value: 49, icon: '🌊' },
  { label: '雷阵雨', value: 15, icon: '⚡' },
  { label: '雾', value: 33, icon: '🌫️' },
  { label: '霾', value: 75, icon: '😷' },
]

const subscribeRules: FormRules = {
  subscribeName: [{ required: true, message: '请输入订阅名称', trigger: 'blur' }],
  weatherCodes: [{ required: true, message: '请选择天气条件', trigger: 'change', type: 'array' }],
}

const quickQuestions = [
  '明天北京天气怎么样？',
  '杭州未来几天会下雨吗？',
  '成都有大风预警吗？',
  '上海适合户外活动吗？',
]

function scrollToBottom() {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

async function sendMessage(content?: string) {
  const msg = content || inputMessage.value.trim()
  if (!msg || sending.value) return

  inputMessage.value = ''
  chatStore.addUserMessage(msg)
  scrollToBottom()

  sending.value = true
  const loadingId = chatStore.addLoadingMessage()
  scrollToBottom()

  try {
    const res: any = await askWeather({ question: msg })
    chatStore.updateMessage(loadingId, res.data?.answer || '抱歉，未能获取到天气信息。')
  } catch {
    chatStore.updateMessage(loadingId, '请求失败，请稍后重试。')
  } finally {
    sending.value = false
    scrollToBottom()
  }
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}

function formatTime(timestamp: number) {
  return new Date(timestamp).toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
  })
}

function openSubscribeDialog() {
  subscribeDialogVisible.value = true
  subscribeForm.value = {
    subscribeName: '',
    location: '',
    weatherCodes: [],
    taskType: 1,
    notifyCondition: '',
    disasterLevel: 3,
  }
}

function toggleWeatherCode(code: number) {
  const index = subscribeForm.value.weatherCodes.indexOf(code)
  if (index > -1) {
    subscribeForm.value.weatherCodes.splice(index, 1)
  } else {
    subscribeForm.value.weatherCodes.push(code)
  }
}

function isWeatherCodeSelected(code: number): boolean {
  return subscribeForm.value.weatherCodes.includes(code)
}

async function handleSubscribeSubmit() {
  const valid = await subscribeFormRef.value?.validate().catch(() => false)
  if (!valid) return

  subscribeLoading.value = true
  try {
    await subscribeWeather(subscribeForm.value)
    ElMessage.success('订阅创建成功')
    subscribeDialogVisible.value = false
  } catch {
  } finally {
    subscribeLoading.value = false
  }
}

function clearChat() {
  chatStore.clearMessages()
  inputMessage.value = ''
}

onMounted(() => {
  userStore.fetchUserInfo()
})
</script>

<template>
  <div class="home-container">
    <el-container class="home-layout">
      <el-header class="home-header">
        <div class="header-left">
          <el-icon :size="28" color="#409eff"><PartlyCloudy /></el-icon>
          <span class="header-title">天气预报系统</span>
        </div>
        <div class="header-right">
          <el-button text @click="openSubscribeDialog">
            <el-icon><Bell /></el-icon>
            <span>天气订阅</span>
          </el-button>
          <el-button text @click="router.push('/map')">
            <el-icon><MapLocation /></el-icon>
            <span>地图</span>
          </el-button>
          <el-button text @click="router.push('/settings')">
            <el-icon><Setting /></el-icon>
            <span>设置</span>
          </el-button>
          <el-dropdown trigger="click">
            <span class="user-info">
              <el-avatar :size="32" icon="UserFilled" />
              <span class="username">{{ userStore.userInfo?.nickname || '用户' }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="clearChat">
                  <el-icon><Delete /></el-icon>
                  清空对话
                </el-dropdown-item>
                <el-dropdown-item @click="userStore.logout()">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="chat-main">
        <div v-if="chatStore.messages.length === 0" class="welcome-section">
          <div class="welcome-icon">
            <el-icon :size="64" color="#409eff"><PartlyCloudy /></el-icon>
          </div>
          <h2>欢迎使用天气预报系统</h2>
          <p>我是您的智能天气助手，可以为您查询天气、提供出行建议和灾害预警</p>
          <div class="quick-questions">
            <div
              v-for="q in quickQuestions"
              :key="q"
              class="quick-item"
              @click="sendMessage(q)"
            >
              <el-icon><ChatLineRound /></el-icon>
              <span>{{ q }}</span>
            </div>
          </div>
        </div>

        <div v-else ref="messageListRef" class="message-list">
          <div
            v-for="msg in chatStore.messages"
            :key="msg.id"
            :class="['message-item', msg.role]"
          >
            <div class="message-avatar">
              <el-avatar
                v-if="msg.role === 'assistant'"
                :size="36"
                icon="Monitor"
                class="ai-avatar"
              />
              <el-avatar v-else :size="36" icon="UserFilled" />
            </div>
            <div class="message-content">
              <div class="message-bubble">
                <div v-if="msg.loading" class="typing-indicator">
                  <span></span><span></span><span></span>
                </div>
                <div v-else class="message-text" v-html="msg.content.replace(/\n/g, '<br/>')"></div>
              </div>
              <div class="message-time">{{ formatTime(msg.timestamp) }}</div>
            </div>
          </div>
        </div>
      </el-main>

      <el-footer class="chat-footer" height="auto">
        <div class="input-area">
          <el-input
            v-model="inputMessage"
            type="textarea"
            :rows="2"
            placeholder="输入您的天气问题，如：明天北京天气如何？"
            resize="none"
            @keydown="handleKeydown"
          />
          <el-button
            type="primary"
            :icon="sending ? '' : 'Promotion'"
            :loading="sending"
            :disabled="!inputMessage.trim() || sending"
            @click="sendMessage()"
          >
            发送
          </el-button>
        </div>
      </el-footer>
    </el-container>

    <el-dialog
      v-model="subscribeDialogVisible"
      title="创建天气订阅"
      width="720px"
      :close-on-click-modal="false"
      class="subscribe-dialog"
    >
      <el-form
        ref="subscribeFormRef"
        :model="subscribeForm"
        :rules="subscribeRules"
        label-position="top"
        size="default"
      >
        <el-form-item label="订阅名称" prop="subscribeName">
          <el-input
            v-model="subscribeForm.subscribeName"
            placeholder="如：雨天出行提醒"
          />
        </el-form-item>

        <el-form-item label="天气条件" prop="weatherCodes">
          <div class="weather-menu">
            <div
              v-for="opt in weatherOptions"
              :key="opt.value"
              class="weather-dish"
              :class="{ selected: isWeatherCodeSelected(opt.value) }"
              @click="toggleWeatherCode(opt.value)"
            >
              <span class="dish-icon">{{ opt.icon }}</span>
              <span class="dish-name">{{ opt.label }}</span>
              <el-icon v-if="isWeatherCodeSelected(opt.value)" class="dish-check"><Check /></el-icon>
            </div>
          </div>
        </el-form-item>

        <el-form-item label="监控模式">
          <el-radio-group v-model="subscribeForm.taskType">
            <el-radio :label="0">仅一次</el-radio>
            <el-radio :label="1">持续监控</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="补充说明">
          <el-input
            v-model="subscribeForm.notifyCondition"
            type="textarea"
            :rows="2"
            placeholder="选填，描述您希望在什么情况下收到通知"
            resize="none"
          />
        </el-form-item>

        <div class="location-hint">
          <el-icon><Location /></el-icon>
          <span>监控地点将根据您的IP自动定位，灾害等级由系统根据天气类型自动判定</span>
        </div>
      </el-form>

      <template #footer>
        <el-button @click="subscribeDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="subscribeLoading"
          @click="handleSubscribeSubmit"
        >
          创建订阅
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.home-container {
  height: 100vh;
  overflow: hidden;
}

.home-layout {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.home-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
  height: 60px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
  transition: background 0.2s;
}

.user-info:hover {
  background: #f5f7fa;
}

.username {
  font-size: 14px;
  color: #606266;
}

.chat-main {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  background: linear-gradient(180deg, #f0f4ff 0%, #f5f7fa 100%);
}

.welcome-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
  color: #606266;
  animation: fadeIn 0.5s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.welcome-section h2 {
  margin: 16px 0 8px;
  color: #303133;
  font-size: 24px;
}

.welcome-section p {
  margin-bottom: 32px;
  color: #909399;
  font-size: 15px;
}

.quick-questions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 12px;
  max-width: 600px;
}

.quick-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  background: #fff;
  border-radius: 20px;
  border: 1px solid #e4e7ed;
  cursor: pointer;
  font-size: 14px;
  color: #606266;
  transition: all 0.2s;
}

.quick-item:hover {
  border-color: #409eff;
  color: #409eff;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 800px;
  margin: 0 auto;
}

.message-item {
  display: flex;
  gap: 12px;
  animation: messageIn 0.3s ease;
}

@keyframes messageIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.message-item.user {
  flex-direction: row-reverse;
}

.message-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-width: 70%;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
}

.message-item.assistant .message-bubble {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-top-left-radius: 4px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.message-item.user .message-bubble {
  background: linear-gradient(135deg, #409eff, #337ecc);
  color: #fff;
  border-top-right-radius: 4px;
}

.message-time {
  font-size: 12px;
  color: #909399;
}

.message-item.user .message-time {
  text-align: right;
}

.ai-avatar {
  background: #409eff;
}

.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 8px 0;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  background: #c0c4cc;
  border-radius: 50%;
  animation: typing 1.4s infinite;
}

.typing-indicator span:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-indicator span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 60%, 100% { transform: translateY(0); }
  30% { transform: translateY(-10px); }
}

.chat-footer {
  padding: 16px 24px;
  background: #fff;
  border-top: 1px solid #e4e7ed;
}

.input-area {
  display: flex;
  gap: 12px;
  max-width: 800px;
  margin: 0 auto;
}

.input-area .el-input {
  flex: 1;
}

.input-area .el-button {
  height: auto;
  padding: 0 24px;
}

:deep(.subscribe-dialog) {
  border-radius: 12px;
}

:deep(.subscribe-dialog .el-dialog__header) {
  margin: 0;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
}

:deep(.subscribe-dialog .el-dialog__body) {
  padding: 16px 20px;
}

:deep(.subscribe-dialog .el-dialog__footer) {
  padding: 12px 20px;
  border-top: 1px solid #f0f0f0;
}

:deep(.subscribe-dialog .el-form-item) {
  margin-bottom: 12px;
}

:deep(.subscribe-dialog .el-form-item__label) {
  font-weight: 500;
  padding-bottom: 4px;
  line-height: 1.4;
}

.weather-menu {
  display: grid;
  grid-template-columns: repeat(10, 1fr);
  gap: 8px;
}

.weather-dish {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  padding: 8px 2px;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
  background: #fafafa;
  cursor: pointer;
  transition: all 0.15s;
  position: relative;
}

.weather-dish:hover {
  border-color: #c0c4cc;
  background: #f5f5f5;
}

.weather-dish.selected {
  border-color: #409eff;
  background: #f0f9ff;
}

.dish-icon {
  font-size: 20px;
  line-height: 1;
}

.dish-name {
  font-size: 12px;
  color: #606266;
}

.weather-dish.selected .dish-name {
  color: #409eff;
  font-weight: 500;
}

.dish-check {
  position: absolute;
  top: 2px;
  right: 2px;
  font-size: 10px;
  color: #409eff;
}

.form-row {
  display: flex;
  gap: 24px;
}

.flex-1 {
  flex: 1;
}

.location-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.location-hint .el-icon {
  color: #409eff;
}
</style>
