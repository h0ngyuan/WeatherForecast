package com.wf.agent.map.graph.node;

import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.fastjson.JSON;
import com.wf.agent.map.constants.MapGraphConstants;
import com.wf.agent.map.entity.CityWeatherDaily;
import com.wf.agent.map.entity.HistoricalCase;
import com.wf.mapper.CityWeatherDailyMapper;
import com.wf.mapper.HistoricalCaseMapper;
import com.wf.object.entity.CityInfoEntity;
import com.wf.mapper.CityInfoMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 季节评估子图 - 历史统计节点
 * 职责：查询该城市历史季节灾害统计
 */
@Component
@Slf4j
public class SeasonHistoryNode implements NodeAction {

    @Autowired
    private HistoricalCaseMapper historicalCaseMapper;

    @Autowired
    private CityWeatherDailyMapper cityWeatherDailyMapper;

    @Autowired
    private CityInfoMapper cityInfoMapper;

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> apply(OverAllState state) {
        String location = state.value(MapGraphConstants.KEY_LOCATION, "");
        String season = state.value(MapGraphConstants.KEY_SEASON, "");
        String dateStr = state.value(MapGraphConstants.KEY_DATE, LocalDate.now().toString());
        LocalDate date = LocalDate.parse(dateStr);
        
        log.info("[SeasonHistoryNode] 查询 {} {} 历史灾害统计", location, season);

        List<HistoricalCase> seasonCases = new ArrayList<>();
        try {
            List<HistoricalCase> allCases = historicalCaseMapper.selectRecentCases(100);
            for (HistoricalCase c : allCases) {
                if (c.getStartTime() != null) {
                    String caseSeason = getSeason(c.getStartTime().getMonthValue());
                    if (caseSeason.equals(season)) {
                        seasonCases.add(c);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[SeasonHistoryNode] 查询历史案例失败（表可能不存在），使用空数据: {}", e.getMessage());
        }

        // 2. 查询该城市近期的天气数据
        CityInfoEntity city = findCityByName(location);
        Map<String, Object> stats = new HashMap<>();
        stats.put("season", season);
        stats.put("location", location);
        stats.put("totalCases", seasonCases.size());
        stats.put("eventTypeCounts", countEventTypes(seasonCases));
        
        if (city != null) {
            stats.put("cityCode", city.getCityCode());
            stats.put("hasCurrentWeather", cityWeatherDailyMapper.selectByCityAndDate(city.getCityCode(), date) != null);
        }

        log.info("[SeasonHistoryNode] 历史统计：{} 共 {} 个案例", season, seasonCases.size());
        
        return Map.of(MapGraphConstants.KEY_HISTORY_STATS, JSON.toJSONString(stats));
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

    private Map<String, Long> countEventTypes(List<HistoricalCase> cases) {
        return cases.stream()
                .collect(Collectors.groupingBy(HistoricalCase::getEventType, Collectors.counting()));
    }

    private String getSeason(int month) {
        if (month >= 3 && month <= 5) return "春季";
        if (month >= 6 && month <= 8) return "夏季";
        if (month >= 9 && month <= 11) return "秋季";
        return "冬季";
    }
}
