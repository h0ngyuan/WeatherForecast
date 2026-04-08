package com.wf.controller;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.wf.agent.entity.DisasterInfo;
import com.wf.job.WeatherEmergencyJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 紧急响应测试接口
 *
 * 用于手动测试 EmergencyResponseGraph 和 WeatherEmergencyJob
 *
 * @author author
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/test/emergency")
public class EmergencyTestController {

    private final CompiledGraph emergencyResponseGraph;
    private final WeatherEmergencyJob weatherEmergencyJob;

    @Autowired
    public EmergencyTestController(
            @Qualifier("emergencyResponseGraph") CompiledGraph emergencyResponseGraph,
            WeatherEmergencyJob weatherEmergencyJob) {
        this.emergencyResponseGraph = emergencyResponseGraph;
        this.weatherEmergencyJob = weatherEmergencyJob;
    }

    /**
     * 测试完整的 EmergencyResponseGraph
     *
     * 直接调用 Graph，不走 Job 的通知逻辑
     */
    @GetMapping("/graph")
    public String testGraph(
            @RequestParam(defaultValue = "南通") String location,
            @RequestParam(defaultValue = "32.0") double latitude,
            @RequestParam(defaultValue = "120.0") double longitude) {

        log.info("========== [测试] EmergencyResponseGraph ==========");
        log.info("测试地区: {}, 坐标: ({}, {})", location, latitude, longitude);

        StringBuilder result = new StringBuilder();
        result.append("╔══════════════════════════════════════════════════╗\n");
        result.append("║     EmergencyResponseGraph 测试结果              ║\n");
        result.append("╚══════════════════════════════════════════════════╝\n\n");

        try {
            Map<String, Object> initialState = new HashMap<>();
            initialState.put("location", location);
            initialState.put("latitude", latitude);
            initialState.put("longitude", longitude);

            Optional<OverAllState> graphResult = emergencyResponseGraph.invoke(initialState);

            if (graphResult.isEmpty()) {
                result.append("❌ Graph 执行失败，返回空结果\n");
                return result.toString();
            }

            OverAllState finalState = graphResult.get();

            // 获取天气码
            @SuppressWarnings("unchecked")
            List<Integer> weatherCodes = finalState.value("weatherCodes", List.of());
            result.append("📊 【Step 1】天气预测结果\n");
            result.append("   获取到 ").append(weatherCodes.size()).append(" 个天气码\n");
            if (!weatherCodes.isEmpty()) {
                result.append("   天气码: ").append(weatherCodes).append("\n");
            }
            result.append("\n");

            // 获取初步灾害
            @SuppressWarnings("unchecked")
            List<DisasterInfo> preliminaryDisasters = finalState.value("preliminaryDisasters", List.of());
            result.append("📊 【Step 2】灾害识别结果\n");
            result.append("   识别到 ").append(preliminaryDisasters.size()).append(" 个初步灾害\n");
            if (!preliminaryDisasters.isEmpty()) {
                for (DisasterInfo d : preliminaryDisasters) {
                    result.append("   - ").append(d.getType())
                          .append(" (第").append(d.getStartHour()).append("-").append(d.getEndHour()).append("小时)\n");
                }
            }
            result.append("\n");

            // 获取确认灾害（带等级）
            @SuppressWarnings("unchecked")
            List<DisasterInfo> confirmedDisasters = finalState.value("confirmedDisasters", List.of());
            result.append("📊 【Step 3】等级评估结果\n");
            result.append("   评估后 ").append(confirmedDisasters.size()).append(" 个灾害\n");
            if (!confirmedDisasters.isEmpty()) {
                for (DisasterInfo d : confirmedDisasters) {
                    String levelStr = switch (d.getLevel()) {
                        case 1 -> "🔴 一级（严重）";
                        case 2 -> "🟡 二级（中等）";
                        case 3 -> "🟢 三级（轻微）";
                        default -> "⚪ 未知";
                    };
                    result.append("   - ").append(d.getType()).append(" ").append(levelStr).append("\n");
                    result.append("     ").append(d.getDescription()).append("\n");
                }
            }
            result.append("\n");

            // 获取预警文本
            String alertText = finalState.value("alertText", "");
            result.append("📊 【Step 4】预警文本生成\n");
            if (!alertText.isEmpty()) {
                result.append("   预警文本长度: ").append(alertText.length()).append(" 字符\n");
                // 只显示前200字符
                String preview = alertText.length() > 200 ? alertText.substring(0, 200) + "..." : alertText;
                result.append("   预览:\n").append(preview).append("\n");
            } else {
                result.append("   ⚠️ 未生成预警文本\n");
            }

            // 统计
            long level1Count = confirmedDisasters.stream().filter(d -> d.getLevel() == 1).count();
            long level2Count = confirmedDisasters.stream().filter(d -> d.getLevel() == 2).count();
            long level3Count = confirmedDisasters.stream().filter(d -> d.getLevel() == 3).count();

            result.append("\n");
            result.append("╔══════════════════════════════════════════════════╗\n");
            result.append("║                 统计汇总                         ║\n");
            result.append("╠══════════════════════════════════════════════════╣\n");
            result.append("║  一级灾害: ").append(level1Count).append(" 个").append("\n");
            result.append("║  二级灾害: ").append(level2Count).append(" 个").append("\n");
            result.append("║  三级灾害: ").append(level3Count).append(" 个").append("\n");
            result.append("╚══════════════════════════════════════════════════╝\n");

            result.append("\n✅ Graph 执行成功！\n");

        } catch (Exception e) {
            log.error("[测试] Graph 执行失败", e);
            result.append("\n❌ 执行异常: ").append(e.getMessage()).append("\n");
            result.append("异常类型: ").append(e.getClass().getName()).append("\n");
        }

        return result.toString();
    }

