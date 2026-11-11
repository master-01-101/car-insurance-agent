package com.example.carinsurance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.carinsurance.entity.RenewalLookup;
import org.apache.ibatis.annotations.Mapper;

/**
 * 续保查询记录 Mapper（对应表 {@code renewal_lookup}）。
 */
@Mapper
public interface RenewalLookupMapper extends BaseMapper<RenewalLookup> {
}
