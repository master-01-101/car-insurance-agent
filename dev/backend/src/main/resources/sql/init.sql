-- =============================================================
-- 车险出单智能体 · 整合版数据库（31 张表）
-- MySQL 8.0 / InnoDB / utf8mb4 / utf8mb4_0900_ai_ci
--
-- 来源整合：
--   1) docs/前端-数据库设计总结.md（27 张候选表）
--   2) docs/AI_Java_API_requirements.md（AI → Java 契约）
--   3) 已归档的 17 表后端设计（docs/archive/数据库设计_17表_v1.md）
--
-- 统一约定：
--   * 主键 BIGINT UNSIGNED AUTO_INCREMENT
--   * 金额 DECIMAL(12,2)（元）；保额 DECIMAL(14,2)；未知金额为 NULL
--   * 时间 DATETIME；状态 VARCHAR（不用 ENUM）
--   * 逻辑外键：只建索引，不建 FOREIGN KEY
--   * 文件只存 object_key，不存二进制
--   * 模拟数据用 data_source='DEMO' 明确标记，不伪装真实报价/支付/保单
--   * 脚本只建表（IF NOT EXISTS），不含 DROP / TRUNCATE / DELETE
-- =============================================================

CREATE DATABASE IF NOT EXISTS `car_insurance`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE `car_insurance`;

