# 地图模块多Agent协作重构任务清单

## 阶段1：数据结构和常量定义（预计1-2天）

### 1.1 定义DTO类
- [ ] 创建 `agent/map/dto/AgentReport.java` - Agent报告标准格式
- [ ] 创建 `agent/map/dto/ArbitrationResult.java` - 仲裁结果格式
- [ ] 创建 `agent/map/dto/ConflictRecord.java` - 冲突记录格式
- [ ] 创建 `agent/map/dto/EvaluationReport.java` - 评测报告格式
- [ ] 创建 `agent/map/dto/AblationReport.java` - 消融报告格式
- [ ] 创建 `agent/map/dto/AblationResult.java` - 消融结果（内部类可独立）
- [ ] 创建 `agent/map/dto/AgentContribution.java` - Agent贡献度（内部类可独立）
- [ ] 创建 `agent/map/dto/CaseEvaluation.java` - 案例评估详情（内部类可独立）
- [ ] 创建 `agent/map/dto/MapInsightResponse.java` - 地图分析响应
- [ ] 创建 `agent/map/dto/ReplayRequest.java` - 回放请求
- [ ] 创建 `agent/map/dto/AblationRequest.java` - 消融请求
- [ ] 创建 `agent/map/dto/BenchmarkRequest.java` - 批量评测请求
- [ ] 创建 `agent/map/dto/BenchmarkReport.java` - 批量评测报告

### 1.2 定义状态常量
- [ ] 创建 `agent/map/constants/MapGraphConstants.java` - 状态Key常量类

---

## 阶段2：TrendAgent子图实现（预计2-3天）

### 2.1 创建子图节点
- [ ] 创建 `agent/map/graph/node/TrendQueryNode.java`
  - 功能：查询目标城市周边N个城市天气数据
  - 输入：location, radiusKm, date
  - 输出：nearbyCities (List<CityWeatherVO>)
  - 依赖：SpatialQueryTools
  
- [ ] 创建 `agent/map/graph/node/TrendAnalysisNode.java`
  - 功能：分析灾害传播趋势（方向、严重程度）
  - 输入：nearbyCities
  - 输出：trendDirection, trendSeverity
  - 逻辑：
    - 按地理位置排序城市
    - 识别灾害梯度（从无到有到严重）
    - 判断传播方向
  
- [ ] 创建 `agent/map/graph/node/TrendReportNode.java`
  - 功能：生成趋势分析报告
  - 输入：trendDirection, trendSeverity, nearbyCities
  - 输出：trendReport, trendRiskLevel, trendConfidence

### 2.2 创建子图配置
- [ ] 创建 `agent/map/graph/TrendAgentGraph.java`
  - 定义KeyStrategyFactory
  - 构建StateGraph
  - 注册3个节点
  - 配置边连接
  - 编译为CompiledGraph

---

## 阶段3：SeasonAgent子图实现（预计2-3天）

### 3.1 创建子图节点
- [ ] 创建 `agent/map/graph/node/SeasonInfoNode.java`
  - 功能：获取当前季节信息
  - 输入：date
  - 输出：season, month
  - 逻辑：根据月份判断季节（春3-5/夏6-8/秋9-11/冬12-2）
  
- [ ] 创建 `agent/map/graph/node/SeasonHistoryNode.java`
  - 功能：查询历史季节灾害统计
  - 输入：location, season
  - 输出：historyStats
  - 依赖：HistoricalCaseMapper
  
- [ ] 创建 `agent/map/graph/node/SeasonEvalNode.java`
  - 功能：评估季节性风险
  - 输入：historyStats
  - 输出：seasonRiskBase
  - 逻辑：根据历史统计计算基线风险
  
- [ ] 创建 `agent/map/graph/node/SeasonReportNode.java`
  - 功能：生成季节评估报告
  - 输入：seasonRiskBase, season, historyStats
  - 输出：seasonReport, seasonRiskLevel, seasonConfidence

### 3.2 创建子图配置
- [ ] 创建 `agent/map/graph/SeasonAgentGraph.java`
  - 定义KeyStrategyFactory
  - 构建StateGraph
  - 注册4个节点
  - 配置边连接
  - 编译为CompiledGraph

---

## 阶段4：ImpactAgent子图实现（预计2-3天）

### 4.1 创建子图节点
- [ ] 创建 `agent/map/graph/node/DisasterQueryNode.java`
  - 功能：查找有灾害的周边城市
  - 输入：location, radiusKm, date
  - 输出：disasterCities (List<CityWeatherVO>)
  - 依赖：SpatialQueryTools
  
- [ ] 创建 `agent/map/graph/node/ImpactCalcNode.java`
  - 功能：计算影响程度
  - 输入：disasterCities, location
  - 输出：impactScore, impactDirection
  - 逻辑：
    - 计算距离权重
    - 计算灾害等级权重
    - 判断影响方向
  
- [ ] 创建 `agent/map/graph/node/ImpactReportNode.java`
  - 功能：生成影响分析报告
  - 输入：impactScore, impactDirection, disasterCities
  - 输出：impactReport, impactRiskLevel, impactConfidence

### 4.2 创建子图配置
- [ ] 创建 `agent/map/graph/ImpactAgentGraph.java`
  - 定义KeyStrategyFactory
  - 构建StateGraph
  - 注册3个节点
  - 配置边连接
  - 编译为CompiledGraph

