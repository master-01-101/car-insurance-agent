-- =============================================================
-- 车险出单智能体 · 第一版数据库（17 张核心表）
-- MySQL 8.0 / InnoDB / utf8mb4 / utf8mb4_0900_ai_ci
--
-- 约定：
--   1. 主键 BIGINT UNSIGNED AUTO_INCREMENT
--   2. 金额 DECIMAL(12,2)，单位「元」，未知金额存 NULL
--   3. 时间 DATETIME
--   4. 业务状态 VARCHAR + 应用层枚举
--   5. 逻辑外键：只建索引，不建 FOREIGN KEY（原因见 docs/数据库设计.md）
--   6. 只建表，不含 DROP / TRUNCATE / DELETE，可重复执行
-- =============================================================

CREATE DATABASE IF NOT EXISTS `car_insurance`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE `car_insurance`;

-- -------------------------------------------------------------
-- 1. 用户表：客户 / 业务员 / 管理员共用一套登录
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `phone`         VARCHAR(20)     NOT NULL                COMMENT '登录手机号，唯一',
  `password_hash` VARCHAR(100)    NOT NULL                COMMENT 'BCrypt 密码哈希，禁止明文',
  `user_type`     VARCHAR(20)     NOT NULL                COMMENT '用户类型：CUSTOMER/SALESMAN/ADMIN',
  `real_name`     VARCHAR(50)     NULL                    COMMENT '真实姓名',
  `email`         VARCHAR(100)    NULL                    COMMENT '邮箱',
  `avatar_url`    VARCHAR(500)    NULL                    COMMENT '头像地址',
  `status`        VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE-正常 DISABLED-禁用 LOCKED-锁定',
  `last_login_at` DATETIME        NULL                    COMMENT '最近登录时间',
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_user_phone` (`phone`),
  KEY `idx_sys_user_type_status` (`user_type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表（客户/业务员/管理员）';

