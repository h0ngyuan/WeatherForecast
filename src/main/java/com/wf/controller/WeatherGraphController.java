package com.wf.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.wf.agent.tool.MCPPredictionTool;
import com.wf.mapper.CityInfoMapper;
import com.wf.object.entity.CityInfoEntity;
import com.wf.utils.LocationUtils;
import com.wf.object.entity.ChatHistoryEntity;
import com.wf.object.request.WeatherAskRequest;
import com.wf.object.request.WeatherPermissionRequest;
import com.wf.object.request.WeatherSubscribeRequest;
import com.wf.object.response.WeatherAskResponse;
import com.wf.service.ChatHistoryService;
import com.wf.service.MilvusService;
import com.wf.service.ReminderTaskService;
import com.wf.service.WeatherDataService;
import com.wf.service.WeatherGraphOrchestrator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.tool.api.R;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/weather")
@RequiredArgsConstructor
public class WeatherGraphController {

    private final WeatherGraphOrchestrator orchestrator;
    private final WeatherDataService weatherDataService;
    private final MilvusService milvusService;
    private final MCPPredictionTool mcpPredictionTool;
    private final ReminderTaskService reminderTaskService;
    private final ChatHistoryService chatHistoryService;
    private final CityInfoMapper cityInfoMapper;

