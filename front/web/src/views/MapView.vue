<template>
  <div class="map-view">
    <el-header class="map-header">
      <div class="header-left" @click="router.push('/chat')" style="cursor: pointer;">
        <el-icon :size="28" color="#409eff"><MapLocation /></el-icon>
        <span class="header-title">灾害预警地图</span>
      </div>
      <div class="header-right">
        <el-date-picker
          v-model="selectedDate"
          type="date"
          placeholder="选择日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          size="small"
          @change="loadWeatherData"
          :clearable="false"
        />
        <el-button text @click="loadWeatherData" :loading="loading">
          <el-icon><Refresh /></el-icon>
          <span>刷新</span>
        </el-button>
        <el-button text @click="showAgentPanel = !showAgentPanel">
          <el-icon><ChatLineRound /></el-icon>
          <span>{{ showAgentPanel ? '隐藏' : '显示' }}智能分析</span>
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
              <el-dropdown-item @click="userStore.logout()">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>

    <div class="main-content">
      <div class="map-container">
        <div v-loading="loading" element-loading-text="加载地图数据中..." id="map" ref="mapRef" class="map-instance"></div>

        <div class="legend">
          <h4>天气图例</h4>
          <div class="legend-item">
            <span class="dot" style="background: #52C41A;"></span>
            <span>正常</span>
          </div>
          <div class="legend-item">
            <span class="dot" style="background: #FF4444;"></span>
            <span>灾害</span>
          </div>
          <div class="legend-divider"></div>
          <h4>天气类型</h4>
          <div class="legend-weather-grid">
            <div class="legend-weather-item"><span class="dot" style="background: #FFD700;"></span><span>晴</span></div>
            <div class="legend-weather-item"><span class="dot" style="background: #87CEEB;"></span><span>多云</span></div>
            <div class="legend-weather-item"><span class="dot" style="background: #A9A9A9;"></span><span>阴</span></div>
            <div class="legend-weather-item"><span class="dot" style="background: #87CEFA;"></span><span>小雨</span></div>
            <div class="legend-weather-item"><span class="dot" style="background: #4682B4;"></span><span>中雨</span></div>
            <div class="legend-weather-item"><span class="dot" style="background: #1E90FF;"></span><span>大雨</span></div>
            <div class="legend-weather-item"><span class="dot" style="background: #0000CD;"></span><span>暴雨</span></div>
            <div class="legend-weather-item"><span class="dot" style="background: #800080;"></span><span>雷阵雨</span></div>
            <div class="legend-weather-item"><span class="dot" style="background: #D3D3D3;"></span><span>雾</span></div>
            <div class="legend-weather-item"><span class="dot" style="background: #DEB887;"></span><span>霾</span></div>
          </div>
        </div>

        <div class="stats-bar">
          <div class="stat-item">
            <span class="stat-label">城市数</span>
            <span class="stat-value">{{ cities.length }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">灾害城市</span>
            <span class="stat-value disaster">{{ disasterCityCount }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">查询日期</span>
            <span class="stat-value">{{ selectedDate }}</span>
          </div>
        </div>
      </div>

      <div v-if="showAgentPanel" class="agent-panel">
        <div class="panel-header">
          <h3>MapInsightAgent 智能分析</h3>
          <el-button text @click="showAgentPanel = false" size="small">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>

        <div class="query-section">
          <el-input
            v-model="agentQuery"
            type="textarea"
            :rows="3"
            placeholder="输入你的分析需求，如：分析北京周边100公里内的灾害分布"
          />
          <el-button type="primary" @click="runAgentAnalysis" :loading="analyzing" class="analyze-btn">
            <el-icon v-if="!analyzing"><Search /></el-icon>
            {{ analyzing ? '分析中...' : '分析' }}
          </el-button>
        </div>

        <div v-if="agentResult" class="result-section">
          <el-collapse v-model="activeCollapse">
            <el-collapse-item title="分析结论" name="conclusion">
              <p class="conclusion">{{ agentResult.conclusion }}</p>
            </el-collapse-item>
            <el-collapse-item title="分析过程" name="explanation">
              <pre class="explanation">{{ agentResult.explanation }}</pre>
            </el-collapse-item>
            <el-collapse-item title="使用工具" name="tools">
              <el-tag
                v-for="tool in agentResult.toolsUsed"
                :key="tool"
                size="small"
                class="tool-tag"
              >
                {{ tool }}
              </el-tag>
            </el-collapse-item>
            <el-collapse-item title="关键数据点" name="data">
              <div class="data-points">
                <div
                  v-for="point in agentResult.dataPoints"
                  :key="point.cityCode"
                  class="data-point"
                >
                  <span class="point-name">{{ point.cityName }}</span>
                  <el-tag :type="point.dataType === 'DISASTER' ? 'danger' : 'success'" size="small">
                    {{ point.dataType === 'DISASTER' ? '灾害' : '正常' }}
                  </el-tag>
                </div>
              </div>
            </el-collapse-item>
          </el-collapse>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

const router = useRouter()
const userStore = useUserStore()

const mapRef = ref(null)
const map = ref(null)
const markers = ref([])
const cities = ref([])
const selectedDate = ref(new Date().toISOString().split('T')[0])
const showAgentPanel = ref(false)
const agentQuery = ref('')
const analyzing = ref(false)
const agentResult = ref(null)
const loading = ref(false)
const activeCollapse = ref(['conclusion'])

const disasterCityCount = computed(() => cities.value.filter(c => c.hasDisaster).length)

const weatherCodeMap = {
  1: { label: '晴', icon: '☀️', color: '#FFD700' },
  7: { label: '多云', icon: '⛅', color: '#87CEEB' },
  8: { label: '阴', icon: '☁️', color: '#A9A9A9' },
  46: { label: '小雨', icon: '🌦️', color: '#87CEFA' },
  47: { label: '中雨', icon: '🌧️', color: '#4682B4' },
  48: { label: '大雨', icon: '⛈️', color: '#1E90FF' },
  49: { label: '暴雨', icon: '🌊', color: '#0000CD' },
  15: { label: '雷阵雨', icon: '⚡', color: '#800080' },
  33: { label: '雾', icon: '🌫️', color: '#D3D3D3' },
  75: { label: '霾', icon: '😷', color: '#DEB887' },
}

const initMap = () => {
  map.value = L.map('map', {
    center: [35.8617, 104.1954],
    zoom: 4,
    minZoom: 3,
    maxZoom: 18
  })

  L.tileLayer('https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}', {
    subdomains: ['1', '2', '3', '4'],
    attribution: '&copy; 高德地图'
  }).addTo(map.value)
}

const clearMarkers = () => {
  markers.value.forEach(marker => {
    map.value.removeLayer(marker)
  })
  markers.value = []
}

const getWeatherInfo = (code) => {
  return weatherCodeMap[code] || { label: '未知', icon: '❓', color: '#CCC' }
}

const getRiskLevelText = (level) => {
  switch (level) {
    case 1: return '严重'
    case 2: return '中等'
    case 3: return '轻微'
    default: return '-'
  }
}

const addCityMarkers = () => {
  clearMarkers()

  cities.value.forEach(city => {
    const color = city.hasDisaster ? '#FF4444' : '#52C41A'
    const weatherInfo = getWeatherInfo(city.dayWeatherCode)

    const customIcon = L.divIcon({
      className: 'custom-marker-container',
      html: `
        <div class="custom-marker" style="background-color: ${color};">
          <span class="marker-icon">${weatherInfo.icon}</span>
          <span class="marker-text">${city.cityName}</span>
        </div>
      `,
      iconSize: [70, 30],
      iconAnchor: [35, 15]
    })

    const tooltipContent = `
      <div style="text-align: center; min-width: 140px;">
        <h4 style="margin: 0 0 8px 0; font-size: 14px;">${city.cityName}</h4>
        <p style="margin: 4px 0; font-size: 13px;">${weatherInfo.icon} ${weatherInfo.label}</p>
        ${city.hasDisaster 
          ? `<p style="color: #FF4444; margin: 4px 0; font-weight: bold; font-size: 13px;">⚠️ ${getRiskLevelText(city.maxDisasterLevel)} (${city.disasterTypes || '灾害'})</p>` 
          : '<p style="color: #52C41A; margin: 4px 0; font-size: 13px;">✅ 天气正常</p>'}
      </div>
    `

    const marker = L.marker([city.latitude, city.longitude], { icon: customIcon })
      .addTo(map.value)
      .bindTooltip(tooltipContent, {
        permanent: false,
        direction: 'top',
        offset: [0, -15],
        className: 'city-tooltip'
      })

    markers.value.push(marker)
  })
}

const loadWeatherData = async () => {
  loading.value = true
  try {
    const response = await axios.get('/api2/v1/map/weather-data', {
      params: { date: selectedDate.value }
    })
    if (response.data.code === 200) {
      cities.value = response.data.data.cities || []
      addCityMarkers()
      if (map.value) {
        setTimeout(() => map.value.invalidateSize(), 100)
      }
    }
  } catch (error) {
    ElMessage.error('加载地图数据失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const runAgentAnalysis = async () => {
  if (!agentQuery.value.trim()) {
    ElMessage.warning('请输入分析需求')
    return
  }

  analyzing.value = true
  agentResult.value = null
  try {
    const response = await axios.post('/map/analyze', {
      query: agentQuery.value,
      date: selectedDate.value
    })
    if (response.data.code === 200) {
      agentResult.value = response.data.data
      activeCollapse.value = ['conclusion', 'explanation']
    }
  } catch (error) {
    ElMessage.error('分析失败')
    console.error(error)
  } finally {
    analyzing.value = false
  }
}

onMounted(() => {
  initMap()
  loadWeatherData()
})

onUnmounted(() => {
  if (map.value) {
    map.value.remove()
  }
})
</script>

<style scoped>
.map-view {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.map-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
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
  gap: 12px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background 0.2s;
}

.user-info:hover {
  background: #f5f7fa;
}

.username {
  font-size: 14px;
  color: #606266;
}

.main-content {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.map-container {
  flex: 1;
  position: relative;
}

.map-instance {
  width: 100%;
  height: 100%;
}

:global(.custom-marker-container) {
  background: transparent !important;
  border: none !important;
}

:global(.custom-marker) {
  width: 70px;
  height: 30px;
  border-radius: 15px;
  border: 2px solid #fff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  cursor: pointer;
  transition: transform 0.2s;
}

:global(.custom-marker:hover) {
  transform: scale(1.1);
}

:global(.marker-icon) {
  font-size: 12px;
  line-height: 1;
}

:global(.marker-text) {
  font-size: 11px;
  color: #fff;
  font-weight: bold;
  text-shadow: 0 1px 2px rgba(0,0,0,0.5);
  white-space: nowrap;
}

.legend {
  position: absolute;
  bottom: 20px;
  left: 20px;
  background: rgba(255,255,255,0.95);
  padding: 16px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
  z-index: 1000;
  min-width: 160px;
}

.legend h4 {
  margin: 0 0 8px 0;
  font-size: 13px;
  color: #303133;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  font-size: 12px;
  color: #606266;
}

.legend-divider {
  height: 1px;
  background: #e4e7ed;
  margin: 8px 0;
}

.legend-weather-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px;
}

.legend-weather-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: #606266;
}

.legend-weather-item .dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.legend-item .dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
}

.stats-bar {
  position: absolute;
  top: 12px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(255,255,255,0.95);
  padding: 8px 24px;
  border-radius: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  z-index: 1000;
  display: flex;
  gap: 24px;
  align-items: center;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.stat-label {
  font-size: 12px;
  color: #909399;
}

.stat-value {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.stat-value.disaster {
  color: #FF4444;
}

.agent-panel {
  width: 400px;
  background: #fff;
  border-left: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #e4e7ed;
}

.panel-header h3 {
  margin: 0;
  font-size: 15px;
  color: #303133;
}

.query-section {
  padding: 16px 20px;
  border-bottom: 1px solid #e4e7ed;
}

.analyze-btn {
  margin-top: 12px;
  width: 100%;
}

.result-section {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
}

.conclusion {
  font-size: 14px;
  color: #303133;
  line-height: 1.6;
  margin: 0;
}

.explanation {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  white-space: pre-wrap;
  margin: 0;
}

.tool-tag {
  margin-right: 8px;
  margin-bottom: 8px;
}

.data-points {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.data-point {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 13px;
}

.point-name {
  color: #303133;
}

.point-type {
  color: #909399;
}

:deep(.city-tooltip) {
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
  padding: 12px;
}

:deep(.city-tooltip .leaflet-tooltip-content) {
  margin: 0;
}

:deep(.el-collapse-item__header) {
  font-size: 13px;
}
</style>
