package com.wf.agent.map.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.wf.agent.map.dto.CityWeatherVO;
import com.wf.agent.map.dto.MapInsightRequest;
import com.wf.agent.map.dto.MapInsightResponse;
import com.wf.agent.map.entity.CityWeatherDaily;
import com.wf.mapper.CityWeatherDailyMapper;
import com.wf.agent.map.tool.SpatialQueryTools;
import com.wf.mapper.CityInfoMapper;
import com.wf.object.entity.CityInfoEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 风险预测服务
 * 由定时任务调用，为每个城市预测风险
 */
@Service
@Slf4j
public class RiskPredictionService {

    @Autowired
    private CityInfoMapper cityInfoMapper;

    @Autowired
    private CityWeatherDailyMapper cityWeatherDailyMapper;

    @Autowired
    private SpatialQueryTools spatialQueryTools;

    /**
     * 预测指定日期的城市风险
     */
    public void predictRisksForDate(LocalDate date) {
        log.info("[RiskPrediction] 开始预测 {} 的城市风险", date);

        // 查询所有有效城市
        List<CityInfoEntity> cities = cityInfoMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CityInfoEntity>()
                        .eq(CityInfoEntity::getAvailable, 1)
                        .isNotNull(CityInfoEntity::getLatitude)
                        .isNotNull(CityInfoEntity::getLongitude)
        );

        for (CityInfoEntity city : cities) {
            try {
                // 查询该城市当天的天气记录
                CityWeatherDaily weatherDaily = cityWeatherDailyMapper.selectByCityAndDate(city.getCityCode(), date);

                if (weatherDaily == null) {
                    log.warn("[RiskPrediction] 城市 {} 在 {} 无天气记录，跳过", city.getCityName(), date);
                    continue;
                }

                // 分析周边城市情况
                List<PredictedRisk> risks = analyzeSurroundingRisks(city, weatherDaily, date);

                // 保存预测结果
                if (!risks.isEmpty()) {
                    weatherDaily.setPredictedRisks(JSON.toJSONString(risks));
                    cityWeatherDailyMapper.updateById(weatherDaily);
                    log.debug("[RiskPrediction] 城市 {} 预测到 {} 个风险", city.getCityName(), risks.size());
                }

            } catch (Exception e) {
                log.error("[RiskPrediction] 预测城市 {} 风险失败", city.getCityName(), e);
            }
        }

        log.info("[RiskPrediction] 完成 {} 个城市风险预测", cities.size());
    }

    /**
     * 分析周边城市风险情况
     */
    private List<PredictedRisk> analyzeSurroundingRisks(CityInfoEntity city, CityWeatherDaily weatherDaily, LocalDate date) {
        List<PredictedRisk> risks = new ArrayList<>();

        // 1. 查询周边200km的城市
        List<CityWeatherVO> nearbyCities = spatialQueryTools.queryCitiesInRadius(
                city.getLatitude().doubleValue(),
                city.getLongitude().doubleValue(),
                200,
                date
        );

        // 2. 统计周边灾害情况
        long disasterCount = nearbyCities.stream()
                .filter(c -> !c.getCityCode().equals(city.getCityCode()))
                .filter(c -> Boolean.TRUE.equals(c.getHasDisaster()))
                .count();

        // 3. 如果有周边灾害，预测扩散风险
        if (disasterCount > 0) {
            // 查询周边城市的天气记录获取详细信息
            List<String> nearbyCityCodes = nearbyCities.stream()
                    .map(CityWeatherVO::getCityCode)
                    .collect(Collectors.toList());

            List<CityWeatherDaily> nearbyWeathers = cityWeatherDailyMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CityWeatherDaily>()
                            .in(CityWeatherDaily::getCityCode, nearbyCityCodes)
                            .eq(CityWeatherDaily::getRecordDate, date)
                            .eq(CityWeatherDaily::getHasDisaster, 1)
            );

            // 按灾害类型统计
            for (CityWeatherDaily nearby : nearbyWeathers) {
                if (nearby.getDisasterTypes() != null) {
                    String[] types = nearby.getDisasterTypes().split(",");
                    for (String type : types) {
                        // 计算距离
                        CityInfoEntity nearbyCity = cityInfoMapper.selectOne(
                                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CityInfoEntity>()
                                        .eq(CityInfoEntity::getCityCode, nearby.getCityCode())
                        );

                        if (nearbyCity != null && nearbyCity.getLatitude() != null) {
                            double distance = calculateDistance(
                                    city.getLatitude().doubleValue(), city.getLongitude().doubleValue(),
                                    nearbyCity.getLatitude().doubleValue(), nearbyCity.getLongitude().doubleValue()
                            );

                            // 距离越近，风险越高
                            int riskLevel = calculateRiskLevel(distance, nearby.getMaxDisasterLevel());

                            PredictedRisk risk = new PredictedRisk();
                            risk.setType(type.trim());
                            risk.setLevel(riskLevel);
                            risk.setReason(String.format("周边城市%s已发生%s，距离%.0f公里，可能扩散",
                                    nearbyCity.getCityName(), type.trim(), distance));
                            risk.setPredictedBy("MapInsightAgent");
                            risk.setConfidence(calculateConfidence(distance, riskLevel));

                            risks.add(risk);
                        }
                    }
                }
            }
        }

        // 4. 基于天气码预测风险
        if (weatherDaily.getDayWeatherCode() != null) {
            int weatherCode = weatherDaily.getDayWeatherCode();
            // 暴雨天气码：46-49
            if (weatherCode >= 46 && weatherCode <= 49) {
                PredictedRisk risk = new PredictedRisk();
                risk.setType("暴雨");
                risk.setLevel(weatherCode - 45); // 46->1, 47->2, 48->3
                risk.setReason("当前天气为暴雨，存在内涝风险");
                risk.setPredictedBy("WeatherAnalysis");
                risk.setConfidence(0.7 + (weatherCode - 46) * 0.1);
                risks.add(risk);
            }
            // 雷电天气码：15-17
            if (weatherCode >= 15 && weatherCode <= 17) {
                PredictedRisk risk = new PredictedRisk();
                risk.setType("雷电");
                risk.setLevel(weatherCode - 14);
                risk.setReason("当前有雷电活动，注意安全");
                risk.setPredictedBy("WeatherAnalysis");
                risk.setConfidence(0.8);
                risks.add(risk);
            }
        }

        return risks;
    }

    private int calculateRiskLevel(double distanceKm, Integer sourceLevel) {
        if (sourceLevel == null) sourceLevel = 1;

        // 距离越近风险越高
        if (distanceKm < 50) return Math.min(sourceLevel + 1, 3);
        if (distanceKm < 100) return sourceLevel;
        if (distanceKm < 200) return Math.max(sourceLevel - 1, 1);
        return 1;
    }

    private double calculateConfidence(double distanceKm, int riskLevel) {
        // 距离越近置信度越高
        double baseConfidence = 1.0 - (distanceKm / 400);
        // 风险等级越高置信度越高
        double levelBoost = (riskLevel - 1) * 0.1;
        return Math.min(baseConfidence + levelBoost, 0.95);
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
     * 预测风险内部类
     */
    public static class PredictedRisk {
        private String type;
        private int level;
        private String reason;
        private String predictedBy;
        private double confidence;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public int getLevel() { return level; }
        public void setLevel(int level) { this.level = level; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public String getPredictedBy() { return predictedBy; }
        public void setPredictedBy(String predictedBy) { this.predictedBy = predictedBy; }
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
    }
}