-- -------------------------------------------------------------
-- 1. 用户表（合并前端 account）
-- 客户 / 业务员 / 管理员共用；业务员归属用 parent_agent_id 表达
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`        VARCHAR(50)     NOT NULL                COMMENT '登录名，唯一',
  `phone`           VARCHAR(20)     NULL                    COMMENT '手机号',
  `password_hash`   VARCHAR(100)    NOT NULL                COMMENT 'BCrypt 密码哈希',
  `user_type`       VARCHAR(20)     NOT NULL                COMMENT '角色：CUSTOMER/SALESMAN/ADMIN',
  `agent_no`        VARCHAR(32)     NULL                    COMMENT '业务员工号，仅 SALESMAN',
  `org_name`        VARCHAR(100)    NULL                    COMMENT '所属机构/团队',
  `parent_agent_id` BIGINT UNSIGNED NULL                    COMMENT '客户归属业务员 sys_user.id',
  `real_name`       VARCHAR(50)     NULL                    COMMENT '真实姓名',
  `email`           VARCHAR(100)    NULL                    COMMENT '邮箱',
  `avatar_url`      VARCHAR(500)    NULL                    COMMENT '头像地址',
  `status`          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/DISABLED/LOCKED',
  `last_login_at`   DATETIME        NULL                    COMMENT '最近登录时间',
  `version`         INT             NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
  `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`         TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除：0-否 1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_user_username` (`username`),
  KEY `idx_sys_user_phone` (`phone`),
  KEY `idx_sys_user_type` (`user_type`, `status`),
  KEY `idx_sys_user_parent_agent` (`parent_agent_id`),
  KEY `idx_sys_user_agent_no` (`agent_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表（客户/业务员/管理员）';

-- -------------------------------------------------------------
-- 2. 车辆主数据（当前可信值）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `vehicle` (
  `id`                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `owner_user_id`       BIGINT UNSIGNED NOT NULL                COMMENT '车主 sys_user.id',
  `plate_number`        VARCHAR(16)     NULL                    COMMENT '车牌号（原始展示值）',
  `plate_norm`          VARCHAR(16)     NULL                    COMMENT '归一化车牌（去空格/点），唯一',
  `vin`                 VARCHAR(32)     NULL                    COMMENT '车架号 VIN，唯一',
  `engine_no_enc`       VARBINARY(512)  NULL                    COMMENT '发动机号密文 AES-256-GCM',
  `engine_no_masked`    VARCHAR(32)     NULL                    COMMENT '发动机号脱敏值',
  `owner_name_masked`   VARCHAR(50)     NULL                    COMMENT '车主姓名脱敏值',
  `vehicle_type`        VARCHAR(32)     NULL                    COMMENT '车辆类型',
  `brand_model`         VARCHAR(100)    NULL                    COMMENT '品牌型号',
  `use_nature`          VARCHAR(20)     NULL                    COMMENT '使用性质：NON_OPERATING/OPERATING',
  `register_date`       DATE            NULL                    COMMENT '注册登记日期',
  `issue_date`          DATE            NULL                    COMMENT '发证日期',
  `approved_passengers` SMALLINT        NULL                    COMMENT '核定载客人数',
  `source`              VARCHAR(20)     NOT NULL DEFAULT 'MANUAL' COMMENT '来源：OCR/MANUAL/IMPORT',
  `status`              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/INACTIVE',
  `version`             INT             NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
  `created_at`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`             TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除：0-否 1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_vehicle_plate_norm` (`plate_norm`),
  UNIQUE KEY `uk_vehicle_vin` (`vin`),
  KEY `idx_vehicle_owner` (`owner_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车辆主数据（当前可信值）';

-- -------------------------------------------------------------
-- 3. 车辆资料确认快照（合并前端 vehicle_profile）
-- 用户每次确认产生一条，保留修改留痕
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `vehicle_profile` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `vehicle_id`       BIGINT UNSIGNED NOT NULL                COMMENT '车辆 vehicle.id',
  `conversation_id`  BIGINT UNSIGNED NULL                    COMMENT '来源会话 conversation.id',
  `version`          INT             NOT NULL DEFAULT 1      COMMENT '资料版本号',
  `fields_json`      JSON            NOT NULL                COMMENT '本次确认的车辆字段快照',
  `corrected_fields` JSON            NULL                    COMMENT '用户修正过的字段及前后值',
  `source`           VARCHAR(20)     NOT NULL DEFAULT 'OCR'  COMMENT '来源：OCR/MANUAL',
  `ocr_record_id`    BIGINT UNSIGNED NULL                    COMMENT '依据的 ocr_record.id',
  `status`           VARCHAR(20)     NOT NULL DEFAULT 'CONFIRMED' COMMENT '状态：DRAFT/CONFIRMED/SUPERSEDED',
  `confirmed_by`     BIGINT UNSIGNED NULL                    COMMENT '确认人 sys_user.id',
  `confirmed_at`     DATETIME        NULL                    COMMENT '确认时间',
  `created_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_vehicle_profile_version` (`vehicle_id`, `version`),
  KEY `idx_vehicle_profile_conversation` (`conversation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车辆资料确认快照与修改留痕';

-- -------------------------------------------------------------
-- 4. 会话表（一个会话对应一个车牌）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `conversation` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `conv_no`         VARCHAR(32)     NOT NULL                COMMENT '会话编号，唯一',
  `customer_id`     BIGINT UNSIGNED NOT NULL                COMMENT '客户 sys_user.id',
  `salesman_id`     BIGINT UNSIGNED NULL                    COMMENT '业务员 sys_user.id',
  `vehicle_id`      BIGINT UNSIGNED NULL                    COMMENT '车辆 vehicle.id',
  `plate_number`    VARCHAR(16)     NULL                    COMMENT '会话绑定车牌（标题取此值）',
  `title`           VARCHAR(100)    NOT NULL                COMMENT '会话标题，默认车牌号',
  `stage`           VARCHAR(32)     NOT NULL DEFAULT 'COLLECTING' COMMENT '业务阶段，见状态机',
  `handling_mode`   VARCHAR(20)     NOT NULL DEFAULT 'AI'   COMMENT '处理方式：AI/HUMAN',
  `status`          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/HANDOFF/CLOSED/ABORTED',
  `version`         INT             NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
  `last_message_at` DATETIME        NULL                    COMMENT '最后一条消息时间',
  `message_count`   INT             NOT NULL DEFAULT 0      COMMENT '消息数量（冗余计数，便于列表展示）',
  `closed_at`       DATETIME        NULL                    COMMENT '关闭时间',
  `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_conversation_no` (`conv_no`),
  KEY `idx_conversation_customer` (`customer_id`, `last_message_at`),
  KEY `idx_conversation_salesman` (`salesman_id`),
  KEY `idx_conversation_vehicle` (`vehicle_id`),
  KEY `idx_conversation_status` (`status`, `stage`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会话表（一个会话一个车牌）';

-- -------------------------------------------------------------
-- 5. 聊天消息表（合并前端 message）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `chat_message` (
  `id`                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `msg_no`             VARCHAR(40)     NOT NULL                COMMENT '消息编号，唯一',
  `conversation_id`    BIGINT UNSIGNED NOT NULL                COMMENT '会话 conversation.id',
  `seq`                INT             NOT NULL                COMMENT '会话内序号，从 1 递增',
  `sender_type`        VARCHAR(20)     NOT NULL                COMMENT '发送者：USER/ASSISTANT/HUMAN_AGENT/SYSTEM',
  `message_type`       VARCHAR(20)     NOT NULL DEFAULT 'TEXT' COMMENT '消息类型：TEXT/IMAGE/CARD/STATUS/HANDOFF',
  `content`            MEDIUMTEXT      NULL                    COMMENT '文本内容',
  `payload_json`       JSON            NULL                    COMMENT '卡片/动作/引用等结构',
  `request_id`         VARCHAR(64)     NULL                    COMMENT '涉及的业务请求 ID',
  `reply_to_message_id` BIGINT UNSIGNED NULL                   COMMENT '回复的消息 id',
  `related_plan_id`    BIGINT UNSIGNED NULL                    COMMENT '关联方案 insurance_plan.id',
  `status`             VARCHAR(20)     NOT NULL DEFAULT 'COMPLETED' COMMENT '处理状态：PROCESSING/COMPLETED/FAILED',
  `client_msg_id`      VARCHAR(64)     NULL                    COMMENT '前端幂等 ID',
  `created_at`         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_chat_message_no` (`msg_no`),
  UNIQUE KEY `uk_chat_message_seq` (`conversation_id`, `seq`),
  UNIQUE KEY `uk_chat_message_client` (`conversation_id`, `client_msg_id`),
  KEY `idx_chat_message_conversation` (`conversation_id`, `id`),
  KEY `idx_chat_message_request` (`request_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='聊天消息表';

-- -------------------------------------------------------------
-- 6. 消息与文件关联表（一条消息可带多份材料）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `message_attachment` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `message_id` BIGINT UNSIGNED NOT NULL                COMMENT '消息 chat_message.id',
  `file_id`    BIGINT UNSIGNED NOT NULL                COMMENT '文件 file_object.id',
  `category`   VARCHAR(40)     NULL                    COMMENT '用途：LICENSE/ID_CARD/INSPECTION/CHAT_IMAGE',
  `sort_no`    INT             NOT NULL DEFAULT 0      COMMENT '排序',
  `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_message_attachment` (`message_id`, `file_id`),
  KEY `idx_message_attachment_file` (`file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息与文件关联表';

-- -------------------------------------------------------------
-- 7. 统一文件元数据表（替代 insurance_image）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `file_object` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `file_no`          VARCHAR(40)     NOT NULL                COMMENT '文件业务编号（接口 fileId），唯一',
  `conversation_id`  BIGINT UNSIGNED NULL                    COMMENT '所属会话 conversation.id',
  `uploader_user_id` BIGINT UNSIGNED NOT NULL                COMMENT '上传人 sys_user.id',
  `category`         VARCHAR(40)     NOT NULL                COMMENT '类型：LICENSE_FRONT/LICENSE_BACK/ID_FRONT/ID_BACK/INSPECTION_PHOTO/INSPECTION_VIDEO/CHAT_IMAGE/OTHER',
  `party_role`       VARCHAR(20)     NULL                    COMMENT '证件归属角色：APPLICANT/INSURED/VEHICLE_OWNER',
  `object_key`       VARCHAR(500)    NOT NULL                COMMENT '对象存储 key，不存二进制',
  `storage`          VARCHAR(16)     NOT NULL DEFAULT 'LOCAL' COMMENT '存储位置：LOCAL/OSS/COS',
  `bucket`           VARCHAR(100)    NULL                    COMMENT '对象存储 bucket 名称',
  `file_name`        VARCHAR(255)    NULL                    COMMENT '原始文件名',
  `mime_type`        VARCHAR(100)    NULL                    COMMENT 'MIME 类型',
  `size_bytes`       BIGINT UNSIGNED NULL                    COMMENT '文件大小（字节）',
  `width`            INT             NULL                    COMMENT '图片宽度（像素）',
  `height`           INT             NULL                    COMMENT '图片高度（像素）',
  `sha256`           CHAR(64)        NULL                    COMMENT '文件摘要，用于秒传/去重',
  `revision`         INT             NOT NULL DEFAULT 1      COMMENT '同一材料重传版本',
  `status`           VARCHAR(20)     NOT NULL DEFAULT 'UPLOADED' COMMENT '状态：UPLOADED/PROCESSING/PARSED/CONFIRMED/REJECTED',
  `created_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_file_object_no` (`file_no`),
  KEY `idx_file_object_conversation` (`conversation_id`, `category`),
  KEY `idx_file_object_sha256` (`sha256`),
  KEY `idx_file_object_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='统一文件元数据表（不存二进制）';

-- -------------------------------------------------------------
-- 8. OCR 识别记录表（同一文件允许多次识别）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ocr_record` (
  `id`                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `file_id`               BIGINT UNSIGNED NOT NULL                COMMENT '文件 file_object.id',
  `recognition_request_id` VARCHAR(64)    NOT NULL                COMMENT '识别请求 ID，与 file_id 联合唯一',
  `ocr_type`              VARCHAR(40)     NOT NULL                COMMENT '类型：VEHICLE_LICENSE/ID_CARD',
  `side`                  VARCHAR(20)     NULL                    COMMENT '面：MAIN/SUB/FRONT/BACK',
  `party_role`            VARCHAR(20)     NULL                    COMMENT '证件角色：APPLICANT/INSURED/VEHICLE_OWNER',
  `provider`              VARCHAR(32)     NOT NULL                COMMENT 'OCR 服务商，可为 MOCK',
  `status`                VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/SUCCEEDED/NEEDS_REVIEW/FAILED',
  `fields_json`           JSON            NULL                    COMMENT '归一化字段（含置信度/异常标记）',
  `raw_json`              JSON            NULL                    COMMENT '服务商原始返回',
  `confidence_avg`        DECIMAL(5,4)    NULL                    COMMENT '平均置信度 0~1',
  `missing_fields`        JSON            NULL                    COMMENT '缺失字段列表',
  `warnings`              JSON            NULL                    COMMENT '告警列表',
  `recognized_at`         DATETIME        NULL                    COMMENT '识别完成时间',
  `error_message`         VARCHAR(500)    NULL                    COMMENT '失败原因',
  `version`               INT             NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
  `created_at`            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ocr_record_request` (`file_id`, `recognition_request_id`),
  KEY `idx_ocr_record_file` (`file_id`, `created_at`),
  KEY `idx_ocr_record_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='OCR 识别记录表';

-- -------------------------------------------------------------
-- 9. 会话人员资料表（合并前端 applicant）
-- 投保人 / 被保人 / 车主可分别不同；版本化保存
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `conversation_party` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `conversation_id` BIGINT UNSIGNED NOT NULL                COMMENT '会话 conversation.id',
  `party_role`      VARCHAR(20)     NOT NULL                COMMENT '角色：APPLICANT/INSURED/VEHICLE_OWNER',
  `name`            VARCHAR(50)     NOT NULL                COMMENT '姓名',
  `id_type`         VARCHAR(20)     NOT NULL DEFAULT 'ID_CARD' COMMENT '证件类型：ID_CARD/PASSPORT/OTHER',
  `id_no_enc`       VARBINARY(512)  NULL                    COMMENT '证件号密文 AES-256-GCM',
  `id_no_hash`      CHAR(64)        NULL                    COMMENT '证件号 HMAC-SHA256，用于查重',
  `id_masked`       VARCHAR(32)     NULL                    COMMENT '证件号脱敏值',
  `valid_to`        DATE            NULL                    COMMENT '证件有效期',
  `phone`           VARCHAR(20)     NULL                    COMMENT '手机号',
  `gender`          VARCHAR(10)     NULL                    COMMENT '性别：MALE/FEMALE/UNKNOWN',
  `birth_date`      DATE            NULL                    COMMENT '出生日期',
  `relation_to_applicant` VARCHAR(20) NULL                  COMMENT '与被保人关系：SELF/SPOUSE/CHILD/PARENT/OTHER',
  `source_ocr_id`   BIGINT UNSIGNED NULL                    COMMENT '依据的 ocr_record.id',
  `version`         INT             NOT NULL DEFAULT 1      COMMENT '资料版本',
  `confirm_status`  VARCHAR(20)     NOT NULL DEFAULT 'UNCONFIRMED' COMMENT '确认状态：UNCONFIRMED/CONFIRMED/REJECTED',
  `confirmed_at`    DATETIME        NULL                    COMMENT '确认时间',
  `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_conversation_party_version` (`conversation_id`, `party_role`, `version`),
  KEY `idx_conversation_party_hash` (`id_no_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会话人员资料（投保人/被保人/车主）';

-- -------------------------------------------------------------
-- 10. 资料草稿表（AI J17：待确认版本，不直接写正式资料）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `profile_draft` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `draft_no`        VARCHAR(40)     NOT NULL                COMMENT '草稿编号，唯一',
  `conversation_id` BIGINT UNSIGNED NOT NULL                COMMENT '会话 conversation.id',
  `kind`            VARCHAR(30)     NOT NULL                COMMENT '类型：VEHICLE_PROFILE/PARTY_PROFILE',
  `party_role`      VARCHAR(20)     NULL                    COMMENT '人员角色，车辆草稿为空',
  `target_id`       BIGINT UNSIGNED NULL                    COMMENT '目标 ID（vehicle 或 conversation_party）',
  `draft_version`   INT             NOT NULL DEFAULT 1      COMMENT '草稿版本',
  `ocr_record_ids`  JSON            NULL                    COMMENT '依据的 OCR 记录 ID 列表',
  `display_fields`  JSON            NOT NULL                COMMENT '脱敏后的展示字段',
  `missing_fields`  JSON            NULL                    COMMENT '缺失字段',
  `abnormal_fields` JSON            NULL                    COMMENT '异常字段',
  `can_confirm`     TINYINT         NOT NULL DEFAULT 0      COMMENT '是否可确认：1-是 0-否',
  `status`          VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/CONFIRMED/EXPIRED/CANCELLED',
  `created_by`      BIGINT UNSIGNED NULL                    COMMENT '创建人/服务',
  `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_profile_draft_no` (`draft_no`),
  KEY `idx_profile_draft_conversation` (`conversation_id`, `kind`, `draft_version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资料草稿表（待用户确认）';

-- -------------------------------------------------------------
-- 11. 用户确认记录表（AI J18~J21：确认必须绑定目标版本）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `confirmation` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `conversation_id`  BIGINT UNSIGNED NOT NULL                COMMENT '会话 conversation.id',
  `kind`             VARCHAR(30)     NOT NULL                COMMENT '确认类型：PLAN/VEHICLE_PROFILE/PARTY_PROFILE',
  `target_id`        BIGINT UNSIGNED NOT NULL                COMMENT '确认目标 ID',
  `target_version`   INT             NOT NULL                COMMENT '确认目标版本',
  `status`           VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/CONFIRMED/EXPIRED/SUPERSEDED/CANCELLED',
  `display_data`     JSON            NOT NULL                COMMENT '展示数据（脱敏）',
  `display_digest`   CHAR(64)        NULL                    COMMENT '展示内容摘要，防篡改',
  `missing_fields`   JSON            NULL                    COMMENT '缺失字段',
  `abnormal_fields`  JSON            NULL                    COMMENT '异常字段',
  `token_hash`       CHAR(64)        NULL                    COMMENT '确认令牌哈希，仅存摘要',
  `expires_at`       DATETIME        NULL                    COMMENT '过期时间',
  `confirmed_by`     BIGINT UNSIGNED NULL                    COMMENT '确认人 sys_user.id',
  `confirmed_at`     DATETIME        NULL                    COMMENT '确认时间',
  `version`          INT             NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
  `created_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_confirmation_conversation` (`conversation_id`, `status`),
  KEY `idx_confirmation_target` (`kind`, `target_id`, `target_version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户确认记录表（绑定目标版本）';

-- -------------------------------------------------------------
-- 12. 保险方案表（只追加，版本化）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `insurance_plan` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `plan_no`          VARCHAR(32)     NOT NULL                COMMENT '方案对外编号（ULID），唯一',
  `conversation_id`  BIGINT UNSIGNED NOT NULL                COMMENT '会话 conversation.id',
  `order_id`         BIGINT UNSIGNED NULL                    COMMENT '订单 insurance_order.id，可空',
  `previous_plan_id` BIGINT UNSIGNED NULL                    COMMENT '上一版本方案 id',
  `version`          INT             NOT NULL DEFAULT 1      COMMENT '方案版本号',
  `insurer_code`     VARCHAR(32)     NULL                    COMMENT '保险公司编码',
  `insurer_name`     VARCHAR(100)    NULL                    COMMENT '保险公司名称',
  `source`           VARCHAR(20)     NOT NULL DEFAULT 'RECOMMEND' COMMENT '来源：RECOMMEND/HISTORY/MODIFY/TEMPLATE',
  `status`           VARCHAR(24)     NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/PENDING_CONFIRMATION/CONFIRMED/SUPERSEDED/CANCELLED',
  `estimated_total`  DECIMAL(12,2)   NULL                    COMMENT '预估总保费（元），未知 NULL',
  `currency`         CHAR(3)         NOT NULL DEFAULT 'CNY'  COMMENT '币种',
  `price_source`     VARCHAR(24)     NOT NULL DEFAULT 'ESTIMATE' COMMENT '价格来源：DEMO/ESTIMATE/INSURER_QUOTE/PENDING_REQUOTE',
  `instruction`      VARCHAR(500)    NULL                    COMMENT '本次修改的用户指令',
  `changed_json`     JSON            NULL                    COMMENT '相对上一版本的变化明细',
  `confirmed_at`     DATETIME        NULL                    COMMENT '确认时间',
  `confirmed_by`     BIGINT UNSIGNED NULL                    COMMENT '确认人 sys_user.id',
  `created_by`       BIGINT UNSIGNED NULL                    COMMENT '创建人/服务',
  `created_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_insurance_plan_no` (`plan_no`),
  UNIQUE KEY `uk_insurance_plan_version` (`conversation_id`, `version`),
  KEY `idx_insurance_plan_order` (`order_id`),
  KEY `idx_insurance_plan_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='保险方案表（只追加，版本化）';

-- -------------------------------------------------------------
-- 13. 方案险种明细表
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `insurance_plan_item` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `plan_id`           BIGINT UNSIGNED NOT NULL                COMMENT '方案 insurance_plan.id',
  `coverage_code`     VARCHAR(40)     NOT NULL                COMMENT '险种编码',
  `coverage_name`     VARCHAR(64)     NOT NULL                COMMENT '险种名称',
  `selected`          TINYINT         NOT NULL DEFAULT 1      COMMENT '是否选择：1-是 0-否',
  `is_required`       TINYINT         NOT NULL DEFAULT 0      COMMENT '是否必投（如交强险）',
  `sum_insured`       DECIMAL(14,2)   NULL                    COMMENT '保额（元），未选/不适用为 NULL',
  `deductible`        DECIMAL(12,2)   NULL                    COMMENT '免赔额（元），不适用为 NULL',
  `estimated_premium` DECIMAL(12,2)   NULL                    COMMENT '预估保费（元）',
  `terms`             JSON            NULL                    COMMENT '条款摘要',
  `sort_order`        INT             NOT NULL DEFAULT 0      COMMENT '展示排序',
  `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_insurance_plan_item` (`plan_id`, `coverage_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='方案险种明细表';

-- -------------------------------------------------------------
-- 14. 保险订单表（聚合根 + 不可变快照）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `insurance_order` (
  `id`                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no`             VARCHAR(32)     NOT NULL                COMMENT '订单号，唯一',
  `conversation_id`      BIGINT UNSIGNED NOT NULL                COMMENT '会话 conversation.id',
  `customer_id`          BIGINT UNSIGNED NOT NULL                COMMENT '客户 sys_user.id',
  `salesman_id`          BIGINT UNSIGNED NULL                    COMMENT '业务员 sys_user.id',
  `vehicle_id`           BIGINT UNSIGNED NOT NULL                COMMENT '车辆 vehicle.id',
  `plan_id`              BIGINT UNSIGNED NOT NULL                COMMENT '已确认方案 insurance_plan.id',
  `plan_version`         INT             NOT NULL                COMMENT '已确认方案版本',
  `vehicle_version`      INT             NOT NULL                COMMENT '车辆资料版本',
  `party_versions`       JSON            NOT NULL                COMMENT '各角色人员资料版本',
  `status`               VARCHAR(24)     NOT NULL DEFAULT 'DRAFT' COMMENT '订单状态，见状态机',
  `version`              INT             NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
  `total_premium`        DECIMAL(12,2)   NULL                    COMMENT '总保费（元）',
  `tax_amount`           DECIMAL(12,2)   NULL                    COMMENT '税额（元）',
  `payable_amount`       DECIMAL(12,2)   NULL                    COMMENT '应付金额（元）',
  `fail_reason`          VARCHAR(500)    NULL                    COMMENT '订单失败原因（对外展示）',
  `currency`             CHAR(3)         NOT NULL DEFAULT 'CNY'  COMMENT '币种',
  `active_quote_id`      BIGINT UNSIGNED NULL                    COMMENT '当前有效报价 quote_record.id',
  `active_underwriting_id` BIGINT UNSIGNED NULL                  COMMENT '当前有效核保 underwriting_record.id',
  `active_payment_id`    BIGINT UNSIGNED NULL                    COMMENT '当前支付 payment_record.id',
  `vehicle_snapshot`     JSON            NOT NULL                COMMENT '下单时车辆快照',
  `party_snapshot`       JSON            NOT NULL                COMMENT '下单时人员快照',
  `plan_snapshot`        JSON            NOT NULL                COMMENT '下单时方案及险种快照',
  `submitted_at`         DATETIME        NULL                    COMMENT '用户明确发起报价时间',
  `created_at`           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_insurance_order_no` (`order_no`),
  KEY `idx_insurance_order_conversation` (`conversation_id`, `created_at`),
  KEY `idx_insurance_order_customer` (`customer_id`),
  KEY `idx_insurance_order_salesman` (`salesman_id`, `status`),
  KEY `idx_insurance_order_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='保险订单表（聚合根）';

-- -------------------------------------------------------------
-- 15. 报价记录表（一单多次报价）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `quote_record` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id`         BIGINT UNSIGNED NOT NULL                COMMENT '订单 insurance_order.id',
  `quote_no`         VARCHAR(64)     NOT NULL                COMMENT '报价编号，唯一',
  `plan_id`          BIGINT UNSIGNED NOT NULL                COMMENT '报价依据方案 id',
  `plan_version`     INT             NOT NULL                COMMENT '方案版本',
  `insurer_code`     VARCHAR(32)     NULL                    COMMENT '保险公司编码',
  `insurer_name`     VARCHAR(100)    NULL                    COMMENT '保险公司名称',
  `status`           VARCHAR(20)     NOT NULL DEFAULT 'PROCESSING' COMMENT '状态：PROCESSING/SUCCEEDED/FAILED/EXPIRED',
  `total_premium`    DECIMAL(12,2)   NULL                    COMMENT '总保费（元）',
  `tax_amount`       DECIMAL(12,2)   NULL                    COMMENT '税额（元）',
  `payable_amount`   DECIMAL(12,2)   NULL                    COMMENT '应付金额（元）',
  `currency`         CHAR(3)         NOT NULL DEFAULT 'CNY'  COMMENT '币种',
  `valid_until`      DATETIME        NULL                    COMMENT '报价有效期',
  `external_quote_no` VARCHAR(64)    NULL                    COMMENT '外部报价编号',
  `provider`         VARCHAR(32)     NOT NULL DEFAULT 'MOCK' COMMENT '报价渠道',
  `data_source`      VARCHAR(16)     NOT NULL DEFAULT 'DEMO' COMMENT '数据来源：DEMO/REAL，DEMO 不得当真实报价',
  `response_summary` JSON            NULL                    COMMENT '第三方响应摘要（脱敏）',
  `error_code`       VARCHAR(64)     NULL                    COMMENT '失败错误码',
  `error_message`    VARCHAR(500)    NULL                    COMMENT '失败原因',
  `requested_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '请求时间',
  `completed_at`     DATETIME        NULL                    COMMENT '完成时间',
  `version`          INT             NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
  `created_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_quote_record_no` (`quote_no`),
  KEY `idx_quote_record_order` (`order_id`, `status`),
  KEY `idx_quote_record_external` (`external_quote_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='报价记录表';

-- -------------------------------------------------------------
-- 16. 报价险种明细表
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `quote_item` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `quote_id`       BIGINT UNSIGNED NOT NULL                COMMENT '报价 quote_record.id',
  `coverage_code`  VARCHAR(40)     NOT NULL                COMMENT '险种编码',
  `coverage_name`  VARCHAR(64)     NOT NULL                COMMENT '险种名称',
  `sum_insured`    DECIMAL(14,2)   NULL                    COMMENT '保额（元）',
  `premium`        DECIMAL(12,2)   NULL                    COMMENT '保费（元）',
  `sort_order`     INT             NOT NULL DEFAULT 0      COMMENT '展示排序',
  `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_quote_item` (`quote_id`, `coverage_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='报价险种明细表';

-- -------------------------------------------------------------
-- 17. 核保记录表
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `underwriting_record` (
  `id`                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id`            BIGINT UNSIGNED NOT NULL                COMMENT '订单 insurance_order.id',
  `quote_id`            BIGINT UNSIGNED NOT NULL                COMMENT '报价 quote_record.id',
  `case_no`             VARCHAR(64)     NOT NULL                COMMENT '核保编号，唯一',
  `previous_case_id`    BIGINT UNSIGNED NULL                    COMMENT '补件重提关联的上一案件',
  `status`              VARCHAR(20)     NOT NULL DEFAULT 'PROCESSING' COMMENT '状态：PROCESSING/PASSED/REJECTED/NEED_MATERIAL',
  `reason_code`         VARCHAR(64)     NULL                    COMMENT '失败/补件原因码',
  `reason`              VARCHAR(500)    NULL                    COMMENT '可读原因',
  `required_materials`  JSON            NULL                    COMMENT '补件要求（类型、数量、说明）',
  `external_case_no`    VARCHAR(64)     NULL                    COMMENT '外部核保编号',
  `provider`            VARCHAR(32)     NOT NULL DEFAULT 'MOCK' COMMENT '核保渠道',
  `data_source`         VARCHAR(16)     NOT NULL DEFAULT 'DEMO' COMMENT '数据来源：DEMO/REAL',
  `submitted_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `completed_at`        DATETIME        NULL                    COMMENT '完成时间',
  `version`             INT             NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
  `created_at`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_underwriting_case_no` (`case_no`),
  KEY `idx_underwriting_order` (`order_id`, `status`),
  KEY `idx_underwriting_quote` (`quote_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='核保记录表';

-- -------------------------------------------------------------
-- 18. 核保材料关联表
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `underwriting_material` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `case_id`       BIGINT UNSIGNED NOT NULL                COMMENT '核保记录 underwriting_record.id',
  `file_id`       BIGINT UNSIGNED NOT NULL                COMMENT '文件 file_object.id',
  `material_type` VARCHAR(40)     NULL                    COMMENT '材料类型：INSPECTION_PHOTO/INSPECTION_VIDEO/OTHER',
  `accepted`      TINYINT         NOT NULL DEFAULT 1      COMMENT '是否被接受：1-是 0-否',
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_underwriting_material` (`case_id`, `file_id`),
  KEY `idx_underwriting_material_file` (`file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='核保材料关联表';

-- -------------------------------------------------------------
-- 19. 支付记录表（状态只能由后端核验写入）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `payment_record` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id`          BIGINT UNSIGNED NOT NULL                COMMENT '订单 insurance_order.id',
  `quote_id`          BIGINT UNSIGNED NULL                    COMMENT '报价 quote_record.id',
  `underwriting_id`   BIGINT UNSIGNED NULL                    COMMENT '核保 underwriting_record.id',
  `payment_no`        VARCHAR(64)     NOT NULL                COMMENT '支付编号，唯一',
  `out_trade_no`      VARCHAR(64)     NOT NULL                COMMENT '商户支付单号，唯一（幂等）',
  `amount`            DECIMAL(12,2)   NOT NULL                COMMENT '支付金额（元）',
  `currency`          CHAR(3)         NOT NULL DEFAULT 'CNY'  COMMENT '币种',
  `channel`           VARCHAR(32)     NOT NULL                COMMENT '渠道：MOCK/ALIPAY/WECHAT',
  `provider`          VARCHAR(32)     NOT NULL DEFAULT 'MOCK' COMMENT '支付平台',
  `transaction_id`    VARCHAR(128)    NULL                    COMMENT '第三方交易流水号',
  `status`            VARCHAR(20)     NOT NULL DEFAULT 'CREATED' COMMENT '状态：CREATED/PENDING/SUCCESS/FAILED/CLOSED/REFUNDED',
  `pay_url`           VARCHAR(500)    NULL                    COMMENT '已批准支付地址',
  `expires_at`        DATETIME        NULL                    COMMENT '支付过期时间',
  `paid_at`           DATETIME        NULL                    COMMENT '支付成功时间',
  `verified_at`       DATETIME        NULL                    COMMENT '后端查单核验时间',
  `callback_payload`  JSON            NULL                    COMMENT '回调原文（脱敏）',
  `data_source`       VARCHAR(16)     NOT NULL DEFAULT 'DEMO' COMMENT '数据来源：DEMO/REAL',
  `error_message`     VARCHAR(500)    NULL                    COMMENT '失败原因',
  `version`           INT             NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
  `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_no` (`payment_no`),
  UNIQUE KEY `uk_payment_out_trade_no` (`out_trade_no`),
  UNIQUE KEY `uk_payment_channel_txn` (`channel`, `transaction_id`),
  KEY `idx_payment_order` (`order_id`, `status`),
  KEY `idx_payment_transaction` (`transaction_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付记录表';

-- -------------------------------------------------------------
-- 20. 保单表（一单可多张：交强险/商业险）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `policy` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id`          BIGINT UNSIGNED NOT NULL                COMMENT '订单 insurance_order.id',
  `vehicle_id`        BIGINT UNSIGNED NULL                    COMMENT '车辆 vehicle.id（便于按车查历史保单）',
  `policy_no`         VARCHAR(64)     NOT NULL                COMMENT '正式保单号，唯一',
  `policy_type`       VARCHAR(20)     NOT NULL DEFAULT 'COMMERCIAL' COMMENT '类型：COMPULSORY/COMMERCIAL',
  `insurer_code`      VARCHAR(32)     NULL                    COMMENT '保险公司编码',
  `insurer_name`      VARCHAR(100)    NULL                    COMMENT '保险公司名称',
  `status`            VARCHAR(20)     NOT NULL DEFAULT 'ISSUING' COMMENT '状态：ISSUING/ISSUED/CANCELLED/EXPIRED',
  `effective_from`    DATETIME        NOT NULL                COMMENT '保障开始时间',
  `effective_to`      DATETIME        NOT NULL                COMMENT '保障结束时间',
  `total_premium`     DECIMAL(12,2)   NULL                    COMMENT '保单总保费（元）',
  `currency`          CHAR(3)         NOT NULL DEFAULT 'CNY'  COMMENT '币种',
  `applicant_name_masked` VARCHAR(50) NULL                    COMMENT '投保人姓名脱敏',
  `applicant_id_masked`   VARCHAR(32) NULL                    COMMENT '投保人证件号脱敏',
  `insured_name_masked`   VARCHAR(50) NULL                    COMMENT '被保人姓名脱敏',
  `insured_id_masked`     VARCHAR(32) NULL                    COMMENT '被保人证件号脱敏',
  `vehicle_snapshot`  JSON            NOT NULL                COMMENT '出单时车辆快照',
  `party_snapshot`    JSON            NOT NULL                COMMENT '出单时人员快照',
  `terms`             JSON            NULL                    COMMENT '条款摘要',
  `document_file_id`  BIGINT UNSIGNED NULL                    COMMENT '保单文件 file_object.id',
  `data_source`       VARCHAR(16)     NOT NULL DEFAULT 'DEMO' COMMENT '数据来源：DEMO/REAL',
  `issued_at`         DATETIME        NULL                    COMMENT '出单时间',
  `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_policy_no` (`policy_no`),
  KEY `idx_policy_order` (`order_id`, `status`),
  KEY `idx_policy_vehicle` (`vehicle_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='保单表（一单可多张）';

-- -------------------------------------------------------------
-- 21. 保单险种明细表
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `policy_item` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `policy_id`      BIGINT UNSIGNED NOT NULL                COMMENT '保单 policy.id',
  `coverage_code`  VARCHAR(40)     NOT NULL                COMMENT '险种编码',
  `coverage_name`  VARCHAR(64)     NOT NULL                COMMENT '险种名称',
  `sum_insured`    DECIMAL(14,2)   NULL                    COMMENT '保额（元）',
  `premium`        DECIMAL(12,2)   NULL                    COMMENT '保费（元）',
  `sort_order`     INT             NOT NULL DEFAULT 0      COMMENT '展示排序',
  `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_policy_item` (`policy_id`, `coverage_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='保单险种明细表';

-- -------------------------------------------------------------
-- 22. 批量任务表
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `batch_job` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `job_no`            VARCHAR(32)     NOT NULL                COMMENT '任务编号，唯一',
  `agent_account_id`  BIGINT UNSIGNED NOT NULL                COMMENT '发起业务员 sys_user.id',
  `client_request_id` VARCHAR(64)     NOT NULL                COMMENT '前端幂等 ID，唯一',
  `action`            VARCHAR(32)     NOT NULL                COMMENT '动作：SEND_MESSAGE/CREATE_ORDER/QUOTE',
  `total_count`       INT             NOT NULL DEFAULT 0      COMMENT '总条数',
  `success_count`     INT             NOT NULL DEFAULT 0      COMMENT '成功数',
  `fail_count`        INT             NOT NULL DEFAULT 0      COMMENT '失败数',
  `status`            VARCHAR(24)     NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/RUNNING/COMPLETED/PARTIAL_FAILED/FAILED',
  `started_at`        DATETIME        NULL                    COMMENT '开始时间',
  `completed_at`      DATETIME        NULL                    COMMENT '完成时间',
  `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_batch_job_no` (`job_no`),
  UNIQUE KEY `uk_batch_job_client` (`agent_account_id`, `client_request_id`),
  KEY `idx_batch_job_status` (`status`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='批量任务表';

-- -------------------------------------------------------------
-- 23. 批量任务明细表（逐条结果）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `batch_job_item` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `batch_id`         BIGINT UNSIGNED NOT NULL                COMMENT '批量任务 batch_job.id',
  `customer_id`      BIGINT UNSIGNED NOT NULL                COMMENT '客户 sys_user.id',
  `conversation_id`  BIGINT UNSIGNED NULL                    COMMENT '会话 conversation.id',
  `plate_number`     VARCHAR(16)     NULL                    COMMENT '车牌号',
  `request_id`       VARCHAR(64)     NULL                    COMMENT '单项请求 ID',
  `status`           VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/RUNNING/SUCCEEDED/FAILED',
  `error_code`       VARCHAR(64)     NULL                    COMMENT '错误码',
  `error_message`    VARCHAR(500)    NULL                    COMMENT '失败原因',
  `reply_message_id` BIGINT UNSIGNED NULL                    COMMENT 'AI 回复消息 chat_message.id',
  `created_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_batch_job_item_batch` (`batch_id`, `status`),
  KEY `idx_batch_job_item_customer` (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='批量任务明细表';

-- -------------------------------------------------------------
-- 24. 人工接管工单表
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `handoff_task` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `ticket_no`        VARCHAR(32)     NOT NULL                COMMENT '工单编号，唯一',
  `conversation_id`  BIGINT UNSIGNED NOT NULL                COMMENT '会话 conversation.id',
  `order_id`         BIGINT UNSIGNED NULL                    COMMENT '订单 insurance_order.id',
  `customer_id`      BIGINT UNSIGNED NOT NULL                COMMENT '客户 sys_user.id',
  `reason_code`      VARCHAR(64)     NOT NULL                COMMENT '原因码：OCR_CONFLICT/USER_REQUEST/BUSINESS_FAILURE',
  `reason`           VARCHAR(500)    NULL                    COMMENT '原因描述',
  `summary`          VARCHAR(1000)   NULL                    COMMENT '交接摘要',
  `status`           VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/ACCEPTED/RESOLVED/CANCELLED',
  `queue_position`   INT             NULL                    COMMENT '排队位次',
  `assigned_agent_id` BIGINT UNSIGNED NULL                   COMMENT '处理业务员 sys_user.id',
  `resolution`       VARCHAR(1000)   NULL                    COMMENT '处理结果',
  `version`          INT             NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
  `requested_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发起时间',
  `accepted_at`      DATETIME        NULL                    COMMENT '领取时间',
  `resolved_at`      DATETIME        NULL                    COMMENT '解决时间',
  `created_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_handoff_task_no` (`ticket_no`),
  KEY `idx_handoff_conversation` (`conversation_id`, `status`),
  KEY `idx_handoff_agent` (`assigned_agent_id`, `status`),
  KEY `idx_handoff_queue` (`status`, `requested_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='人工接管工单表';

-- -------------------------------------------------------------
-- 25. 人工接管事件时间线表
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `handoff_event` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `handoff_id`  BIGINT UNSIGNED NOT NULL                COMMENT '工单 handoff_task.id',
  `event_type`  VARCHAR(40)     NOT NULL                COMMENT '事件：REQUESTED/QUEUED/ACCEPTED/MESSAGE/RESOLVED/CANCELLED',
  `actor_type`  VARCHAR(20)     NOT NULL                COMMENT '操作者类型：CUSTOMER/SALESMAN/ADMIN/SYSTEM/AI',
  `actor_id`    BIGINT UNSIGNED NULL                    COMMENT '操作者 sys_user.id',
  `payload_json` JSON           NULL                    COMMENT '事件载荷（脱敏）',
  `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
  PRIMARY KEY (`id`),
  KEY `idx_handoff_event` (`handoff_id`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='人工接管事件时间线';

-- -------------------------------------------------------------
-- 26. 行业术语表
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `kb_term` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `term`       VARCHAR(100)    NOT NULL                COMMENT '术语，唯一',
  `normalized` VARCHAR(100)    NOT NULL                COMMENT '标准名称',
  `definition` VARCHAR(1000)   NULL                    COMMENT '解释',
  `aliases`    JSON            NULL                    COMMENT '同义词列表',
  `example`    VARCHAR(500)    NULL                    COMMENT '示例',
  `source`     VARCHAR(100)    NULL                    COMMENT '来源',
  `version`    INT             NOT NULL DEFAULT 1      COMMENT '版本',
  `status`     VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/INACTIVE',
  `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_kb_term` (`term`),
  KEY `idx_kb_term_normalized` (`normalized`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='行业术语表（向量数据不入库）';

-- -------------------------------------------------------------
-- 27. 会话与 LangGraph 线程映射表
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `agent_session` (
  `id`                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `conversation_id`     BIGINT UNSIGNED NOT NULL                COMMENT '会话 conversation.id',
  `thread_id`           VARCHAR(64)     NOT NULL                COMMENT 'LangGraph thread_id，唯一',
  `graph_version`       VARCHAR(32)     NULL                    COMMENT '图版本',
  `last_checkpoint_id`  VARCHAR(64)     NULL                    COMMENT '最近 checkpoint ID（仅引用，不存状态）',
  `status`              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/CLOSED',
  `created_at`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_thread` (`thread_id`),
  KEY `idx_agent_session_conversation` (`conversation_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会话与 LangGraph 线程映射';

-- -------------------------------------------------------------
-- 28. 审计日志表（只追加，不得写入证件号明文）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `audit_log` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `trace_id`      VARCHAR(64)     NULL                    COMMENT '链路追踪 ID',
  `operator_id`   BIGINT UNSIGNED NULL                    COMMENT '操作者 sys_user.id',
  `operator_type` VARCHAR(20)     NOT NULL DEFAULT 'SYSTEM' COMMENT '类型：CUSTOMER/SALESMAN/ADMIN/SYSTEM/AI',
  `action`        VARCHAR(64)     NOT NULL                COMMENT '动作：LOGIN/ORDER_CREATE/PAY_CALLBACK/...',
  `resource_type` VARCHAR(40)     NULL                    COMMENT '资源类型',
  `resource_id`   BIGINT UNSIGNED NULL                    COMMENT '资源 ID',
  `result`        VARCHAR(20)     NOT NULL DEFAULT 'SUCCESS' COMMENT '结果：SUCCESS/FAILED',
  `ip`            VARCHAR(64)     NULL                    COMMENT '来源 IP',
  `detail_json`   JSON            NULL                    COMMENT '详情（必须脱敏）',
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
  PRIMARY KEY (`id`),
  KEY `idx_audit_operator` (`operator_id`, `created_at`),
  KEY `idx_audit_resource` (`resource_type`, `resource_id`),
  KEY `idx_audit_action` (`action`, `created_at`),
  KEY `idx_audit_trace` (`trace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审计日志表';

-- -------------------------------------------------------------
-- 29. 幂等记录表
-- 同一 key 重试返回原结果；不同请求指纹返回冲突
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `idempotency_key` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `idem_key`       VARCHAR(128)    NOT NULL                COMMENT '幂等键（如 requestId），唯一',
  `scope`          VARCHAR(40)     NOT NULL DEFAULT 'DEFAULT' COMMENT '业务范围：ORDER/QUOTE/PAYMENT/OCR',
  `request_hash`   CHAR(64)        NOT NULL                COMMENT '请求参数指纹',
  `status`         VARCHAR(20)     NOT NULL DEFAULT 'PROCESSING' COMMENT '状态：PROCESSING/COMPLETED/FAILED',
  `response_code`  VARCHAR(32)     NULL                    COMMENT '首次执行结果码',
  `response_body`  JSON            NULL                    COMMENT '首次执行结果（用于回放）',
  `resource_type`  VARCHAR(40)     NULL                    COMMENT '产生的资源类型',
  `resource_id`    BIGINT UNSIGNED NULL                    COMMENT '产生的资源 ID',
  `expires_at`     DATETIME        NULL                    COMMENT '过期时间',
  `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_idempotency_key` (`idem_key`),
  KEY `idx_idempotency_expire` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='幂等记录表';

-- -------------------------------------------------------------
-- 30. 订单状态日志表（只追加）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `order_status_log` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id`      BIGINT UNSIGNED NOT NULL                COMMENT '订单 insurance_order.id',
  `from_status`   VARCHAR(24)     NULL                    COMMENT '变更前状态',
  `to_status`     VARCHAR(24)     NOT NULL                COMMENT '变更后状态',
  `operator_id`   BIGINT UNSIGNED NULL                    COMMENT '操作人 sys_user.id',
  `operator_type` VARCHAR(20)     NOT NULL DEFAULT 'SYSTEM' COMMENT '操作者类型：CUSTOMER/SALESMAN/ADMIN/SYSTEM/AI',
  `remark`        VARCHAR(500)    NULL                    COMMENT '备注',
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_status_log_order` (`order_id`, `id`),
  KEY `idx_order_status_log_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单状态变更日志表';

-- -------------------------------------------------------------
-- 31. 续保查询记录表（AI J04/J05）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `renewal_lookup` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `lookup_no`      VARCHAR(40)     NOT NULL                COMMENT '查询编号，唯一',
  `customer_id`    BIGINT UNSIGNED NOT NULL                COMMENT '客户 sys_user.id',
  `plate_number`   VARCHAR(16)     NULL                    COMMENT '查询车牌',
  `vin`            VARCHAR(32)     NULL                    COMMENT '查询 VIN',
  `policy_year`    SMALLINT        NULL                    COMMENT '保单年度',
  `status`         VARCHAR(20)     NOT NULL DEFAULT 'PROCESSING' COMMENT '状态：PROCESSING/COMPLETED/FAILED',
  `matched`        TINYINT         NULL                    COMMENT '是否匹配到历史保单',
  `result_json`    JSON            NULL                    COMMENT '候选历史保单（快照）',
  `error_message`  VARCHAR(500)    NULL                    COMMENT '失败原因',
  `requested_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '请求时间',
  `completed_at`   DATETIME        NULL                    COMMENT '完成时间',
  `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_renewal_lookup_no` (`lookup_no`),
  KEY `idx_renewal_lookup_customer` (`customer_id`, `status`),
  KEY `idx_renewal_lookup_plate` (`plate_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='续保查询记录表';
