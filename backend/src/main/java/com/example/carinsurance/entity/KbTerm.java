package com.example.carinsurance.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 行业术语库（表 kb_term，含 version 乐观锁）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("kb_term")
public class KbTerm {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 术语原文，唯一 */
    private String term;

    /** 标准化名称 */
    private String normalized;

    /** 术语解释 */
    private String definition;

    /** 同义词列表（JSON） */
    private String aliases;

    /** 示例 */
    private String example;

    /** 来源 */
    private String source;

    /** 乐观锁版本号 */
    @Version
    @TableField(fill = FieldFill.INSERT)
    private Integer version;

    /** 状态：ACTIVE / INACTIVE */
    private String status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
