# 地图模块多Agent协作重构规范

## 1. 概述

### 1.1 背景

当前地图洞察模块（MapInsightAgent）存在以下问题：
- 硬编码严重（北京坐标、固定半径值）
- 意图识别落后（简单关键词匹配）
- 分析维度单一（仅支持周边分析和省份分析）
- 缺少冲突仲裁机制
- 无评测能力（无法验证Agent准确性）

### 1.2 目标

基于Spring AI Alibaba Graph的子图（Subgraph）机制，重构地图模块为真正的多Agent协作系统，实现：
- **多Agent独立执行**：每个Agent是一个CompiledGraph（子图）
- **父图动态编排**：Coordinator父图编排多个子图
- **冲突仲裁**：多Agent结果不一致时自动仲裁
- **评测能力**：历史案例回放 + 消融对比测试
- **Harness思想**：父图控制流程，子图执行具体任务

### 1.3 技术要求

- 使用Spring AI Alibaba Graph 1.1.2.0
- 每个Agent = 一个CompiledGraph
- 父子图共享状态（OverAllState）
- 支持并行执行子图

---

## 2. 架构设计

### 2.1 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                    MapInsightGraph (父图)                        │
│                     Coordinator / Harness                        │
│                                                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐           │
│  │ TrendAgent   │  │ SeasonAgent  │  │ ImpactAgent  │           │
│  │ (子图)       │  │ (子图)       │  │ (子图)       │           │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘           │
│         │                 │                 │                    │
│         └─────────────────┼─────────────────┘                    │
│                           ▼                                      │
│                  ┌──────────────────┐                            │
│                  │ ArbitrationNode  │                            │
│                  │ (冲突仲裁)        │                            │
│                  └────────┬─────────┘                            │
│                           ▼                                      │
│                  ┌──────────────────┐                            │
│                  │ ResponseNode     │                            │
│                  │ (响应生成)        │                            │
│                  └──────────────────┘                            │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                   EvaluationGraph (评测父图)                     │
│                                                                 │
│  ┌──────────────────┐        ┌──────────────────┐               │
│  │ ReplaySubGraph   │        │ AblationSubGraph │               │
│  │ (案例回放子图)    │        │ (消融测试子图)    │               │
│  └──────────────────┘        └──────────────────┘               │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 子图定义

#### 2.2.1 TrendAgent（趋势分析子图）

**职责**：分析周边城市天气趋势，判断灾害传播方向

**输入状态**：
- `location`: 目标城市名称
- `radiusKm`: 分析半径（公里）
- `date`: 分析日期

**输出状态**：
- `trendReport`: 趋势分析报告（JSON字符串）
- `trendRiskLevel`: 趋势风险等级（0-3）
- `trendConfidence`: 趋势置信度（0.0-1.0）

**子图结构**：
```
START → queryNearbyCities → analyzeTrend → generateTrendReport → END
```

**节点说明**：
| 节点名 | 职责 | 输入 | 输出 |
|--------|------|------|------|
| queryNearbyCities | 查询周边城市天气 | location, radiusKm, date | nearbyCities (List) |
| analyzeTrend | 分析灾害传播趋势 | nearbyCities | trendDirection, trendSeverity |
| generateTrendReport | 生成趋势报告 | trendDirection, trendSeverity | trendReport, trendRiskLevel, trendConfidence |

---

#### 2.2.2 SeasonAgent（季节评估子图）

**职责**：结合季节特征，评估城市风险基线

**输入状态**：
- `location`: 目标城市名称
- `date`: 分析日期

**输出状态**：
- `seasonReport`: 季节评估报告（JSON字符串）
- `seasonRiskLevel`: 季节风险等级（0-3）
- `seasonConfidence`: 季节置信度（0.0-1.0）

**子图结构**：
```
START → getSeasonInfo → queryHistoryStats → evaluateSeasonRisk → generateSeasonReport → END
```

**节点说明**：
| 节点名 | 职责 | 输入 | 输出 |
|--------|------|------|------|
| getSeasonInfo | 获取当前季节信息 | date | season, month |
| queryHistoryStats | 查询历史季节灾害统计 | location, season | historyStats |
| evaluateSeasonRisk | 评估季节性风险 | historyStats | seasonRiskBase |
| generateSeasonReport | 生成季节评估报告 | seasonRiskBase | seasonReport, seasonRiskLevel, seasonConfidence |

