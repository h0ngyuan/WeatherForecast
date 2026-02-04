package com.wf.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wf.object.entity.UserOperationLogEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserOperationLogMapper extends BaseMapper<UserOperationLogEntity> {
}
