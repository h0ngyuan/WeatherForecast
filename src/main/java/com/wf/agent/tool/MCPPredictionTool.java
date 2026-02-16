package com.wf.agent.tool;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wf.object.entity.ParamDataEntity;
import com.wf.object.query.WeatherCodeQuery;
import com.wf.service.ParamService;
import com.wf.service.WeatherForecastService;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.WebFluxSseClientTransport;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MCPPredictionTool {

    private final WeatherForecastService localForecastService;
    private final ParamService paramService;

    @Value("${mcp.weather.endpoint:https://dashscope.aliyuncs.com/api/v1/mcps/zuimei-getweather/sse}")
    private String mcpEndpoint;

    @Value("${mcp.weather.base-url:https://dashscope.aliyuncs.com}")
    private String mcpBaseUrl;

    @Value("${mcp.weather.sse-path:/api/v1/mcps/zuimei-getweather/sse}")
    private String mcpSsePath;

    @Value("${mcp.weather.api-key:}")
    private String mcpApiKey;

    /**
     * 通过MCP服务获取天气码 - 供Agent自动调用
     */
    @Tool(name = "getWeatherFromMCP", description = "通过MCP服务获取指定经纬度的天气信息，返回天气码列表。当本地天气服务无法获取数据时使用此工具。")
    public String getWeatherFromMCP(
            @ToolParam(description = "纬度，例如：39.9042") Double latitude,
            @ToolParam(description = "经度，例如：116.4074") Double longitude) {
        
        log.info("========== [MCPPredictionTool] Agent调用MCP服务 ==========");
        log.info("参数: latitude={}, longitude={}", latitude, longitude);

        if (latitude == null || longitude == null) {
            log.warn("经纬度参数为空");
            return null;
        }

        if (mcpApiKey == null || mcpApiKey.isEmpty()) {
            log.warn("MCP API Key未配置，MCP服务不可用");
            return null;
        }

        McpSyncClient client = null;
        try {
            // 创建 WebClient.Builder 并添加 Authorization Header
            // baseUrl 应该是基础 URL (如 https://dashscope.aliyuncs.com)
            // SSE 路径是 /api/v1/mcps/zuimei-getweather/sse
            log.info("MCP Base URL: {}", mcpBaseUrl);
            log.info("MCP SSE Path: {}", mcpSsePath);
            
            WebClient.Builder webClientBuilder = WebClient.builder()
                    .baseUrl(mcpBaseUrl)
                    .defaultHeader("Authorization", "Bearer " + mcpApiKey);

            // 创建 JSON Mapper
            JacksonMcpJsonMapper jsonMapper = new JacksonMcpJsonMapper(new ObjectMapper());

            // 创建 SSE 传输层 - 使用 builder 模式设置 SSE endpoint
            WebFluxSseClientTransport transport = WebFluxSseClientTransport.builder(webClientBuilder)
                    .sseEndpoint(mcpSsePath)
                    .build();

            // 创建 MCP Client
            client = McpClient.sync(transport)
                    .requestTimeout(Duration.ofSeconds(60))
                    .build();

            // 初始化客户端
            log.info("初始化 MCP Client...");
            client.initialize();
            log.info("MCP Client 初始化完成");

            // 获取并显示可用工具列表
            try {
                McpSchema.ListToolsResult toolsResult = client.listTools();
                List<McpSchema.Tool> tools = toolsResult.tools();
                log.info("========== MCP 可用工具列表 ==========");
                log.info("共发现 {} 个工具:", tools.size());
                for (int i = 0; i < tools.size(); i++) {
                    McpSchema.Tool tool = tools.get(i);
                    log.info("  {}. {} - {}", i + 1, tool.name(), tool.description());
                }
                log.info("======================================");
            } catch (Exception e) {
                log.warn("获取工具列表失败: {}", e.getMessage());
            }

            // 调用工具
            log.info("调用 getWeather 工具...");
            
            Map<String, Object> arguments = Map.of(
                    "latitude", latitude,
                    "longitude", longitude
            );
            
            McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("getWeather", arguments);
            McpSchema.CallToolResult result = client.callTool(request);
            
            // 直接获取content并解析JSON
            if (result == null || result.content() == null || result.content().isEmpty()) {
                log.warn("MCP返回结果为空");
                return null;
            }
            
            McpSchema.Content content = result.content().get(0);
            if (!(content instanceof McpSchema.TextContent textContent)) {
                log.warn("MCP返回内容不是文本类型");
                return null;
            }
            
            String jsonText = textContent.text();
            log.info("收到MCP响应，开始解析JSON...");
            
            // 直接解析JSON提取天气码
            List<String> weatherCodes = new ArrayList<>();
            try {
                JSONObject root = JSON.parseObject(jsonText);
                // 先获取 data 对象
                if (!root.containsKey("data")) {
                    log.warn("JSON中缺少data字段");
                    return null;
                }
                JSONObject data = root.getJSONObject("data");
                
                // 提取24小时逐小时天气码
                if (data.containsKey("hourlys")) {
                    JSONArray hourlys = data.getJSONArray("hourlys");
                    for (int i = 0; i < hourlys.size(); i++) {
                        JSONObject hourly = hourlys.getJSONObject(i);
                        if (hourly.containsKey("wid")) {
                            weatherCodes.add(String.valueOf(hourly.getInteger("wid")));
                        }
                    }
                }
                
                // 提取16天逐日天气码（白天+夜间）
//                if (data.containsKey("dailys")) {
//                    JSONObject dailys = data.getJSONObject("dailys");
//                    if (dailys.containsKey("dailyweathers")) {
//                        JSONArray dailyWeathers = dailys.getJSONArray("dailyweathers");
//                        for (int i = 0; i < dailyWeathers.size(); i++) {
//                            JSONObject daily = dailyWeathers.getJSONObject(i);
//                            // 白天天气码
//                            if (daily.containsKey("conditionDay")) {
//                                JSONObject day = daily.getJSONObject("conditionDay");
//                                if (day.containsKey("cc")) {
//                                    weatherCodes.add(String.valueOf(day.getInteger("cc")));
//                                }
//                            }
//                            // 夜间天气码
//                            if (daily.containsKey("conditionNight")) {
//                                JSONObject night = daily.getJSONObject("conditionNight");
//                                if (night.containsKey("cc")) {
//                                    weatherCodes.add(String.valueOf(night.getInteger("cc")));
//                                }
//                            }
//                        }
//                    }
//                }

                log.info("解析到 {} 个天气码", weatherCodes.size());
            } catch (Exception e) {
                log.error("解析JSON失败: {}", e.getMessage());
                return null;
            }
            
            if (!weatherCodes.isEmpty()) {
                String resultStr = String.join(",", weatherCodes);
                log.info("MCP返回天气码: {}", resultStr);
                return resultStr;
            }
            
            return null;

        } catch (Exception e) {
            log.error("MCP调用异常: {}", e.getMessage(), e);
            return null;
        } finally {
            if (client != null) {
                try {
                    client.close();
                    log.info("MCP Client 已关闭");
                } catch (Exception e) {
                    log.warn("关闭MCP Client失败: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * 供WeatherPredictionService直接调用的方法
     */
    public List<String> acquireWeatherCodeFromMCP(WeatherCodeQuery query) {
        log.info("========== [MCPPredictionTool] 开始调用MCP服务 ==========");
        log.info("查询参数: location={}, beginTime={}, endTime={}, lat={}, lon={}", 
                query.getLocation(), query.getBeginTime(), query.getEndTime(),
                query.getLatitude(), query.getLongitude());

        if (query.getLatitude() == null || query.getLongitude() == null) {
            log.warn("MCP调用需要经纬度，但未提供。location={}", query.getLocation());
            return null;
        }

        String result = getWeatherFromMCP(query.getLatitude(), query.getLongitude());
        
        if (result != null && !result.isEmpty()) {
            return List.of(result.split(","));
        }
        
        return null;
    }

    /**
     * 从JSON中提取天气码
     */
    private List<String> extractWeatherCodesFromJson(String jsonText) {
        List<String> weatherCodes = new ArrayList<>();
        
        try {
            JSONObject root = JSON.parseObject(jsonText);
            if (!root.containsKey("data")) {
                log.warn("JSON中未找到data字段");
                return weatherCodes;
            }
            
            JSONObject data = root.getJSONObject("data");
            
            // 1. 提取当前天气码
            if (data.containsKey("condition")) {
                JSONObject condition = data.getJSONObject("condition");
                if (condition != null && condition.containsKey("cc")) {
                    weatherCodes.add(String.valueOf(condition.getInteger("cc")));
                }
            }
            
            // 2. 提取24小时逐小时天气码
            if (data.containsKey("hourlys")) {
                com.alibaba.fastjson2.JSONArray hourlys = data.getJSONArray("hourlys");
                if (hourlys != null) {
                    for (int i = 0; i < hourlys.size(); i++) {
                        JSONObject hourly = hourlys.getJSONObject(i);
                        if (hourly != null && hourly.containsKey("cc")) {
                            weatherCodes.add(String.valueOf(hourly.getInteger("cc")));
                        }
                    }
                }
            }
            
            // 3. 提取16天逐日天气码（白天+夜间）
            if (data.containsKey("dailys")) {
                JSONObject dailys = data.getJSONObject("dailys");
                if (dailys != null && dailys.containsKey("dailyweathers")) {
                    com.alibaba.fastjson2.JSONArray dailyWeathers = dailys.getJSONArray("dailyweathers");
                    if (dailyWeathers != null) {
                        for (int i = 0; i < dailyWeathers.size(); i++) {
                            JSONObject daily = dailyWeathers.getJSONObject(i);
                            if (daily != null) {
                                // 白天天气码
                                if (daily.containsKey("conditionDay")) {
                                    JSONObject day = daily.getJSONObject("conditionDay");
                                    if (day != null && day.containsKey("cc")) {
                                        weatherCodes.add(String.valueOf(day.getInteger("cc")));
                                    }
                                }
                                // 夜间天气码
                                if (daily.containsKey("conditionNight")) {
                                    JSONObject night = daily.getJSONObject("conditionNight");
                                    if (night != null && night.containsKey("cc")) {
                                        weatherCodes.add(String.valueOf(night.getInteger("cc")));
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            log.info("共提取 {} 个天气码", weatherCodes.size());
        } catch (Exception e) {
            log.error("解析JSON失败: {}", e.getMessage());
        }
        
        return weatherCodes;
    }

    public boolean isAvailable() {
        return mcpApiKey != null && !mcpApiKey.isEmpty();
    }

    /**
     * 获取天气码（本地优先，MCP兜底）
     */
    public String acquireWeatherCodesWithFallback(WeatherCodeQuery query) {
        log.info("========== [MCPPredictionTool] 开始获取天气码 ==========");
        log.info("查询参数: location={}, beginTime={}, endTime={}, lat={}, lon={}",
                query.getLocation(), query.getBeginTime(), query.getEndTime(),
                query.getLatitude(), query.getLongitude());

        List<String> localResult = tryLocalService(query);
        if (localResult != null && !localResult.isEmpty()) {
            log.info("本地服务返回成功，天气码数量: {}", localResult.size());
            String result = String.join(",", localResult);
            log.info("========== [MCPPredictionTool] 本地服务成功 ==========");
            return result;
        }

        log.warn("本地服务返回空或失败，尝试MCP服务...");
        enrichCoordinates(query);

        List<String> mcpResult = tryMCPService(query);
        if (mcpResult != null && !mcpResult.isEmpty()) {
            log.info("MCP服务返回成功，天气码数量: {}", mcpResult.size());
            String result = String.join(",", mcpResult);
            log.info("========== [MCPPredictionTool] MCP服务成功 ==========");
            return result;
        }

        log.warn("本地和MCP服务均失败，返回兜底值");
        log.info("========== [MCPPredictionTool] 双重失败，返回兜底值 ==========");
        return "0,0,0";
    }

    private void enrichCoordinates(WeatherCodeQuery query) {
        if (query.getLatitude() != null && query.getLongitude() != null) {
            log.info("查询已包含经纬度，无需补全");
            return;
        }

        String city = query.getLocation();
        if (city == null || city.isEmpty()) {
            log.warn("城市名称为空，无法补全经纬度");
            return;
        }

        try {
            List<ParamDataEntity> cities = paramService.getCities();
            if (cities == null || cities.isEmpty()) {
                log.warn("城市列表为空，无法补全经纬度");
                return;
            }

            for (ParamDataEntity cityEntity : cities) {
                String description = cityEntity.getDescription();
                if (description == null || description.isEmpty()) {
                    continue;
                }

                JSONObject cityInfo = JSON.parseObject(description);
                String cityName = cityInfo.getString("city");

                if (city != null && city.equals(cityName)) {
                    Double lat = cityInfo.getDouble("latitude");
                    Double lon = cityInfo.getDouble("longitude");

                    if (lat != null && lon != null) {
                        query.setLatitude(lat);
                        query.setLongitude(lon);
                        log.info("成功补全经纬度: city={}, lat={}, lon={}", city, lat, lon);
                        return;
                    }
                }
            }

            log.warn("未找到城市 {} 对应的经纬度", city);
        } catch (Exception e) {
            log.error("补全经纬度失败: {}", e.getMessage(), e);
        }
    }

    private List<String> tryLocalService(WeatherCodeQuery query) {
        try {
            log.info("尝试本地服务...");
            List<String> result = localForecastService.acquireWeatherCodeValueByRangeTime(query);
            if (result != null && !result.isEmpty()) {
                boolean hasValidData = result.stream().anyMatch(code -> code != null && !code.isEmpty());
                if (hasValidData) {
                    return result;
                }
            }
            log.warn("本地服务返回数据无效");
            return null;
        } catch (Exception e) {
            log.error("本地服务调用异常: {}", e.getMessage(), e);
            return null;
        }
    }

    private List<String> tryMCPService(WeatherCodeQuery query) {
        if (!isAvailable()) {
            log.warn("MCP服务未配置API Key，跳过");
            return null;
        }

        if (query.getLatitude() == null || query.getLongitude() == null) {
            log.warn("MCP服务需要经纬度，但未提供");
            return null;
        }

        try {
            log.info("尝试MCP服务...");
            return acquireWeatherCodeFromMCP(query);
        } catch (Exception e) {
            log.error("MCP服务调用异常: {}", e.getMessage(), e);
            return null;
        }
    }
}
