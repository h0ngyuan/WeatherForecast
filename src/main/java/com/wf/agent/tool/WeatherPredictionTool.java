package com.wf.agent.tool;

import com.wf.object.query.WeatherCodeQuery;

import java.util.List;

import com.wf.service.WeatherForecastService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WeatherPredictionTool {

    @Autowired
    private WeatherForecastService weatherForecastService;
    
    @Tool(description = "获取过去或未来规定地点规定时间的天气预测值")
    List<String> acquireWeatherCodeValueByRangeTime(@ToolParam(description = "这边需要三个参数，城市，开始时间，结束时间，这个其实上一NODE提供了") WeatherCodeQuery query){
        log.info("========== [WeatherPredictionTool] 调用 acquireWeatherCodeValueByRangeTime ==========");
        log.info("查询参数: 城市={}, 开始时间={}, 结束时间={}", query.getLocation(), query.getBeginTime(), query.getEndTime());
        
        List<String> result = weatherForecastService.acquireWeatherCodeValueByRangeTime(query);
        
        log.info("查询结果数量: {}", result != null ? result.size() : 0);
        if (result != null && !result.isEmpty()) {
            log.info("天气预测数据: {}", result);
        }
        log.info("========== [WeatherPredictionTool] acquireWeatherCodeValueByRangeTime 完成 ==========");
        
        return result;
    }
}
