package com.wf.agent.map.graph.node;

import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.wf.agent.map.constants.MapGraphConstants;
import com.wf.agent.map.dto.CityWeatherVO;
import com.wf.mapper.CityInfoMapper;
import com.wf.object.entity.CityInfoEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 影响分析子图 - 影响计算节点
 * 职责：基于距离和灾害等级计算影响程度
 */
@Component
@Slf4j
public class ImpactCalcNode implements NodeAction {

    @Autowired
    private CityInfoMapper cityInfoMapper;

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> apply(OverAllState state) {
        List<CityWeatherVO> disasterCitiesRaw = state.value(MapGraphConstants.KEY_DISASTER_CITIES, new ArrayList<>());
        String location = state.value(MapGraphConstants.KEY_LOCATION, "");
        
        // 找到目标城市坐标
        CityInfoEntity centerCity = findCityByName(location);
        if (centerCity == null || centerCity.getLatitude() == null) {
            return Map.of(
                MapGraphConstants.KEY_IMPACT_SCORE, 0.0,
                MapGraphConstants.KEY_IMPACT_DIRECTION, "无影响"
            );
        }

        double centerLat = centerCity.getLatitude().doubleValue();
        double centerLon = centerCity.getLongitude().doubleValue();

        if (disasterCitiesRaw.isEmpty()) {
            return Map.of(
                MapGraphConstants.KEY_IMPACT_SCORE, 0.0,
                MapGraphConstants.KEY_IMPACT_DIRECTION, "无影响"
            );
        }

        // 计算加权影响分数
        double totalImpactScore = 0;
        double minDistance = Double.MAX_VALUE;
        String impactDirection = "未知";
        
        for (Object raw : disasterCitiesRaw) {
            try {
                // raw 可能是 Map (from state serialization)
                Map<String, Object> city = (Map<String, Object>) raw;
                double lat = ((Number) city.get("latitude")).doubleValue();
                double lon = ((Number) city.get("longitude")).doubleValue();
                int level = city.get("maxDisasterLevel") != null ? ((Number) city.get("maxDisasterLevel")).intValue() : 1;
                double distance = city.get("displayIcon") != null ? Double.parseDouble((String) city.get("displayIcon")) : 100;
                
                // 影响分数 = 灾害等级 * 距离衰减因子
                double distanceFactor = Math.max(0.1, 1.0 - (distance / 200.0));
                double cityImpact = level * distanceFactor;
                totalImpactScore += cityImpact;
                
                if (distance < minDistance) {
                    minDistance = distance;
                    impactDirection = getDirection(centerLat, centerLon, lat, lon);
                }
            } catch (Exception e) {
                log.warn("[ImpactCalcNode] 解析城市数据失败", e);
            }
        }

        // 限制影响分数范围
        totalImpactScore = Math.min(10.0, totalImpactScore);
        
        log.info("[ImpactCalcNode] 影响分数：{}，影响方向：{}", totalImpactScore, impactDirection);
        
        return Map.of(
            MapGraphConstants.KEY_IMPACT_SCORE, totalImpactScore,
            MapGraphConstants.KEY_IMPACT_DIRECTION, impactDirection
        );
    }

    private CityInfoEntity findCityByName(String cityName) {
        List<CityInfoEntity> allCities = cityInfoMapper.selectList(null);
        for (CityInfoEntity city : allCities) {
            if (city.getCityName() != null && (city.getCityName().contains(cityName) || cityName.contains(city.getCityName()))) {
                return city;
            }
        }
        return null;
    }

    private String getDirection(double centerLat, double centerLon, double cityLat, double cityLon) {
        double latDiff = cityLat - centerLat;
        double lonDiff = cityLon - centerLon;
        
        StringBuilder dir = new StringBuilder();
        if (latDiff > 0.5) dir.append("北");
        else if (latDiff < -0.5) dir.append("南");
        if (lonDiff > 0.5) dir.append("东");
        else if (lonDiff < -0.5) dir.append("西");
        
        return dir.length() > 0 ? dir.toString() : "周边";
    }
}
