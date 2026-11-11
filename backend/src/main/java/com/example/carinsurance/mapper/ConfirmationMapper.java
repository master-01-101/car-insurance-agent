package com.example.carinsurance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.carinsurance.entity.Confirmation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户确认记录 Mapper（对应表 confirmation）。
 */
@Mapper
public interface ConfirmationMapper extends BaseMapper<Confirmation> {
}
