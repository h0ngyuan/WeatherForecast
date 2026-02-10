package com.wf.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wf.object.entity.ParamDataEntity;

import java.util.List;

public interface ParamService extends IService<ParamDataEntity> {
    List<ParamDataEntity> getCities();
}