-- -------------------------------------------------------------
-- 2. 车辆资料表
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `vehicle` (
  `id`                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `owner_user_id`       BIGINT UNSIGNED NOT NULL                COMMENT '车主 sys_user.id',
  `plate_no`            VARCHAR(16)     NOT NULL                COMMENT '车牌号',
  `vin`                 VARCHAR(32)     NULL                    COMMENT '车架号 VIN',
  `engine_no_cipher`    VARBINARY(512)  NULL                    COMMENT '发动机号密文（AES-GCM，敏感字段）',
  `engine_no_masked`    VARCHAR(32)     NULL                    COMMENT '发动机号脱敏展示值',
  `vehicle_type`        VARCHAR(32)     NULL                    COMMENT '车辆类型，如 小型轿车',
  `brand_model`         VARCHAR(100)    NULL                    COMMENT '品牌型号',
  `register_date`       DATE            NULL                    COMMENT '注册登记日期',
  `approved_passengers` SMALLINT        NULL                    COMMENT '核定载客人数',
  `usage_type`          VARCHAR(20)     NULL                    COMMENT '使用性质：NON_OPERATING-非营运 OPERATING-营运',
  `source`              VARCHAR(20)     NOT NULL DEFAULT 'MANUAL' COMMENT '来源：OCR/NEW/MANUAL',
  `status`              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/INACTIVE',
  `version`             INT             NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
  `created_at`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`             TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_vehicle_owner` (`owner_user_id`),
  KEY `idx_vehicle_plate` (`plate_no`),
  KEY `idx_vehicle_vin` (`vin`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车辆资料表';

-- -------------------------------------------------------------
-- 3. 会话表
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `conversation` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `customer_id`     BIGINT UNSIGNED NOT NULL                COMMENT '客户 sys_user.id',
  `salesman_id`     BIGINT UNSIGNED NULL                    COMMENT '业务员 sys_user.id，NULL 表示未分配',
  `vehicle_id`      BIGINT UNSIGNED NULL                    COMMENT '当前车辆 vehicle.id',
  `title`           VARCHAR(100)    NOT NULL                COMMENT '会话标题，默认车牌号',
  `stage`           VARCHAR(32)     NOT NULL DEFAULT 'COLLECTING_INFO' COMMENT '业务阶段：COLLECTING_INFO/RECOMMENDING/ID_UPLOAD/QUOTING/UNDERWRITING/PAYING/POLICY_ISSUED',
  `handling_mode`   VARCHAR(20)     NOT NULL DEFAULT 'AI'    COMMENT '处理方式：AI/HUMAN/AI_HUMAN',
  `status`          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE-进行中 HANDOFF-已转人工 CLOSED-已关闭',
  `version`         INT             NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
  `last_message_at` DATETIME        NULL                    COMMENT '最后一条消息时间',
  `closed_at`       DATETIME        NULL                    COMMENT '关闭时间',
  `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_conversation_customer` (`customer_id`, `last_message_at`),
  KEY `idx_conversation_salesman` (`salesman_id`),
  KEY `idx_conversation_vehicle` (`vehicle_id`),
  KEY `idx_conversation_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会话表';

-- -------------------------------------------------------------
-- 4. 聊天消息表
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `chat_message` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `conversation_id`   BIGINT UNSIGNED NOT NULL                COMMENT '会话 conversation.id',
  `seq`               INT             NOT NULL                COMMENT '会话内序号，从 1 递增',
  `sender_type`       VARCHAR(20)     NOT NULL                COMMENT '发送者类型：USER/ASSISTANT/HUMAN_AGENT/SYSTEM',
  `content_type`      VARCHAR(20)     NOT NULL DEFAULT 'TEXT' COMMENT '消息类型：TEXT/IMAGE/CARD/FILE',
  `content`           MEDIUMTEXT      NULL                    COMMENT '文本内容',
  `metadata`          JSON            NULL                    COMMENT '扩展结构：卡片、工具调用、引用等',
  `image_id`          BIGINT UNSIGNED NULL                    COMMENT '关联材料 insurance_image.id',
  `client_message_id` VARCHAR(64)     NULL                    COMMENT '前端幂等 ID，防重复发送',
  `request_id`        VARCHAR(64)     NULL                    COMMENT '请求追踪 ID',
  `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_chat_message_seq` (`conversation_id`, `seq`),
  UNIQUE KEY `uk_chat_message_client` (`conversation_id`, `client_message_id`),
  KEY `idx_chat_message_conversation` (`conversation_id`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='聊天消息表';

-- -------------------------------------------------------------
-- 5. 保险订单表
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `insurance_order` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no`         VARCHAR(32)     NOT NULL                COMMENT '业务订单号，唯一',
  `customer_id`      BIGINT UNSIGNED NOT NULL                COMMENT '客户 sys_user.id',
  `salesman_id`      BIGINT UNSIGNED NULL                    COMMENT '经办业务员 sys_user.id',
  `vehicle_id`       BIGINT UNSIGNED NOT NULL                COMMENT '车辆 vehicle.id',
  `conversation_id`  BIGINT UNSIGNED NOT NULL                COMMENT '来源会话 conversation.id',
  `plan_id`          BIGINT UNSIGNED NOT NULL                COMMENT '已确认方案 insurance_plan.id',
  `plan_version`     INT             NOT NULL                COMMENT '已确认方案版本',
  `status`           VARCHAR(24)     NOT NULL DEFAULT 'CREATED' COMMENT '状态：CREATED/QUOTING/QUOTED/UNDERWRITING/UNDERWRITTEN/PENDING_PAYMENT/PAID/ISSUING/ISSUED/FAILED/CANCELLED',
  `version`          INT             NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
  `total_premium`    DECIMAL(12,2)   NULL                    COMMENT '总保费（元），未知 NULL',
  `payable_amount`   DECIMAL(12,2)   NULL                    COMMENT '应付金额（元），未知 NULL',
  `vehicle_snapshot` JSON            NOT NULL                COMMENT '下单时车辆资料快照',
  `party_snapshot`   JSON            NOT NULL                COMMENT '下单时投保人/被保人快照',
  `plan_snapshot`    JSON            NOT NULL                COMMENT '下单时方案及险种快照',
  `remark`           VARCHAR(500)    NULL                    COMMENT '备注',
  `submitted_at`     DATETIME        NULL                    COMMENT '用户明确发起报价的时间',
  `created_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_insurance_order_no` (`order_no`),
  KEY `idx_insurance_order_customer` (`customer_id`, `created_at`),
  KEY `idx_insurance_order_salesman` (`salesman_id`),
  KEY `idx_insurance_order_conversation` (`conversation_id`),
  KEY `idx_insurance_order_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='保险订单表';

-- -------------------------------------------------------------
-- 6. 会话人员资料表：投保人 / 被保人 / 车主
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `conversation_party` (
  `id`                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `conversation_id`       BIGINT UNSIGNED NOT NULL                COMMENT '会话 conversation.id',
  `party_role`            VARCHAR(20)     NOT NULL                COMMENT '角色：APPLICANT-投保人 INSURED-被保人 VEHICLE_OWNER-车主',
  `name`                  VARCHAR(50)     NOT NULL                COMMENT '姓名',
  `id_type`               VARCHAR(20)     NOT NULL DEFAULT 'ID_CARD' COMMENT '证件类型：ID_CARD/PASSPORT/OTHER',
  `id_no_cipher`          VARBINARY(512)  NULL                    COMMENT '证件号密文（AES-GCM）',
  `id_no_masked`          VARCHAR(32)     NULL                    COMMENT '证件号脱敏值',
  `id_no_hash`            CHAR(64)        NULL                    COMMENT '证件号 HMAC-SHA256，用于精确查询',
  `phone`                 VARCHAR(20)     NULL                    COMMENT '手机号',
  `gender`                VARCHAR(10)     NULL                    COMMENT '性别：MALE/FEMALE/UNKNOWN',
  `birth_date`            DATE            NULL                    COMMENT '出生日期',
  `relation_to_applicant` VARCHAR(20)     NULL                    COMMENT '与被保人关系：SELF/SPOUSE/CHILD/PARENT/OTHER',
  `image_id`              BIGINT UNSIGNED NULL                    COMMENT '证件材料 insurance_image.id',
  `version`               INT             NOT NULL DEFAULT 1      COMMENT '资料版本',
  `confirm_status`        VARCHAR(20)     NOT NULL DEFAULT 'UNCONFIRMED' COMMENT '确认状态：UNCONFIRMED/CONFIRMED/REJECTED',
  `confirmed_at`          DATETIME        NULL                    COMMENT '确认时间',
  `created_at`            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_conversation_party_version` (`conversation_id`, `party_role`, `version`),
  KEY `idx_conversation_party_conversation` (`conversation_id`),
  KEY `idx_conversation_party_hash` (`id_no_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会话人员资料表（投保人/被保人/车主）';

-- -------------------------------------------------------------
-- 7. 上传材料表：只存地址和元信息
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `insurance_image` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `conversation_id`  BIGINT UNSIGNED NOT NULL                COMMENT '会话 conversation.id',
  `uploader_user_id` BIGINT UNSIGNED NOT NULL                COMMENT '上传人 sys_user.id',
  `image_type`       VARCHAR(40)     NOT NULL                COMMENT '材料类型：DRIVING_LICENSE_MAIN/DRIVING_LICENSE_SUB/ID_CARD_FRONT/ID_CARD_BACK/VEHICLE_PHOTO/OTHER',
  `file_url`         VARCHAR(500)    NOT NULL                COMMENT '文件访问地址或对象 key，不存二进制',
  `file_name`        VARCHAR(255)    NULL                    COMMENT '原始文件名',
  `mime_type`        VARCHAR(100)    NULL                    COMMENT 'MIME 类型',
  `file_size`        BIGINT UNSIGNED NULL                    COMMENT '文件字节数',
  `status`           VARCHAR(20)     NOT NULL DEFAULT 'UPLOADED' COMMENT '状态：UPLOADED/PROCESSING/PARSED/CONFIRMED/REJECTED',
  `remark`           VARCHAR(500)    NULL                    COMMENT '备注',
  `created_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_insurance_image_conversation` (`conversation_id`, `image_type`),
  KEY `idx_insurance_image_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='上传材料表（只存元信息）';

-- -------------------------------------------------------------
-- 8. OCR 识别记录表：同一文件可多次识别
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ocr_record` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `image_id`          BIGINT UNSIGNED NOT NULL                COMMENT '材料 insurance_image.id',
  `provider`          VARCHAR(32)     NOT NULL                COMMENT 'OCR 服务商，当前可为 MOCK',
  `ocr_type`          VARCHAR(40)     NOT NULL                COMMENT '识别类型：DRIVING_LICENSE/ID_CARD/VEHICLE_VIN',
  `status`            VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/SUCCESS/FAILED',
  `structured_result` JSON            NULL                    COMMENT '结构化识别结果',
  `confidence`        DECIMAL(5,4)    NULL                    COMMENT '整体置信度 0~1',
  `error_message`     VARCHAR(500)    NULL                    COMMENT '失败原因',
  `recognized_at`     DATETIME        NULL                    COMMENT '识别完成时间',
  `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_ocr_record_image` (`image_id`, `created_at`),
  KEY `idx_ocr_record_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='OCR 识别记录表';

-- -------------------------------------------------------------
-- 9. 保险方案表：版本化，不覆盖历史
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `insurance_plan` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `conversation_id`  BIGINT UNSIGNED NOT NULL                COMMENT '会话 conversation.id',
  `order_id`         BIGINT UNSIGNED NULL                    COMMENT '关联订单 insurance_order.id，NULL 表示未下单',
  `previous_plan_id` BIGINT UNSIGNED NULL                    COMMENT '上一版本方案 id，首版 NULL',
  `version`          INT             NOT NULL DEFAULT 1      COMMENT '方案版本号',
  `insurer_code`     VARCHAR(32)     NULL                    COMMENT '保险公司编码',
  `insurer_name`     VARCHAR(100)    NULL                    COMMENT '保险公司名称',
  `source`           VARCHAR(20)     NOT NULL DEFAULT 'AI'   COMMENT '来源：AI/MANUAL/TEMPLATE',
  `status`           VARCHAR(20)     NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/PENDING_CONFIRM/CONFIRMED/REJECTED/SUPERSEDED',
  `estimated_total`  DECIMAL(12,2)   NULL                    COMMENT '预估总保费（元），未知 NULL',
  `price_source`     VARCHAR(20)     NOT NULL DEFAULT 'ESTIMATE' COMMENT '价格来源：ESTIMATE-预估 QUOTE-真实报价 MANUAL-手工',
  `confirmed_at`     DATETIME        NULL                    COMMENT '确认时间',
  `confirmed_by`     BIGINT UNSIGNED NULL                    COMMENT '确认人 sys_user.id',
  `created_by`       BIGINT UNSIGNED NULL                    COMMENT '创建人 sys_user.id',
  `remark`           VARCHAR(500)    NULL                    COMMENT '备注',
  `created_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_insurance_plan_version` (`conversation_id`, `version`),
  KEY `idx_insurance_plan_order` (`order_id`),
  KEY `idx_insurance_plan_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='保险方案表（版本化）';

-- -------------------------------------------------------------
-- 10. 方案险种明细表
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `insurance_plan_item` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `plan_id`           BIGINT UNSIGNED NOT NULL                COMMENT '方案 insurance_plan.id',
  `coverage_code`     VARCHAR(32)     NOT NULL                COMMENT '险种编码，如 COMPULSORY/THIRD_PARTY/VEHICLE_DAMAGE',
  `coverage_name`     VARCHAR(64)     NOT NULL                COMMENT '险种名称',
  `selected`          TINYINT         NOT NULL DEFAULT 1      COMMENT '是否选择：1-是 0-否',
  `sum_insured`       DECIMAL(14,2)   NULL                    COMMENT '保额（元），无保额时 NULL',
  `estimated_premium` DECIMAL(12,2)   NULL                    COMMENT '预估保费（元），未知 NULL',
  `sort_order`        INT             NOT NULL DEFAULT 0      COMMENT '展示排序',
  `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_insurance_plan_item` (`plan_id`, `coverage_code`),
  KEY `idx_insurance_plan_item_plan` (`plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='方案险种明细表';

-- -------------------------------------------------------------
-- 11. 报价记录表：同一订单可多次报价
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `quote_record` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id`         BIGINT UNSIGNED NOT NULL                COMMENT '订单 insurance_order.id',
  `quote_no`         VARCHAR(64)     NOT NULL                COMMENT '报价编号，唯一',
  `insurer_code`     VARCHAR(32)     NULL                    COMMENT '保险公司编码',
  `insurer_name`     VARCHAR(100)    NULL                    COMMENT '保险公司名称',
  `status`           VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/SUCCESS/FAILED/EXPIRED',
  `total_premium`    DECIMAL(12,2)   NULL                    COMMENT '总保费（元），未知 NULL',
  `payable_amount`   DECIMAL(12,2)   NULL                    COMMENT '应付金额（元）',
  `valid_until`      DATETIME        NULL                    COMMENT '报价失效时间',
  `is_mock`          TINYINT         NOT NULL DEFAULT 1      COMMENT '是否模拟数据：1-模拟 0-真实',
  `provider`         VARCHAR(32)     NOT NULL DEFAULT 'MOCK' COMMENT '报价渠道',
  `response_summary` JSON            NULL                    COMMENT '第三方响应摘要（脱敏）',
  `error_message`    VARCHAR(500)    NULL                    COMMENT '失败原因',
  `requested_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '请求时间',
  `completed_at`     DATETIME        NULL                    COMMENT '完成时间',
  `created_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_quote_record_no` (`quote_no`),
  KEY `idx_quote_record_order` (`order_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='报价记录表';

-- -------------------------------------------------------------
-- 12. 报价明细表
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `quote_item` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `quote_id`       BIGINT UNSIGNED NOT NULL                COMMENT '报价 quote_record.id',
  `coverage_code`  VARCHAR(32)     NOT NULL                COMMENT '险种编码',
  `coverage_name`  VARCHAR(64)     NOT NULL                COMMENT '险种名称',
  `sum_insured`    DECIMAL(14,2)   NULL                    COMMENT '保额（元）',
  `premium`        DECIMAL(12,2)   NULL                    COMMENT '保费（元）',
  `sort_order`     INT             NOT NULL DEFAULT 0      COMMENT '展示排序',
  `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_quote_item_quote` (`quote_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='报价险种明细表';

-- -------------------------------------------------------------
-- 13. 核保记录表
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `underwriting_record` (
  `id`                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id`           BIGINT UNSIGNED NOT NULL                COMMENT '订单 insurance_order.id',
  `quote_id`           BIGINT UNSIGNED NOT NULL                COMMENT '报价 quote_record.id',
  `underwriting_no`    VARCHAR(64)     NOT NULL                COMMENT '核保编号，唯一',
  `status`             VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/PASSED/FAILED/NEED_MATERIAL',
  `fail_reason`        VARCHAR(500)    NULL                    COMMENT '失败原因',
  `need_material`      TINYINT         NOT NULL DEFAULT 0      COMMENT '是否需补件：1-是 0-否',
  `required_materials` JSON            NULL                    COMMENT '补件要求列表',
  `is_mock`            TINYINT         NOT NULL DEFAULT 1      COMMENT '是否模拟数据：1-模拟 0-真实',
  `provider`           VARCHAR(32)     NOT NULL DEFAULT 'MOCK' COMMENT '核保渠道',
  `submitted_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `completed_at`       DATETIME        NULL                    COMMENT '完成时间',
  `created_at`         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_underwriting_record_no` (`underwriting_no`),
  KEY `idx_underwriting_record_order` (`order_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='核保记录表';

-- -------------------------------------------------------------
-- 14. 支付记录表
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `payment_record` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `payment_no`        VARCHAR(64)     NOT NULL                COMMENT '支付编号，唯一',
  `order_id`          BIGINT UNSIGNED NOT NULL                COMMENT '订单 insurance_order.id',
  `amount`            DECIMAL(12,2)   NOT NULL                COMMENT '支付金额（元）',
  `channel`           VARCHAR(32)     NOT NULL                COMMENT '支付渠道：MOCK/ALIPAY/WECHAT',
  `provider`          VARCHAR(32)     NOT NULL DEFAULT 'MOCK' COMMENT '支付平台',
  `external_trade_no` VARCHAR(128)    NULL                    COMMENT '第三方交易流水号',
  `status`            VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/SUCCESS/FAILED/CLOSED/REFUNDED',
  `paid_at`           DATETIME        NULL                    COMMENT '支付成功时间',
  `verified_at`       DATETIME        NULL                    COMMENT '后端查单核验时间',
  `callback_payload`  JSON            NULL                    COMMENT '回调原文（脱敏）',
  `is_mock`           TINYINT         NOT NULL DEFAULT 1      COMMENT '是否模拟数据：1-模拟 0-真实',
  `error_message`     VARCHAR(500)    NULL                    COMMENT '失败原因',
  `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_record_no` (`payment_no`),
  KEY `idx_payment_record_order` (`order_id`, `status`),
  KEY `idx_payment_record_external` (`external_trade_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付记录表';

-- -------------------------------------------------------------
-- 15. 保单表：一笔订单可对应多张保单
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `policy` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id`         BIGINT UNSIGNED NOT NULL                COMMENT '订单 insurance_order.id',
  `policy_no`        VARCHAR(64)     NOT NULL                COMMENT '正式保单号，唯一',
  `insurer_code`     VARCHAR(32)     NULL                    COMMENT '保险公司编码',
  `insurer_name`     VARCHAR(100)    NULL                    COMMENT '保险公司名称',
  `status`           VARCHAR(20)     NOT NULL DEFAULT 'ISSUING' COMMENT '状态：ISSUING/ISSUED/CANCELLED/EXPIRED',
  `effective_from`   DATETIME        NOT NULL                COMMENT '保障开始时间',
  `effective_to`     DATETIME        NOT NULL                COMMENT '保障结束时间',
  `total_premium`    DECIMAL(12,2)   NULL                    COMMENT '保单总保费（元）',
  `vehicle_snapshot` JSON            NOT NULL                COMMENT '出单时车辆快照',
  `party_snapshot`   JSON            NOT NULL                COMMENT '出单时投保人/被保人快照',
  `details`          JSON            NULL                    COMMENT '保单详情（特别约定、险种明细等）',
  `is_mock`          TINYINT         NOT NULL DEFAULT 1      COMMENT '是否模拟数据：1-模拟 0-真实',
  `issued_at`        DATETIME        NULL                    COMMENT '出单时间',
  `created_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_policy_no` (`policy_no`),
  KEY `idx_policy_order` (`order_id`),
  KEY `idx_policy_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='保单表';

-- -------------------------------------------------------------
-- 16. 转人工任务表
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `handoff_task` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_no`         VARCHAR(32)     NOT NULL                COMMENT '任务编号，唯一',
  `conversation_id` BIGINT UNSIGNED NOT NULL                COMMENT '会话 conversation.id',
  `order_id`        BIGINT UNSIGNED NULL                    COMMENT '关联订单 insurance_order.id，可空',
  `customer_id`     BIGINT UNSIGNED NOT NULL                COMMENT '客户 sys_user.id',
  `reason`          VARCHAR(64)     NOT NULL                COMMENT '转人工原因：OCR_MISMATCH/CUSTOMER_REQUEST/ABNORMAL',
  `description`     VARCHAR(500)    NULL                    COMMENT '问题描述',
  `status`          VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/ASSIGNED/PROCESSING/RESOLVED/CANCELLED',
  `handler_id`      BIGINT UNSIGNED NULL                    COMMENT '处理人（业务员）sys_user.id',
  `result`          VARCHAR(1000)   NULL                    COMMENT '处理结果',
  `version`         INT             NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
  `requested_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '转人工时间',
  `accepted_at`     DATETIME        NULL                    COMMENT '领取时间',
  `resolved_at`     DATETIME        NULL                    COMMENT '解决时间',
  `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_handoff_task_no` (`task_no`),
  KEY `idx_handoff_task_conversation` (`conversation_id`),
  KEY `idx_handoff_task_status` (`status`, `requested_at`),
  KEY `idx_handoff_task_handler` (`handler_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='转人工任务表';

-- -------------------------------------------------------------
-- 17. 订单状态变更日志表：只追加
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `order_status_log` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id`      BIGINT UNSIGNED NOT NULL                COMMENT '订单 insurance_order.id',
  `from_status`   VARCHAR(24)     NULL                    COMMENT '变更前状态，创建时 NULL',
  `to_status`     VARCHAR(24)     NOT NULL                COMMENT '变更后状态',
  `operator_id`   BIGINT UNSIGNED NULL                    COMMENT '操作人 sys_user.id，系统为 NULL',
  `operator_type` VARCHAR(20)     NOT NULL DEFAULT 'SYSTEM' COMMENT '操作者类型：CUSTOMER/SALESMAN/ADMIN/SYSTEM/AI',
  `remark`        VARCHAR(500)    NULL                    COMMENT '备注',
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_status_log_order` (`order_id`, `id`),
  KEY `idx_order_status_log_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单状态变更日志表';
