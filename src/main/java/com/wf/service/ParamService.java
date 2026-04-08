package com.wf.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wf.object.entity.ParamDataEntity;

import java.util.List;

public interface ParamService extends IService<ParamDataEntity> {

    /**
     * 获取所有城市列表
     */
    List<ParamDataEntity> getCities();

    /**
     * 获取所有支持的地区列表（用于紧急响应系统）
     *
     * @return 地区名称列表
     */
    List<String> getAllLocations();
}
