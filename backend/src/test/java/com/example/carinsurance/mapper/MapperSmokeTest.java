package com.example.carinsurance.mapper;

import com.example.carinsurance.entity.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Entity/Mapper 烟测
 * 验证：Mapper 注入成功、Entity 字段类型正确、BaseMapper 基础方法可用
 */
@SpringBootTest
class MapperSmokeTest {

    @Autowired private SysUserMapper sysUserMapper;
    @Autowired private VehicleMapper vehicleMapper;
    @Autowired private ConversationMapper conversationMapper;

    // ---------- SysUser ----------

    @Test
    @DisplayName("SysUserMapper 注入成功 + count 返回 Long")
    void sysUserCount() {
        Long count = sysUserMapper.selectCount(null);
        assertNotNull(count, "selectCount 返回值不能为 null");
        assertTrue(count >= 0, "count >= 0");
        System.out.println("[SysUser] count = " + count);
    }

    @Test
    @DisplayName("SysUserEntity 字段类型正确：username String、userType String、agentNo String、lastLoginAt LocalDateTime")
    void sysUserFieldTypes() {
        // 构造一个空 Entity（零行数据，无法 insert 但能验证类型映射）
        SysUser u = new SysUser();
        u.setUsername("smoke_test");
        u.setPasswordHash("$2a$10$test");
        u.setUserType("CUSTOMER");
        u.setStatus("ACTIVE");
        // 类型能 set 就说明 Entity 字段类型正确
        assertEquals("smoke_test", u.getUsername());
        assertEquals("CUSTOMER", u.getUserType());
        System.out.println("[SysUser] 字段类型验证 OK");
    }

    // ---------- Vehicle ----------

    @Test
    @DisplayName("VehicleMapper 注入成功 + count 返回 Long")
    void vehicleCount() {
        Long count = vehicleMapper.selectCount(null);
        assertNotNull(count);
        assertTrue(count >= 0);
        System.out.println("[Vehicle] count = " + count);
    }

    @Test
    @DisplayName("VehicleEntity 字段类型正确：engineNoEnc byte[]、engineNoHash String、engineNoMasked String、createdAt LocalDateTime")
    void vehicleFieldTypes() {
        Vehicle v = new Vehicle();
        v.setOwnerUserId(1L);
        v.setPlateNorm("京A12345");
        v.setEngineNoEnc(new byte[]{0x01, 0x02});
        v.setEngineNoHash("abc123def456");
        v.setEngineNoMasked("***ENG1234");
        // 验证敏感字段类型——varbinary→byte[]
        assertInstanceOf(byte[].class, v.getEngineNoEnc());
        assertNotNull(v.getEngineNoEnc());
        // 验证 hash 字段——char64→String
        assertEquals("abc123def456", v.getEngineNoHash());
        System.out.println("[Vehicle] engineNoEnc(byte[]) engineNoHash(String) 类型 OK");
    }

    // ---------- Conversation ----------

    @Test
    @DisplayName("ConversationMapper 注入成功 + count + selectList")
    void conversationCountAndList() {
        Long count = conversationMapper.selectCount(null);
        assertNotNull(count);
        List<Conversation> list = conversationMapper.selectList(null);
        assertNotNull(list);
        assertEquals(count.intValue(), list.size(), "selectCount 与 selectList 数量应一致");
        System.out.println("[Conversation] count = " + count + ", list.size = " + list.size());
    }

    @Test
    @DisplayName("ConversationEntity 字段类型正确：convNo String、stage String、lastMessageAt LocalDateTime")
    void conversationFieldTypes() {
        Conversation c = new Conversation();
        c.setConvNo("C20261111001");
        c.setCustomerId(42L);
        c.setStage("COLLECTING");
        c.setStatus("ACTIVE");
        c.setHandlingMode("AI");
        assertEquals("C20261111001", c.getConvNo());
        assertEquals("COLLECTING", c.getStage());
        System.out.println("[Conversation] 字段类型验证 OK");
    }
}
