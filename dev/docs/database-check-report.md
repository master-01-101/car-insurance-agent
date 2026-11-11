# 数据库检查报告 — 车险出单智能体

> 检查时间：2026-11-11
> 检查对象：`backend/src/main/resources/sql/init.sql`
> 参照文档：`docs/数据库设计.md`、`docs/数据库关系说明.md`、`docs/数据库需求待确认.md`、`docs/数据库整合差异说明.md`
> 检查范围：纯静态分析，**未执行任何 SQL，未修改数据库**

---

## 一、SQL 文件定位

项目中共有 3 份 `.sql` 文件，最终用于建库的是：

| 文件 | 定位 | 说明 |
|---|---|---|
| `backend/src/main/resources/sql/init.sql` | **最终执行脚本（31 表）** | 整合版，已在 MySQL 3306 / 3307 两个实例执行 |
| `docs/前端-API手册/schema/mysql-schema.sql` | 前端设计草案（未执行） | 内部注明"设计草案，供成员 B 评审；未在真实实例上执行过"，部分字段/表名与最终版不一致 |
| `backend/src/main/resources/sql/archive/init_17tables_v1.sql` | 17 表旧版（已归档） | 备份在 `car_insurance_v1_backup` 库 |

**结论：以 `init.sql` 为唯一权威脚本，前端 schema 草案无需单独处理。**

---

## 二、表清单（31 张，与设计文档一致）

### A. 身份与车辆（3 张）

| # | 表名 | 主键 | 唯一约束 | 主要普通索引 |
|---|---|---|---|---|
| 1 | `sys_user` | id | username | phone、user_type+status、parent_agent_id、agent_no |
| 2 | `vehicle` | id | plate_norm、vin | owner_user_id |
| 3 | `vehicle_profile` | id | (vehicle_id, version) | conversation_id |

### B. 会话、消息与文件（5 张）

| # | 表名 | 主键 | 唯一约束 | 主要普通索引 |
|---|---|---|---|---|
| 4 | `conversation` | id | conv_no | customer_id+last_message_at、salesman_id、vehicle_id、status+stage |
| 5 | `chat_message` | id | msg_no、(conversation_id, seq)、(conversation_id, client_msg_id) | conversation_id+id、request_id |
| 6 | `message_attachment` | id | (message_id, file_id) | file_id |
| 7 | `file_object` | id | file_no | conversation_id+category、sha256、status |
| 8 | `ocr_record` | id | (file_id, recognition_request_id) | file_id+created_at、status |

### C. 人员资料与确认（3 张）

| # | 表名 | 主键 | 唯一约束 | 主要普通索引 |
|---|---|---|---|---|
| 9 | `conversation_party` | id | (conversation_id, party_role, version) | id_no_hash |
| 10 | `profile_draft` | id | draft_no | conversation_id+kind+draft_version |
| 11 | `confirmation` | id | — | conversation_id+status、kind+target_id+target_version |

### D. 方案→订单→报价→核保→支付→保单（10 张）

| # | 表名 | 主键 | 唯一约束 | 主要普通索引 |
|---|---|---|---|---|
| 12 | `insurance_plan` | id | plan_no、(conversation_id, version) | order_id、status |
| 13 | `insurance_plan_item` | id | (plan_id, coverage_code) | — |
| 14 | `insurance_order` | id | order_no | conversation_id+created_at、customer_id、salesman_id+status、status |
| 15 | `quote_record` | id | quote_no | order_id+status、external_quote_no |
| 16 | `quote_item` | id | (quote_id, coverage_code) | — |
| 17 | `underwriting_record` | id | case_no | order_id+status、quote_id |
| 18 | `underwriting_material` | id | (case_id, file_id) | file_id |
| 19 | `payment_record` | id | payment_no、out_trade_no、(channel, transaction_id) | order_id+status、transaction_id |
| 20 | `policy` | id | policy_no | order_id+status、vehicle_id |
| 21 | `policy_item` | id | (policy_id, coverage_code) | — |

### E. 运营（4 张）

| # | 表名 | 主键 | 唯一约束 | 主要普通索引 |
|---|---|---|---|---|
| 22 | `batch_job` | id | job_no、(agent_account_id, client_request_id) | status+created_at |
| 23 | `batch_job_item` | id | — | batch_id+status、customer_id |
| 24 | `handoff_task` | id | ticket_no | conversation_id+status、assigned_agent_id+status、status+requested_at |
| 25 | `handoff_event` | id | — | handoff_id+id |

