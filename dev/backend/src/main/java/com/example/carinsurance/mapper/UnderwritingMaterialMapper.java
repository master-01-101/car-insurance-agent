package com.example.carinsurance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.carinsurance.entity.UnderwritingMaterial;
import org.apache.ibatis.annotations.Mapper;

/**
 * 核保材料关联 Mapper（对应表 {@code underwriting_material}）。
 */
@Mapper
public interface UnderwritingMaterialMapper extends BaseMapper<UnderwritingMaterial> {
}
