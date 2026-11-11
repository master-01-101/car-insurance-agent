package com.example.carinsurance.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 核保材料关联。
 *
 * <p>对应数据库表 {@code underwriting_material}。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("underwriting_material")
public class UnderwritingMaterial {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 核保记录 underwriting_record.id */
    private Long caseId;

    /** 文件 file_object.id */
    private Long fileId;

    /** 材料类型：INSPECTION_PHOTO/INSPECTION_VIDEO/OTHER */
    private String materialType;

    /** 是否被接受：1-是 0-否 */
    private Integer accepted;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