### F. 辅助（6 张）

| # | 表名 | 主键 | 唯一约束 | 主要普通索引 |
|---|---|---|---|---|
| 26 | `kb_term` | id | term | normalized |
| 27 | `agent_session` | id | thread_id | conversation_id+status |
| 28 | `audit_log` | id | — | operator_id+created_at、resource_type+resource_id、action+created_at、trace_id |
| 29 | `idempotency_key` | id | idem_key | expires_at |
| 30 | `order_status_log` | id | — | order_id+id、created_at |
| 31 | `renewal_lookup` | id | lookup_no | customer_id+status、plate_number |

---

## 三、金额字段检查

全部金额/保费字段均使用 `DECIMAL`，类型符合规范，**无 FLOAT / DOUBLE / INT 混用问题**。

| 表 | 字段 | 类型 | 说明 |
|---|---|---|---|
| `insurance_plan.estimated_total` | 预估总保费 | DECIMAL(12,2) | 元，未知 NULL |
| `insurance_plan_item.sum_insured` | 保额 | DECIMAL(14,2) | 元，未选 NULL |
| `insurance_plan_item.deductible` | 免赔额 | DECIMAL(12,2) | 元，不适用 NULL |
| `insurance_plan_item.estimated_premium` | 预估保费 | DECIMAL(12,2) | 元 |
| `insurance_order.total_premium` | 总保费 | DECIMAL(12,2) | 元 |
| `insurance_order.tax_amount` | 税额 | DECIMAL(12,2) | 元 |
| `insurance_order.payable_amount` | 应付金额 | DECIMAL(12,2) | 元 |
| `quote_record.total_premium` | 总保费 | DECIMAL(12,2) | 元 |
| `quote_record.tax_amount` | 税额 | DECIMAL(12,2) | 元 |
| `quote_record.payable_amount` | 应付金额 | DECIMAL(12,2) | 元 |
| `quote_item.sum_insured` | 保额 | DECIMAL(14,2) | 元 |
| `quote_item.premium` | 保费 | DECIMAL(12,2) | 元 |
| `payment_record.amount` | 支付金额 | DECIMAL(12,2) | 元，NOT NULL |
| `policy.total_premium` | 保单总保费 | DECIMAL(12,2) | 元 |
| `policy_item.sum_insured` | 保额 | DECIMAL(14,2) | 元 |
| `policy_item.premium` | 保费 | DECIMAL(12,2) | 元 |
| `ocr_record.confidence_avg` | 平均置信度 | DECIMAL(5,4) | 0~1，类型合理 |

✅ **全部正确。** 金额单位统一为元，保额用更大精度 `DECIMAL(14,2)`，未知金额统一存 NULL。

---

## 四、时间字段检查

所有业务时间字段统一使用 `DATETIME`，**无 DATE / TIMESTAMP / DATETIME(3) 混用问题**。

| 表 | 字段 | 类型 | 默认值 | 说明 |
|---|---|---|---|---|
| `sys_user.created_at / updated_at / last_login_at` | 创建/更新/最后登录 | DATETIME | createdAt 有 CURRENT_TIMESTAMP，updatedAt 有 ON UPDATE | ✅ |
| `vehicle.created_at / updated_at` | 创建/更新 | DATETIME | 同上 | ✅ |
| `conversation.last_message_at / closed_at` | 最后消息/关闭 | DATETIME | 无默认，合理（事件触发） | ✅ |
| `insurance_order.submitted_at` | 发起报价 | DATETIME | 无默认，合理 | ✅ |
| `quote_record.requested_at / completed_at / valid_until` | 请求/完成/有效期 | DATETIME | requestedAt 有默认 | ✅ |
| `payment_record.paid_at / verified_at / expires_at` | 支付/核验/过期 | DATETIME | 无默认 | ✅ |
| `policy.effective_from / effective_to / issued_at` | 保障期/出单 | DATETIME | 无默认 | ✅ |
| `renewal_lookup.policy_year` | 保单年度 | SMALLINT | 合理（年份是整数） | ✅ |

✅ **全部正确。** `policy_year` 用 SMALLINT 而非 DATETIME 是正确的，年份是整数语义而非时间点。

---

## 五、敏感字段三列法检查

