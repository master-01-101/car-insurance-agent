-- =============================================================
-- 数据库迁移脚本：修复 P1 / P2 两个问题
-- 执行目标：3306（本机 MySQL）、3307（Docker MySQL）
-- 数据库：car_insurance
-- 创建时间：2026-11-11
--
-- 前置检查结果（已在两个实例上验证）：
--   * vehicle        — 当前 0 行数据，缺 engine_no_hash 字段
--   * sys_user       — 当前 0 行数据，agent_no 是普通索引，需改 UNIQUE
--   * 两个实例结构与 init.sql 完全一致
--   * 无任何重复/空数据风险（零行）
--
-- 执行方式：逐段执行，每段独立，任一步失败可单独回滚
-- =============================================================

USE `car_insurance`;

-- =============================================================
-- P1：vehicle 表新增 engine_no_hash 字段
-- 设计依据：conversation_party.id_no_hash 已用 CHAR(64) + 普通索引
-- 后续在 Java 后端 CryptoUtil 中用 HMAC-SHA256 计算（密钥 PII_HMAC_KEY）
-- 空库无历史数据回填需求
-- =============================================================

-- 步骤 P1-1：新增 engine_no_hash 列
-- 类型 CHAR(64) 与 conversation_party.id_no_hash 对齐
-- NULL 允许（engine_no_enc 本身允许 NULL）
-- 不设默认值：空值保持 NULL，业务层在写入 engine_no_enc 时同步计算 hash
ALTER TABLE `vehicle`
    ADD COLUMN `engine_no_hash` CHAR(64) NULL
        COMMENT '发动机号 HMAC-SHA256 哈希，用于查重/查询（密钥 PII_HMAC_KEY）'
        AFTER `engine_no_masked`;

-- 步骤 P1-2：新增普通索引
-- 只建普通索引，不建 UNIQUE（业务上不同车辆可能使用相同发动机号？此处按"不强制唯一"处理，等业务方确认后再决定是否升级为 UNIQUE）
ALTER TABLE `vehicle`
    ADD KEY `idx_vehicle_engine_no_hash` (`engine_no_hash`);

-- =============================================================
-- P2：sys_user.agent_no 从普通索引升级为 UNIQUE
-- 先 DROP 旧的普通索引，再建 UNIQUE
-- 两个实例当前都是空库，agent_no 全为 NULL，唯一约束对 NULL 不生效（MySQL 的 UNIQUE 允许多个 NULL）
-- 所以直接改，不会有冲突
-- =============================================================

-- 步骤 P2-1：先 DROP 掉旧的普通索引（Non_unique=1 的 idx_sys_user_agent_no）
ALTER TABLE `sys_user`
    DROP INDEX `idx_sys_user_agent_no`;

-- 步骤 P2-2：建新的 UNIQUE 索引
-- MySQL 的 UNIQUE 对 NULL 值不强制唯一（允许多行 agent_no = NULL）
-- 对空字符串 '' 会强制唯一（如果有人写入 '' 会触发冲突）
-- 空库当前无数据，安全
ALTER TABLE `sys_user`
    ADD UNIQUE KEY `uk_sys_user_agent_no` (`agent_no`);

-- =============================================================
-- 验证 SQL（执行完上述 ALTER 后运行，确认生效）
-- =============================================================

-- 验证 P1-1：engine_no_hash 列存在
SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, IS_NULLABLE, COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'vehicle' AND COLUMN_NAME = 'engine_no_hash';

-- 验证 P1-2：索引存在
SELECT INDEX_NAME, NON_UNIQUE, COLUMN_NAME, SEQ_IN_INDEX
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'vehicle' AND INDEX_NAME = 'idx_vehicle_engine_no_hash';

-- 验证 P2-1：旧普通索引已删除、新 UNIQUE 已存在
SELECT INDEX_NAME, NON_UNIQUE, COLUMN_NAME
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'agent_no';
-- 期望结果：只剩 uk_sys_user_agent_no，NON_UNIQUE=0

-- 验证 P2-2：agent_no 唯一约束功能测试（预期第二行失败）
INSERT INTO `sys_user` (username, password_hash, user_type, agent_no, status)
VALUES ('_migrate_test_a', '$2a$10$test', 'SALESMAN', 'MIGRATE001', 'ACTIVE');
INSERT INTO `sys_user` (username, password_hash, user_type, agent_no, status)
VALUES ('_migrate_test_b', '$2a$10$test', 'SALESMAN', 'MIGRATE001', 'ACTIVE');
-- 第二行预期报错：Duplicate entry 'MIGRATE001' for key 'uk_sys_user_agent_no'

-- 清理测试数据
DELETE FROM `sys_user` WHERE username IN ('_migrate_test_a', '_migrate_test_b');

-- =============================================================
-- 回滚 SQL（仅在执行失败需要回退时使用）
-- 顺序：先 P2 回滚，再 P1 回滚
-- =============================================================

-- P2 回滚：DROP UNIQUE，重建普通索引
-- ALTER TABLE `sys_user` DROP INDEX `uk_sys_user_agent_no`;
-- ALTER TABLE `sys_user` ADD KEY `idx_sys_user_agent_no` (`agent_no`);

-- P1 回滚：DROP 索引，再 DROP 列
-- ALTER TABLE `vehicle` DROP INDEX `idx_vehicle_engine_no_hash`;
-- ALTER TABLE `vehicle` DROP COLUMN `engine_no_hash`;
