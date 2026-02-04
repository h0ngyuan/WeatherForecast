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

    /**
     * 获取当前时间
     * @return 当前时间的JSON字符串，包含ISO格式时间和未来三天时间
     */
    @Tool(description = "获取当前时间，用于天气查询")
    public String getCurrentTime() {
        log.info("========== [TimeTool] 调用 getCurrentTime ==========");
        String result = TimeUtils.getCurrentFormatHourTime();
        log.info("当前时间: {}", result);
        log.info("========== [TimeTool] getCurrentTime 完成 ==========");
        return result;
    }

    @Tool(description = "获取过去某段时间，用于天气查询")
    public String acquirePastFormatHourTime(@ToolParam(description = "时间长短") Long amount,@ToolParam(description = "时间单位") TimeUnit unit) {
        log.info("========== [TimeTool] 调用 acquirePastFormatHourTime ==========");
        log.info("时间长度: {}, 时间单位: {}", amount, unit);
        String result = TimeUtils.acquirePastFormatHourTime(amount, unit);
        log.info("过去时间: {}", result);
        log.info("========== [TimeTool] acquirePastFormatHourTime 完成 ==========");
        return result;
    }
}