package com.wf.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wf.mapper.PredictWeatherCodeMapper;
import com.wf.mapper.WeatherDataMapper;
import com.wf.object.entity.PredictWeatherCodeEntity;
import com.wf.object.entity.WeatherDataEntity;
import com.wf.object.query.WeatherCodeQuery;
import com.wf.service.WeatherForecastService;

import com.wf.utils.WeatherCodeCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class WeatherForecastServiceImpl implements WeatherForecastService {

    @Autowired
    private WeatherDataMapper weatherDataMapper;

    @Autowired
    private PredictWeatherCodeMapper predictWeatherCodeMapper;

    @Override
    public List<String> acquireWeatherCodeValueByRangeTime(WeatherCodeQuery query) {
        //TODO 这边其实不完善，如果时间超出了规定最大时间（这边默认最大是三天）会怎么样呢？
        List<String> result = new ArrayList<>();

        if (query.getBeginTime() == null || query.getEndTime() == null) {
            log.warn("查询时间范围为空");
            return result;
        }

        LocalDateTime currentDayZero = LocalDate.now().atStartOfDay();

        if (query.getEndTime().isBefore(currentDayZero)) {
            result.addAll(getFormalWeatherCodes(query));
        } else if (query.getBeginTime().isAfter(currentDayZero) || query.getBeginTime().equals(currentDayZero)) {
            result.addAll(getPredictedWeatherCodes(query));
        } else {
            WeatherCodeQuery formalQuery = new WeatherCodeQuery();
            formalQuery.setLocation(query.getLocation());
            formalQuery.setBeginTime(query.getBeginTime());
            formalQuery.setEndTime(currentDayZero.minusSeconds(1));
            result.addAll(getFormalWeatherCodes(formalQuery));

            WeatherCodeQuery predictedQuery = new WeatherCodeQuery();
            predictedQuery.setLocation(query.getLocation());
            predictedQuery.setBeginTime(currentDayZero);
            predictedQuery.setEndTime(query.getEndTime());
            result.addAll(getPredictedWeatherCodes(predictedQuery));
        }
        return result;
    }

    private List<String> getFormalWeatherCodes(WeatherCodeQuery query) {
        List<String> result = new ArrayList<>();

        try {
            LambdaQueryWrapper<WeatherDataEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(WeatherDataEntity::getLocation, query.getLocation())
                    .ge(WeatherDataEntity::getTime, query.getBeginTime())
                    .le(WeatherDataEntity::getTime, query.getEndTime())
                    .orderByAsc(WeatherDataEntity::getTime);

            List<WeatherDataEntity> entityList = weatherDataMapper.selectList(wrapper);

            for (WeatherDataEntity entity : entityList) {
                String codeValue = WeatherCodeCache.getWeatherCodeValue(entity.getWeatherCode());
                result.add(codeValue != null ? codeValue : String.valueOf(entity.getWeatherCode()));
            }

            log.info("从正式数据表查询到 {} 条天气代码", entityList.size());

        } catch (Exception e) {
            log.error("查询正式天气数据失败", e);
        }

        return result;
    }

    private List<String> getPredictedWeatherCodes(WeatherCodeQuery query) {
        List<String> result = new ArrayList<>();

        try {
            LambdaQueryWrapper<PredictWeatherCodeEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(PredictWeatherCodeEntity::getLocation, query.getLocation())
                    .ge(PredictWeatherCodeEntity::getTime, query.getBeginTime())
                    .le(PredictWeatherCodeEntity::getTime, query.getEndTime())
                    .orderByAsc(PredictWeatherCodeEntity::getTime);

            List<PredictWeatherCodeEntity> entityList = predictWeatherCodeMapper.selectList(wrapper);

            for (PredictWeatherCodeEntity entity : entityList) {
                String codeValue = WeatherCodeCache.getWeatherCodeValue(entity.getWeatherCode());
                result.add(codeValue != null ? codeValue : String.valueOf(entity.getWeatherCode()));
            }

            log.info("从预测数据表查询到 {} 条天气代码", entityList.size());

        } catch (Exception e) {
            log.error("查询预测天气数据失败", e);
        }

        return result;
    }

}
