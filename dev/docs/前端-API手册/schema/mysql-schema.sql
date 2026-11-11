-- =============================================================================
-- 车险出单智能体 · 业务库参考 DDL（MySQL 8.0.32+）
-- =============================================================================
-- 状态：设计草案，供成员 B 评审；未在真实实例上执行过。
-- 定位：这是「前端功能 → 数据模型」的对齐材料，不是最终建库脚本。
--
-- 约定：
--   * 引擎 InnoDB，字符集 utf8mb4 / utf8mb4_0900_ai_ci
--   * 主键一律 BIGINT UNSIGNED AUTO_INCREMENT；对外暴露的资源另加 CHAR(26) ULID
--   * 金额 DECIMAL(12,2) / 保额 DECIMAL(14,2)，禁止用 FLOAT/DOUBLE
--   * 时间 DATETIME(3)，统一按 UTC 存取，接口层转 +08:00
--   * 状态用 VARCHAR + 注释枚举（不用 MySQL ENUM，便于扩展与迁移）
--   * 物理外键一律不加：关系在应用层维护，避免分库/归档时被约束卡住；
--     需要的索引已单独建好
--   * JSON 列不设 DEFAULT（MySQL 8 限制），未赋值保持 NULL
--   * 敏感字段（身份证号）：密文列 + 哈希列 + 脱敏列，不存明文
-- =============================================================================

SET NAMES utf8mb4;

