package com.wf.agent.map.service.impl;

import com.wf.agent.map.MapInsightAgent;
import com.wf.agent.map.dto.*;
import com.wf.agent.map.entity.CityWeatherDaily;
import com.wf.agent.map.service.MapService;
import com.wf.mapper.CityInfoMapper;
import com.wf.mapper.CityWeatherDailyMapper;
import com.wf.object.entity.CityInfoEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 地图服务实现
 */
@Service
@Slf4j
public class MapServiceImpl implements MapService {

    @Autowired
    private CityInfoMapper cityInfoMapper;

    @Autowired
    private CityWeatherDailyMapper cityWeatherDailyMapper;

    @Autowired
    private MapInsightAgent mapInsightAgent;

    @Override
    public MapWeatherResponse getMapWeatherData(LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }

        // 查询指定日期的天气数据
        List<CityWeatherDaily> weatherDailyList = cityWeatherDailyMapper.selectByDate(date);
        Map<String, CityWeatherDaily> weatherMap = weatherDailyList.stream()
                .collect(Collectors.toMap(CityWeatherDaily::getCityCode, w -> w, (w1, w2) -> w1));

        // 查询所有有效城市（带经纬度）
        List<CityInfoEntity> cities = cityInfoMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CityInfoEntity>()
                        .isNotNull(CityInfoEntity::getLatitude)
                        .isNotNull(CityInfoEntity::getLongitude)
        );

        List<CityWeatherVO> cityWeathers = cities.stream()
                .map(city -> convertToVO(city, weatherMap.get(city.getCityCode())))
                .peek(this::setDisplayColor)
                .collect(Collectors.toList());

        MapWeatherResponse response = new MapWeatherResponse();
        response.setRecordDate(date);
        response.setCityCount(cityWeathers.size());
        response.setCities(cityWeathers);

        return response;
    }

    @Override
    public MapInsightResponse analyze(MapInsightRequest request) {
        return mapInsightAgent.analyze(request);
    }

    private CityWeatherVO convertToVO(CityInfoEntity city, CityWeatherDaily weather) {
        CityWeatherVO vo = new CityWeatherVO();
        vo.setCityCode(city.getCityCode());
        vo.setCityName(city.getCityName());
        vo.setLatitude(city.getLatitude());
        vo.setLongitude(city.getLongitude());
        vo.setProvince(city.getProvince());
        vo.setCityLevel(city.getCityLevel());

        if (weather != null) {
            // 从 city_weather_daily 读取数据
            vo.setDayWeatherCode(weather.getDayWeatherCode());
            vo.setHasDisaster(weather.getHasDisaster() != null && weather.getHasDisaster() == 1);
            vo.setMaxDisasterLevel(weather.getMaxDisasterLevel());
            vo.setDisasterTypes(weather.getDisasterTypes());
        } else {
            // 无数据时默认正常
            vo.setDayWeatherCode(1);
            vo.setHasDisaster(false);
            vo.setMaxDisasterLevel(0);
        }

        return vo;
    }

    private void setDisplayColor(CityWeatherVO vo) {
        // 只区分是否有灾害：绿色（无灾害）/ 红色（有灾害）
        String color = Boolean.TRUE.equals(vo.getHasDisaster()) ? "#FF4444" : "#52C41A";
        vo.setDisplayColor(color);
    }
}
