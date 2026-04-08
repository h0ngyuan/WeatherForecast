---
name: "disaster-risk-assessment"
description: "灾害风险等级评估Skill，根据天气码值和灾害类型判定风险级别(1-3级)并提供可解释性说明。Invoke when need to assess disaster risk level with explainable rules."
---

# 灾害风险等级评估 Skill

## 职责
根据天气码值、灾害类型和持续时间，规则化判定风险等级，并提供详细的可解释性说明。

## 天气码说明（最美天气自定义代码 - wid）
- 1: 晴（无降水）
- 7: 多云
- 8: 阴
- 15: 雷阵雨（强对流，有雷电）
- 33: 雾/轻雾（能见度<10km）
- 46: 小雨（0.1~10mm）
- 47: 中雨（10~25mm）
- 48: 大雨（25~50mm）
- 49: 暴雨（≥50mm，极端天气）
- 75: 霾/沙尘（空气质量差）

## 风险等级定义

### 1级（严重）- 需全员通知
触发条件：
- 暴雨（码值=49 或 持续≥6小时）
- 大雨（码值=48 且 持续≥6小时）
- 冰雹（任意码值）
- 极端大风（码值≥70 或 持续≥6小时）
- 台风/龙卷风

解释模板：
"{灾害类型}持续时间长或强度大，可能造成{具体危害}，需全员预警，建议{应急措施}"

### 2级（中等）- 影响特定活动
触发条件：
- 中雨（码值=47）
- 大雨（码值=48 且 持续<6小时）
- 雷阵雨（码值=15）
- 短时大风（持续<6小时）
- 长时间大雾（码值=33 且 持续≥6小时）
- 高温/低温/寒潮
- 一般降雪

解释模板：
"{灾害类型}影响{活动类型}，{具体影响}，需关注天气变化，建议{防范措施}"

### 3级（轻微）- 一般性提醒
触发条件：
- 小雨（码值=46，持续<6小时）
- 短时大雾（码值=33 且 持续<6小时）
- 多云/阴天（码值=7,8）
- 晴天（码值=1）
- 其他一般性天气

解释模板：
"{灾害类型}对一般活动影响较小，{简单提醒}"

## 判定规则

### 降雨判定（使用wid码值）
```
if (weatherCode == 49 || (weatherCode == 48 && durationHours >= 6)) -> 1级
else if (weatherCode == 47 || weatherCode == 48) -> 2级
else if (weatherCode == 46 && durationHours >= 6) -> 2级
else -> 3级
```

### 大风判定
```
if (weatherCode >= 70 || durationHours >= 6) -> 1级
else if (durationHours >= 3) -> 2级
else -> 3级
```

### 降雪判定
```
if (weatherCode >= 64 || durationHours >= 6) -> 1级
else -> 2级
```

### 雷电判定
```
always -> 2级（因触电风险）
```

### 冰雹判定
```
always -> 1级（因直接威胁）
```

### 大雾判定（使用wid码值33）
```
if (weatherCode == 33 && durationHours >= 6) -> 2级
else -> 3级
```

## 使用方法

```java
// 单个判定
RiskAssessmentResult result = skill.assessRisk("暴雨", 49, 5);
// 返回: level=1, explanation="暴雨极端天气，可能造成城市内涝..."

// 批量判定
List<RiskAssessmentResult> results = skill.assessRisks(disasterList);
```

## 输出格式

```java
public class RiskAssessmentResult {
    private int level;        // 1=严重, 2=中等, 3=轻微
    private String explanation;  // 可解释性说明
}
```

## 注意事项

1. **可解释性优先**：每个判定必须附带详细的解释说明
2. **规则透明**：判定逻辑必须清晰可追溯
3. **边界处理**：码值未知时默认3级，并记录日志
4. **扩展性**：新增灾害类型时，在switch中添加对应规则
