package com.wf.agent.map;

import com.wf.agent.map.entity.CityWeatherDaily;
import com.wf.mapper.CityInfoMapper;
import com.wf.mapper.CityWeatherDailyMapper;
import com.wf.object.entity.CityInfoEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * WeatherImpactAgent - 天气影响分析Agent
 * 
 * 职责：
 * 分析周边城市天气对目标城市的影响
 * 基于距离和天气模式判断潜在风险传播
 */
@Component
@Slf4j
public class WeatherImpactAgent {

    @Autowired
    private CityInfoMapper cityInfoMapper;

    @Autowired
    private CityWeatherDailyMapper cityWeatherDailyMapper;

    /**
     * 分析周边天气影响（简化版，不调用AI，基于规则判断）
     * 
     * @param targetCityCode 目标城市编码
     * @param targetCityName 目标城市名称
     * @param date 日期
     * @param radiusKm 分析半径（公里）
     * @return 影响分析结果
     */
    public ImpactAnalysisResult analyzeImpact(String targetCityCode, String targetCityName, 
                                               LocalDate date, int radiusKm) {
        log.debug("[WeatherImpactAgent] 分析 {} 周边{}公里天气影响", targetCityName, radiusKm);

        // 1. 获取目标城市信息
        CityInfoEntity targetCity = cityInfoMapper.selectById(targetCityCode);
        if (targetCity == null || targetCity.getLatitude() == null || targetCity.getLongitude() == null) {
            log.warn("[WeatherImpactAgent] 目标城市 {} 无坐标信息", targetCityName);
            return ImpactAnalysisResult.noImpact();
        }

        // 2. 获取周边城市
        List<CityInfoEntity> nearbyCities = findNearbyCities(targetCity, radiusKm);
        if (nearbyCities.isEmpty()) {
            log.debug("[WeatherImpactAgent] 未找到周边城市");
            return ImpactAnalysisResult.noImpact();
        }

        // 3. 获取周边城市的天气数据，基于规则判断影响
        List<CityWeatherImpact> impacts = new ArrayList<>();
        int maxLevel = 0;
        double minDistance = Double.MAX_VALUE;
        
        for (CityInfoEntity city : nearbyCities) {
            CityWeatherDaily weather = cityWeatherDailyMapper.selectByCityAndDate(city.getCityCode(), date);
            if (weather != null && weather.getHasDisaster() != null && weather.getHasDisaster() == 1) {
                double distance = calculateDistance(
                    targetCity.getLatitude().doubleValue(), targetCity.getLongitude().doubleValue(),
                    city.getLatitude().doubleValue(), city.getLongitude().doubleValue()
                );
                
                int level = weather.getMaxDisasterLevel() != null ? weather.getMaxDisasterLevel() : 1;
                impacts.add(new CityWeatherImpact(city.getCityName(), distance, 
                    weather.getDisasterTypes(), level));
                
                maxLevel = Math.max(maxLevel, level);
                minDistance = Math.min(minDistance, distance);
            }
        }

        if (impacts.isEmpty()) {
            log.debug("[WeatherImpactAgent] 周边城市无灾害");
            return ImpactAnalysisResult.noImpact();
        }

        // 4. 基于规则判断影响（简化结论）
        boolean hasImpact = false;
        int suggestedLevel = 0;
        
        if (minDistance < 50 && maxLevel >= 2) {
            hasImpact = true;
            suggestedLevel = Math.min(maxLevel, 3);
        } else if (minDistance < 100 && maxLevel >= 3) {
            hasImpact = true;
            suggestedLevel = 2;
        } else if (minDistance < 200 && maxLevel >= 2) {
            hasImpact = true;
            suggestedLevel = 1;
        }

        log.debug("[WeatherImpactAgent] 分析完成，是否有影响: {}", hasImpact);
        
        return new ImpactAnalysisResult(hasImpact, suggestedLevel, null, null);
    }

    /**
     * 查找周边城市
     */
    private List<CityInfoEntity> findNearbyCities(CityInfoEntity center, int radiusKm) {
        List<CityInfoEntity> allCities = cityInfoMapper.selectList(null);
        return allCities.stream()
            .filter(c -> !c.getCityCode().equals(center.getCityCode()))
            .filter(c -> c.getLatitude() != null && c.getLongitude() != null)
            .filter(c -> calculateDistance(center.getLatitude().doubleValue(), center.getLongitude().doubleValue(), 
                                          c.getLatitude().doubleValue(), c.getLongitude().doubleValue()) <= radiusKm)
            .collect(Collectors.toList());
    }

    /**
     * 计算两点间距离（公里）
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // 地球半径（公里）
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * 影响分析结果
     */
    public static class ImpactAnalysisResult {
        private final boolean hasImpact;
        private final int suggestedLevel;
        private final String analysis;
        private final List<CityWeatherImpact> sourceCities;

        public ImpactAnalysisResult(boolean hasImpact, int suggestedLevel, 
                                    String analysis, List<CityWeatherImpact> sourceCities) {
            this.hasImpact = hasImpact;
            this.suggestedLevel = suggestedLevel;
            this.analysis = analysis;
            this.sourceCities = sourceCities;
        }

        public static ImpactAnalysisResult noImpact() {
            return new ImpactAnalysisResult(false, 0, null, null);
        }

        public boolean isHasImpact() { return hasImpact; }
        public int getSuggestedLevel() { return suggestedLevel; }
        public String getAnalysis() { return analysis; }
        public List<CityWeatherImpact> getSourceCities() { return sourceCities; }
    }

    /**
     * 周边城市天气影响
     */
    public static class CityWeatherImpact {
        private final String cityName;
        private final double distance;
        private final String disasterTypes;
        private final Integer disasterLevel;

        public CityWeatherImpact(String cityName, double distance, 
                                 String disasterTypes, Integer disasterLevel) {
            this.cityName = cityName;
            this.distance = distance;
            this.disasterTypes = disasterTypes;
            this.disasterLevel = disasterLevel;
        }

        public String getCityName() { return cityName; }
        public double getDistance() { return distance; }
        public String getDisasterTypes() { return disasterTypes; }
        public Integer getDisasterLevel() { return disasterLevel; }
    }
}
