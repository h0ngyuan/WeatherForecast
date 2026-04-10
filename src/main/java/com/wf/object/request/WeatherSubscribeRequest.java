package com.wf.object.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 天气订阅请求
 *
 * @author author
 * @since 1.0.0
 */
@Data
@Schema(description = "天气订阅请求")
public class WeatherSubscribeRequest {

    @NotBlank(message = "订阅名称不能为空")
    @Schema(description = "订阅名称", required = true, example = "杭州雨天提醒")
    private String subscribeName;

    @Schema(description = "监控地点，为空时自动根据IP定位", example = "杭州")
    private String location;

    @NotNull(message = "天气条件不能为空")
    @Schema(description = "天气条件码值列表", required = true, example = "[46, 47, 48, 49]")
    private List<Integer> weatherCodes;

    @Schema(description = "任务类型：0=一次，1=总是", example = "1")
    private Integer taskType;

    @Schema(description = "预计最早执行时间")
    private LocalDateTime expectedEarliestTime;

    @Schema(description = "预计最晚执行时间")
    private LocalDateTime expectedLatestTime;

    @Schema(description = "通知条件描述", example = "当杭州有降雨时通知我")
    private String notifyCondition;

    @Schema(description = "灾害等级：1=一级，2=二级，3=三级", example = "2")
    private Integer disasterLevel;
}
