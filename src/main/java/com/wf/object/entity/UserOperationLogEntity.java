package com.wf.object.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("USER_OPERATION_LOG")
@Schema(description = "USER_OPERATION_LOG 表对应的实体类")
public class UserOperationLogEntity implements Serializable {

    @Schema(description = "日志ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "关联 USER_INFO.ID")
    private Long userId;

    @Schema(description = "操作类型")
    private String operationType;

    @Schema(description = "操作描述")
    private String operationDesc;

    @Schema(description = "客户端IP")
    private String clientIp;

    @Schema(description = "IP解析的地理位置")
    private String location;

    @Schema(description = "请求参数")
    private String requestParams;

    @Schema(description = "返回结果")
    private String responseResult;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
