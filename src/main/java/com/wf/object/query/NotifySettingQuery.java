package com.wf.object.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
public class NotifySettingQuery implements Serializable {

    @Schema(description = "微信通知权限：1=允许，0=禁止")
    private Integer wechatNotifyPermission;

    @Schema(description = "邮箱通知权限：1=允许，0=禁止")
    private Integer emailNotifyPermission;

    @Schema(description = "手机号通知权限：1=允许，0=禁止")
    private Integer phoneNotifyPermission;
}
