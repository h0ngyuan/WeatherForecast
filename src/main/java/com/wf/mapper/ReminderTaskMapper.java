package com.wf.mapper;

import com.wf.object.entity.ReminderTaskEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 提醒任务 Mapper
 *
 * @author author
 * @since 1.0.0
 */
@Mapper
public interface ReminderTaskMapper {

    /**
     * 插入任务
     */
    int insert(ReminderTaskEntity entity);

    /**
     * 根据ID查询
     */
    ReminderTaskEntity selectById(Long id);

    /**
     * 根据用户ID查询
     */
    List<ReminderTaskEntity> selectByUserId(Long userId);

    /**
     * 查询指定地区的待执行任务
     */
    List<ReminderTaskEntity> selectPendingByLocation(
            @Param("location") String location,
            @Param("currentTime") LocalDateTime currentTime);

    /**
     * 更新任务状态
     */
    int updateTaskStatus(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 更新可用状态
     */
    int updateAvailable(@Param("id") Long id, @Param("available") Integer available);

    /**
     * 查询指定地区的一级灾害提醒任务（alwaysRemind=1）
     */
    List<ReminderTaskEntity> selectLevel1TasksByLocation(@Param("location") String location);

    /**
     * 查询指定地区和天气码的二三级灾害提醒任务
     */
    List<ReminderTaskEntity> selectLevel2And3TasksByLocationAndWeatherCode(
            @Param("location") String location,
            @Param("weatherCode") Integer weatherCode,
            @Param("currentTime") LocalDateTime currentTime);
}
