# 数据库初始化说明（整合版 · 31 张表 · v2.1）

> 两个实例的 `car_insurance` 都已建好 31 张表（v2.1）：
> 本机 MySQL **3306**（root / 20050623Slq）与 Docker MySQL **3307**（root / change_me_dev_only）。
> 两边旧的 17 张表都已整体搬到各自的 `car_insurance_v1_backup`，未删除任何数据。

脚本：[init.sql](init.sql)
设计文档：[../../../../../docs/数据库设计.md](../../../../../docs/数据库设计.md)

## 1. 包含什么

| 模块 | 表 |
|---|---|
| 身份与车辆 | `sys_user`、`vehicle`、`vehicle_profile` |
| 会话与文件 | `conversation`、`chat_message`、`message_attachment`、`file_object`、`ocr_record` |
| 人员与确认 | `conversation_party`、`profile_draft`、`confirmation` |
| 方案与订单 | `insurance_plan`、`insurance_plan_item`、`insurance_order` |
| 报价/核保/支付/保单 | `quote_record`、`quote_item`、`underwriting_record`、`underwriting_material`、`payment_record`、`policy`、`policy_item` |
| 运营 | `batch_job`、`batch_job_item`、`handoff_task`、`handoff_event` |
| 辅助 | `kb_term`、`agent_session`、`audit_log`、`idempotency_key`、`order_status_log`、`renewal_lookup` |

## 2. 脚本特性

- 只有 `CREATE DATABASE IF NOT EXISTS` + `CREATE TABLE IF NOT EXISTS`
- **不含** `DROP` / `TRUNCATE` / `DELETE` / `ALTER`
- 可重复执行，不会清空已有数据
- 不建物理外键，关系由应用层维护

## 3. 重复执行说明

正式库现在已是 31 张表。脚本只有 `CREATE TABLE IF NOT EXISTS`，重复执行会跳过已存在的表，**不会清空数据**。

但要注意：`IF NOT EXISTS` 只保证“表存在就不动”，**不会补字段**。以后脚本加字段时，必须另写 `ALTER TABLE` 迁移脚本，不能指望重跑 init.sql。

如果你在别的机器上从 17 表旧版升级，安全做法是先建新库验证：

```bash
# 1. 复制脚本并把库名改成 car_insurance_v2（只在 /tmp 操作，不改仓库文件）
sed 's/`car_insurance`/`car_insurance_v2`/g' \
  backend/src/main/resources/sql/init.sql > /tmp/init_v2.sql

# 2. 在本机 MySQL 3306 执行（密码用环境变量，不写进命令历史）
export DB_PASS='你的本机密码'
mysql -h127.0.0.1 -P3306 -uroot -p"$DB_PASS" < /tmp/init_v2.sql

# 3. 验证表数量，应为 31
mysql -h127.0.0.1 -P3306 -uroot -p"$DB_PASS" -N -e \
  "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='car_insurance_v2';"

# 4. 确认无误后再决定是否替换正式库
```

Docker（3307）同理：

```bash
docker exec -i car-insurance-mysql \
  sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD"' < /tmp/init_v2.sql
```

## 4. 本地环境信息

| 环境 | 地址 | 账号 | 说明 |
|---|---|---|---|
| 本机 MySQL | 127.0.0.1:3306 | `root` / 你的密码 | MySQL 9.7，已有旧 17 表 |
| Docker MySQL | 127.0.0.1:3307 | `root` / 容器环境变量 | MySQL 8.0，已有旧 17 表 |
| Redis | 127.0.0.1:6379 | 无密码 | 会话 seq 取号、JWT 黑名单可用 |

> 密码只从环境变量或 `.env` 读取，禁止写进脚本、文档或 Git。

## 5. 验证清单

```sql
-- 表数量
SELECT COUNT(*) FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'car_insurance_v2';

-- 是否有非 InnoDB 表（应为空）
SELECT TABLE_NAME, ENGINE FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'car_insurance_v2' AND ENGINE <> 'InnoDB';

-- 是否有非 utf8mb4 表（应为空）
SELECT TABLE_NAME, TABLE_COLLATION FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'car_insurance_v2'
  AND TABLE_COLLATION NOT LIKE 'utf8mb4%';

-- 唯一约束抽查
SHOW INDEX FROM insurance_order WHERE Key_name = 'uk_insurance_order_no';
```

## 6. 常见问题

| 问题 | 原因 | 处理 |
|---|---|---|
| `Table already exists` 后字段不全 | 旧库已有同名表，`IF NOT EXISTS` 跳过 | 用新库验证，或手工 `ALTER` |
| 中文注释乱码 | 客户端字符集不是 utf8mb4 | 连接串加 `characterEncoding=utf8` |
| 时间差 8 小时 | 时区不是 +08:00 | 连接串加 `serverTimezone=Asia/Shanghai` |
| 身份证号明文出现在日志 | 业务代码直接打印 | 只允许输出 `id_masked`，禁止打印 `id_no_enc` 解密值 |

## 7. 模拟数据约定

`quote_record`、`underwriting_record`、`payment_record`、`policy` 都有 `data_source` 字段：

- `DEMO`：Mock 流程产生，接口必须明确提示“模拟数据”
- `REAL`：接入真实保险公司/支付平台后写入

**禁止**把 `DEMO` 数据展示成真实报价、真实支付成功或真实保单。
