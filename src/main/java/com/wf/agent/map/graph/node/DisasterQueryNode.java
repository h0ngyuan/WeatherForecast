package com.wf.agent.map.graph.node;

import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.wf.agent.map.constants.MapGraphConstants;
import com.wf.agent.map.dto.CityWeatherVO;
import com.wf.agent.map.entity.CityWeatherDaily;
import com.wf.mapper.CityInfoMapper;
import com.wf.mapper.CityWeatherDailyMapper;
import com.wf.object.entity.CityInfoEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 影响分析子图 - 灾害城市查询节点
 * 职责：查找目标城市周边有灾害的城市
 */
@Component
@Slf4j
public class DisasterQueryNode implements NodeAction {

    @Autowired
    private CityInfoMapper cityInfoMapper;

    @Autowired
    private CityWeatherDailyMapper cityWeatherDailyMapper;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String location = state.value(MapGraphConstants.KEY_LOCATION, "");
        Integer radiusKm = state.value(MapGraphConstants.KEY_RADIUS_KM, 100);
        String dateStr = state.value(MapGraphConstants.KEY_DATE, LocalDate.now().toString());
        LocalDate date = LocalDate.parse(dateStr);
        
        log.info("[DisasterQueryNode] 查询 {} 周边{}km内有灾害的城市，日期：{}", location, radiusKm, date);

        // 1. 找到目标城市坐标
        CityInfoEntity centerCity = findCityByName(location);
        if (centerCity == null || centerCity.getLatitude() == null) {
            log.warn("[DisasterQueryNode] 未找到城市 {} 的坐标信息", location);
            return Map.of(MapGraphConstants.KEY_DISASTER_CITIES, new ArrayList<CityWeatherVO>());
        }

        double centerLat = centerCity.getLatitude().doubleValue();
        double centerLon = centerCity.getLongitude().doubleValue();

        // 2. 查询半径内有灾害的城市
        List<CityInfoEntity> allCities = cityInfoMapper.selectList(null);
        List<CityWeatherVO> disasterCities = new ArrayList<>();

        for (CityInfoEntity city : allCities) {
            if (city.getLatitude() != null && city.getLongitude() != null) {
                double distance = calculateDistance(centerLat, centerLon,
                        city.getLatitude().doubleValue(), city.getLongitude().doubleValue());
                if (distance <= radiusKm) {
                    CityWeatherDaily weather = cityWeatherDailyMapper.selectByCityAndDate(city.getCityCode(), date);
                    if (weather != null && weather.getHasDisaster() != null && weather.getHasDisaster() == 1) {
                        CityWeatherVO vo = new CityWeatherVO();
                        vo.setCityCode(city.getCityCode());
                        vo.setCityName(city.getCityName());
                        vo.setLatitude(city.getLatitude());
                        vo.setLongitude(city.getLongitude());
                        vo.setProvince(city.getProvince());
                        vo.setHasDisaster(true);
                        vo.setDisasterTypes(weather.getDisasterTypes());
                        vo.setMaxDisasterLevel(weather.getMaxDisasterLevel());
                        vo.setDisplayIcon(String.valueOf(Math.round(distance)));
                        disasterCities.add(vo);
                    }
                }
            }
        }

        // 按距离排序
        disasterCities.sort((a, b) -> Double.compare(
                Double.parseDouble(a.getDisplayIcon()),
                Double.parseDouble(b.getDisplayIcon())
        ));

        log.info("[DisasterQueryNode] 找到 {} 个有灾害的周边城市", disasterCities.size());
        
        return Map.of(MapGraphConstants.KEY_DISASTER_CITIES, disasterCities);
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
}
