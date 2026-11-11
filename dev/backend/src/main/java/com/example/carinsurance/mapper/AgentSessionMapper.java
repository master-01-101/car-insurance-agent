package com.example.carinsurance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.carinsurance.entity.AgentSession;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会话与 LangGraph 线程映射 Mapper（对应表 {@code agent_session}）。
 */
@Mapper
public interface AgentSessionMapper extends BaseMapper<AgentSession> {
}
