package com.wf.agent.map;

import com.wf.agent.base.AIClient;
import com.wf.agent.map.dto.*;
import com.wf.agent.map.memory.Evidence;
import com.wf.agent.map.memory.SharedMemoryService;
import com.wf.agent.map.tool.PatternAnalysisTools;
import com.wf.agent.map.tool.SpatialQueryTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * MapInsightAgent - 地图洞察Sub Agent
 */
@Component
@Slf4j
public class MapInsightAgent {

    @Autowired
    private AIClient aiClient;

    @Autowired
    private SharedMemoryService memoryService;

    @Autowired
    private SpatialQueryTools spatialQueryTools;

    @Autowired
    private PatternAnalysisTools patternAnalysisTools;

    /**
     * Agent主入口 - 处理地图分析查询
     */
    public MapInsightResponse analyze(MapInsightRequest request) {
        log.info("[MapInsightAgent] 接收查询: {}", request.getQuery());

        String eventId = request.getEventId();

        // 1. 读取共享记忆
        List<Evidence> previousEvidences = new ArrayList<>();
        if (eventId != null) {
            previousEvidences = memoryService.getEvidences(eventId);
        }

        // 2. 解析查询意图，执行相应分析
        AnalysisResult result = executeAnalysis(request);

        // 3. 写入证据到共享记忆
        if (eventId != null) {
            Evidence evidence = new Evidence();
            evidence.setAgentName("MapInsightAgent");
            evidence.setEvidenceType("SPATIAL_ANALYSIS");
            evidence.setContent(result.getConclusion());
            evidence.setConfidence(result.getConfidence());
            evidence.setDataSources(result.getToolsUsed());
            evidence.setRelatedRule("空间分析规则");
            memoryService.addEvidence(eventId, evidence);
        }

        // 4. 构建响应
        MapInsightResponse response = new MapInsightResponse();
        response.setQuery(request.getQuery());
        response.setAnalysisDate(LocalDateTime.now());
        response.setConclusion(result.getConclusion());
        response.setExplanation(result.getExplanation());
        response.setToolsUsed(result.getToolsUsed());
        response.setDataPoints(result.getDataPoints());
        response.setVisualizationSuggestion(result.getVisualizationSuggestion());

        log.info("[MapInsightAgent] 分析完成，使用工具: {}", result.getToolsUsed());
        return response;
    }

    /**
     * 执行分析（简化版，基于关键词匹配）
     */
    private AnalysisResult executeAnalysis(MapInsightRequest request) {
        AnalysisResult result = new AnalysisResult();
        List<String> toolsUsed = new ArrayList<>();
        List<MapDataPoint> dataPoints = new ArrayList<>();

        String query = request.getQuery().toLowerCase();

        // 场景1: 周边分析
        if (query.contains("周边") || query.contains("附近") || query.contains("半径")) {
            toolsUsed.add("SpatialQueryTools.queryCitiesInRadius");

            double lat = 39.9042; // 默认北京
            double lon = 116.4074;
            int radius = 100;

            // 解析半径
            if (query.contains("100")) radius = 100;
            else if (query.contains("50")) radius = 50;
            else if (query.contains("200")) radius = 200;

            List<CityWeatherVO> cities = spatialQueryTools.queryCitiesInRadius(lat, lon, radius, request.getDate());

            // 分析灾害模式
            PatternAnalysisTools.SpatialPattern pattern = patternAnalysisTools.analyzeDisasterPattern(cities);

            result.setConclusion(String.format("北京周边%d公里内发现%d个城市，其中%d个有灾害，呈%s分布",
                    radius, cities.size(), pattern.getAffectedCityCount(), pattern.getDistributionDirection()));

            result.setExplanation(generateExplanation(cities, pattern));

            dataPoints = cities.stream()
                    .filter(c -> Boolean.TRUE.equals(c.getHasDisaster()))
                    .map(this::convertToDataPoint)
                    .collect(Collectors.toList());

            result.setVisualizationSuggestion(generateVisualizationSuggestion(pattern));
        }
        // 场景2: 省份分析
        else if (query.contains("省") || query.contains("市")) {
            toolsUsed.add("SpatialQueryTools.queryCitiesByProvince");

            String province = extractProvince(query);
            List<CityWeatherVO> cities = spatialQueryTools.queryCitiesByProvince(province);

            long disasterCount = cities.stream().filter(c -> Boolean.TRUE.equals(c.getHasDisaster())).count();

            result.setConclusion(String.format("%s共有%d个城市，其中%d个有灾害",
                    province, cities.size(), disasterCount));
            result.setExplanation(String.format("查询了%s的所有城市，分析了灾害分布情况", province));

            dataPoints = cities.stream()
                    .map(this::convertToDataPoint)
                    .collect(Collectors.toList());
        }
        // 场景3: 默认响应
        else {
            result.setConclusion("请提供更具体的查询，如'分析北京周边100公里内的灾害分布'");
            result.setExplanation("系统支持周边分析、省份分析等空间查询");
        }

        result.setToolsUsed(toolsUsed);
        result.setDataPoints(dataPoints);
        result.setConfidence(0.85);

        return result;
    }

