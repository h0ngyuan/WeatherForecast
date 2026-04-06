package com.wf.object.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "天气预警通知权限授权请求")
public class WeatherPermissionRequest {

    @NotBlank(message = "线程ID不能为空")
    @Schema(description = "流程线程ID")
    private String threadId;

    @Schema(description = "是否授权手机号通知权限", example = "true")
    private Boolean grantPhonePermission;

    @Schema(description = "是否授权邮箱通知权限", example = "true")
    private Boolean grantEmailPermission;

    @Schema(description = "是否授权微信通知权限", example = "true")
    private Boolean grantWechatPermission;

    @Schema(description = "用户手机号（如果授权手机号通知且未绑定）", example = "13800138000")
    private String phone;

    @Schema(description = "用户邮箱（如果授权邮箱通知且未绑定）", example = "user@example.com")
    private String email;
}
