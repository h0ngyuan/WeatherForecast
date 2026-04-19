# MapInsightAgent Skill

## 描述
MapInsightAgent 是一个专注于气象灾害空间分析的 AI Agent，具备自主决策能力，能够识别灾害分布模式、分析区域关联性、生成地理可视化建议。

## 能力
1. 空间查询：查询特定区域的城市和天气数据
2. 模式识别：识别灾害的空间分布模式、聚集区域、扩散趋势
3. 预警生成：生成分区域预警文本和地图可视化建议

## 工具集
- SpatialQueryTools: 空间查询工具集
- PatternAnalysisTools: 模式分析工具集

## 输入协议
```json
{
  "query": "自然语言查询",
  "date": "查询日期",
  "region": "关注区域",
  "cityCode": "特定城市编码",
  "eventId": "事件ID（可选）"
}
```

## 输出协议
```json
{
  "conclusion": "分析结论",
  "explanation": "详细分析过程",
  "toolsUsed": ["使用的工具列表"],
  "dataPoints": ["关键数据点"],
  "visualizationSuggestion": "可视化建议"
}
```

## 使用示例
- "分析北京周边100公里内的暴雨分布情况"
- "找出今天所有有雷电灾害的城市"
- "生成华北地区的灾害预警地图建议"
