package com.example.carinsurance.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户（客户/业务员/管理员）。
 * 对应表 sys_user。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_user")
public class SysUser {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录名，唯一 */
    private String username;

    /** 手机号 */
    private String phone;

    /** BCrypt 密码哈希 */
    private String passwordHash;

    /** 角色：CUSTOMER/SALESMAN/ADMIN */
    private String userType;

    /** 业务员工号，仅 SALESMAN */
    private String agentNo;

    /** 所属机构/团队 */
    private String orgName;

    /** 客户归属业务员 sys_user.id */
    private Long parentAgentId;

    /** 真实姓名 */
    private String realName;

    /** 邮箱 */
    private String email;

    /** 头像地址 */
    private String avatarUrl;

    /** 状态：ACTIVE/DISABLED/LOCKED */
    private String status;

    /** 最近登录时间 */
    private LocalDateTime lastLoginAt;

    /** 乐观锁版本号 */
    @Version
    @TableField(fill = FieldFill.INSERT)
    private Integer version;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 逻辑删除：0-否 1-是 */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