---

#### 2.2.3 ImpactAgent（影响分析子图）

**职责**：分析周边灾害对目标城市的直接影响

**输入状态**：
- `location`: 目标城市名称
- `radiusKm`: 影响半径（公里）
- `date`: 分析日期

**输出状态**：
- `impactReport`: 影响分析报告（JSON字符串）
- `impactRiskLevel`: 影响风险等级（0-3）
- `impactConfidence`: 影响置信度（0.0-1.0）

**子图结构**：
```
START → findDisasterCities → calculateImpact → generateImpactReport → END
```

**节点说明**：
| 节点名 | 职责 | 输入 | 输出 |
|--------|------|------|------|
| findDisasterCities | 查找有灾害的周边城市 | location, radiusKm, date | disasterCities (List) |
| calculateImpact | 计算影响程度 | disasterCities, location | impactScore, impactDirection |
| generateImpactReport | 生成影响分析报告 | impactScore, impactDirection | impactReport, impactRiskLevel, impactConfidence |

---

### 2.3 父图定义（MapInsightGraph）

**职责**：编排3个子图，收集结果，仲裁冲突，生成响应

**输入状态**：
- `location`: 目标城市名称
- `radiusKm`: 分析半径（公里）
- `date`: 分析日期
- `query`: 原始查询文本

**输出状态**：
- `finalReport`: 最终分析报告（JSON字符串）
- `finalRiskLevel`: 最终风险等级（0-3）
- `arbitrationReason`: 仲裁理由说明

**父图结构**：
```
START ──┬──> trendAgent (子图)
        ├──> seasonAgent (子图)     ← 并行执行
        └──> impactAgent (子图)
                │
                ▼
        ┌──────────────┐
        │ arbitration   │ ← 收集3个子图结果，仲裁冲突
        └──────┬───────┘
               ▼
        ┌──────────────┐
        │ generateResp  │ ← 生成最终响应
        └──────┬───────┘
               ▼
              END
```

**仲裁规则**：
1. 收集3个子图的风险等级和置信度
2. 如果3个一致 → 直接采用
3. 如果2个一致 → 采用多数意见，记录冲突
4. 如果3个都不同 → 采用最高风险等级，记录冲突和理由
5. 优先级：ImpactAgent > TrendAgent > SeasonAgent（动态分析 > 静态基线）

---

### 2.4 评测图定义（EvaluationGraph）

#### 2.4.1 ReplaySubGraph（案例回放子图）

**职责**：用历史案例回放，验证Agent准确性

**流程**：
```
START → loadCase → runMapInsightGraph → compareResult → calcMetrics → END
```

**输出**：
- `accuracy`: 准确性（风险等级一致率，0.0-1.0）
- `leadTimeMinutes`: 提前量（分钟）
- `consistency`: 一致性（建议匹配率，0.0-1.0）
- `responseDelayMs`: 响应时延（毫秒）

#### 2.4.2 AblationSubGraph（消融测试子图）

**职责**：关闭部分Agent，分析各Agent贡献度

**流程**：
```
START ──┬──> runFullConfig (完整配置)
        ├──> runWithoutTrend (去掉TrendAgent)
        ├──> runWithoutSeason (去掉SeasonAgent)
        └──> runWithoutImpact (去掉ImpactAgent)
                │
                ▼
        ┌──────────────┐
        │ compareDiff   │
        └──────┬───────┘
               ▼
        ┌──────────────┐
        │ genAblationRpt│
        └──────┬───────┘
               ▼
              END
```

**输出**：
- `fullConfigResult`: 完整配置结果
- `trendAgentContribution`: TrendAgent贡献度（风险等级差值、置信度差值）
- `seasonAgentContribution`: SeasonAgent贡献度
- `impactAgentContribution`: ImpactAgent贡献度
- `mostCriticalAgent`: 最关键Agent名称

---

## 3. 数据结构

### 3.1 AgentReport（Agent报告标准格式）

```java
public class AgentReport {
    private String agentName;           // Agent名称
    private String reportType;          // 报告类型：TREND/SEASON/IMPACT
    private String conclusion;          // 分析结论
    private int riskLevel;              // 风险等级 0-3
    private double confidence;          // 置信度 0.0-1.0
    private List<String> evidence;      // 证据列表
    private Map<String, Object> details; // 详细信息
    private long executionTimeMs;       // 执行时间（毫秒）
}
```

