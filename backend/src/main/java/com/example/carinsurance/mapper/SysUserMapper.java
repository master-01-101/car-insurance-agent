package com.example.carinsurance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.carinsurance.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper（对应表 sys_user）。
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
