package com.wf.object.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
public class LoginQuery implements Serializable {

    @Schema(description = "微信传的用户唯一标识")
    public String openId;

    @Schema(description = "手机号作为账号")
    public String phone;

    @Schema(description = "邮箱作为账号")
    public String email;

    @Schema(description = "密码")
    public String password;
}