    private String generateExplanation(List<CityWeatherVO> cities, PatternAnalysisTools.SpatialPattern pattern) {
        StringBuilder sb = new StringBuilder();
        sb.append("分析过程：\n");
        sb.append("1. 使用空间查询工具获取周边城市列表\n");
        sb.append("2. 筛选出有灾害的城市共").append(pattern.getAffectedCityCount()).append("个\n");
        sb.append("3. 计算聚集中心位置\n");
        sb.append("4. 分析分布模式为").append(pattern.getDistributionDirection()).append("\n");
        sb.append("5. 影响范围约").append(String.format("%.1f", pattern.getSpreadRangeKm())).append("公里");
        return sb.toString();
    }

    private VisualizationSuggestion generateVisualizationSuggestion(PatternAnalysisTools.SpatialPattern pattern) {
        VisualizationSuggestion suggestion = new VisualizationSuggestion();
        suggestion.setTitle("灾害分布可视化建议");
        suggestion.setDescription("建议使用热力图展示灾害分布");
        suggestion.setColorScheme("disaster");
        suggestion.setShowCluster(pattern.isHasPattern() && pattern.getAffectedCityCount() >= 3);
        return suggestion;
    }

    private MapDataPoint convertToDataPoint(CityWeatherVO city) {
        MapDataPoint point = new MapDataPoint();
        point.setCityCode(city.getCityCode());
        point.setCityName(city.getCityName());
        point.setLatitude(city.getLatitude().doubleValue());
        point.setLongitude(city.getLongitude().doubleValue());
        point.setDataType(Boolean.TRUE.equals(city.getHasDisaster()) ? "DISASTER" : "NORMAL");
        point.setValue(city.getMaxDisasterLevel());
        point.setDescription(city.getDisasterTypes());
        return point;
    }

    private String extractProvince(String query) {
        String[] provinces = {"北京", "上海", "广东", "浙江", "江苏", "山东", "河南", "河北"};
        for (String province : provinces) {
            if (query.contains(province)) {
                return province;
            }
        }
        return "北京";
    }

    /**
     * 分析结果内部类
     */
    private static class AnalysisResult {
        private String conclusion;
        private String explanation;
        private List<String> toolsUsed;
        private List<MapDataPoint> dataPoints;
        private VisualizationSuggestion visualizationSuggestion;
        private double confidence;

        // Getters and Setters
        public String getConclusion() { return conclusion; }
        public void setConclusion(String conclusion) { this.conclusion = conclusion; }
        public String getExplanation() { return explanation; }
        public void setExplanation(String explanation) { this.explanation = explanation; }
        public List<String> getToolsUsed() { return toolsUsed; }
        public void setToolsUsed(List<String> toolsUsed) { this.toolsUsed = toolsUsed; }
        public List<MapDataPoint> getDataPoints() { return dataPoints; }
        public void setDataPoints(List<MapDataPoint> dataPoints) { this.dataPoints = dataPoints; }
        public VisualizationSuggestion getVisualizationSuggestion() { return visualizationSuggestion; }
        public void setVisualizationSuggestion(VisualizationSuggestion visualizationSuggestion) { this.visualizationSuggestion = visualizationSuggestion; }
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
    }
}
