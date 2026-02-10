package com.wf.agent.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.fastjson.JSONObject;
import com.wf.agent.base.AIClient;
import com.wf.agent.constants.WeatherGraphConstants;
import com.wf.object.query.WeatherCodeQuery;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class WeatherForecastNode implements NodeAction {

    private final AIClient aiClient;

    public WeatherForecastNode(AIClient aiClient) {
        this.aiClient = aiClient;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        /*
            这边的逻辑我想是这样的：
            首先这个节点的任务是根据请求query（就是上一节点转化的WeatherCodeQuery）通过调用MCP的WeatherPredictionTool
            获得规定的这一时间段的天气数据（原始天气码序列，按逗号分隔）
            然后进行语义转化，将天气码序列转化为更自然的描述
            比方说假设现在是 20260204 16:00:00
            人家传过来的是 成都 20260205 16:00:00 到 20260205 18:00:00
            返回的数据可能是：100,100,100（晴天）
            转化为：全天天气晴朗
            人家传过来的是 成都 20260202 18:00:00 到 20260205 18:00:00
            返回的数据可能是：100,100,101,100,100,100（晴天，晴天，多云，晴天，晴天，晴天）
            转化为：上午到下午天气晴朗，傍晚转多云

         */
        log.info("---------- [forecast节点] 开始执行 ----------");
        
        String weatherCodeQueryStr = state.value(WeatherGraphConstants.KEY_WEATHER_CODE_QUERY, "");
        log.info("天气代码查询: {}", weatherCodeQueryStr);
        
        log.info("开始获取天气数据流程...");
        String forecastResult = aiClient.performForecast(weatherCodeQueryStr);
        log.info("获取天气数据完成: {}", forecastResult);

        log.info("开始语义转化流程...");
        String transformedResult = aiClient.performForecastTransform(forecastResult);
        log.info("语义转化完成: {}", transformedResult);
        
        log.info("---------- [forecast节点] 执行完成 ----------");
        
        return Map.of(
            WeatherGraphConstants.KEY_FORECAST_RESULT, transformedResult
        );
    }
}
