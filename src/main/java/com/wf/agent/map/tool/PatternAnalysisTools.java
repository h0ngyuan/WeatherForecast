package com.wf.agent.map.tool;

import com.wf.agent.map.dto.CityWeatherVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 模式分析工具集
 */
@Component
@Slf4j
public class PatternAnalysisTools {

    /**
     * 分析灾害的空间分布模式
     */
    public SpatialPattern analyzeDisasterPattern(List<CityWeatherVO> cities) {
        log.debug("[PatternAnalysis] 分析{}个城市灾害分布模式", cities.size());

        SpatialPattern pattern = new SpatialPattern();

        // 过滤有灾害的城市
        List<CityWeatherVO> disasterCities = cities.stream()
                .filter(c -> Boolean.TRUE.equals(c.getHasDisaster()))
                .collect(Collectors.toList());

        if (disasterCities.isEmpty()) {
            pattern.setHasPattern(false);
            return pattern;
        }

        pattern.setHasPattern(true);
        pattern.setAffectedCityCount(disasterCities.size());

        // 计算聚集中心（平均位置）
        double avgLat = disasterCities.stream()
                .mapToDouble(c -> c.getLatitude().doubleValue())
                .average()
                .orElse(0);
        double avgLon = disasterCities.stream()
                .mapToDouble(c -> c.getLongitude().doubleValue())
                .average()
                .orElse(0);
        pattern.setClusterCenter(new double[]{avgLat, avgLon});

        // 计算分布范围（最大距离）
        double maxDistance = 0;
        for (int i = 0; i < disasterCities.size(); i++) {
            for (int j = i + 1; j < disasterCities.size(); j++) {
                double dist = calculateDistance(
                        disasterCities.get(i).getLatitude().doubleValue(), disasterCities.get(i).getLongitude().doubleValue(),
                        disasterCities.get(j).getLatitude().doubleValue(), disasterCities.get(j).getLongitude().doubleValue()
                );
                maxDistance = Math.max(maxDistance, dist);
            }
        }
        pattern.setSpreadRangeKm(maxDistance);

        // 判断分布方向
        pattern.setDistributionDirection(calculateDirection(disasterCities));

        // 统计灾害等级
        Map<Integer, Long> levelCount = disasterCities.stream()
                .filter(c -> c.getMaxDisasterLevel() != null)
                .collect(Collectors.groupingBy(CityWeatherVO::getMaxDisasterLevel, Collectors.counting()));
        pattern.setLevelDistribution(levelCount);

        return pattern;
    }

    /**
     * 识别高风险区域
     */
    public List<RiskArea> identifyRiskAreas(List<CityWeatherVO> cities, String centerCityCode) {
        log.debug("[PatternAnalysis] 识别高风险区域");

        List<RiskArea> riskAreas = new ArrayList<>();

        // 找到中心城市
        CityWeatherVO centerCity = cities.stream()
                .filter(c -> c.getCityCode().equals(centerCityCode))
                .findFirst()
                .orElse(null);

        if (centerCity == null) {
            return riskAreas;
        }

        // 找出周边有灾害的城市
        List<CityWeatherVO> nearbyDisasterCities = cities.stream()
                .filter(c -> !c.getCityCode().equals(centerCityCode))
                .filter(c -> Boolean.TRUE.equals(c.getHasDisaster()))
                .filter(c -> {
                    double dist = calculateDistance(
                            centerCity.getLatitude().doubleValue(), centerCity.getLongitude().doubleValue(),
                            c.getLatitude().doubleValue(), c.getLongitude().doubleValue()
                    );
                    return dist <= 200; // 200km内
                })
                .collect(Collectors.toList());

        if (!nearbyDisasterCities.isEmpty()) {
            RiskArea area = new RiskArea();
            area.setCenterCity(centerCity.getCityName());
            area.setRiskLevel(nearbyDisasterCities.stream()
                    .mapToInt(CityWeatherVO::getMaxDisasterLevel)
                    .max()
                    .orElse(1));
            area.setAffectedCities(nearbyDisasterCities.stream()
                    .map(CityWeatherVO::getCityName)
                    .collect(Collectors.toList()));
            area.setDescription(String.format("%s周边%d个城市出现灾害，形成风险聚集区",
                    centerCity.getCityName(), nearbyDisasterCities.size()));
            riskAreas.add(area);
        }

        return riskAreas;
    }

    private String calculateDirection(List<CityWeatherVO> cities) {
        if (cities.size() < 2) {
            return "点状分布";
        }

        double minLat = cities.stream().mapToDouble(c -> c.getLatitude().doubleValue()).min().orElse(0);
        double maxLat = cities.stream().mapToDouble(c -> c.getLatitude().doubleValue()).max().orElse(0);
        double minLon = cities.stream().mapToDouble(c -> c.getLongitude().doubleValue()).min().orElse(0);
        double maxLon = cities.stream().mapToDouble(c -> c.getLongitude().doubleValue()).max().orElse(0);

        double latRange = maxLat - minLat;
        double lonRange = maxLon - minLon;

        if (latRange > lonRange * 2) {
            return "南北走向";
        } else if (lonRange > latRange * 2) {
            return "东西走向";
        } else {
            return "面状分布";
        }
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * 空间分布模式
     */
    public static class SpatialPattern {
        private boolean hasPattern;
        private int affectedCityCount;
        private double[] clusterCenter;
        private double spreadRangeKm;
        private String distributionDirection;
        private Map<Integer, Long> levelDistribution;

        // Getters and Setters
        public boolean isHasPattern() { return hasPattern; }
        public void setHasPattern(boolean hasPattern) { this.hasPattern = hasPattern; }
        public int getAffectedCityCount() { return affectedCityCount; }
        public void setAffectedCityCount(int affectedCityCount) { this.affectedCityCount = affectedCityCount; }
        public double[] getClusterCenter() { return clusterCenter; }
        public void setClusterCenter(double[] clusterCenter) { this.clusterCenter = clusterCenter; }
        public double getSpreadRangeKm() { return spreadRangeKm; }
        public void setSpreadRangeKm(double spreadRangeKm) { this.spreadRangeKm = spreadRangeKm; }
        public String getDistributionDirection() { return distributionDirection; }
        public void setDistributionDirection(String distributionDirection) { this.distributionDirection = distributionDirection; }
        public Map<Integer, Long> getLevelDistribution() { return levelDistribution; }
        public void setLevelDistribution(Map<Integer, Long> levelDistribution) { this.levelDistribution = levelDistribution; }
    }

    /**
     * 风险区域
     */
    public static class RiskArea {
        private String centerCity;
        private int riskLevel;
        private List<String> affectedCities;
        private String description;

        // Getters and Setters
        public String getCenterCity() { return centerCity; }
        public void setCenterCity(String centerCity) { this.centerCity = centerCity; }
        public int getRiskLevel() { return riskLevel; }
        public void setRiskLevel(int riskLevel) { this.riskLevel = riskLevel; }
        public List<String> getAffectedCities() { return affectedCities; }
        public void setAffectedCities(List<String> affectedCities) { this.affectedCities = affectedCities; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}
