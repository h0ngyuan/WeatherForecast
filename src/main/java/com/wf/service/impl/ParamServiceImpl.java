package com.wf.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wf.object.entity.ParamDataEntity;
import com.wf.mapper.ParamMapper;
import com.wf.service.ParamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class ParamServiceImpl implements ParamService {

    private final ParamMapper paramMapper;

    public ParamServiceImpl(ParamMapper paramMapper) {
        this.paramMapper = paramMapper;
    }

    @Override
    public List<ParamDataEntity> getCities() {
        LambdaQueryWrapper<ParamDataEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ParamDataEntity::getDictTypeId, 3);
        wrapper.eq(ParamDataEntity::getAvailable, 1);
        wrapper.isNotNull(ParamDataEntity::getDescription);
        wrapper.orderByAsc(ParamDataEntity::getSortOrder);

        List<ParamDataEntity> cityList = paramMapper.selectList(wrapper);

        if (cityList == null || cityList.isEmpty()) {
            log.warn("未找到可预测城市数据");
        }

        return cityList;
    }
}
