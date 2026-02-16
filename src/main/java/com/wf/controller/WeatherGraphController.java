package com.wf.controller;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.context.model.SaRequest;
import com.wf.agent.tool.MCPPredictionTool;
import com.wf.object.request.WeatherAskRequest;
import com.wf.object.response.WeatherAskResponse;
import com.wf.service.MilvusService;
import com.wf.service.WeatherDataService;
import com.wf.service.WeatherGraphOrchestrator;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.tool.api.R;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
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

    @PostMapping("/query")
    public ResponseEntity<WeatherAskResponse> ask(@Valid @RequestBody WeatherAskRequest request) {
        try {
            WeatherAskResponse response = orchestrator.process(request.question());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new WeatherAskResponse("服务暂时不可用", false, 0.0, 0.0, 0));
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