---

## 阶段5：父图MapInsightGraph实现（预计2-3天）

### 5.1 创建父图节点
- [ ] 创建 `agent/map/graph/node/ArbitrationNode.java`
  - 功能：收集3个Agent结果，仲裁冲突
  - 输入：trendRiskLevel, seasonRiskLevel, impactRiskLevel, trendConfidence, seasonConfidence, impactConfidence
  - 输出：arbitrationResult, finalRiskLevel, finalConfidence, arbitrationReason
  - 逻辑：
    - 冲突检测
    - 多数投票
    - 优先级仲裁
    - 记录冲突详情
  
- [ ] 创建 `agent/map/graph/node/ResponseNode.java`
  - 功能：生成最终响应
  - 输入：arbitrationResult, 3个Agent报告
  - 输出：finalReport

### 5.2 创建父图配置
- [ ] 创建 `agent/map/graph/MapInsightGraph.java`
  - 定义KeyStrategyFactory（包含所有子图Key）
  - 注入3个CompiledGraph
  - 构建StateGraph
  - 注册子图节点 + 仲裁节点 + 响应节点
  - 配置边连接（START并行到3个子图，汇总到仲裁，再到响应）
  - 编译为CompiledGraph

---

## 阶段6：评测图EvaluationGraph实现（预计3-4天）

### 6.1 创建评测节点
- [ ] 创建 `agent/map/graph/node/CaseLoaderNode.java`
  - 功能：从数据库加载历史案例
  - 输入：caseId（可选，为空则加载全部）
  - 输出：caseList
  
- [ ] 创建 `agent/map/graph/node/ResultComparatorNode.java`
  - 功能：对比预期结果和实际结果
  - 输入：expectedResult, actualResult
  - 输出：matches, accuracy, leadTimeMinutes, consistency
  
- [ ] 创建 `agent/map/graph/node/AblationRunnerNode.java`
  - 功能：运行消融配置
  - 输入：ablationConfig (排除的Agent列表)
  - 输出：ablationResult

### 6.2 创建评测图配置
- [ ] 创建 `agent/map/graph/EvaluationGraph.java`
  - 创建回放子图（ReplaySubGraph）
  - 创建消融子图（AblationSubGraph）
  - 配置批量评测接口
  - 编译为CompiledGraph

---

## 阶段7：Controller和Service适配（预计1-2天）

### 7.1 重构Controller
- [ ] 重构 `agent/map/controller/MapController.java`
  - 添加 `/api/map/analyze` 接口（调用MapInsightGraph）
  - 添加 `/api/map/evaluate/replay` 接口（调用回放子图）
  - 添加 `/api/map/evaluate/ablation` 接口（调用消融子图）
  - 添加 `/api/map/evaluate/benchmark` 接口（调用批量评测）

### 7.2 创建Service层（可选）
- [ ] 创建 `agent/map/service/MapAnalysisService.java`
  - 封装MapInsightGraph调用逻辑
  - 提供同步/异步分析接口
  
- [ ] 创建 `agent/map/service/EvaluationService.java`
  - 封装EvaluationGraph调用逻辑
  - 提供评测接口

---

## 阶段8：测试和验证（预计2-3天）

### 8.1 单元测试
- [ ] 测试TrendAgent子图独立执行
- [ ] 测试SeasonAgent子图独立执行
- [ ] 测试ImpactAgent子图独立执行
- [ ] 测试ArbitrationNode仲裁逻辑
  - 测试一致场景
  - 测试2个一致场景
  - 测试3个都不同场景
- [ ] 测试ResultComparatorNode对比逻辑

### 8.2 集成测试
- [ ] 测试MapInsightGraph父图编排
- [ ] 测试状态传递和共享
- [ ] 测试并行执行3个子图
- [ ] 测试EvaluationGraph回放功能
- [ ] 测试AblationSubGraph消融功能

### 8.3 手动测试
- [ ] 使用Postman测试 `/api/map/analyze` 接口
- [ ] 使用历史数据测试 `/api/map/evaluate/replay` 接口
- [ ] 测试消融对比功能
- [ ] 验证仲裁结果正确性

---

## 阶段9：文档和优化（预计1-2天）

### 9.1 文档
- [ ] 更新API文档
- [ ] 编写架构说明文档
- [ ] 编写使用说明

### 9.2 优化
- [ ] 优化并行执行性能
- [ ] 添加缓存机制
- [ ] 添加日志追踪
- [ ] 添加异常处理

---

## 总览

| 阶段 | 任务数 | 预计天数 | 状态 |
|------|--------|----------|------|
| 阶段1：数据结构和常量 | 13 | 1-2 | 待开始 |
| 阶段2：TrendAgent子图 | 4 | 2-3 | 待开始 |
| 阶段3：SeasonAgent子图 | 5 | 2-3 | 待开始 |
| 阶段4：ImpactAgent子图 | 4 | 2-3 | 待开始 |
| 阶段5：父图MapInsightGraph | 3 | 2-3 | 待开始 |
| 阶段6：评测图EvaluationGraph | 4 | 3-4 | 待开始 |
| 阶段7：Controller和Service | 3 | 1-2 | 待开始 |
| 阶段8：测试和验证 | 13 | 2-3 | 待开始 |
| 阶段9：文档和优化 | 5 | 1-2 | 待开始 |
| **总计** | **54** | **19-28** | - |
