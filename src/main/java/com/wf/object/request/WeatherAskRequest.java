package com.wf.object.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "天气查询请求")
public class WeatherAskRequest {

    @NotBlank(message = "问题不能为空")
    @Schema(description = "用户的问题", required = true, example = "明天北京适合爬山吗？")
    private String question;
}
