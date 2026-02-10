package com.wf.agent.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wf.mapper.ParamMapper;
import com.wf.object.entity.ParamDataEntity;
import com.wf.service.ParamService;
import com.wf.utils.WeatherCodeCache;
import io.netty.util.internal.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class WeatherCodeTool {

    @Autowired
    private ParamMapper paramMapper;
    @Autowired
    private WeatherCodeCache weatherCodeCache;

    @Tool(description = "获得天气码值")
    public List<String> getWeatherCodes(@ToolParam(description = "传过来的是code的列表") List<Integer> codes){
        List<String> list = new ArrayList<>();
        for (Integer code : codes) {
            String v = WeatherCodeCache.getWeatherCodeValue(code);
            if (v!=null){
                list.add(v);
                continue;
            }
            LambdaQueryWrapper<ParamDataEntity> queryWrapper = new LambdaQueryWrapper<ParamDataEntity>()
                    .eq(ParamDataEntity::getDictTypeId, "1")
                    .eq(ParamDataEntity::getDictKey, code);
            v = paramMapper.selectOne(queryWrapper).getDictValue();
            list.add(v);
        }
        return list;
    }
}
