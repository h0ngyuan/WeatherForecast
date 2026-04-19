package com.wf.agent.map.controller;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.wf.agent.map.constants.MapGraphConstants;
import com.wf.agent.map.dto.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.tool.api.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

/**
 * 多Agent地图分析控制器
 * 提供基于SubGraph的地图分析、评测接口
 */
@RestController
@RequestMapping("/map")
@Slf4j
public class MapAgentController {

    @Autowired
    @Qualifier("mapInsightCompiledGraph")
    private CompiledGraph mapInsightGraph;

    @Autowired
    @Qualifier("replayGraph")
    private CompiledGraph replayGraph;

    @Autowired
    @Qualifier("ablationGraph")
    private CompiledGraph ablationGraph;

    /**
     * 1. 地图分析（主功能）- 多Agent协作
     */
    @PostMapping("/analyze")
    public R<MapInsightResponse> analyze(@RequestBody MapAgentAnalysisRequest request) {
        log.info("[MapAgentController] 多Agent分析: location={}, radius={}, date={}", 
                request.getLocation(), request.getRadiusKm(), request.getDate());

        Map<String, Object> initialState = new HashMap<>();
        initialState.put(MapGraphConstants.KEY_LOCATION, request.getLocation());
        Integer radius = request.getRadiusKm();
        String date = request.getDate();
        String query = request.getQuery();
        initialState.put(MapGraphConstants.KEY_RADIUS_KM, radius != null ? radius : 100);
        initialState.put(MapGraphConstants.KEY_DATE, date != null ? date : LocalDate.now().toString());
        initialState.put(MapGraphConstants.KEY_QUERY, query != null ? query : "地图分析");

        try {
            String threadId = "map-agent-" + UUID.randomUUID();
            RunnableConfig config = RunnableConfig.builder().threadId(threadId).build();
            Optional<OverAllState> result = mapInsightGraph.invoke(initialState, config);

            if (result.isPresent()) {
                OverAllState state = result.get();
                String finalReportStr = state.value(MapGraphConstants.KEY_FINAL_REPORT, "{}");
                JSONObject finalReport = JSON.parseObject(finalReportStr);

                MapInsightResponse response = new MapInsightResponse();
                response.setQuery(query != null ? query : "地图分析");
                response.setConclusion(finalReport.getString("conclusion"));
                response.setExplanation(finalReport.getString("arbitrationReason"));
                
                log.info("[MapAgentController] 分析完成: riskLevel={}", finalReport.getInteger("riskLevel"));
                return R.data(response);
            } else {
                return R.fail("分析执行失败");
            }
        } catch (Exception e) {
            log.error("[MapAgentController] 分析异常", e);
            return R.fail("分析异常: " + e.getMessage());
        }
    }

    /**
     * 2. 历史案例回放
     */
    @PostMapping("/evaluate/replay")
    public R<EvaluationReport> replay(@RequestBody ReplayRequest request) {
        log.info("[MapAgentController] 历史案例回放: caseId={}", request.getCaseId());

        Map<String, Object> initialState = new HashMap<>();
        String caseId = request.getCaseId();
        initialState.put("caseId", caseId != null ? caseId : "");
        initialState.put("caseLimit", 10);
        initialState.put(MapGraphConstants.KEY_EXECUTION_START_TIME, String.valueOf(System.currentTimeMillis()));

        try {
            String threadId = "replay-" + UUID.randomUUID();
            RunnableConfig config = RunnableConfig.builder().threadId(threadId).build();
            Optional<OverAllState> result = replayGraph.invoke(initialState, config);

            if (result.isPresent()) {
                OverAllState state = result.get();
                String reportStr = state.value("evaluationReport", "{}");
                EvaluationReport report = JSON.parseObject(reportStr, EvaluationReport.class);
                return R.data(report);
            } else {
                return R.fail("回放执行失败");
            }
        } catch (Exception e) {
            log.error("[MapAgentController] 回放异常", e);
            return R.fail("回放异常: " + e.getMessage());
        }
    }

