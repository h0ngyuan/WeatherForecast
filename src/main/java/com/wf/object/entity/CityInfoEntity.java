package com.wf.object.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 城市信息实体
 *
 * @author author
 * @since 1.0.0
 */
@Data
@TableName("CITY_INFO")
@Schema(description = "城市信息表")
public class CityInfoEntity implements Serializable {

    @Schema(description = "城市ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "城市名称")
    private String cityName;

    @Schema(description = "城市编码")
    private String cityCode;

    @Schema(description = "纬度")
    private BigDecimal latitude;

    @Schema(description = "经度")
    private BigDecimal longitude;

    @Schema(description = "所属省份")
    private String province;

    @Schema(description = "所属区县")
    private String district;

    @Schema(description = "城市级别：1=直辖市，2=省会城市，3=地级市，4=县级市")
    private Integer cityLevel;

    @Schema(description = "时区")
    private String timezone;

    @Schema(description = "是否可用：1=可用，0=不可用")
    private Integer available;

    @Schema(description = "是否热门城市：1=是，0=否")
    private Integer isHot;

    @Schema(description = "城市描述或别名")
    private String description;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
