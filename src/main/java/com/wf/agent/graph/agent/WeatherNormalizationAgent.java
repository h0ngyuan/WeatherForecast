package com.wf.agent.graph.agent;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.wf.agent.base.NormalizationResult;
import com.wf.agent.base.WeatherAiService;
import com.wf.utils.LocationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class WeatherNormalizationAgent {

    private final WeatherAiService weatherAiService;

    public WeatherNormalizationAgent(WeatherAiService weatherAiService) {
        this.weatherAiService = weatherAiService;
    }

    public NormalizationResult normalize(String question) {
        log.info("开始语义规范化，问题: {}", question);

        NormalizationResult result = weatherAiService.normalize(question);

        JSONObject locationInfo = new JSONObject(LocationUtils.getGpsInfo());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = now.plusDays(3);

        locationInfo.put("beginTime", now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        locationInfo.put("endTime", endTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        log.info("规范化完成: {}", result.getNormalizedQuestion());
        log.info("位置信息: {}", locationInfo.toJSONString());

        return new NormalizationResult(result.getNormalizedQuestion(), locationInfo.toJSONString());
    }
}
