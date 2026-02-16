package com.wf.object.query;

import lombok.Data;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class WeatherCodeQuery implements Serializable {

    @ToolParam(description = "获取当前位置 如 杭州、成都")
    private String location;
    @ToolParam(description = "获取想要查询的时间范围的开始时间，如2026.1.26 22:53:22 ->2026-01-26 23:00:00")
    private LocalDateTime beginTime;
    @ToolParam(description = "获取想要查询的时间范围的结束时间，如2026.1.26 22:53:22 ->2026-01-26 23:00:00")
    private LocalDateTime endTime;
    @ToolParam(description = "纬度，用于MCP服务调用")
    private Double latitude;
    @ToolParam(description = "经度，用于MCP服务调用")
    private Double longitude;
}