### 3.2 ArbitrationResult（仲裁结果格式）

```java
public class ArbitrationResult {
    private int finalRiskLevel;              // 最终风险等级
    private double finalConfidence;          // 最终置信度
    private String arbitrationReason;        // 仲裁理由
    private List<AgentReport> agentReports;  // 所有Agent报告
    private List<ConflictRecord> conflicts;  // 冲突记录
}

public class ConflictRecord {
    private String conflictType;             // 冲突类型：RISK_LEVEL_DIFF/CONFIDENCE_DIFF
    private String agentA;                   // Agent A名称
    private String agentB;                   // Agent B名称
    private Object valueA;                   // Agent A的值
    private Object valueB;                   // Agent B的值
    private String resolution;               // 解决方式
}
```

### 3.3 EvaluationReport（评测报告格式）

```java
public class EvaluationReport {
    private int totalCases;                  // 总案例数
    private double accuracy;                 // 准确性 0.0-1.0
    private double avgLeadTimeMinutes;       // 平均提前量（分钟）
    private double consistency;              // 一致性 0.0-1.0
    private long avgResponseDelayMs;         // 平均响应时延（毫秒）
    private List<CaseEvaluation> caseDetails; // 案例详情
}

public class CaseEvaluation {
    private String caseId;                   // 案例ID
    private String eventType;                // 事件类型
    private int expectedRiskLevel;           // 预期风险等级
    private int actualRiskLevel;             // 实际风险等级
    private boolean matches;                 // 是否匹配
    private int leadTimeMinutes;             // 提前量
    private long responseDelayMs;            // 响应时延
}
```

### 3.4 AblationReport（消融报告格式）

```java
public class AblationReport {
    private AblationResult fullConfig;       // 完整配置结果
    private AblationResult withoutTrend;     // 去掉TrendAgent结果
    private AblationResult withoutSeason;    // 去掉SeasonAgent结果
    private AblationResult withoutImpact;    // 去掉ImpactAgent结果
    private AgentContribution trendContribution;   // TrendAgent贡献度
    private AgentContribution seasonContribution;  // SeasonAgent贡献度
    private AgentContribution impactContribution;  // ImpactAgent贡献度
    private String mostCriticalAgent;        // 最关键Agent
}

public class AblationResult {
    private int riskLevel;                   // 风险等级
    private double confidence;               // 置信度
    private String conclusion;               // 结论
}

public class AgentContribution {
    private String agentName;                // Agent名称
    private int riskLevelImpact;             // 风险等级影响（差值）
    private double confidenceImpact;         // 置信度影响（差值）
    private String description;              // 描述
}
```

---

## 4. 状态Key设计

```java
public class MapGraphConstants {
    // ===== 输入 =====
    public static final String KEY_LOCATION = "location";
    public static final String KEY_RADIUS_KM = "radiusKm";
    public static final String KEY_DATE = "date";
    public static final String KEY_QUERY = "query";
    
    // ===== TrendAgent输出 =====
    public static final String KEY_NEARBY_CITIES = "nearbyCities";
    public static final String KEY_TREND_DIRECTION = "trendDirection";
    public static final String KEY_TREND_SEVERITY = "trendSeverity";
    public static final String KEY_TREND_REPORT = "trendReport";
    public static final String KEY_TREND_RISK_LEVEL = "trendRiskLevel";
    public static final String KEY_TREND_CONFIDENCE = "trendConfidence";
    
    // ===== SeasonAgent输出 =====
    public static final String KEY_SEASON = "season";
    public static final String KEY_MONTH = "month";
    public static final String KEY_HISTORY_STATS = "historyStats";
    public static final String KEY_SEASON_RISK_BASE = "seasonRiskBase";
    public static final String KEY_SEASON_REPORT = "seasonReport";
    public static final String KEY_SEASON_RISK_LEVEL = "seasonRiskLevel";
    public static final String KEY_SEASON_CONFIDENCE = "seasonConfidence";
    
    // ===== ImpactAgent输出 =====
    public static final String KEY_DISASTER_CITIES = "disasterCities";
    public static final String KEY_IMPACT_SCORE = "impactScore";
    public static final String KEY_IMPACT_DIRECTION = "impactDirection";
    public static final String KEY_IMPACT_REPORT = "impactReport";
    public static final String KEY_IMPACT_RISK_LEVEL = "impactRiskLevel";
    public static final String KEY_IMPACT_CONFIDENCE = "impactConfidence";
    
    // ===== 仲裁输出 =====
    public static final String KEY_ARBITRATION_RESULT = "arbitrationResult";
    public static final String KEY_FINAL_RISK_LEVEL = "finalRiskLevel";
    public static final String KEY_FINAL_CONFIDENCE = "finalConfidence";
    public static final String KEY_ARBITRATION_REASON = "arbitrationReason";
    
    // ===== 最终输出 =====
    public static final String KEY_FINAL_REPORT = "finalReport";
}
```