    @Operation(summary = "天气查询", description = "启动天气查询流程，支持人工干预机制。如果用户没有通知权限但需要发送预警，流程会暂停等待授权")
    @PostMapping("/query")
    public R<WeatherAskResponse> ask(@Valid @RequestBody WeatherAskRequest request) {
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            log.info("查询天气，question={}, userId={}", request.getQuestion(), userId);

            // 获取或创建会话
            Long sessionId = request.getSessionId();
            if (sessionId == null) {
                sessionId = chatHistoryService.getOrCreateCurrentSession(userId);
            }

            // 保存用户消息
            ChatHistoryEntity userMessage = new ChatHistoryEntity();
            userMessage.setSessionId(sessionId);
            userMessage.setUserId(userId);
            userMessage.setRole("user");
            userMessage.setContent(request.getQuestion());
            userMessage.setMessageType(0); // 0=文本
            chatHistoryService.saveMessage(userMessage);

            // 执行查询
            WeatherAskResponse result = orchestrator.processWithThread(request.getQuestion(), userId, sessionId);

            // 保存AI回复
            ChatHistoryEntity assistantMessage = new ChatHistoryEntity();
            assistantMessage.setSessionId(sessionId);
            assistantMessage.setUserId(userId);
            assistantMessage.setRole("assistant");
            assistantMessage.setContent(result.answer());
            assistantMessage.setMessageType(0); // 0=文本
            chatHistoryService.saveMessage(assistantMessage);

            return R.data(result);
        } catch (Exception e) {
            log.error("流程执行失败", e);
            return R.fail("执行失败: " + e.getMessage());
        }
    }

    @Operation(summary = "授权通知权限", description = "用户在人工干预节点授权通知权限后调用此接口，只更新用户权限设置，不恢复流程")
    @PostMapping("/grant-permission")
    public R<Void> grantPermission(@Valid @RequestBody WeatherPermissionRequest request) {
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            log.info("用户授权通知权限，threadId={}, userId={}", request.getThreadId(), userId);
            orchestrator.grantPermission(request.getThreadId(), userId, request);
            return R.success("授权成功");
        } catch (Exception e) {
            log.error("授权失败", e);
            return R.fail("授权失败: " + e.getMessage());
        }
    }

    @Operation(summary = "恢复流程", description = "用户授权后调用此接口恢复流程执行")
    @PostMapping("/resume")
    public R<WeatherAskResponse> resume(@RequestParam("threadId") String threadId) {
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            log.info("恢复流程，threadId={}, userId={}", threadId, userId);
            WeatherAskResponse result = orchestrator.resume(threadId, userId);
            return R.data(result);
        } catch (Exception e) {
            log.error("恢复流程失败", e);
            return R.fail("恢复流程失败: " + e.getMessage());
        }
    }

    @Operation(summary = "拒绝授权并结束流程", description = "用户在人工干预节点拒绝授权时调用此接口，会直接结束流程")
    @PostMapping("/reject-permission")
    public R<WeatherAskResponse> rejectPermission(@RequestParam("threadId") String threadId) {
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            log.info("用户拒绝授权，threadId={}, userId={}", threadId, userId);
            WeatherAskResponse result = orchestrator.rejectPermission(threadId, userId);
            return R.data(result);
        } catch (Exception e) {
            log.error("拒绝授权失败", e);
            return R.fail("拒绝授权失败: " + e.getMessage());
        }
    }

    @Schema(description = "测试——获取历史范围内天气数据")
    @PostMapping("/fetch-data")
    public R fetchWeatherData(@RequestParam("beginTime")Integer beginTime,@RequestParam("endTime")Integer endTime) {
        try {
            weatherDataService.manualFetchWeatherData(beginTime,endTime);
            return R.success("天气数据获取成功");
        } catch (Exception e) {
            log.error("手动获取天气数据失败", e);
            return R.fail("天气数据获取失败: " + e.getMessage());
        }
    }

    @PostMapping("/predict")
    public R predictWeatherData() {
        try {
            log.info("手动触发天气预测任务");
            weatherDataService.predictWeatherData();
            return R.success("天气预测任务执行成功");
        } catch (Exception e) {
            log.error("手动触发天气预测失败", e);
            return R.fail("天气预测任务失败: " + e.getMessage());
        }
    }

    @Schema(description = "导入Milvus向量数据")
    @PostMapping("/milvus/import")
    public R importMilvusData() {
        try {
            log.info("开始导入Milvus向量数据");
            
            log.info("1. 从Excel读取数据...");
            List<com.wf.object.entity.MilvusData> allData = milvusService.acquireDataFromExcel();
            log.info("读取到 {} 条数据", allData.size());

            if (allData.isEmpty()) {
                return R.fail("数据为空，请先准备Excel文件");
            }

            log.info("2. 分批处理数据...");
            int batchSize = 1000;
            int totalBatches = (int) Math.ceil((double) allData.size() / batchSize);
            
            for (int i = 0; i < totalBatches; i++) {
                int fromIndex = i * batchSize;
                int toIndex = Math.min(fromIndex + batchSize, allData.size());
                List<com.wf.object.entity.MilvusData> batch = allData.subList(fromIndex, toIndex);
                
                log.info("处理批次 {}/{}，大小: {}", i + 1, totalBatches, batch.size());

                log.info("  生成向量...");
                List<com.wf.object.entity.MilvusData> dataWithVectors = milvusService.acquireEmbedVector(batch);
                log.info("  向量生成完成");

                log.info("  批量插入Milvus...");
                String insertResult = milvusService.milvusBatchInsert(dataWithVectors);
                log.info("  插入结果: {}", insertResult);
            }

            log.info("Milvus向量数据导入完成");
            return R.success("导入成功，共处理 " + allData.size() + " 条数据");
        } catch (Exception e) {
            log.error("导入Milvus数据失败", e);
            return R.fail("导入失败: " + e.getMessage());
        }
    }

    @Schema(description = "测试MCP连接")
    @GetMapping("/mcp/test")
    public R testMCPConnection(@RequestParam("lat") Double lat, @RequestParam("lon") Double lon) {
        try {
            log.info("测试MCP连接，lat={}, lon={}", lat, lon);

            if (lat == null || lon == null) {
                return R.fail("请提供经纬度参数");
            }

            String result = mcpPredictionTool.getWeatherFromMCP(lat, lon);

            if (result != null) {
                return R.success("MCP连接成功，返回天气码: " + result);
            } else {
                return R.fail("MCP连接失败或返回空结果");
            }
        } catch (Exception e) {
            log.error("MCP测试失败", e);
            return R.fail("MCP测试失败: " + e.getMessage());
        }
    }

    @Operation(summary = "天气订阅", description = "用户订阅特定天气条件，当条件满足时发送通知")
    @PostMapping("/subscribe")
    public R<Long> subscribe(@Valid @RequestBody WeatherSubscribeRequest request) {
        try {
            Long userId = StpUtil.getLoginIdAsLong();

            // 如果location为空，从请求IP解析
            String cityName = request.getLocation();
            Double latitude = null;
            Double longitude = null;
            if (cityName == null || cityName.isEmpty()) {
                try {
                    Map<String, Object> location = LocationUtils.getCurrentLocationMap();
                    if (location != null && location.get("city") != null) {
                        cityName = location.get("city").toString();
                        latitude = (Double) location.get("lat");
                        longitude = (Double) location.get("lon");
                        log.info("从IP解析到城市: {}", cityName);
                    } else {
                        cityName = "成都";
                    }
                } catch (Exception e) {
                    log.warn("IP解析城市失败，使用默认值", e);
                    cityName = "成都";
                }
                request.setLocation(cityName);
            }

            // 检查并确保城市信息存在于 CITY_INFO 表中
            ensureCityInfoExists(cityName, latitude, longitude);

            log.info("用户订阅天气, userId={}, subscribeName={}, location={}, weatherCodes={}",
                    userId, request.getSubscribeName(), request.getLocation(), request.getWeatherCodes());

            Long taskId = reminderTaskService.createSubscribeTask(userId, request);
            log.info("天气订阅创建成功, taskId={}", taskId);

            return R.data(taskId, "订阅成功");
        } catch (Exception e) {
            log.error("天气订阅失败", e);
            return R.fail("订阅失败: " + e.getMessage());
        }
    }

    /**
     * 确保城市信息存在于 CITY_INFO 表中，不存在则插入
     *
     * @param cityName  城市名称
     * @param latitude  纬度
     * @param longitude 经度
     */
    private void ensureCityInfoExists(String cityName, Double latitude, Double longitude) {
        try {
            if (cityName == null || cityName.isEmpty()) {
                log.warn("[WeatherGraphController] 城市名称为空，跳过CITY_INFO检查");
                return;
            }

            // 查询城市是否已存在
            CityInfoEntity existingCity = cityInfoMapper.selectByCityName(cityName);
            if (existingCity != null) {
                log.info("[WeatherGraphController] 城市 {} 已存在于CITY_INFO表中", cityName);
                return;
            }

            // 城市不存在，插入新记录
            CityInfoEntity newCity = new CityInfoEntity();
            newCity.setCityName(cityName);
            newCity.setCityCode(null);
            newCity.setLatitude(latitude != null ? BigDecimal.valueOf(latitude) : null);
            newCity.setLongitude(longitude != null ? BigDecimal.valueOf(longitude) : null);
            newCity.setProvince(null);
            newCity.setDistrict(null);
            newCity.setCityLevel(3);
            newCity.setTimezone("Asia/Shanghai");
            newCity.setAvailable(1);
            newCity.setIsHot(0);
            newCity.setDescription("天气订阅时自动添加的城市");

            cityInfoMapper.insert(newCity);
            log.info("[WeatherGraphController] 成功插入新城市到CITY_INFO表: {}, 经纬度: ({}, {})",
                    cityName, latitude, longitude);

        } catch (Exception e) {
            log.error("[WeatherGraphController] 检查/插入城市信息失败: {}", cityName, e);
            // 不影响主流程
        }
    }

    @Operation(summary = "获取聊天记录", description = "获取指定会话的聊天记录")
    @GetMapping("/chat-history")
    public R<List<ChatHistoryEntity>> getChatHistory(@RequestParam("sessionId") Long sessionId) {
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            log.info("获取聊天记录, sessionId={}, userId={}", sessionId, userId);
            List<ChatHistoryEntity> messages = chatHistoryService.getSessionMessages(sessionId);
            return R.data(messages);
        } catch (Exception e) {
            log.error("获取聊天记录失败", e);
            return R.fail("获取聊天记录失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取当前会话", description = "获取用户当前活跃的会话ID")
    @GetMapping("/current-session")
    public R<Long> getCurrentSession() {
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            Long sessionId = chatHistoryService.getOrCreateCurrentSession(userId);
            return R.data(sessionId);
        } catch (Exception e) {
            log.error("获取当前会话失败", e);
            return R.fail("获取当前会话失败: " + e.getMessage());
        }
    }

    @Operation(summary = "创建新会话", description = "创建一个新的聊天会话")
    @PostMapping("/new-session")
    public R<Long> createNewSession(@RequestParam(value = "title", required = false) String title) {
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            Long sessionId = chatHistoryService.createSession(userId, title);
            return R.data(sessionId, "会话创建成功");
        } catch (Exception e) {
            log.error("创建会话失败", e);
            return R.fail("创建会话失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取当前定位", description = "根据请求IP获取当前城市定位")
    @GetMapping("/current-location")
    public R<java.util.Map<String, Object>> getCurrentLocation() {
        try {
            java.util.Map<String, Object> location = LocationUtils.getCurrentLocationMap();
            if (location == null) {
                return R.fail("无法获取定位信息");
            }
            log.info("获取当前定位: {}", location);
            return R.data(location);
        } catch (Exception e) {
            log.error("获取当前定位失败", e);
            return R.fail("获取定位失败: " + e.getMessage());
        }
    }
}
