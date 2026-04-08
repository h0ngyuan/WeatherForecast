---
name: "disaster-rainstorm-assessment"
description: "暴雨灾害风险等级评估Skill。Invoke when weather data indicates rain/storm conditions and need to determine risk level (1-3) with explainable rules."
---

# 暴雨灾害风险评估 Skill

## 天气码说明（最美天气自定义代码 - wid）
- 46: 小雨（0.1~10mm）
- 47: 中雨（10~25mm）
- 48: 大雨（25~50mm）
- 49: 暴雨（≥50mm，极端天气）

## 触发条件
当天气码值在以下范围时触发：
- 46 = 小雨
- 47 = 中雨  
- 48 = 大雨
- 49 = 暴雨

## 判定规则

### 输入参数
- `weatherCode`: 天气码值 (46-49)
- `durationHours`: 持续小时数

### 判定逻辑
```
IF weatherCode == 49 OR (weatherCode == 48 AND durationHours >= 6):
    → 1级（严重）
ELSE IF weatherCode == 47 OR weatherCode == 48:
    → 2级（中等）
ELSE IF weatherCode == 46 AND durationHours >= 6:
    → 2级（中等）
ELSE:
    → 3级（轻微）
```

### 等级定义

#### 1级 - 严重（全员预警）
**触发条件**：
- 码值 = 49（暴雨）
- 或码值 = 48（大雨）且持续时间 ≥ 6小时

**风险描述**：
暴雨强度大或持续时间长，可能引发：
- 城市内涝
- 山洪暴发
- 道路积水
- 交通中断

**建议措施**：
- 避免外出
- 远离低洼地带和河道
- 做好防汛准备
- 关注官方预警

#### 2级 - 中等（影响特定活动）
**触发条件**：
- 码值 = 47（中雨）
- 或码值 = 48（大雨）且持续时间 < 6小时
- 或码值 = 46（小雨）且持续时间 ≥ 6小时

**风险描述**：
中到大雨，影响户外活动和交通出行

**建议措施**：
- 外出携带雨具
- 驾车减速慢行
- 关注路况信息
- 避免长时间户外活动

#### 3级 - 轻微（一般提醒）
**触发条件**：
- 码值 = 46（小雨）且持续时间 < 6小时

**风险描述**：
小雨天气，对一般活动影响较小

**建议措施**：
- 正常出行
- 随身携带雨具
- 关注天气变化

## 输出格式
```json
{
  "level": 1|2|3,
  "type": "暴雨/大雨/中雨/小雨",
  "explanation": "详细的风险描述和建议措施"
}
```

## 示例

### 示例1：特大暴雨
```
输入: weatherCode=49, durationHours=4
输出: {
  "level": 1,
  "type": "暴雨",
  "explanation": "暴雨极端天气（≥50mm），可能引发城市内涝、山洪等灾害，需全员预警。建议：避免外出，远离低洼地带，做好防汛准备"
}
```

### 示例2：大雨持续4小时
```
输入: weatherCode=48, durationHours=4
输出: {
  "level": 2,
  "type": "大雨",
  "explanation": "大雨（25~50mm），影响户外活动和交通出行。建议：外出携带雨具，驾车减速慢行，关注路况信息"
}
```

### 示例3：小雨短时
```
输入: weatherCode=46, durationHours=2
输出: {
  "level": 3,
  "type": "小雨",
  "explanation": "小雨天气（0.1~10mm），对一般活动影响较小。建议：正常出行，随身携带雨具即可"
}
```
