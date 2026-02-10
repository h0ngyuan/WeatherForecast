package com.wf.object.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
public class CaptchaQuery implements Serializable {

    @Schema(description = "验证码key")
    private String key;

    @Schema(description = "验证码")
    private String code;
}
