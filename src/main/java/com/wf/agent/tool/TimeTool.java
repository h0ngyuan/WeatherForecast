package com.wf.agent.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.wf.utils.TimeUtils;

import java.util.concurrent.TimeUnit;


/**
 * 时间工具类，作为MCP工具供AI模型调用
 */
@Slf4j
@Component
public class TimeTool {

    @Tool(description = "获取时间，用于天气查询。amount为负数表示过去，正数表示未来，0表示现在")
    public String acquireFormatHourTime(@ToolParam(description = "开始时间（负数表示过去，正数表示未来，0表示现在）") Integer amount, 
                                        @ToolParam(description = "时间单位") TimeUnit unit) {
        log.info("========== [TimeTool] 调用 acquireFormatHourTime ==========");
        log.info("时间偏移: {}, 时间单位: {}", amount, unit);
        String result;
        if (amount == 0) {
            result = TimeUtils.getCurrentFormatHourTime();
        } else if (amount < 0) {
            result = TimeUtils.acquirePastFormatHourTime(Math.abs(amount), unit);
        } else {
            result = TimeUtils.acquireFutureFormatHourTime(amount, unit);
        }
        log.info("结果时间: {}", result);
        log.info("========== [TimeTool] acquireFormatHourTime 完成 ==========");
        return result;
    }
}