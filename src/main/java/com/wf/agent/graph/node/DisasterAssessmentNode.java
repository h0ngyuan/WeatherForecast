package com.wf.agent.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.wf.agent.base.AIClient;
import com.wf.agent.entity.DisasterInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 灾害评估 Node
 *
 * 职责：
 * 分析24小时天气码，识别出有哪些灾害
 *
 * 输入 State:
 *   - location: String (地区名称)
 *   - weatherCodes: List<Integer> (24小时天气码)
 *
 * 输出 State:
 *   - preliminaryDisasters: List<DisasterInfo> (初步识别的灾害列表，无等级)
 *
 * @author author
 * @since 1.0.0
 */
@Slf4j
@Component
public class DisasterAssessmentNode implements NodeAction {

    private final AIClient aiClient;

    public DisasterAssessmentNode(AIClient aiClient) {
        this.aiClient = aiClient;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String location = state.value("location", "unknown");
        @SuppressWarnings("unchecked")
        List<Integer> weatherCodes = state.value("weatherCodes", List.of());

        log.info("[DisasterAssessmentNode] 分析 {} 的天气码序列", location);

        String response = aiClient.analyzeDisasters(location, weatherCodes);

        List<DisasterInfo> disasters = parseDisasterResponse(response);

        log.info("[DisasterAssessmentNode] 识别到 {} 个灾害", disasters.size());
        return Map.of("preliminaryDisasters", disasters);
    }

    /**
     * 解析 AI 响应
     */
    private List<DisasterInfo> parseDisasterResponse(String response) {
        List<DisasterInfo> disasters = new ArrayList<>();

        try {
            String jsonStr = extractJson(response);
            JSONObject json = JSON.parseObject(jsonStr);
            JSONArray array = json.getJSONArray("disasters");

            if (array == null) {
                return disasters;
            }

            for (int i = 0; i < array.size(); i++) {
                JSONObject item = array.getJSONObject(i);
                DisasterInfo d = new DisasterInfo();
                
                // 解析type字段，如果为空则根据weatherCode推断
                String type = item.getString("type");
                if (type == null || type.isEmpty()) {
                    Integer weatherCode = item.getInteger("weatherCode");
                    type = inferDisasterType(weatherCode);
                    log.warn("[DisasterAssessmentNode] AI返回的type为空，根据weatherCode={}推断为: {}", weatherCode, type);
                }
                d.setType(type);
                
                d.setWeatherCode(item.getInteger("weatherCode"));
                d.setStartHour(item.getInteger("startHour"));
                d.setEndHour(item.getInteger("endHour"));
                d.setDescription(item.getString("description"));
                disasters.add(d);
            }

        } catch (Exception e) {
            log.error("[DisasterAssessmentNode] 解析响应失败: {}", response, e);
        }

        return disasters;
    }

    /**
     * 从响应中提取 JSON
     */
    private String extractJson(String response) {
        if (response.contains("```json")) {
            int start = response.indexOf("```json") + 7;
            int end = response.indexOf("```", start);
            return response.substring(start, end).trim();
        }
        if (response.contains("```")) {
            int start = response.indexOf("```") + 3;
            int end = response.indexOf("```", start);
            return response.substring(start, end).trim();
        }
        return response.trim();
    }

    /**
     * 根据天气码推断灾害类型
     */
    private String inferDisasterType(Integer weatherCode) {
        if (weatherCode == null) {
            return "未知灾害";
        }
        return switch (weatherCode) {
            case 1 -> "晴天";
            case 7 -> "多云";
            case 8 -> "阴天";
            case 15 -> "雷阵雨";
            case 33 -> "雾/轻雾";
            case 46 -> "小雨";
            case 47 -> "中雨";
            case 48 -> "大雨";
            case 49 -> "暴雨";
            case 75 -> "霾/沙尘";
            default -> "天气异常";
        };
    }
}
