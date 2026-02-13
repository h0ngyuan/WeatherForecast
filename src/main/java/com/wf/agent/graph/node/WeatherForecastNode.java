package com.wf.agent.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.wf.agent.base.AIClient;
import com.wf.agent.constants.WeatherGraphConstants;
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
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public WeatherForecastNode(AIClient aiClient) {
        this.aiClient = aiClient;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("========== [forecast节点] 开始执行 ==========");
        log.info("forecast节点已被调用，准备获取状态数据");
        
        Object weatherCodeQueryObj = state.value(WeatherGraphConstants.KEY_WEATHER_CODE_QUERY, null);
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
                log.info("解析后的查询参数: 城市={}, 开始={}, 结束={}", 
                        query.getLocation(), query.getBeginTime(), query.getEndTime());
                
                log.info("开始调用AI获取天气数据...");
                forecastResult = aiClient.performForecast(query);
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

    private WeatherCodeQuery parseWeatherCodeQuery(Object obj) {
        if (obj == null) {
            return null;
        }
        
        try {
            if (obj instanceof WeatherCodeQuery) {
                return (WeatherCodeQuery) obj;
            }
            
            if (obj instanceof String) {
                String jsonStr = (String) obj;
                JSONObject json = JSON.parseObject(jsonStr);
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
                
                return query;
            }
            
            if (obj instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) obj;
                WeatherCodeQuery query = new WeatherCodeQuery();
                
                if (map.containsKey("city")) {
                    query.setLocation(String.valueOf(map.get("city")));
                } else if (map.containsKey("location")) {
                    query.setLocation(String.valueOf(map.get("location")));
                }
                
                if (map.containsKey("beginTime")) {
                    Object beginTime = map.get("beginTime");
                    if (beginTime instanceof LocalDateTime) {
                        query.setBeginTime((LocalDateTime) beginTime);
                    } else if (beginTime instanceof String) {
                        query.setBeginTime(LocalDateTime.parse((String) beginTime, FORMATTER));
                    }
                }
                
                if (map.containsKey("endTime")) {
                    Object endTime = map.get("endTime");
                    if (endTime instanceof LocalDateTime) {
                        query.setEndTime((LocalDateTime) endTime);
                    } else if (endTime instanceof String) {
                        query.setEndTime(LocalDateTime.parse((String) endTime, FORMATTER));
                    }
                }
                
                return query;
            }
        } catch (Exception e) {
            log.error("解析WeatherCodeQuery失败: {}", e.getMessage(), e);
        }
        
        return null;
    }
}
