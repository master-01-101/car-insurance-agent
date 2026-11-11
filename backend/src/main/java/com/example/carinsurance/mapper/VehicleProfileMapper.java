package com.example.carinsurance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.carinsurance.entity.VehicleProfile;
import org.apache.ibatis.annotations.Mapper;

/**
 * 车辆资料确认快照 Mapper（对应表 vehicle_profile）。
 */
@Mapper
public interface VehicleProfileMapper extends BaseMapper<VehicleProfile> {
}
