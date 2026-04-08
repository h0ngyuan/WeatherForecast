package com.wf.agent.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.wf.agent.base.AIClient;
import com.wf.agent.entity.DisasterInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 预警文本生成 Node
 *
 * 职责：
 * 根据灾害信息生成自然语言预警文本，用于邮件/消息推送
 *
 * 作为 Graph Node 使用，接收 State 输入，输出预警文本
 *
 * @author author
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertTextGenerationNode implements NodeAction {

    private final AIClient aiClient;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("MM月dd日 HH:mm");

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String location = state.value("location", "");
        @SuppressWarnings("unchecked")
        List<DisasterInfo> disasters = state.value("confirmedDisasters", List.of());
        String alertType = state.value("alertType", "level1"); // level1 或 level2
        String concernWord = state.value("concernWord", "");

        log.info("[预警生成Node] 生成 {} 的预警文本，类型: {}", location, alertType);

        String alertText;
        if ("level1".equals(alertType)) {
            alertText = generateLevel1Alert(location, disasters);
        } else {
            // level2: 取第一个灾害生成个性化提醒
            DisasterInfo disaster = disasters.isEmpty() ? null : disasters.get(0);
            alertText = generateLevel2Alert(location, disaster, concernWord);
        }

        return Map.of("alertText", alertText);
    }

    /**
     * 生成一级灾害预警文本（全员通知）- 供外部直接调用
     */
    public String generateLevel1Alert(String location, List<DisasterInfo> disasters) {
        StringBuilder alert = new StringBuilder();

        // 标题
        alert.append("╔═══════════════════════════════════════╗\n");
        alert.append("║     ⚠️ 气象灾害预警通知 ⚠️              ║\n");
        alert.append("╚═══════════════════════════════════════╝\n\n");

        // 基本信息
        alert.append("📍 预警地区：").append(location).append("\n");
        alert.append("🕐 发布时间：").append(LocalDateTime.now().format(FORMATTER)).append("\n");
        alert.append("⚡ 预警级别：🔴 一级（严重）\n\n");

        // 灾害详情
        alert.append("【灾害概况】\n");
        for (int i = 0; i < disasters.size(); i++) {
            DisasterInfo d = disasters.get(i);
            alert.append(i + 1).append(". ").append(d.getType()).append("\n");
            alert.append("   时间：第").append(d.getStartHour()).append("-").append(d.getEndHour()).append("小时\n");
            alert.append("   描述：").append(d.getDescription()).append("\n\n");
        }

        // 综合建议
        alert.append("【防范建议】\n");
        String allAdvice = disasters.stream()
                .map(d -> generateAdviceByDisaster(d.getType()))
                .distinct()
                .collect(Collectors.joining("\n"));
        alert.append(allAdvice).append("\n\n");

        // 结尾
        alert.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        alert.append("请密切关注天气变化，做好防范措施\n");
        alert.append("智能天气预警系统");

        return alert.toString();
    }

    /**
     * 生成二级灾害提醒文本（个性化通知）- 供外部直接调用
     */
    public String generateLevel2Alert(String location, DisasterInfo disaster, String concernWord) {
        if (disaster == null) {
            return "暂无灾害预警信息";
        }

        StringBuilder alert = new StringBuilder();

        // 标题
        alert.append("╔═══════════════════════════════════════╗\n");
        alert.append("║     📢 天气提醒通知                   ║\n");
        alert.append("╚═══════════════════════════════════════╝\n\n");

        // 基本信息
        alert.append("📍 地区：").append(location).append("\n");
        alert.append("🕐 时间：").append(LocalDateTime.now().format(FORMATTER)).append("\n");
        alert.append("⚠️ 天气：").append(disaster.getType()).append("\n\n");

        // 个性化提醒
        alert.append("【提醒内容】\n");
        String personalizedTip = generatePersonalizedTip(disaster.getType(), concernWord);
        alert.append(personalizedTip).append("\n\n");

        // 建议
        alert.append("【建议措施】\n");
        alert.append(generateAdviceByDisaster(disaster.getType())).append("\n\n");

        // 结尾
        alert.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        alert.append("智能天气提醒系统");

        return alert.toString();
    }

    /**
     * 生成个性化提醒
     */
    private String generatePersonalizedTip(String disasterType, String taskInfo) {
        if (taskInfo == null || taskInfo.isEmpty()) {
            taskInfo = "相关活动";
        }

        return switch (taskInfo) {
            case "晒被子", "晾晒", "晒衣服" ->
                    disasterType.contains("雨") || disasterType.contains("雪") ?
                            "「" + taskInfo + "」不适合，雨水会淋湿物品" :
                            "「" + taskInfo + "」可能受影响，建议关注天气变化";
            case "洗车" ->
                    disasterType.contains("雨") || disasterType.contains("雪") ?
                            "「" + taskInfo + "」后可能很快被雨水弄脏，建议推迟" :
                            "「" + taskInfo + "」可以进行，但建议关注后续天气";
            case "户外活动", "爬山", "郊游", "运动" ->
                    "「" + taskInfo + "」不建议进行，" + disasterType + "会影响安全和体验";
            case "带伞", "雨伞" ->
                    "记得「" + taskInfo + "」，" + disasterType + "会影响出行";
            default ->
                    "您的「" + taskInfo + "」计划可能受" + disasterType + "影响，请合理安排";
        };
    }

    /**
     * 根据灾害类型生成建议
     */
    private String generateAdviceByDisaster(String disasterType) {
        return switch (disasterType) {
            case "暴雨", "大暴雨", "特大暴雨" ->
                    "• 外出请携带雨具\n• 驾车注意积水路段\n• 避免在树下、广告牌下停留";
            case "小雨", "中雨", "雨" ->
                    "• 外出携带雨伞\n• 驾车减速慢行\n• 注意路面湿滑";
            case "大风", "强风" ->
                    "• 注意防风，妥善安置室外物品\n• 避免在高空坠物风险区域停留\n• 关好门窗";
            case "雷电", "雷暴" ->
                    "• 避免在空旷地带活动\n• 不要使用金属柄雨伞\n• 关闭家用电器电源";
            case "冰雹" ->
                    "• 立即停止户外活动\n• 车辆寻找遮蔽处\n• 远离窗户";
            case "暴雪", "大雪", "雪" ->
                    "• 注意防寒保暖\n• 驾车安装防滑链\n• 步行注意防滑";
            case "大雾" ->
                    "• 驾车开启雾灯\n• 减速慢行\n• 保持车距";
            case "高温" ->
                    "• 避免长时间户外活动\n• 多补充水分\n• 做好防暑降温";
            case "低温", "寒潮" ->
                    "• 增添衣物保暖\n• 注意取暖安全\n• 防范管道冻裂";
            default ->
                    "• 关注天气变化\n• 做好相应防范\n• 合理安排出行";
        };
    }
}
