package com.wf.object.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("USER_INFO")
@Schema(description = "用户信息主表")
public class UserInfoEntity implements Serializable {

    @Schema(description = "用户ID，从100000开始")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "手机号（可选）")
    private String phone;

    @Schema(description = "邮箱（可选）")
    private String email;

    @Schema(description = "微信唯一标识（如 openid 或 unionid）")
    private String wechatOpenid;

    @Schema(description = "密码")
    private String password;

    @Schema(description = "用户昵称")
    private String nickname;

    @Schema(description = "头像URL")
    private String avatarUrl;

    @Schema(description = "账户来源：0=本地注册，1=微信，2=其他第三方")
    private Integer accountSource;

    @Schema(description = "用户角色，如 USER / ADMIN")
    private String role;

    @Schema(description = "注册时地理位置（如 IP 解析的城市）")
    private String registerLocation;

    @Schema(description = "微信通知权限：1=允许，0=禁止")
    private Integer wechatNotifyPermission;

    @Schema(description = "邮箱通知权限：1=允许，0=禁止")
    private Integer emailNotifyPermission;

    @Schema(description = "手机号通知权限：1=允许，0=禁止")
    private Integer phoneNotifyPermission;

    @Schema(description = "是否可用：1=正常，0=禁用/注销")
    @TableLogic
    private Integer available;

    @Schema(description = "记录插入时间")
    private LocalDateTime createTime;

    @Schema(description = "记录最后更新时间")
    private LocalDateTime updateTime;
}
