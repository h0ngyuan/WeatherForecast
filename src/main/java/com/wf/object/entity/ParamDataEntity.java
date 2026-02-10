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
@TableName("SYS_PARAM_DICT")
@Schema(description = "通用数据字典表（码表）")
public class ParamDataEntity implements Serializable {

    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "字典类型ID，如 1=天气代码，2=风向等")
    private Integer dictTypeId;

    @Schema(description = "字典类型编码，如 weather_code")
    private String dictType;

    @Schema(description = "字典类型名称，如 \"WMO天气代码\"")
    private String dictTypeName;

    @Schema(description = "字典项的键（code值，如 \"0\", \"51\"）")
    private Integer dictKey;

    @Schema(description = "字典项的值（描述，如 \"晴天\"）")
    private String dictValue;

    @Schema(description = "详细说明")
    private String description;

    @Schema(description = "排序权重")
    private Integer sortOrder;

    @Schema(description = "是否启用：1=可用，0=禁用")
    @TableLogic
    private Integer available;

    @Schema(description = "记录插入时间")
    private LocalDateTime createTime;

    @Schema(description = "记录最后更新时间")
    private LocalDateTime updateTime;
}
