package com.example.carinsurance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.carinsurance.entity.PolicyItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 保单险种明细 Mapper（对应表 {@code policy_item}）。
 */
@Mapper
public interface PolicyItemMapper extends BaseMapper<PolicyItem> {
}
