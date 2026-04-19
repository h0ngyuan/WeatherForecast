package com.wf.agent.map.graph.node;

import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.wf.agent.map.constants.MapGraphConstants;
import com.wf.agent.map.dto.CityWeatherVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 趋势分析子图 - 趋势分析节点
 * 职责：分析灾害传播趋势（方向、严重程度）
 */
@Component
@Slf4j
public class TrendAnalysisNode implements NodeAction {

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> apply(OverAllState state) {
        List<CityWeatherVO> nearbyCities = state.value(MapGraphConstants.KEY_NEARBY_CITIES, new ArrayList<>());
        log.info("[TrendAnalysisNode] 分析 {} 个城市灾害趋势", nearbyCities.size());

        // 1. 过滤有灾害的城市
        List<CityWeatherVO> disasterCities = new ArrayList<>();
        for (CityWeatherVO city : nearbyCities) {
            if (Boolean.TRUE.equals(city.getHasDisaster())) {
                disasterCities.add(city);
            }
        }

        if (disasterCities.isEmpty()) {
            log.info("[TrendAnalysisNode] 未发现灾害城市");
            return Map.of(
                MapGraphConstants.KEY_TREND_DIRECTION, "无灾害",
                MapGraphConstants.KEY_TREND_SEVERITY, 0
            );
        }

        // 2. 计算灾害梯度（按经纬度排序找趋势）
        String direction = calculateTrendDirection(disasterCities);
        int severity = calculateTrendSeverity(disasterCities);

        log.info("[TrendAnalysisNode] 趋势方向：{}，严重程度：{}", direction, severity);
        
        return Map.of(
            MapGraphConstants.KEY_TREND_DIRECTION, direction,
            MapGraphConstants.KEY_TREND_SEVERITY, severity
        );
    }

    /**
     * 计算灾害传播趋势方向
     * 基于灾害城市的经纬度分布判断传播方向
     */
    private String calculateTrendDirection(List<CityWeatherVO> disasterCities) {
        if (disasterCities.size() < 2) {
            return "局部点状";
        }

        // 按灾害等级降序排列
        List<CityWeatherVO> sorted = new ArrayList<>(disasterCities);
        sorted.sort(Comparator.comparingInt(CityWeatherVO::getMaxDisasterLevel).reversed());

        // 最严重城市和最轻城市的位置差判断方向
        CityWeatherVO mostSevere = sorted.get(0);
        CityWeatherVO leastSevere = sorted.get(sorted.size() - 1);

        double latDiff = mostSevere.getLatitude().doubleValue() - leastSevere.getLatitude().doubleValue();
        double lonDiff = mostSevere.getLongitude().doubleValue() - leastSevere.getLongitude().doubleValue();

        StringBuilder direction = new StringBuilder();
        if (latDiff > 0.5) direction.append("北");
        else if (latDiff < -0.5) direction.append("南");
        if (lonDiff > 0.5) direction.append("东");
        else if (lonDiff < -0.5) direction.append("西");
        
        if (direction.length() == 0) {
            return "分散分布";
        }
        return "向" + direction.toString() + "方向聚集";
    }

    /**
     * 计算趋势严重程度
     * 基于灾害城市数量、最高等级、聚集程度
     */
    private int calculateTrendSeverity(List<CityWeatherVO> disasterCities) {
        if (disasterCities.isEmpty()) return 0;

        int maxLevel = 0;
        int affectedCount = disasterCities.size();
        
        for (CityWeatherVO city : disasterCities) {
            if (city.getMaxDisasterLevel() != null) {
                maxLevel = Math.max(maxLevel, city.getMaxDisasterLevel());
            }
        }

        // 严重度计算：考虑数量和等级
        if (maxLevel >= 3 && affectedCount >= 3) return 3;
        if (maxLevel >= 2 && affectedCount >= 2) return 2;
        if (maxLevel >= 1 || affectedCount >= 1) return 1;
        return 0;
    }
}
