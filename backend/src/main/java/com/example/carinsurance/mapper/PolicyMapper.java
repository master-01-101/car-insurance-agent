package com.example.carinsurance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.carinsurance.entity.Policy;
import org.apache.ibatis.annotations.Mapper;

/**
 * 保单（一单可多张） Mapper（对应表 {@code policy}）。
 */
@Mapper
public interface PolicyMapper extends BaseMapper<Policy> {
}