设计文档要求：`*_enc`（AES-256-GCM 密文）+ `*_hash`（HMAC-SHA256 查重）+ `*_masked`（展示值）

### 5.1 conversation_party（证件号）—— ✅ 完整三列

| 列 | 类型 | 用途 |
|---|---|---|
| `id_no_enc` | VARBINARY(512) | 密文 |
| `id_no_hash` | CHAR(64) | 哈希查重，建有索引 |
| `id_masked` | VARCHAR(32) | 脱敏展示 |

### 5.2 vehicle（发动机号）—— 🔴 缺 hash 列

| 列 | 类型 | 状态 |
|---|---|---|
| `engine_no_enc` | VARBINARY(512) | ✅ 有 |
| `engine_no_hash` | — | ❌ **缺失** |
| `engine_no_masked` | VARCHAR(32) | ✅ 有 |

**影响**：无法按发动机号查重或查询。如果业务上需要"按发动机号查车辆"（这是续保查询的常见场景），缺少 hash 列意味着每次查询必须全表扫描解密，严重影响性能。

### 5.3 vehicle.owner_name_masked — 🟡 需确认

只有脱敏列，没有加密/哈希。设计文档明确"敏感字段三列法"，车主姓名在车险业务中属于 PII（个人可识别信息），是否应该加密存储需要确认。

### 5.4 policy（保单）—— ✅ 合理

保单表中的 `applicant_name_masked`、`applicant_id_masked`、`insured_name_masked`、`insured_id_masked` 只有脱敏列。因为保单是最终产出，加密的证件号仍保存在 `conversation_party`，保单只需脱敏展示，设计合理。

---

## 六、核心业务表关系检查

### 6.1 conversation → insurance_order → quote_record → policy 主线

```
conversation (1) ──(N)──> insurance_order (1) ──(N)──> quote_record
      │                            │
      │                            ├──(N)──> underwriting_record
      │                            ├──(N)──> payment_record
      │                            └──(N)──> policy (1:N, 交强/商业各一张)
      │
      └──(1)──> agent_session ──(1)──> thread_id (LangGraph)
```

✅ 关联字段齐全，每个父表在子表都有索引。

### 6.2 conversation → agent_session → thread_id 关联

| 位置 | 字段 | 说明 |
|---|---|---|
| `agent_session.conversation_id` | BIGINT UNSIGNED | 指向 conversation.id，建有普通索引 |
| `agent_session.thread_id` | VARCHAR(64) | LangGraph thread_id，唯一约束 |
| `conversation` | — | **没有 thread_id 字段** |

✅ 设计合理：thread_id 独立存储，解耦会话与 AI 线程。一个 conversation 可以没有 thread（还没开始对话），也可以重建 thread（旧的 CLOSED 后开新的）。

🟡 **待确认**：agent_session.conversation_id 当前只是普通索引，没有唯一约束。如果业务上一个会话只允许一个活跃 thread，应改为 UNIQUE；如果允许重建（旧 session 状态变 CLOSED，创建新 session），当前设计 OK。

### 6.3 方案版本链

```
insurance_plan (version=1)
    └── previous_plan_id ──> insurance_plan (version=2)
            └── previous_plan_id ──> insurance_plan (version=3, 当前生效)
```

✅ 正确。订单通过 `plan_id + plan_version` 锁定快照。

### 6.4 订单三份快照

| 快照 | 字段 | 类型 | 目的 |
|---|---|---|---|
| 车辆快照 | `vehicle_snapshot` | JSON | 车辆资料后续修改不影响历史订单 |
| 人员快照 | `party_snapshot` | JSON | 人员改证件不影响历史订单 |
| 方案快照 | `plan_snapshot` | JSON | 方案升版不影响已下单记录 |

✅ 正确。`party_versions` JSON 额外固化各角色的资料版本号。

### 6.5 payment_record 的三道幂等防线

| 防线 | 约束 | 兜底场景 |
|---|---|---|
| 1 | `UNIQUE (out_trade_no)` | 商户单号重复提交 |
| 2 | `UNIQUE (payment_no)` | 内部支付编号重复 |
| 3 | `UNIQUE (channel, transaction_id)` | 同一渠道同一第三方流水重复入账 |

✅ 设计完善。三道防线覆盖了"前端重试"、"后端重复发单"、"第三方回调重复"三种场景。

---

## 七、SQL vs 设计文档一致性比对

