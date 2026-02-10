package com.wf.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wf.object.entity.ParamDataEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ParamMapper extends BaseMapper<ParamDataEntity> {
    List<String> getWeatherCodes(List<Integer> codes);
}
