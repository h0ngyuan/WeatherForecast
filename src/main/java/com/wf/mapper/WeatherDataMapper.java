package com.wf.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wf.object.entity.WeatherDataEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WeatherDataMapper extends BaseMapper<WeatherDataEntity> {

    @Select("select * from FORMAL_WEATHER_DATA f where f.time > #{beginTime} and location = #{location} order by f.time asc  ")
    List<WeatherDataEntity> selectEntitys(@Param("beginTime") String beginTime, @Param("location") String location);
}
