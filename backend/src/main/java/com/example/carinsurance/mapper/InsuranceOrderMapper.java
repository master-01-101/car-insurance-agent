package com.example.carinsurance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.carinsurance.entity.InsuranceOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 保险订单（聚合根） Mapper（对应表 {@code insurance_order}）。
 */
@Mapper
public interface InsuranceOrderMapper extends BaseMapper<InsuranceOrder> {
}
