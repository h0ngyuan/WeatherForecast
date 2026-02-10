package com.wf.utils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wf.mapper.ParamDataMapper;
import com.wf.object.entity.ParamDataEntity;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class WeatherCodeCache {

    private static final String WEATHER_CODE_DICT_TYPE = "weather_code";
    private static final Integer WEATHER_CODE_DICT_TYPE_ID = 1;

    @Autowired
    private ParamDataMapper paramDataMapper;

    private static Map<Integer, String> weatherCodeMap = new HashMap<>();

    @PostConstruct
    public void init() {
        loadWeatherCodeDict();
    }

    private void loadWeatherCodeDict() {
        try {
            List<ParamDataEntity> datas = paramDataMapper.selectList(new LambdaQueryWrapper<ParamDataEntity>()
                    .eq(ParamDataEntity::getDictTypeId, "1"));
            weatherCodeMap.putAll(datas.stream().collect(Collectors.toMap(ParamDataEntity::getDictKey, ParamDataEntity::getDictValue)));
        } catch (Exception ignored){
        }
    }

    public static String getWeatherCodeValue(Integer code) {
        if (code == null) {
            return null;
        }
        return weatherCodeMap.get(code);
    }

    public static void reload() {
        log.info("重新加载天气代码码表");
    }

    public static int getCacheSize() {
        return weatherCodeMap.size();
    }
}