-- -----------------------------------------------------------------------------
-- 1. 身份与权限
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS account (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  username        VARCHAR(64)     NOT NULL COMMENT '登录账号（手机号或工号）',
  password_hash   VARCHAR(100)    NOT NULL COMMENT 'BCrypt 哈希，不存明文',
  real_name       VARCHAR(64)     NOT NULL DEFAULT '',
  phone           VARCHAR(20)     NOT NULL DEFAULT '',
  role            VARCHAR(16)     NOT NULL COMMENT 'USER | AGENT',
  agent_no        VARCHAR(32)     NULL COMMENT '业务员工号，role=AGENT 时必填',
  org_name        VARCHAR(128)    NULL COMMENT '所属机构',
  parent_agent_id BIGINT UNSIGNED NULL COMMENT '普通用户绑定的业务员 account.id（登录时自带）',
  status          VARCHAR(16)     NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE | DISABLED | LOCKED',
  last_login_at   DATETIME(3)     NULL,
  created_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username),
  UNIQUE KEY uk_agent_no (agent_no),
  KEY idx_role_status (role, status),
  KEY idx_parent_agent (parent_agent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='账号：普通用户与业务员共用一张表，用 role 区分';

-- -----------------------------------------------------------------------------
-- 2. 车辆
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS vehicle (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  plate_number  VARCHAR(16)     NOT NULL DEFAULT '' COMMENT '原始车牌，含分隔点，用于展示',
  plate_norm    VARCHAR(16)     NOT NULL DEFAULT '' COMMENT '归一化车牌（去空格/点/横线），用于查询匹配',
  vin           VARCHAR(32)     NULL COMMENT '车架号，未知为 NULL（唯一索引允许多个 NULL）',
  engine_no     VARCHAR(32)     NOT NULL DEFAULT '',
  owner_name    VARCHAR(64)     NOT NULL DEFAULT '',
  register_date DATE            NULL,
  usage_nature  VARCHAR(32)     NOT NULL DEFAULT '' COMMENT '使用性质：非营运 / 营运',
  model         VARCHAR(64)     NOT NULL DEFAULT '',
  source        VARCHAR(16)     NOT NULL DEFAULT 'OCR' COMMENT 'OCR | MANUAL | HISTORY',
  created_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_vin (vin),
  KEY idx_plate_norm (plate_norm)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='车辆：一辆车可被多次投保，因此车主信息不放这里，放 applicant / policy';

CREATE TABLE IF NOT EXISTS vehicle_profile (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  conversation_id  BIGINT UNSIGNED NOT NULL,
  vehicle_id       BIGINT UNSIGNED NOT NULL,
  fields_json      JSON            NOT NULL COMMENT '用户确认后的字段快照',
  corrected_fields JSON            NULL COMMENT '用户手动改过的字段 key 数组',
  source           VARCHAR(16)     NOT NULL DEFAULT 'OCR' COMMENT 'OCR | MANUAL',
  ocr_record_id    BIGINT UNSIGNED NULL,
  confirmed_by     BIGINT UNSIGNED NOT NULL COMMENT '确认人 account.id',
  confirmed_at     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_conversation (conversation_id, confirmed_at),
  KEY idx_vehicle (vehicle_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='车辆资料确认留痕：OCR 原值可被用户修正，这里保存"确认态"与修正字段';

-- -----------------------------------------------------------------------------
-- 3. 会话与消息
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS conversation (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  conv_no          CHAR(26)        NOT NULL COMMENT '对外 ULID',
  account_id       BIGINT UNSIGNED NOT NULL COMMENT '会话归属用户',
  agent_account_id BIGINT UNSIGNED NULL COMMENT '归属业务员（来自用户绑定关系）',
  vehicle_id       BIGINT UNSIGNED NULL,
  plate_number     VARCHAR(16)     NOT NULL DEFAULT '' COMMENT '会话标题 = 车牌号',
  stage            VARCHAR(24)     NOT NULL DEFAULT 'COLLECTING'
                   COMMENT 'COLLECTING|RECOMMENDED|IDENTITY|QUOTED|UNDERWRITING|PAYING|ISSUED|ABORTED',
  status           VARCHAR(16)     NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE | HANDOFF | CLOSED',
  last_message_at  DATETIME(3)     NULL,
  message_count    INT UNSIGNED    NOT NULL DEFAULT 0,
  created_at       DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at       DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_conv_no (conv_no),
  KEY idx_account_recent (account_id, status, updated_at),
  KEY idx_agent_status (agent_account_id, status, updated_at),
  KEY idx_plate (plate_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='会话：标题取车牌号；出现新号牌时新建会话，同号牌保留历史会话';

CREATE TABLE IF NOT EXISTS message (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  msg_no         CHAR(26)        NOT NULL COMMENT '对外 ULID',
  conversation_id BIGINT UNSIGNED NOT NULL,
  sender_type    VARCHAR(16)     NOT NULL
                 COMMENT 'USER | ASSISTANT | TOOL | HUMAN_AGENT | SYSTEM',
  sender_id      BIGINT UNSIGNED NULL COMMENT 'USER/ HUMAN_AGENT 时为 account.id',
  role           VARCHAR(16)     NOT NULL COMMENT 'user | assistant | tool（与前端契约一致）',
  message_type   VARCHAR(16)     NOT NULL DEFAULT 'text'
                 COMMENT 'text | image | plan | status | handoff',
  content        MEDIUMTEXT      NULL,
  payload_json   JSON            NULL COMMENT '方案卡片 / 状态卡片 / 转人工等结构化载荷',
  client_msg_id  VARCHAR(64)     NULL COMMENT '前端幂等键，重发不重复落库',
  token_input    INT UNSIGNED    NULL COMMENT 'Agent 侧用量（便于成本核算）',
  token_output   INT UNSIGNED    NULL,
  latency_ms     INT UNSIGNED    NULL,
  created_at     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_msg_no (msg_no),
  UNIQUE KEY uk_client_msg (conversation_id, client_msg_id),
  KEY idx_conv_cursor (conversation_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='消息：按 (conversation_id, id) 做游标分页，避免深翻页';

CREATE TABLE IF NOT EXISTS message_attachment (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  message_id BIGINT UNSIGNED NOT NULL,
  file_id    BIGINT UNSIGNED NOT NULL,
  category   VARCHAR(24)     NOT NULL COMMENT 'LICENSE_MAIN | IDCARD_FRONT | CHAT_IMAGE ...',
  sort_no    INT             NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_message (message_id, sort_no),
  KEY idx_file (file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='消息与文件的关联（一条消息可带多张图）';

-- -----------------------------------------------------------------------------
-- 4. 影像与 OCR
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS file_object (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  file_no         CHAR(26)        NOT NULL COMMENT '对外 ULID，接口里就是 fileId',
  account_id      BIGINT UNSIGNED NOT NULL COMMENT '上传人',
  conversation_id BIGINT UNSIGNED NULL,
  category        VARCHAR(24)     NOT NULL
                  COMMENT 'LICENSE_MAIN|LICENSE_SUB|IDCARD_FRONT|IDCARD_BACK|INSPECTION_PHOTO|INSPECTION_VIDEO|CHAT_IMAGE',
  storage         VARCHAR(16)     NOT NULL DEFAULT 'OSS' COMMENT 'OSS | LOCAL',
  bucket          VARCHAR(64)     NOT NULL DEFAULT '',
  object_key      VARCHAR(255)    NOT NULL COMMENT '对象存储 key，DB 不存二进制',
  mime_type       VARCHAR(64)     NOT NULL,
  size_bytes      BIGINT UNSIGNED NOT NULL,
  sha256          CHAR(64)        NOT NULL COMMENT '内容指纹，用于秒传与去重',
  width           INT UNSIGNED    NULL,
  height          INT UNSIGNED    NULL,
  status          VARCHAR(16)     NOT NULL DEFAULT 'READY' COMMENT 'UPLOADING | READY | DELETED',
  created_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_file_no (file_no),
  KEY idx_conv_category (conversation_id, category),
  KEY idx_sha (sha256)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='文件元数据：图片本体放对象存储，访问一律走短期签名 URL';

CREATE TABLE IF NOT EXISTS ocr_record (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  file_id        BIGINT UNSIGNED NOT NULL,
  ocr_type       VARCHAR(24)     NOT NULL
                 COMMENT 'LICENSE_MAIN|LICENSE_SUB|IDCARD_FRONT|IDCARD_BACK',
  provider       VARCHAR(32)     NOT NULL DEFAULT '' COMMENT 'OCR 服务商',
  provider_req_id VARCHAR(64)    NULL COMMENT '服务商请求号，便于对账排查',
  status         VARCHAR(16)     NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING | SUCCESS | FAILED',
  fields_json    JSON            NULL COMMENT '归一化字段：[{key,label,value,confidence,abnormal}]',
  raw_json       JSON            NULL COMMENT '服务商原始返回，后续换供应商时用于回溯',
  confidence_avg DECIMAL(5,4)    NULL,
  error_msg      VARCHAR(500)    NULL,
  duration_ms    INT UNSIGNED    NULL,
  created_at     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_file (file_id, ocr_type),
  KEY idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='OCR 识别记录：原始结果与归一化结果都留，确认后的值另存 vehicle_profile';

-- -----------------------------------------------------------------------------
-- 5. 投保人与被保人
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS applicant (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  order_id        BIGINT UNSIGNED NULL COMMENT '所属投保单，建单前可为 NULL',
  conversation_id BIGINT UNSIGNED NOT NULL,
  role_type       VARCHAR(16)     NOT NULL DEFAULT 'APPLICANT' COMMENT 'APPLICANT | INSURED',
  name            VARCHAR(64)     NOT NULL DEFAULT '',
  id_type         VARCHAR(24)     NOT NULL DEFAULT 'ID_CARD' COMMENT 'ID_CARD | PASSPORT | ...',
  id_no_enc       VARBINARY(512)  NULL COMMENT 'AES-256-GCM 密文，密钥在 KMS/环境变量',
  id_no_hash      CHAR(64)        NULL COMMENT 'SHA-256(salt + 号码)，用于查重与精确查询',
  id_masked       VARCHAR(32)     NOT NULL DEFAULT '' COMMENT '前端只展示这个，如 4403**********8821',
  valid_to        DATE            NULL,
  ocr_record_id   BIGINT UNSIGNED NULL,
  confirmed_at    DATETIME(3)     NULL,
  created_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_conversation (conversation_id, role_type),
  KEY idx_order (order_id),
  KEY idx_id_hash (id_no_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='身份证信息：不存明文；完整号仅服务端使用，前端只拿 id_masked';

-- -----------------------------------------------------------------------------
-- 6. 投保单主链（方案 → 报价 → 核保 → 支付 → 保单）
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS insurance_order (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  order_no         VARCHAR(32)     NOT NULL COMMENT '对外投保单号',
  conversation_id  BIGINT UNSIGNED NOT NULL,
  account_id       BIGINT UNSIGNED NOT NULL,
  agent_account_id BIGINT UNSIGNED NULL,
  vehicle_id       BIGINT UNSIGNED NULL,
  plan_id          BIGINT UNSIGNED NULL COMMENT '当前生效方案',
  quotation_id     BIGINT UNSIGNED NULL,
  payment_id       BIGINT UNSIGNED NULL,
  policy_id        BIGINT UNSIGNED NULL COMMENT '出单后回填',
  status           VARCHAR(24)     NOT NULL DEFAULT 'COLLECTING'
                   COMMENT 'COLLECTING|RECOMMENDING|IDENTITY|QUOTED|UNDERWRITING|PAY_PENDING|ISSUED|FAILED|CANCELLED',
  total_premium    DECIMAL(12,2)   NULL,
  fail_reason      VARCHAR(500)    NULL,
  created_at       DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at       DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_order_no (order_no),
  KEY idx_account_status (account_id, status, updated_at),
  KEY idx_agent_status (agent_account_id, status, updated_at),
  KEY idx_conversation (conversation_id),
  KEY idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='投保单聚合根：把方案/报价/核保/支付/保单串成一条流程，订单页与批量操作都查它';

CREATE TABLE IF NOT EXISTS insurance_plan (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  plan_no         CHAR(26)        NOT NULL,
  order_id        BIGINT UNSIGNED NOT NULL,
  conversation_id BIGINT UNSIGNED NOT NULL,
  version         INT UNSIGNED    NOT NULL DEFAULT 1 COMMENT '同一定单内自增，改一次方案 +1',
  parent_plan_id  BIGINT UNSIGNED NULL COMMENT '上一版方案，形成版本链',
  company_code    VARCHAR(32)     NOT NULL DEFAULT '',
  company_name    VARCHAR(64)     NOT NULL DEFAULT '',
  total_premium   DECIMAL(12,2)   NOT NULL DEFAULT 0,
  status          VARCHAR(16)     NOT NULL DEFAULT 'PROPOSED'
                  COMMENT 'PROPOSED | CONFIRMED | SUPERSEDED',
  source          VARCHAR(16)     NOT NULL DEFAULT 'RECOMMEND'
                  COMMENT 'RECOMMEND | REVISE | REUSE_LAST_YEAR',
  instruction     TEXT            NULL COMMENT '触发本版方案的自然语言指令原文',
  changed_json    JSON            NULL COMMENT '相对上一版的变化，前端用于说明"改了什么"',
  confirmed_at    DATETIME(3)     NULL,
  created_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_plan_no (plan_no),
  UNIQUE KEY uk_order_version (order_id, version),
  KEY idx_conversation (conversation_id, created_at),
  KEY idx_parent (parent_plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='保险方案：只追加不修改，改方案 = 新版本 + parent 指向旧版';

CREATE TABLE IF NOT EXISTS plan_item (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  plan_id         BIGINT UNSIGNED NOT NULL,
  coverage_code   VARCHAR(32)     NOT NULL COMMENT 'CI | TPL | VEHICLE_DAMAGE | ...',
  coverage_name   VARCHAR(64)     NOT NULL COMMENT '交强险 / 第三者责任险 / 车损险',
  coverage_amount DECIMAL(14,2)   NULL COMMENT '保额，不适用时为 NULL',
  premium         DECIMAL(12,2)   NOT NULL DEFAULT 0,
  deductible      DECIMAL(12,2)   NULL COMMENT '免赔额',
  is_required     TINYINT         NOT NULL DEFAULT 0 COMMENT '1 = 必选',
  sort_no         INT             NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_plan (plan_id, sort_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='方案险种明细';

CREATE TABLE IF NOT EXISTS quotation (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  quotation_no   VARCHAR(32)     NOT NULL,
  order_id       BIGINT UNSIGNED NOT NULL,
  plan_id        BIGINT UNSIGNED NOT NULL,
  status         VARCHAR(16)     NOT NULL DEFAULT 'QUOTING'
                 COMMENT 'QUOTING | QUOTED | FAILED | EXPIRED',
  total_premium  DECIMAL(12,2)   NULL,
  valid_until    DATETIME(3)     NULL COMMENT '报价有效期，过期前端置灰要求重报',
  carrier_ref    VARCHAR(64)     NULL COMMENT '保险公司报价单号',
  fail_reason    VARCHAR(500)    NULL,
  raw_json       JSON            NULL,
  created_at     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_quotation_no (quotation_no),
  KEY idx_order (order_id, created_at),
  KEY idx_status (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='报价单：一次报价一条，金额是快照，方案再改也不动它';

CREATE TABLE IF NOT EXISTS quotation_item (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  quotation_id    BIGINT UNSIGNED NOT NULL,
  coverage_code   VARCHAR(32)     NOT NULL,
  coverage_name   VARCHAR(64)     NOT NULL,
  coverage_amount DECIMAL(14,2)   NULL,
  premium         DECIMAL(12,2)   NOT NULL DEFAULT 0,
  sort_no         INT             NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_quotation (quotation_id, sort_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='报价明细快照';

CREATE TABLE IF NOT EXISTS underwriting_case (
  id                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  case_no            VARCHAR(32)     NOT NULL,
  order_id           BIGINT UNSIGNED NOT NULL,
  quotation_id       BIGINT UNSIGNED NOT NULL,
  status             VARCHAR(24)     NOT NULL DEFAULT 'PENDING'
                     COMMENT 'PENDING | PASS | REJECT | NEED_MATERIAL',
  reason             VARCHAR(1000)   NULL COMMENT '失败原因，给用户能看懂的话',
  required_materials JSON            NULL COMMENT '["INSPECTION_PHOTO","INSPECTION_VIDEO"]',
  carrier_ref        VARCHAR(64)     NULL,
  submitted_at       DATETIME(3)     NULL,
  decided_at         DATETIME(3)     NULL,
  created_at         DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_case_no (case_no),
  KEY idx_order (order_id, created_at),
  KEY idx_status (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='核保案件：PASS/REJECT/NEED_MATERIAL 三态，前端按此渲染操作入口';

CREATE TABLE IF NOT EXISTS underwriting_material (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  case_id       BIGINT UNSIGNED NOT NULL,
  file_id       BIGINT UNSIGNED NOT NULL,
  material_type VARCHAR(24)     NOT NULL COMMENT 'INSPECTION_PHOTO | INSPECTION_VIDEO',
  created_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_case (case_id, material_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='核保补充材料（验车照片/视频）';

CREATE TABLE IF NOT EXISTS payment_order (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  payment_no     VARCHAR(32)     NOT NULL,
  order_id       BIGINT UNSIGNED NOT NULL,
  quotation_id   BIGINT UNSIGNED NOT NULL,
  channel        VARCHAR(32)     NOT NULL DEFAULT 'MOCK' COMMENT '支付渠道：WECHAT | ALIPAY | MOCK',
  out_trade_no   VARCHAR(64)     NOT NULL COMMENT '我方商户单号，提交给支付平台',
  transaction_id VARCHAR(64)     NULL COMMENT '支付平台流水号',
  amount         DECIMAL(12,2)   NOT NULL,
  status         VARCHAR(16)     NOT NULL DEFAULT 'CREATED'
                 COMMENT 'CREATED|PENDING|SUCCESS|FAILED|CLOSED|REFUNDED',
  pay_url        VARCHAR(512)    NULL COMMENT '第三方收银台地址',
  paid_at        DATETIME(3)     NULL,
  callback_raw   JSON            NULL COMMENT '回调原文，便于对账',
  created_at     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_payment_no (payment_no),
  UNIQUE KEY uk_out_trade_no (out_trade_no),
  UNIQUE KEY uk_channel_txn (channel, transaction_id),
  KEY idx_order (order_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='支付单：状态只能由回调/查询确认，前端不得自行置成功';

CREATE TABLE IF NOT EXISTS policy (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  policy_no      VARCHAR(64)     NOT NULL COMMENT '保险公司保单号',
  order_id       BIGINT UNSIGNED NOT NULL,
  quotation_id   BIGINT UNSIGNED NOT NULL,
  payment_id     BIGINT UNSIGNED NOT NULL,
  applicant_id   BIGINT UNSIGNED NOT NULL,
  insured_id     BIGINT UNSIGNED NOT NULL,
  vehicle_id     BIGINT UNSIGNED NOT NULL,
  effective_from DATE            NOT NULL,
  effective_to   DATE            NOT NULL,
  total_premium  DECIMAL(12,2)   NOT NULL,
  status         VARCHAR(16)     NOT NULL DEFAULT 'ISSUED' COMMENT 'ISSUED | CANCELLED | EXPIRED',
  terms_json     JSON            NULL COMMENT '后续维护条款、特别约定',
  issued_at      DATETIME(3)     NULL,
  created_at     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_policy_no (policy_no),
  KEY idx_order (order_id),
  KEY idx_vehicle (vehicle_id),
  KEY idx_applicant (applicant_id),
  KEY idx_effective (effective_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='保单：只有支付确认成功后才会产生记录';

CREATE TABLE IF NOT EXISTS policy_item (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  policy_id       BIGINT UNSIGNED NOT NULL,
  coverage_code   VARCHAR(32)     NOT NULL,
  coverage_name   VARCHAR(64)     NOT NULL,
  coverage_amount DECIMAL(14,2)   NULL,
  premium         DECIMAL(12,2)   NOT NULL DEFAULT 0,
  sort_no         INT             NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_policy (policy_id, sort_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='保单险种花费快照（保单页"每个险种的花费"直接读这张表）';

-- -----------------------------------------------------------------------------
-- 7. 业务员批量操作
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS batch_job (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  job_no           VARCHAR(32)     NOT NULL,
  agent_account_id BIGINT UNSIGNED NOT NULL,
  action           VARCHAR(24)     NOT NULL COMMENT 'QUOTE | UNDERWRITING | PAYMENT | HANDOFF',
  total_count      INT UNSIGNED    NOT NULL DEFAULT 0,
  success_count    INT UNSIGNED    NOT NULL DEFAULT 0,
  fail_count       INT UNSIGNED    NOT NULL DEFAULT 0,
  status           VARCHAR(16)     NOT NULL DEFAULT 'RUNNING'
                   COMMENT 'RUNNING | DONE | PARTIAL | FAILED',
  created_at       DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  finished_at      DATETIME(3)     NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_job_no (job_no),
  KEY idx_agent (agent_account_id, created_at),
  KEY idx_status (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='批量任务：批量操作必须可追溯，不能只返回一个总数';

CREATE TABLE IF NOT EXISTS batch_job_item (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  job_id     BIGINT UNSIGNED NOT NULL,
  order_id   BIGINT UNSIGNED NOT NULL,
  status     VARCHAR(16)     NOT NULL DEFAULT 'PENDING'
             COMMENT 'PENDING | SUCCESS | FAILED | SKIPPED',
  error_msg  VARCHAR(500)    NULL,
  updated_at DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_job_order (job_id, order_id),
  KEY idx_job_status (job_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='批量任务逐条结果，前端"批量操作有结果反馈"读这张表';

-- -----------------------------------------------------------------------------
-- 8. 人工接管
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS handoff_ticket (
  id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  ticket_no         VARCHAR(32)     NOT NULL,
  conversation_id   BIGINT UNSIGNED NOT NULL,
  order_id          BIGINT UNSIGNED NULL,
  status            VARCHAR(16)     NOT NULL DEFAULT 'REQUESTED'
                    COMMENT 'REQUESTED|QUEUED|SERVING|CLOSED|CANCELLED',
  reason            VARCHAR(500)    NOT NULL DEFAULT '' COMMENT '触发原因：OCR 追问、核保异常等',
  queue_position    INT             NULL,
  assigned_agent_id BIGINT UNSIGNED NULL,
  requested_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  accepted_at       DATETIME(3)     NULL,
  closed_at         DATETIME(3)     NULL,
  close_reason      VARCHAR(500)    NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_ticket_no (ticket_no),
  KEY idx_conversation (conversation_id, status),
  KEY idx_status (status, requested_at),
  KEY idx_assigned (assigned_agent_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='人工接管工单：状态与 AI 接管状态必须一致，禁止前端自行推断';

CREATE TABLE IF NOT EXISTS handoff_event (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  ticket_id  BIGINT UNSIGNED NOT NULL,
  event_type VARCHAR(32)     NOT NULL
             COMMENT 'REQUESTED|QUEUED|ASSIGNED|ACCEPTED|MESSAGE|CLOSED',
  payload_json JSON          NULL,
  actor_type VARCHAR(16)     NOT NULL DEFAULT 'SYSTEM' COMMENT 'SYSTEM | USER | AGENT | AI',
  actor_id   BIGINT UNSIGNED NULL,
  created_at DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_ticket (ticket_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='接管事件流：前端时间线按它渲染，避免猜状态';

-- -----------------------------------------------------------------------------
-- 9. 知识库与 Agent 会话映射
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS kb_term (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  term       VARCHAR(128)    NOT NULL COMMENT '术语，如 三者险、车损险',
  normalized VARCHAR(128)    NOT NULL COMMENT '归一化键（去空格/同义词映射后的标准名）',
  category   VARCHAR(32)     NOT NULL DEFAULT '',
  definition TEXT            NOT NULL COMMENT '行业标准解释',
  synonyms   JSON            NULL COMMENT '口语说法数组：["三者","第三责"]',
  status     VARCHAR(16)     NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE | DISABLED',
  updated_at DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_normalized (normalized),
  KEY idx_category (category, status),
  KEY idx_term (term)
  -- 中文全文检索依赖 ngram 解析器：确认实例已加载后，给上一行补逗号并取消下行注释
  -- FULLTEXT KEY ft_term (term, definition) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='行业术语知识库：默认用 idx_term 前缀匹配，全文检索按需开启';

CREATE TABLE IF NOT EXISTS agent_session (
  id                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  conversation_id    BIGINT UNSIGNED NOT NULL,
  thread_id          VARCHAR(64)     NOT NULL COMMENT 'LangGraph thread_id / checkpoint 会话',
  graph_version      VARCHAR(32)     NOT NULL DEFAULT '' COMMENT '智能体编排版本，便于灰度与回滚',
  last_checkpoint_id VARCHAR(64)     NULL,
  last_active_at     DATETIME(3)     NULL,
  created_at         DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_conversation (conversation_id),
  UNIQUE KEY uk_thread (thread_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='业务 conversation ↔ Agent thread 的映射；checkpoint 本身由 C 侧存储';

-- -----------------------------------------------------------------------------
-- 10. 通用：审计与幂等
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS audit_log (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  account_id    BIGINT UNSIGNED NULL,
  action        VARCHAR(64)     NOT NULL COMMENT 'LOGIN | QUOTE_SUBMIT | PAY_CALLBACK | ...',
  resource_type VARCHAR(32)     NOT NULL DEFAULT '',
  resource_id   VARCHAR(64)     NOT NULL DEFAULT '',
  ip            VARCHAR(45)     NOT NULL DEFAULT '',
  user_agent    VARCHAR(255)    NOT NULL DEFAULT '',
  detail_json   JSON            NULL COMMENT '不得写入身份证号等敏感原文',
  created_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_account_time (account_id, created_at),
  KEY idx_resource (resource_type, resource_id),
  KEY idx_action_time (action, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='审计日志：按月归档/分区，不要与业务查询混用';

CREATE TABLE IF NOT EXISTS idempotency_key (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  idem_key      VARCHAR(64)     NOT NULL COMMENT '客户端 requestId / clientMsgId',
  scope         VARCHAR(32)     NOT NULL COMMENT 'QUOTE | PAY | UPLOAD | MESSAGE',
  request_hash  CHAR(64)        NOT NULL COMMENT '请求体指纹，防止同键不同体',
  response_json JSON            NULL COMMENT '首次结果，重复请求直接回放',
  status        VARCHAR(16)     NOT NULL DEFAULT 'DONE' COMMENT 'PROCESSING | DONE | FAILED',
  expires_at    DATETIME(3)     NULL,
  created_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_key_scope (idem_key, scope),
  KEY idx_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='幂等表：支付/报价重试必备';
