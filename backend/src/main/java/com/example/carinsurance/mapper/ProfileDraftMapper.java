package com.example.carinsurance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.carinsurance.entity.ProfileDraft;
import org.apache.ibatis.annotations.Mapper;

/**
 * 资料草稿 Mapper（对应表 profile_draft）。
 */
@Mapper
public interface ProfileDraftMapper extends BaseMapper<ProfileDraft> {
}
