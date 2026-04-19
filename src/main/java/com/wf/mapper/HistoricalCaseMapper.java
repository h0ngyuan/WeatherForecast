package com.wf.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wf.agent.map.entity.HistoricalCase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 历史案例Mapper
 */
@Mapper
public interface HistoricalCaseMapper extends BaseMapper<HistoricalCase> {

    /**
     * 根据案例ID查询
     */
    @Select("SELECT * FROM HISTORICAL_CASE WHERE case_id = #{caseId}")
    HistoricalCase selectByCaseId(@Param("caseId") String caseId);

    /**
     * 根据事件类型查询
     */
    @Select("SELECT * FROM HISTORICAL_CASE WHERE event_type = #{eventType} ORDER BY start_time DESC")
    List<HistoricalCase> selectByEventType(@Param("eventType") String eventType);

    /**
     * 查询最近的案例
     */
    @Select("SELECT * FROM HISTORICAL_CASE ORDER BY start_time DESC LIMIT #{limit}")
    List<HistoricalCase> selectRecentCases(@Param("limit") int limit);
}
