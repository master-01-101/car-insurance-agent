package com.example.carinsurance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.carinsurance.entity.FileObject;
import org.apache.ibatis.annotations.Mapper;

/**
 * 统一文件元数据 Mapper（对应表 file_object）。
 */
@Mapper
public interface FileObjectMapper extends BaseMapper<FileObject> {
}
