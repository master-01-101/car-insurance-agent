package com.example.carinsurance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.carinsurance.entity.KbTerm;
import org.apache.ibatis.annotations.Mapper;

/**
 * 行业术语（向量数据不入库） Mapper（对应表 {@code kb_term}）。
 */
@Mapper
public interface KbTermMapper extends BaseMapper<KbTerm> {
}