---

## 5. API设计

```java
@RestController
@RequestMapping("/api/map")
public class MapController {
    
    /**
     * 1. 地图分析（主功能）
     */
    @PostMapping("/analyze")
    public MapInsightResponse analyze(@RequestBody MapInsightRequest request);
    
    /**
     * 2. 历史案例回放
     */
    @PostMapping("/evaluate/replay")
    public EvaluationReport replay(@RequestBody ReplayRequest request);
    
    /**
     * 3. 消融测试
     */
    @PostMapping("/evaluate/ablation")
    public AblationReport ablation(@RequestBody AblationRequest request);
    
    /**
     * 4. 批量评测
     */
    @PostMapping("/evaluate/benchmark")
    public BenchmarkReport benchmark(@RequestBody BenchmarkRequest request);
}
```

---

## 6. 关键实现策略

### 6.1 子图编译与复用

```java
// 1. 编译子图
CompiledGraph trendAgent = trendGraph.compile();
CompiledGraph seasonAgent = seasonGraph.compile();
CompiledGraph impactAgent = impactGraph.compile();

// 2. 在父图中直接使用子图作为节点
StateGraph parentGraph = new StateGraph(parentKeyFactory)
    .addNode("trendAgent", trendAgent)      // 直接使用编译的子图
    .addNode("seasonAgent", seasonAgent)
    .addNode("impactAgent", impactAgent)
    .addNode("arbitration", node_async(arbitrationNode))
    .addNode("generateResp", node_async(responseNode))
    .addEdge(START, "trendAgent")
    .addEdge(START, "seasonAgent")           // 并行执行
    .addEdge(START, "impactAgent")
    .addEdge("trendAgent", "arbitration")
    .addEdge("seasonAgent", "arbitration")
    .addEdge("impactAgent", "arbitration")
    .addEdge("arbitration", "generateResp")
    .addEdge("generateResp", END);
```

### 6.2 消融测试实现

```java
// 完整配置
CompiledGraph fullGraph = mapInsightGraph;

// 消融配置：去掉TrendAgent
StateGraph ablationWithoutTrend = new StateGraph(parentKeyFactory)
    .addNode("seasonAgent", seasonAgent)
    .addNode("impactAgent", impactAgent)
    .addNode("arbitration", node_async(new AblationArbitrationNode(excludeAgents = Set.of("TrendAgent"))))
    .addNode("generateResp", node_async(responseNode))
    .addEdge(START, "seasonAgent")
    .addEdge(START, "impactAgent")
    .addEdge("seasonAgent", "arbitration")
    .addEdge("impactAgent", "arbitration")
    .addEdge("arbitration", "generateResp")
    .addEdge("generateResp", END);

CompiledGraph ablationGraph1 = ablationWithoutTrend.compile();
```

### 6.3 冲突仲裁逻辑

```java
public class ArbitrationNode implements NodeAction {
    
    @Override
    public Map<String, Object> apply(OverAllState state) {
        // 1. 收集3个Agent的风险等级
        int trendLevel = state.value("trendRiskLevel", 0);
        int seasonLevel = state.value("seasonRiskLevel", 0);
        int impactLevel = state.value("impactRiskLevel", 0);
        
        // 2. 收集置信度
        double trendConf = state.value("trendConfidence", 0.0);
        double seasonConf = state.value("seasonConfidence", 0.0);
        double impactConf = state.value("impactConfidence", 0.0);
        
        // 3. 冲突检测
        Set<Integer> levels = Set.of(trendLevel, seasonLevel, impactLevel);
        boolean hasConflict = levels.size() > 1;
        
        // 4. 仲裁决策
        int finalLevel;
        String reason;
        
        if (!hasConflict) {
            finalLevel = trendLevel;
            reason = "所有Agent意见一致";
        } else {
            // 采用最高风险等级
            finalLevel = Math.max(trendLevel, Math.max(seasonLevel, impactLevel));
            
            // 记录冲突
            List<ConflictRecord> conflicts = detectConflicts(...);
            
            // 生成仲裁理由
            reason = generateArbitrationReason(conflicts, finalLevel);
        }
        
        // 5. 计算最终置信度（加权平均）
        double finalConf = (trendConf * 0.3 + seasonConf * 0.2 + impactConf * 0.5);
        
        return Map.of(
            "finalRiskLevel", finalLevel,
            "finalConfidence", finalConf,
            "arbitrationReason", reason
        );
    }
}
```

