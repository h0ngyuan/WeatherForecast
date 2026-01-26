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
@TableName("PREDICTED_WEATHER_DATA")
@Schema(description = "预测天气现象数据表")
public class PredictWeatherCodeEntity implements Serializable {

    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "地区名称，如杭州、成都等")
    private String location;

    @Schema(description = "预测目标时间（UTC或统一时区）")
    private LocalDateTime time;

    @Schema(description = "预测天气现象代码（WMO标准）")
    private Integer weatherCode;

    @Schema(description = "预测天气现象代码值")
    private String weatherCodeValue;

    @Schema(description = "数据来源：0=本地模型预测，1=外部API，2=人工修正")
    private Integer sourceType;

    @Schema(description = "是否可用：1=可用，0=不可用")
    @TableLogic
    private Integer available;

    @Schema(description = "记录插入时间")
    private LocalDateTime createTime;

    @Schema(description = "记录最后更新时间")
    private LocalDateTime updateTime;
}
