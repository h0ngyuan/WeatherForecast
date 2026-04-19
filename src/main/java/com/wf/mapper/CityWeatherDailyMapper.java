package com.wf.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wf.agent.map.entity.CityWeatherDaily;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 城市天气日记录Mapper
 */
@Mapper
public interface CityWeatherDailyMapper extends BaseMapper<CityWeatherDaily> {

    /**
     * 根据城市编码和日期查询
     */
    @Select("SELECT * FROM CITY_WEATHER_DAILY WHERE city_code = #{cityCode} AND record_date = #{date}")
    CityWeatherDaily selectByCityAndDate(@Param("cityCode") String cityCode, @Param("date") LocalDate date);

    /**
     * 查询指定日期的所有记录
     */
    @Select("SELECT * FROM CITY_WEATHER_DAILY WHERE record_date = #{date}")
    List<CityWeatherDaily> selectByDate(@Param("date") LocalDate date);

    /**
     * 查询有灾害的记录
     */
    @Select("SELECT * FROM CITY_WEATHER_DAILY WHERE record_date = #{date} AND has_disaster = 1")
    List<CityWeatherDaily> selectDisasterByDate(@Param("date") LocalDate date);

    /**
     * 查询异常记录
     */
    @Select("SELECT * FROM CITY_WEATHER_DAILY WHERE record_date = #{date} AND is_anomaly = 1")
    List<CityWeatherDaily> selectAnomalyByDate(@Param("date") LocalDate date);
}