逐项核对 `数据库设计.md` 第 3 节表清单 + 第 8 节 v2.1 增补字段：

| 设计文档描述 | init.sql 实际情况 | 一致性 |
|---|---|---|
| 31 张表 | CREATE TABLE IF NOT EXISTS × 31 | ✅ |
| 主键 BIGINT UNSIGNED AUTO_INCREMENT | 所有表 id 列均为该类型 | ✅ |
| 金额 DECIMAL(12,2)，保额 DECIMAL(14,2) | 所有金额字段符合 | ✅ |
| 时间 DATETIME | 所有业务时间字段符合 | ✅ |
| 不建物理外键 | 无 FOREIGN KEY 语句，只有普通索引 | ✅ |
| data_source DEMO/REAL 标识 | quote/underwriting/payment/policy 有该字段 | ✅ |
| conversation.message_count | init.sql L125 | ✅ |
| insurance_order.fail_reason | init.sql L387 | ✅ |
| insurance_plan.plan_no（唯一） | init.sql L321 | ✅ |
| insurance_plan_item.deductible | init.sql L358 | ✅ |
| file_object.storage/bucket/width/height | init.sql L189-195 | ✅ |
| policy.vehicle_id + 索引 | init.sql L538 + L563 | ✅ |
| payment_record UNIQUE (channel, transaction_id) | init.sql L527 | ✅ |
| vehicle 不允许 engine_no 明文（改为加密） | VARBINARY(512) | ✅ |
| insurance_order 不设 policy_id（保持 1:N） | policy 通过 order_id 关联 | ✅ |
| policy 保持 DATETIME 不用 DATE | effective_from/to 是 DATETIME | ✅ |
| vehicle_profile 版本唯一键 | (vehicle_id, version) UNIQUE | ✅ |
| conversation_party 版本唯一键 | (conversation_id, party_role, version) UNIQUE | ✅ |
| chat_message seq 唯一约束 | (conversation_id, seq) UNIQUE | ✅ |
| idempotency_key 幂等设计 | idem_key 唯一 + request_hash | ✅ |

✅ **全部对齐，无偏差。**

---

## 八、设计亮点

| # | 亮点 | 价值 |
|---|---|---|
| 1 | 金额全 DECIMAL，无浮点 | 杜绝金额精度丢失 |
| 2 | 证件号三列法 | 加密存密、哈希查重、脱敏展示各司其职 |
| 3 | 版本化方案 + 订单快照 | 方案升版不污染已下单记录 |
| 4 | 不建物理外键，只用索引 | 避免循环依赖、便于未来分库 |
| 5 | 模拟数据 DEMO/REAL 强制标识 | 防止测试数据伪装真实结果 |
| 6 | 支付三道幂等防线 | 覆盖前端重试、后端重复发单、回调重复 |
| 7 | chat_message 的 seq 唯一约束 | 消息顺序在数据库层保证 |
| 8 | 敏感数据边界清晰 | 证件号加密存 conversation_party；保单只存脱敏展示值 |
| 9 | ULID 业务编号与 BIGINT 主键分离 | 内部关联用自增 id，对外暴露用业务编号 |
| 10 | audit_log + idempotency_key + order_status_log | 审计、幂等、状态追溯三件套完整 |

---

## 九、发现的问题清单

### 🔴 严重（开发前必须决定）

| # | 表 | 问题 | 影响 | 建议 |
|---|---|---|---|---|
| P1 | `vehicle` | `engine_no` 只有 enc + masked，**缺 hash 列** | 无法按发动机号查重/查询，续保/历史查询场景无法高效实现 | 新增 `engine_no_hash CHAR(64)` + 普通索引，与 conversation_party 对齐 |
| P2 | `sys_user` | `agent_no` 当前是**普通索引**，不是唯一约束 | 多个业务员工号相同无法区分；前端 schema 草案曾设 UNIQUE | 改为 `UNIQUE KEY uk_sys_user_agent_no (agent_no)` |

### 🟡 一般（开发时留意）

