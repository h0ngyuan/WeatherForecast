package com.wf.agent.map.tool;

import com.wf.agent.map.dto.CityWeatherVO;
import com.wf.mapper.CityInfoMapper;
import com.wf.object.entity.CityInfoEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 空间查询工具集
 */
@Component
@Slf4j
public class SpatialQueryTools {

    @Autowired
    private CityInfoMapper cityInfoMapper;

    /**
     * 查询指定半径内的所有城市
     */
    public List<CityWeatherVO> queryCitiesInRadius(double lat, double lon, int radiusKm, LocalDate date) {
        log.debug("[SpatialQuery] 查询半径{}km内的城市，中心: ({}, {})", radiusKm, lat, lon);

        List<CityInfoEntity> allCities = cityInfoMapper.selectList(null);
        List<CityWeatherVO> result = new ArrayList<>();

        for (CityInfoEntity city : allCities) {
            if (city.getLatitude() != null && city.getLongitude() != null) {
                double distance = calculateDistance(lat, lon,
                        city.getLatitude().doubleValue(), city.getLongitude().doubleValue());
                if (distance <= radiusKm) {
                    CityWeatherVO vo = convertToVO(city);
                    result.add(vo);
                }
            }
        }

        return result.stream()
                .sorted(Comparator.comparingDouble(v -> v.getLatitude().doubleValue()))
                .collect(Collectors.toList());
    }

    /**
     * 查询指定省份的所有城市
     */
    public List<CityWeatherVO> queryCitiesByProvince(String province) {
        log.debug("[SpatialQuery] 查询省份: {}", province);

        List<CityInfoEntity> cities = cityInfoMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CityInfoEntity>()
                        .eq(CityInfoEntity::getProvince, province)
        );

        return cities.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 查询相邻城市（距离最近N个城市）
     */
    public List<CityWeatherVO> queryNearestCities(String cityCode, int limit) {
        CityInfoEntity centerCity = cityInfoMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CityInfoEntity>()
                        .eq(CityInfoEntity::getCityCode, cityCode)
        );

        if (centerCity == null || centerCity.getLatitude() == null) {
            return new ArrayList<>();
        }

        List<CityInfoEntity> allCities = cityInfoMapper.selectList(null);
        List<CityWeatherVO> result = new ArrayList<>();

        for (CityInfoEntity city : allCities) {
            if (!city.getCityCode().equals(cityCode) && city.getLatitude() != null) {
                double distance = calculateDistance(
                        centerCity.getLatitude().doubleValue(),
                        centerCity.getLongitude().doubleValue(),
                        city.getLatitude().doubleValue(),
                        city.getLongitude().doubleValue()
                );
                CityWeatherVO vo = convertToVO(city);
                vo.setDisplayIcon(String.valueOf(distance)); // 临时存储距离
                result.add(vo);
            }
        }

        return result.stream()
                .sorted(Comparator.comparingDouble(v -> Double.parseDouble(v.getDisplayIcon())))
                .limit(limit)
                .peek(v -> v.setDisplayIcon(null))
                .collect(Collectors.toList());
    }

    /**
     * 计算两点间距离（Haversine公式）
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // 地球半径km

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    private CityWeatherVO convertToVO(CityInfoEntity city) {
        CityWeatherVO vo = new CityWeatherVO();
        vo.setCityCode(city.getCityCode());
        vo.setCityName(city.getCityName());
        vo.setLatitude(city.getLatitude());
        vo.setLongitude(city.getLongitude());
        vo.setProvince(city.getProvince());
        vo.setCityLevel(city.getCityLevel());
        return vo;
    }
}
