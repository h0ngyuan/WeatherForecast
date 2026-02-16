package com.wf.agent.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.wf.agent.base.AIClient;
import com.wf.agent.constants.WeatherGraphConstants;
import com.wf.agent.tool.MCPPredictionTool;
import com.wf.object.query.WeatherCodeQuery;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Component
public class WeatherForecastNode implements NodeAction {

    private final AIClient aiClient;
    private final MCPPredictionTool mcpPredictionTool;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public WeatherForecastNode(AIClient aiClient, MCPPredictionTool mcpPredictionTool) {
        this.aiClient = aiClient;
        this.mcpPredictionTool = mcpPredictionTool;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("========== [forecast节点] 开始执行 ==========");
        log.info("forecast节点已被调用，准备获取状态数据");
        
        String weatherCodeQueryObj = state.value(WeatherGraphConstants.KEY_WEATHER_CODE_QUERY, "");
        log.info("天气代码查询对象类型: {}", weatherCodeQueryObj != null ? weatherCodeQueryObj.getClass().getName() : "null");
        log.info("天气代码查询对象: {}", weatherCodeQueryObj);
        
        String forecastResult = "";
        String transformedResult = "";
        
        try {
            log.info("开始解析天气查询参数...");
            WeatherCodeQuery query = parseWeatherCodeQuery(weatherCodeQueryObj);
            if (query == null) {
                log.warn("无法解析天气查询参数，使用默认值");
                forecastResult = "无法获取天气数据";
            } else {
                log.info("解析后的查询参数: 城市={}, 开始={}, 结束={}, 纬度={}, 经度={}", 
                        query.getLocation(), query.getBeginTime(), query.getEndTime(),
                        query.getLatitude(), query.getLongitude());
                
                log.info("开始获取天气码（本地优先，MCP兜底）...");
                String weatherCodes = mcpPredictionTool.acquireWeatherCodesWithFallback(query);
                log.info("获取到的天气码: {}", weatherCodes);
                
                log.info("开始调用AI进行天气数据转化...");
                forecastResult = aiClient.performForecastTransform(weatherCodes);
                log.info("AI返回天气数据: {}", forecastResult);

                if (forecastResult != null && !forecastResult.isEmpty()) {
                    log.info("开始调用AI进行语义转化...");
                    transformedResult = aiClient.performForecastTransform(forecastResult);
                    log.info("语义转化结果: {}", transformedResult);
                }
            }
        } catch (Exception e) {
            log.error("forecast节点执行异常: {}", e.getMessage(), e);
            forecastResult = "获取天气数据失败: " + e.getMessage();
            transformedResult = forecastResult;
        }
        
        log.info("---------- [forecast节点] 执行完成 ----------");
        
        return Map.of(
            WeatherGraphConstants.KEY_FORECAST_RESULT, transformedResult != null ? transformedResult : ""
        );
    }

    private WeatherCodeQuery parseWeatherCodeQuery(String obj) {

        if (obj == null) {
            return null;
        }
        
        try {
            JSONObject json = JSON.parseObject((String) obj);
            WeatherCodeQuery query = new WeatherCodeQuery();

            if (json.containsKey("city")) {
                query.setLocation(json.getString("city"));
            } else if (json.containsKey("location")) {
                query.setLocation(json.getString("location"));
            }

            if (json.containsKey("beginTime")) {
                String beginTimeStr = json.getString("beginTime");
                query.setBeginTime(LocalDateTime.parse(beginTimeStr, FORMATTER));
            }

            if (json.containsKey("endTime")) {
                String endTimeStr = json.getString("endTime");
                query.setEndTime(LocalDateTime.parse(endTimeStr, FORMATTER));
            }

            if (json.containsKey("latitude")) {
                query.setLatitude(json.getDouble("latitude"));
            }

            if (json.containsKey("longitude")) {
                query.setLongitude(json.getDouble("longitude"));
            }

            return query;

        } catch (Exception e) {
            log.error("解析WeatherCodeQuery失败: {}", e.getMessage(), e);
        }
        
        return null;
    }
}