    /**
     * 测试完整的 WeatherEmergencyJob（包含通知逻辑）
     */
    @GetMapping("/job")
    public String testJob() {
        log.info("========== [测试] WeatherEmergencyJob ==========");

        StringBuilder result = new StringBuilder();
        result.append("╔══════════════════════════════════════════════════╗\n");
        result.append("║     WeatherEmergencyJob 测试                     ║\n");
        result.append("╚══════════════════════════════════════════════════╝\n\n");

        try {
            // 异步执行，避免阻塞接口
            new Thread(() -> weatherEmergencyJob.dailyCheck()).start();

            result.append("✅ Job 已启动，请查看日志输出\n");
            result.append("   日志关键字: [紧急响应]\n\n");
            result.append("注意：Job 会遍历所有监控地区，并可能发送邮件通知\n");

        } catch (Exception e) {
            log.error("[测试] Job 启动失败", e);
            result.append("❌ Job 启动失败: ").append(e.getMessage()).append("\n");
        }

        return result.toString();
    }

    /**
     * 测试指定地区的完整流程（Graph + 通知）
     */
    @GetMapping("/full")
    public String testFull(
            @RequestParam(defaultValue = "南通") String location,
            @RequestParam(defaultValue = "32.0") double latitude,
            @RequestParam(defaultValue = "120.0") double longitude) {

        log.info("========== [测试] 完整流程 ==========");

        StringBuilder result = new StringBuilder();
        result.append("╔══════════════════════════════════════════════════╗\n");
        result.append("║     完整流程测试（Graph + 模拟通知）             ║\n");
        result.append("╚══════════════════════════════════════════════════╝\n\n");

        try {
            Map<String, Object> initialState = new HashMap<>();
            initialState.put("location", location);
            initialState.put("latitude", latitude);
            initialState.put("longitude", longitude);

            Optional<OverAllState> graphResult = emergencyResponseGraph.invoke(initialState);

            if (graphResult.isEmpty()) {
                result.append("❌ Graph 执行失败\n");
                return result.toString();
            }

            OverAllState finalState = graphResult.get();

            @SuppressWarnings("unchecked")
            List<DisasterInfo> confirmedDisasters = finalState.value("confirmedDisasters", List.of());

            if (confirmedDisasters.isEmpty()) {
                result.append("ℹ️ 该地区无灾害，无需通知\n");
                return result.toString();
            }

            // 模拟通知逻辑（不实际发送邮件）
            List<DisasterInfo> level1Disasters = confirmedDisasters.stream()
                    .filter(d -> d.getLevel() == 1)
                    .collect(Collectors.toList());
            List<DisasterInfo> otherDisasters = confirmedDisasters.stream()
                    .filter(d -> d.getLevel() > 1)
                    .collect(Collectors.toList());

            result.append("📧 【模拟通知】\n\n");

            if (!level1Disasters.isEmpty()) {
                result.append("🔴 一级灾害 - 全员通知:\n");
                for (DisasterInfo d : level1Disasters) {
                    result.append("   - ").append(d.getType()).append("\n");
                }
                result.append("   → 将向该地区所有用户发送邮件\n\n");
            }

            if (!otherDisasters.isEmpty()) {
                result.append("🟡/🟢 二/三级灾害 - 精准通知:\n");
                for (DisasterInfo d : otherDisasters) {
                    result.append("   - ").append(d.getType())
                          .append(" (关心天气码: ").append(d.getWeatherCode()).append(")\n");
                }
                result.append("   → 将向关心相关天气的用户发送邮件\n");
            }

            result.append("\n✅ 测试完成！\n");

        } catch (Exception e) {
            log.error("[测试] 完整流程执行失败", e);
            result.append("\n❌ 执行异常: ").append(e.getMessage()).append("\n");
        }

        return result.toString();
    }
}