---

## 7. 测试策略

### 7.1 单元测试

- 测试每个子图的独立执行
- 测试仲裁逻辑（一致、冲突、全不同）
- 测试消融对比逻辑

### 7.2 集成测试

- 测试父图编排3个子图
- 测试状态传递和共享
- 测试并行执行

### 7.3 评测测试

- 用历史案例回放，验证准确性
- 做消融对比，验证各Agent贡献度

---

## 8. 文件清单

| 操作 | 文件路径 | 说明 |
|------|----------|------|
| **新建** | `agent/map/graph/TrendAgentGraph.java` | TrendAgent子图配置 |
| **新建** | `agent/map/graph/SeasonAgentGraph.java` | SeasonAgent子图配置 |
| **新建** | `agent/map/graph/ImpactAgentGraph.java` | ImpactAgent子图配置 |
| **新建** | `agent/map/graph/MapInsightGraph.java` | 父图配置（Coordinator） |
| **新建** | `agent/map/graph/EvaluationGraph.java` | 评测图配置 |
| **新建** | `agent/map/graph/node/TrendQueryNode.java` | 查询周边城市节点 |
| **新建** | `agent/map/graph/node/TrendAnalysisNode.java` | 趋势分析节点 |
| **新建** | `agent/map/graph/node/TrendReportNode.java` | 趋势报告生成节点 |
| **新建** | `agent/map/graph/node/SeasonInfoNode.java` | 季节信息节点 |
| **新建** | `agent/map/graph/node/SeasonHistoryNode.java` | 历史统计节点 |
| **新建** | `agent/map/graph/node/SeasonEvalNode.java` | 季节评估节点 |
| **新建** | `agent/map/graph/node/SeasonReportNode.java` | 季节报告生成节点 |
| **新建** | `agent/map/graph/node/DisasterQueryNode.java` | 灾害城市查询节点 |
| **新建** | `agent/map/graph/node/ImpactCalcNode.java` | 影响计算节点 |
| **新建** | `agent/map/graph/node/ImpactReportNode.java` | 影响报告生成节点 |
| **新建** | `agent/map/graph/node/ArbitrationNode.java` | 仲裁节点 |
| **新建** | `agent/map/graph/node/ResponseNode.java` | 响应生成节点 |
| **新建** | `agent/map/graph/node/CaseLoaderNode.java` | 案例加载节点 |
| **新建** | `agent/map/graph/node/ResultComparatorNode.java` | 结果对比节点 |
| **新建** | `agent/map/graph/node/AblationRunnerNode.java` | 消融运行节点 |
| **新建** | `agent/map/constants/MapGraphConstants.java` | 状态Key常量 |
| **新建** | `agent/map/dto/AgentReport.java` | Agent报告标准格式 |
| **新建** | `agent/map/dto/ArbitrationResult.java` | 仲裁结果格式 |
| **新建** | `agent/map/dto/ConflictRecord.java` | 冲突记录格式 |
| **新建** | `agent/map/dto/EvaluationReport.java` | 评测报告格式 |
| **新建** | `agent/map/dto/AblationReport.java` | 消融报告格式 |
| **新建** | `agent/map/dto/MapInsightResponse.java` | 地图分析响应 |
| **重构** | `agent/map/controller/MapController.java` | 适配新API |
| **保留** | `agent/map/arbitration/ConflictArbitrator.java` | 仲裁逻辑复用 |
| **保留** | `agent/map/memory/*` | 共享记忆服务 |
| **保留** | `agent/map/tool/*` | 工具类复用 |
| **保留** | `agent/map/WeatherImpactAgent.java` | 影响分析逻辑复用 |
