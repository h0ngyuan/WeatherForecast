<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { subscribeWeather, type SubscribeParams } from '@/api/weather'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = ref<SubscribeParams>({
  subscribeName: '',
  location: '',
  weatherCodes: [],
  taskType: 1,
  notifyCondition: '',
  disasterLevel: 3,
  alwaysRemind: 0,
})

const weatherOptions = [
  { label: '晴', value: 1, icon: '☀️', color: '#f59e0b', bg: '#fffbeb' },
  { label: '多云', value: 7, icon: '⛅', color: '#6b7280', bg: '#f3f4f6' },
  { label: '阴', value: 8, icon: '☁️', color: '#9ca3af', bg: '#f3f4f6' },
  { label: '小雨', value: 46, icon: '🌦️', color: '#3b82f6', bg: '#eff6ff' },
  { label: '中雨', value: 47, icon: '🌧️', color: '#2563eb', bg: '#dbeafe' },
  { label: '大雨', value: 48, icon: '⛈️', color: '#1d4ed8', bg: '#bfdbfe' },
  { label: '暴雨', value: 49, icon: '🌊', color: '#1e3a8a', bg: '#93c5fd' },
  { label: '雷阵雨', value: 15, icon: '⛈️', color: '#7c3aed', bg: '#ede9fe' },
  { label: '雾', value: 33, icon: '🌫️', color: '#64748b', bg: '#f1f5f9' },
  { label: '霾', value: 75, icon: '😷', color: '#a16207', bg: '#fef3c7' },
]

const rules = ref<FormRules>({
  subscribeName: [{ required: true, message: '请输入订阅名称', trigger: 'blur' }],
  weatherCodes: [{ required: true, message: '请选择天气条件', trigger: 'change', type: 'array' }],
})

function toggleWeatherCode(code: number) {
  const index = form.value.weatherCodes.indexOf(code)
  if (index > -1) {
    form.value.weatherCodes.splice(index, 1)
  } else {
    form.value.weatherCodes.push(code)
  }
}

function isWeatherCodeSelected(code: number): boolean {
  return form.value.weatherCodes.includes(code)
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await subscribeWeather(form.value)
    ElMessage.success('订阅创建成功')
    router.push('/')
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="subscribe-container">
    <el-container class="subscribe-layout">
      <el-header class="page-header">
        <div class="header-left">
          <el-button text @click="router.push('/')">
            <el-icon><ArrowLeft /></el-icon>
            返回
          </el-button>
        </div>
        <h2 class="header-title">创建天气订阅</h2>
        <div style="width: 80px"></div>
      </el-header>

      <el-main class="page-main">
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          size="large"
          class="subscribe-form"
        >
          <div class="form-row">
            <div class="form-block name-block">
              <h3 class="block-title">
                <el-icon><Edit /></el-icon>
                订阅名称
              </h3>
              <el-form-item prop="subscribeName">
                <el-input
                  v-model="form.subscribeName"
                  placeholder="如：雨天出行提醒、周末晴天提醒"
                />
              </el-form-item>
            </div>

            <div class="form-block mode-block">
              <h3 class="block-title">
                <el-icon><Timer /></el-icon>
                监控模式
              </h3>
              <el-form-item>
                <div class="mode-options">
                  <div
                    class="mode-item"
                    :class="{ active: form.taskType === 0 }"
                    @click="form.taskType = 0"
                  >
                    <div class="mode-icon orange">
                      <el-icon><Flag /></el-icon>
                    </div>
                    <span class="mode-label">仅一次</span>
                  </div>
                  <div
                    class="mode-item"
                    :class="{ active: form.taskType === 1 }"
                    @click="form.taskType = 1"
                  >
                    <div class="mode-icon green">
                      <el-icon><Refresh /></el-icon>
                    </div>
                    <span class="mode-label">持续监控</span>
                  </div>
                </div>
              </el-form-item>
            </div>
          </div>

          <div class="form-block weather-block">
            <h3 class="block-title">
              <el-icon><PartlyCloudy /></el-icon>
              天气条件
              <span class="title-hint">选择您关心的天气类型</span>
            </h3>
            <el-form-item prop="weatherCodes">
              <div class="weather-grid">
                <div
                  v-for="opt in weatherOptions"
                  :key="opt.value"
                  class="weather-card"
                  :class="{ active: isWeatherCodeSelected(opt.value) }"
                  :style="isWeatherCodeSelected(opt.value) ? {
                    background: opt.bg,
                    borderColor: opt.color
                  } : {}"
                  @click="toggleWeatherCode(opt.value)"
                >
                  <span class="weather-icon">{{ opt.icon }}</span>
                  <span class="weather-name">{{ opt.label }}</span>
                  <div v-if="isWeatherCodeSelected(opt.value)" class="check-mark">
                    <el-icon><Check /></el-icon>
                  </div>
                </div>
              </div>
            </el-form-item>
          </div>

          <div class="form-block desc-block">
            <h3 class="block-title">
              <el-icon><Document /></el-icon>
              补充说明
              <span class="title-hint optional">选填</span>
            </h3>
            <el-form-item>
              <el-input
                v-model="form.notifyCondition"
                type="textarea"
                :rows="3"
                placeholder="描述一下您希望在什么情况下收到通知..."
                resize="none"
              />
            </el-form-item>
          </div>

          <div class="form-footer">
            <div class="location-tip">
              <el-icon><Location /></el-icon>
              <span>监控地点将根据您的IP自动定位</span>
            </div>
            <el-button
              type="primary"
              size="large"
              :loading="loading"
              class="submit-btn"
              @click="handleSubmit"
            >
              <el-icon v-if="!loading"><Bell /></el-icon>
              {{ loading ? '创建中...' : '创建订阅' }}
            </el-button>
          </div>
        </el-form>
      </el-main>
    </el-container>
  </div>
