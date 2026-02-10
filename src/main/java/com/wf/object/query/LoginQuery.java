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

    @Schema(description = "密码（手机号/邮箱登录时使用，可选）")
    public String password;

    @Schema(description = "图形验证码key")
    public String captchaKey;

    @Schema(description = "图形验证码")
    public String captchaCode;

    @Schema(description = "短信/邮箱验证码")
    public String verifyCode;

    @Schema(description = "登陆方式：wx=微信，phone=手机号，email=邮箱")
    public String type;
}
