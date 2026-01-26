package com.wf.object.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("FORMAL_WEATHER_DATA")
@Schema(description = "FORMAL_WEATHER_DATA 表对应的实体类")
public class WeatherDataEntity implements Serializable {

    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "地区名称，如杭州、成都等")
    private String location;

    @Schema(description = "气象观测时间（UTC或本地时区需统一）")
    private LocalDateTime time;

    @Schema(description = "记录插入数据库时间")
    private LocalDateTime createTime;

    @Schema(description = "记录最后更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "是否可用：1=可用，0=不可用")
    @TableLogic
    private Integer available;

    @Schema(description = "气温 (°C)")
    private BigDecimal temp;

    @Schema(description = "相对湿度 (%)")
    private BigDecimal rh;

    @Schema(description = "露点温度 (°C)")
    private BigDecimal dew;

    @Schema(description = "总降水 (mm)")
    private BigDecimal precip;

    @Schema(description = "降雨量 (mm)")
    private BigDecimal rain;

    @Schema(description = "降雪量 (mm)")
    private BigDecimal snow;

    @Schema(description = "体感温度 (°C)")
    private BigDecimal appTemp;

    @Schema(description = "天气现象代码（WMO标准）")
    private Integer weatherCode;

    @Schema(description = "平均海平面气压 (hPa)")
    private BigDecimal pMsl;

    @Schema(description = "地表气压 (hPa)")
    private BigDecimal surfP;

    @Schema(description = "总云量 (%)")
    private BigDecimal cloud;

    @Schema(description = "10米风速 (m/s)")
    private BigDecimal wind10;

    @Schema(description = "10米风向 (度)")
    private Integer dir10;

    @Schema(description = "10米阵风 (m/s)")
    private BigDecimal gust10;

    @Schema(description = "地表土壤温度 (°C)")
    private BigDecimal soilT0;

    @Schema(description = "积雪深度 (cm)")
    private BigDecimal snowDepth;

    @Schema(description = "低云量 (%)")
    private BigDecimal cloudLow;

    @Schema(description = "中云量 (%)")
    private BigDecimal cloudMid;

    @Schema(description = "高云量 (%)")
    private BigDecimal cloudHigh;

    @Schema(description = "参考作物蒸散量 (mm)")
    private BigDecimal et0;

    @Schema(description = "饱和水汽压差 (kPa)")
    private BigDecimal vpd;

    @Schema(description = "100米风速 (m/s)")
    private BigDecimal wind100;

    @Schema(description = "100米风向 (度)")
    private Integer dir100;

    @Schema(description = "7cm土壤温度 (°C)")
    private BigDecimal soilT7;

    @Schema(description = "地表土壤湿度 (m³/m³)")
    private BigDecimal soilM0;

    @Schema(description = "7cm土壤湿度 (m³/m³)")
    private BigDecimal soilM7;
}
