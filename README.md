# 天气预报系统

基于 Spring AI Alibaba 的智能天气预报与灾害预警平台，提供天气查询、灾害预警、多Agent地图分析和天气订阅功能。

## 技术栈

### 后端
- **框架**: Spring Boot 3.5.6 + Java 17
- **AI**: Spring AI Alibaba 1.1.2.0 (DashScope / 通义千问)
- **Graph**: Spring AI Alibaba Graph (多Agent协作)
- **数据库**: MySQL + MyBatis-Plus 3.5.8
- **认证**: Sa-Token 1.44.0
- **缓存**: Redis + Redisson 3.24.3
- **向量检索**: Milvus 2.4.11
- **API文档**: SpringDoc OpenAPI 2.3.0

### 前端
- **框架**: Vue 3.5 + TypeScript
- **地图**: Leaflet 1.9

## 项目结构

```
WeatherForecast/
├── src/main/java/com/wf/
│   ├── agent/
│   │   ├── graph/              # 天气预测Agent (Graph编排)
│   │   │   ├── node/           # 预测节点: 天气查询、灾害评估、预警生成
│   │   │   └── WeatherGraphConfig.java
│   │   └── map/                # 地图分析Agent (多Agent协作)
│   │       ├── controller/     # MapAgentController, MapController
│   │       ├── entity/         # CityWeatherDaily, HistoricalCase
│   │       ├── graph/          # MapInsightGraph (父图)
│   │       │   ├── node/       # TrendAgent, SeasonAgent, ImpactAgent, 仲裁节点
│   │       │   └── subgraph/   # 三个子图配置
│   │       └── service/        # 地图服务、案例归档、评测服务
│   ├── config/                 # 配置类: SaToken, Redis, MCP, Milvus
│   ├── job/                    # 定时任务: 天气预警、紧急响应
│   ├── mapper/                 # MyBatis-Plus Mapper
│   ├── object/                 # 实体类、DTO、VO
│   └── service/                # 业务服务层
├── front/web/                  # Vue3 前端
│   └── src/
│       ├── views/              # 页面: HomeView, MapView, LoginView, SubscribeView, SettingsView
│       ├── stores/             # Pinia: user, chat
│       ├── api/                # API封装
│       └── router/             # 路由配置
└── sql/ddl/                    # 数据库建表脚本
```

## 核心功能

### 1. 智能天气对话

用户输入自然语言问题（如"明天北京天气怎么样？"），后端通过 Graph 编排的 4 个 Node 串联执行：

- **WeatherPredictionNode**：调用 MCP 天气服务获取 24 小时天气码序列
- **WeatherAnalysisAndAssessmentNode**：AI 根据天气码选择对应 Skill，按规则判定灾害等级（1-3 级）
- **DisasterLevelAssessmentNode**：等级二次校验
- **AlertTextGenerationNode**：生成预警通告和处置建议

> 技术：MCP 天气工具 + Skill 规则引擎 + AI 推理

### 2. 灾害预警地图

地图页展示全国城市天气状态，点击"智能分析"可触发 MapInsightAgent 多Agent协作：

- **TrendAgent**：分析周边 100km 内灾害城市的经纬度分布，判断传播方向和严重度
- **SeasonAgent**：查询历史案例表，统计同季节灾害频次，给出季节性风险评估
- **ImpactAgent**：计算灾害城市占比，判定影响范围（局部/区域/广泛）
- **仲裁节点**：检测 3 个子图结果冲突，按优先级裁决后生成最终报告

> 技术：Spring AI Alibaba Graph 子图编排 + 并行执行 + 冲突仲裁

### 3. 天气订阅

用户选择关心的天气类型和监控模式（仅一次/持续监控），系统每日 0:05 定时检测，匹配时通过邮箱/短信/微信推送通知。

### 4. 评测层——还没做
 
- **历史案例回放**：加载历史案例重新执行分析，对比决策链差异
- **消融测试**：关闭某个子图对比结果差异
- **评分维度**：准确性、提前量、一致性、响应时延


## 快速开始

### 环境要求
- JDK 17+
- Node.js 20+
- MySQL 8.0+
- Redis
- Milvus (可选)

### 后端启动

```bash
# 1. 导入数据库
mysql -u root -p < sql/ddl/*.sql

# 2. 配置 application.yml
# 修改数据库连接、Redis、DashScope API Key等

# 3. 编译运行
mvn clean package -DskipTests
java -jar target/wf-0.0.1-SNAPSHOT.jar
```

### 前端启动

```bash
cd front/web
npm install
npm run dev
```

访问 http://localhost:5173

## API 接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/map/weather-data` | GET | 获取地图天气数据 |
| `/map/analyze` | POST | 多Agent地图分析 |
| `/map/evaluate/replay` | POST | 历史案例回放 |
| `/map/evaluate/ablation` | POST | 消融测试 |
| `/weather/ask` | POST | 智能天气问答 |
| `/weather/subscribe` | POST | 创建天气订阅 |
| `/auth/login` | POST | 邮箱验证码登录 |

## 天气码说明

| 码值 | 天气 | 灾害等级 |
|------|------|----------|
| 1 | 晴 | - |
| 7 | 多云 | - |
| 8 | 阴 | - |
| 46 | 小雨 | 3级(轻微) |
| 47 | 中雨 | 2级(中等) |
| 48 | 大雨 | 1级(严重) |
| 49 | 暴雨 | 1级(严重) |
| 15 | 雷阵雨 | 2级(中等) |
| 33 | 雾 | 2级(中等) |
| 75 | 霾 | - |

## 架构说明

### 多Agent协作架构

```
┌─────────────────────────────────────────────────────────┐
│                    MapInsightGraph                       │
│  ┌──────────┐  ┌──────────  ┌──────────┐              │
│  │TrendAgent│  │SeasonAgent│ │ImpactAgent│  (并行执行)   │
│  │  子图     │  │  子图     │  │  子图     │              │
│  └────┬─────┘  └────┬─────  └────┬─────┘              │
│       └──────────────┼──────────────┘                    │
│                      ▼                                   │
│              ┌──────────────┐                           │
│              │ Arbitration  │  (冲突仲裁)                │
│              └──────┬───────┘                           │
│                     ▼                                   │
│              ┌──────────────┐                           │
│              │  Response    │  (生成最终报告)            │
│              └──────────────┘                           │
└─────────────────────────────────────────────────────────┘
```

### 天气预测 Graph 流程

```
START → WeatherPredictionNode → DisasterAssessmentNode 
        → DisasterLevelAssessmentNode → AlertTextGenerationNode → END
```

## License

MIT
