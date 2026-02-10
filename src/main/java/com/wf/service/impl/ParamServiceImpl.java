package com.wf.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wf.mapper.ParamDataMapper;
import com.wf.object.entity.ParamDataEntity;
import com.wf.mapper.ParamMapper;
import com.wf.service.ParamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class ParamServiceImpl extends ServiceImpl<ParamMapper, ParamDataEntity> implements ParamService  {

    @Autowired
    private ParamMapper paramMapper;

    @Override
    public List<ParamDataEntity> getCities() {
        LambdaQueryWrapper<ParamDataEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ParamDataEntity::getDictTypeId, 3);
        wrapper.eq(ParamDataEntity::getAvailable, 1);
        wrapper.isNotNull(ParamDataEntity::getDescription);
        wrapper.orderByAsc(ParamDataEntity::getSortOrder);

        List<ParamDataEntity> cityList = paramMapper.selectList(wrapper);


        return cityList;
    }
}
