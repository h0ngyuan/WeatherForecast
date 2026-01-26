package com.wf.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wf.object.entity.WeatherDataEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WeatherDataMapper extends BaseMapper<WeatherDataEntity> {
}