    /**
     * 3. 消融测试
     */
    @PostMapping("/evaluate/ablation")
    public R<AblationReport> ablation(@RequestBody AblationRequest request) {
        log.info("[MapAgentController] 消融测试: location={}, exclude={}", 
                request.getLocation(), request.getExcludeAgents());

        Integer radiusKm = request.getRadiusKm();
        String dateStr = request.getDate();
        String location = request.getLocation();
        Set<String> excludeAgents = request.getExcludeAgents();

        Map<String, Object> initialState = new HashMap<>();
        initialState.put(MapGraphConstants.KEY_LOCATION, location != null ? location : "成都");
        initialState.put(MapGraphConstants.KEY_RADIUS_KM, radiusKm != null ? radiusKm : 100);
        initialState.put(MapGraphConstants.KEY_DATE, dateStr != null ? dateStr : LocalDate.now().toString());
        initialState.put(MapGraphConstants.KEY_EXCLUDE_AGENTS, excludeAgents != null ? excludeAgents : new HashSet<>());

        try {
            String threadId = "ablation-" + UUID.randomUUID();
            RunnableConfig config = RunnableConfig.builder().threadId(threadId).build();
            Optional<OverAllState> result = ablationGraph.invoke(initialState, config);

            if (result.isPresent()) {
                OverAllState state = result.get();
                String reportStr = state.value(MapGraphConstants.KEY_ABLATION_REPORT, "{}");
                AblationReport report = JSON.parseObject(reportStr, AblationReport.class);
                return R.data(report);
            } else {
                return R.fail("消融测试执行失败");
            }
        } catch (Exception e) {
            log.error("[MapAgentController] 消融测试异常", e);
            return R.fail("消融测试异常: " + e.getMessage());
        }
    }

    /**
     * 4. 批量评测
     */
    @PostMapping("/evaluate/benchmark")
    public R<BenchmarkReport> benchmark(@RequestBody BenchmarkRequest request) {
        log.info("[MapAgentController] 批量评测: location={}, limit={}, runAblation={}", 
                request.getLocation(), request.getCaseLimit(), request.isRunAblation());

        BenchmarkReport report = new BenchmarkReport();
        Integer caseLimit = request.getCaseLimit();

        try {
            long startTime = System.currentTimeMillis();

            // 1. 回放评测
            Map<String, Object> replayState = new HashMap<>();
            replayState.put("caseId", "");
            replayState.put("caseLimit", caseLimit != null ? caseLimit : 10);
            replayState.put(MapGraphConstants.KEY_EXECUTION_START_TIME, String.valueOf(System.currentTimeMillis()));
            
            Optional<OverAllState> replayResult = replayGraph.invoke(replayState, 
                    RunnableConfig.builder().threadId("benchmark-replay-" + UUID.randomUUID()).build());
            
            if (replayResult.isPresent()) {
                String evalStr = replayResult.get().value("evaluationReport", "{}");
                report.setReplayReport(JSON.parseObject(evalStr, EvaluationReport.class));
            }

            // 2. 消融测试（可选）
            if (request.isRunAblation()) {
                Map<String, Object> ablationState = new HashMap<>();
                ablationState.put(MapGraphConstants.KEY_LOCATION, request.getLocation());
                ablationState.put(MapGraphConstants.KEY_RADIUS_KM, 100);
                ablationState.put(MapGraphConstants.KEY_DATE, LocalDate.now().toString());
                ablationState.put(MapGraphConstants.KEY_EXCLUDE_AGENTS, new HashSet<>());
                
                Optional<OverAllState> ablationResult = ablationGraph.invoke(ablationState, 
                        RunnableConfig.builder().threadId("benchmark-ablation-" + UUID.randomUUID()).build());
                
                if (ablationResult.isPresent()) {
                    String ablStr = ablationResult.get().value(MapGraphConstants.KEY_ABLATION_REPORT, "{}");
                    report.setAblationReport(JSON.parseObject(ablStr, AblationReport.class));
                }
            }

            report.setTotalExecutionTimeMs(System.currentTimeMillis() - startTime);
            report.setTotalCasesEvaluated(caseLimit != null ? caseLimit : 10);

            return R.data(report);
        } catch (Exception e) {
            log.error("[MapAgentController] 批量评测异常", e);
            return R.fail("批量评测异常: " + e.getMessage());
        }
    }

    @Data
    public static class MapAgentAnalysisRequest {
        private String location;
        private Integer radiusKm;
        private String date;
        private String query;
    }
}
