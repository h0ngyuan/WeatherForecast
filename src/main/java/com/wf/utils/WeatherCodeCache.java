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
//        try {
//            LambdaQueryWrapper<ParamDataEntity> wrapper = new LambdaQueryWrapper<>();
//            wrapper.eq(ParamDataEntity::getDictType, WEATHER_CODE_DICT_TYPE)
//                    .or()
//                    .eq(ParamDataEntity::getDictTypeId, WEATHER_CODE_DICT_TYPE_ID)
//                    .eq(ParamDataEntity::getAvailable, 1);
//
//            List<ParamDataEntity> dictList = paramDataMapper.selectList(wrapper);
//
//            weatherCodeMap.clear();
//            for (ParamDataEntity entity : dictList) {
//                if (entity.getDictKey() != null && entity.getDictValue() != null) {
//                    Integer code = Integer.parseInt(entity.getDictKey());
//                    weatherCodeMap.put(code, entity.getDictValue());
//                }
//            }
//
//            log.info("天气代码码表加载完成，共加载 {} 条数据", weatherCodeMap.size());
//
//        } catch (Exception e) {
//            log.error("加载天气代码码表失败", e);
//        }
    }

    public static String getWeatherCodeValue(Integer code) {
//        if (code == null) {
//            return null;
//        }
//        return weatherCodeMap.get(code);
        return null;
    }

    public static void reload() {
        log.info("重新加载天气代码码表");
    }

    public static int getCacheSize() {
        return weatherCodeMap.size();
    }
}
