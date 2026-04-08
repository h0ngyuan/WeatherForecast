package com.wf.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wf.object.entity.CityInfoEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 城市信息 Mapper
 *
 * @author author
 * @since 1.0.0
 */
@Mapper
public interface CityInfoMapper extends BaseMapper<CityInfoEntity> {

    /**
     * 根据城市名称查询
     */
    @Select("SELECT * FROM CITY_INFO WHERE CITY_NAME = #{cityName} AND AVAILABLE = 1")
    CityInfoEntity selectByCityName(@Param("cityName") String cityName);

    /**
     * 查询所有热门城市
     */
    @Select("SELECT * FROM CITY_INFO WHERE IS_HOT = 1 AND AVAILABLE = 1 ORDER BY CITY_NAME")
    List<CityInfoEntity> selectHotCities();

    /**
     * 根据省份查询城市
     */
    @Select("SELECT * FROM CITY_INFO WHERE PROVINCE = #{province} AND AVAILABLE = 1 ORDER BY CITY_NAME")
    List<CityInfoEntity> selectByProvince(@Param("province") String province);
}
