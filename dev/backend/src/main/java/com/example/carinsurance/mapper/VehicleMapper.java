package com.example.carinsurance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.carinsurance.entity.Vehicle;
import org.apache.ibatis.annotations.Mapper;

/**
 * 车辆主数据 Mapper（对应表 vehicle）。
 */
@Mapper
public interface VehicleMapper extends BaseMapper<Vehicle> {
}
