package com.wf.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.wf.agent.tool.MCPPredictionTool;
import com.wf.object.request.WeatherAskRequest;
import com.wf.object.request.WeatherPermissionRequest;
import com.wf.object.response.WeatherAskResponse;
import com.wf.service.MilvusService;
import com.wf.service.WeatherDataService;
import com.wf.service.WeatherGraphOrchestrator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.tool.api.R;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/weather")
@RequiredArgsConstructor
public class WeatherGraphController {

    private final WeatherGraphOrchestrator orchestrator;
    private final WeatherDataService weatherDataService;
    private final MilvusService milvusService;
    private final MCPPredictionTool mcpPredictionTool;

    @Operation(summary = "天气查询", description = "启动天气查询流程，支持人工干预机制。如果用户没有通知权限但需要发送预警，流程会暂停等待授权")
    @PostMapping("/query")
    public R<WeatherAskResponse> ask(@Valid @RequestBody WeatherAskRequest request) {
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            log.info("查询天气，question={}, userId={}", request.getQuestion(), userId);
            WeatherAskResponse result = orchestrator.processWithThread(request.getQuestion(), userId);
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
}