| # | 表 | 问题 | 说明 |
|---|---|---|---|
| P3 | `vehicle` | `owner_name_masked` 只有脱敏列，无加密/哈希 | 需确认车主姓名是否属 PII 需加密；若需，按三列法补 enc/hash |
| P4 | `agent_session` | `conversation_id` 只是普通索引，不是唯一约束 | 需确认：是否允许多个 thread 对应同一会话？如果 1:1 应加 UNIQUE |
| P5 | 多表 | 部分业务表无 `data_source` 字段 | quote/underwriting/payment/policy 有；sys_user/vehicle/conversation/file_object/ocr_record 等没有。若 DEMO 数据会写入这些表，也应加 |
| P6 | `chat_message` | 缺少与 `agent_session` 的直接关联 | 当前通过 conversation 间接关联；如果需要"消息是哪个 thread_id 生成的"可考虑加字段，但非必需 |
| P7 | 多表 | 没有 `created_by` / `updated_by` 字段 | 行级修改人追踪需要查 audit_log，单表不自描述 |

### 🟢 待确认（设计文档已列出，SQL 本身没问题）

这些问题在 `数据库需求待确认.md` 中已记录，属于**跨团队契约问题**，不是 SQL 脚本缺陷：

| # | 问题 | 来源 | SQL 状态 |
|---|---|---|---|
| Q1 | 保单 1:1 vs 1:N | 待确认 #1 | SQL 已按 1:N 设计 ✅ |
| Q2 | 会话 stage 枚举 | 待确认 #2 | SQL 用 AI 集合（大写）✅ |
| Q3 | 业务员角色 SALESMAN vs AGENT | 待确认 #3 | SQL 用 SALESMAN ✅ |
| Q4 | 金额单位字符串 vs 数字 | 待确认 #8 | SQL 用 DECIMAL，接口层处理格式即可 |
| Q5 | 确认令牌签发方 | 待确认 #9 | SQL 只存 token_hash ✅ |
| Q6 | 消息 seq 取号方式（Redis vs DB） | 待确认 #10 | SQL 唯一约束已建 ✅ |
| Q7 | 报价/核保推进方（AI 调用 vs 后台任务） | 待确认 #14 | SQL 状态机完备 ✅ |
| Q8 | 批量办理授权模型 | 待确认 #15 | SQL batch_job_item 只记 customer_id |
| Q9 | 支付状态 SUCCESS vs PAID | 待确认 #13 | SQL 用 SUCCESS ✅ |

---

## 十、是否可以进入后端开发阶段

### 结论：**可以进入，但建议先处理 P1、P2、P4 三个问题**

| 维度 | 评估 |
|---|---|
| 表数量 | ✅ 31 张，与设计一致 |
| 主键/唯一约束 | ✅ 所有业务编号均有唯一约束 |
| 金额字段 | ✅ 全 DECIMAL，无 FLOAT/DOUBLE |
| 时间字段 | ✅ 全 DATETIME，无混用 |
| 敏感字段 | 🟡 conversation_party 完整三列法；vehicle.engine_no 缺 hash；owner_name 需确认 |
| 表关系 | ✅ 所有关联字段存在并有索引；不建物理外键符合设计 |
| 状态字段 | ✅ 全 VARCHAR 大写，无 ENUM |
| SQL vs 文档 | ✅ 完全一致 |
| 待确认项 | 🟢 9 项跨团队契约待 A/C 确认，但 SQL 本身已选默认值并符合设计 |

### 建议进入开发前的快速决策

| 问题 | 建议方案 | 改动量 |
|---|---|---|
| P1: vehicle.engine_no 缺 hash | 加一列 `engine_no_hash CHAR(64)` + 索引 | `ALTER TABLE` 加一列，Entity 加一个字段，不影响已有列 |
| P2: agent_no 唯一 | 改索引为 UNIQUE | 若现有数据无重复则直接 `ALTER INDEX` |
| P4: agent_session.conversation_id 唯一 | 加 UNIQUE（如果业务确定 1:1） | `ALTER TABLE` 加约束，不影响已有数据 |

三项改动都是**加列或改约束**，不涉及删列或改类型，**可以在已建库上安全执行**（在开发库 3306/3307 验证后再考虑生产）。

### 建议开发顺序

1. **先处理 P1/P2/P4**（3 分钟 DDL）
2. 按项目进度总览第 9 节：Maven 骨架 → MySQL+Redis+MyBatis-Plus 连通性 → 统一返回 → Entity/Mapper → JWT 鉴权 → 业务 API 逐阶段实现
3. 开发过程中 A/C 可以并行确认 Q1–Q9 跨团队契约，不阻塞 Entity/Mapper 层编码（因为 SQL 已有合理默认值）
