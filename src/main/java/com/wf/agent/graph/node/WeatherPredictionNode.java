package com.wf.agent.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.wf.agent.tool.MCPPredictionTool;
import com.wf.service.WeatherDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 天气预测 Node
 *
 * 职责：
 * 使用 MCP 工具查询指定地区的24小时天气码值
 *
 * 输入 State:
 *   - location: String (地区名称)
 *   - latitude: Double (纬度)
 *   - longitude: Double (经度)
 *
 * 输出 State:
 *   - weatherCodes: List<Integer> (24小时天气码)
 *
 * @author author
 * @since 1.0.0
 */
@Slf4j
@Component
public class WeatherPredictionNode implements NodeAction {

    private final MCPPredictionTool mcpPredictionTool;
    private final WeatherDataService weatherDataService;

    public WeatherPredictionNode(MCPPredictionTool mcpPredictionTool,
                                 WeatherDataService weatherDataService) {
        this.mcpPredictionTool = mcpPredictionTool;
        this.weatherDataService = weatherDataService;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String location = state.value("location", "unknown");
        double latitude = state.value("latitude", 0.0);
        double longitude = state.value("longitude", 0.0);

        log.info("[WeatherPredictionNode] 查询 {} 的天气码, 坐标: ({}, {})", location, latitude, longitude);

        String result = mcpPredictionTool.getWeatherFromMCP(latitude, longitude);

        if (result == null || result.isEmpty()) {
            log.warn("[WeatherPredictionNode] MCP 返回空结果");
            return Map.of("weatherCodes", List.of());
        }

        List<Integer> weatherCodes = Arrays.stream(result.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .collect(Collectors.toList());

        log.info("[WeatherPredictionNode] 获取到 {} 个天气码: {}", weatherCodes.size(), weatherCodes);

        // 异步写入 city_weather_daily 表
        weatherDataService.saveSingleCityToDailyAsync(location, result);

        return Map.of("weatherCodes", weatherCodes);
    }
}