</template>

<style scoped>
.subscribe-container {
  height: 100vh;
  overflow: hidden;
  background: #f5f7fa;
}

.subscribe-layout {
  height: 100%;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 32px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  height: 60px;
  flex-shrink: 0;
}

.header-title {
  margin: 0;
  font-size: 18px;
  color: #303133;
}

.page-main {
  flex: 1;
  padding: 32px;
  overflow-y: auto;
}

.subscribe-form {
  max-width: 1200px;
  margin: 0 auto;
}

/* 第一行：名称和模式并排 */
.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  margin-bottom: 24px;
}

.form-block {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.block-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 16px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.block-title .el-icon {
  color: #409eff;
}

.title-hint {
  font-size: 13px;
  font-weight: 400;
  color: #909399;
  margin-left: 8px;
}

.title-hint.optional {
  background: #f4f4f5;
  padding: 2px 10px;
  border-radius: 4px;
  font-size: 12px;
}

.subscribe-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.subscribe-form :deep(.el-input__wrapper) {
  border-radius: 10px;
}

.subscribe-form :deep(.el-textarea__inner) {
  border-radius: 10px;
}

/* 模式选择 */
.mode-options {
  display: flex;
  gap: 16px;
}

.mode-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 20px;
  border-radius: 12px;
  border: 2px solid #e4e7ed;
  cursor: pointer;
  transition: all 0.2s;
  background: #fafafa;
}

.mode-item:hover {
  border-color: #c0c4cc;
}

.mode-item.active {
  border-color: #409eff;
  background: #f5f7fa;
}

.mode-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 22px;
}

.mode-icon.orange {
  background: linear-gradient(135deg, #f59e0b, #d97706);
}

.mode-icon.green {
  background: linear-gradient(135deg, #10b981, #059669);
}

.mode-label {
  font-size: 14px;
  font-weight: 500;
  color: #606266;
}

.mode-item.active .mode-label {
  color: #409eff;
  font-weight: 600;
}

/* 天气区块 */
.weather-block {
  margin-bottom: 24px;
}

.weather-grid {
  display: grid;
  grid-template-columns: repeat(10, 1fr);
  gap: 12px;
}

.weather-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 20px 8px;
  border-radius: 14px;
  border: 2px solid #e4e7ed;
  background: #fafafa;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}

.weather-card:hover {
  border-color: #c0c4cc;
  transform: translateY(-2px);
}

.weather-card.active {
  border-width: 2px;
}

.weather-icon {
  font-size: 32px;
  line-height: 1;
}

.weather-name {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.weather-card.active .weather-name {
  font-weight: 600;
}

.check-mark {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #67c23a;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 12px;
}

/* 描述区块 */
.desc-block {
  margin-bottom: 24px;
}

/* 底部 */
.form-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-radius: 16px;
  padding: 20px 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.location-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #909399;
}

.location-tip .el-icon {
  color: #409eff;
}

.submit-btn {
  padding: 0 40px;
  height: 48px;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
}
</style>
