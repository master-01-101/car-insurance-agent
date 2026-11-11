# 车险出单智能体 · API 手册 v1.0

> **状态：设计稿，尚未实现。** 以 `docs/数据库设计总结.md` 的 27 张表和
> 《车险出单需求.md》的功能模块为基础逐项定义。
> 本手册是接口的**唯一权威来源**；`docs/前端API对接清单.md` 是早期讨论记录，
> 两者冲突时以本手册为准。
>
> 本版统一的命名（此前文档里存在两种写法）：
> `/api/quotations`、`/api/underwritings`、`/api/handoffs`、`/api/workbench/batches`；
> 路径参数统一为 `{conversationId}` `{planId}` `{quotationId}` `{caseId}`
> `{paymentId}` `{policyId}` `{orderId}` `{ticketId}` `{fileId}`。

---

## 0. 给三类人的读法

| 角色 | 读哪几章 |
| --- | --- |
| 成员 A（前端） | 1 通用规范、2 枚举、3 总览、4 详解、7 占位符映射、9 接入顺序 |
| 成员 B（后端） | 全部；重点 4 详解、8 表映射、10 待确认 |
| 成员 C（Agent） | 4.3 消息、4.5 OCR、4.7 方案、4.14 接管；其余只需知道 DTO 形状 |

**接口总数 47**（其中 `GET .../stream` 为二期 SSE）。

---

## 1. 通用规范

### 1.1 基地址与版本

```
https://{host}/api
```

- 不引入 `/v1`；出现破坏性变更时新增 `/api/v2` 并存一段时间。
- 前后端同源部署时用相对路径 `/api`（前端已按此实现，`VITE_API_BASE_URL`）。

### 1.2 鉴权

- `Authorization: Bearer <accessToken>`，除了 `POST /api/auth/login`、`POST /api/auth/refresh` 与支付回调。
- accessToken 有效期 2 小时，refreshToken 7 天（数值待定）。
- 前端拦截器已实现注入（`frontend/src/api/http.ts`），token 存在 `localStorage['chexian.token']`。
- **身份只从 token 解析**：不接受请求体或查询参数里的 `userId` / `role` 作为授权依据。

### 1.3 请求头

| 头 | 必填 | 说明 |
| --- | --- | --- |
| `Authorization` | 是 | `Bearer <token>` |
| `Content-Type` | 是 | 除上传外均为 `application/json` |
| `X-Request-Id` | 否 | 客户端生成的 UUID，原样回显，用于串联日志 |
| `Idempotency-Key` | 视接口 | 支付/报价/批量/确认类 POST 必填 |
| `Accept-Language` | 否 | 默认 `zh-CN` |

### 1.4 响应包络

成功与失败**同一结构**（与《前端.md》第五节一致，仅追加两个可选字段）：

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "errorCode": "PLAN_NOT_CONFIRMED",
  "requestId": "8f1f-..."
}
```

- `code`：与 HTTP 状态码同值。
- `errorCode`：仅失败时出现，机器可读，前端据此分支文案。
- 校验失败时 `data` 为 `{ "errors": [{ "field": "idNo", "message": "格式不正确" }] }`。

### 1.5 错误码

| code | 含义 | 典型 errorCode |
| --- | --- | --- |
| 200 | 成功 | — |
| 400 | 参数格式错误 | `INVALID_ARGUMENT` |
| 401 | 未登录 / token 失效 | `AUTH_TOKEN_EXPIRED`、`AUTH_INVALID_CREDENTIALS` |
| 403 | 越权（非本人会话、非业务员） | `FORBIDDEN_RESOURCE` |
| 404 | 资源不存在 | `NOT_FOUND` |
| 409 | 状态冲突 | `PLAN_NOT_CONFIRMED`、`QUOTATION_EXPIRED`、`PAYMENT_ALREADY_PAID`、`HANDOFF_ALREADY_OPEN` |
| 422 | 业务校验不通过 | `VEHICLE_PROFILE_INCOMPLETE`、`APPLICANT_NOT_CONFIRMED`、`OCR_NO_FIELDS` |
| 429 | 限流 | `RATE_LIMITED` |
| 500 | 服务内部错误 | `INTERNAL_ERROR` |
| 502 | 上游异常（保险公司/支付/OCR） | `UPSTREAM_CARRIER_ERROR`、`UPSTREAM_PAYMENT_ERROR` |

**前端约定**：`409 / 422` 必须展示 `message`（后端要写成用户能看懂的话），不做静默重试。

### 1.6 分页

统一游标分页，不用 `offset`：

```
GET ...?cursor=<lastId>&limit=20
data: { "items": [...], "nextCursor": "msg_...", "hasMore": true }
```

- `limit` 默认 20，最大 100；`cursor` 为空表示第一页。
- 列表项必须带 `updatedAt` 或 `createdAt`，前端按服务端顺序渲染，不再排序。

### 1.7 幂等

| 场景 | 机制 |
| --- | --- |
| 发消息 | 请求体 `clientMsgId`（前端生成 UUID），`(conversationId, clientMsgId)` 唯一 |
| 报价 / 支付 / 批量 / 确认 | 请求头 `Idempotency-Key`，重复请求回放首次结果，并带 `Idempotent-Replayed: true` |
| 上传 | `sha256` 命中已有文件则直接复用，秒传 |

### 1.8 字段约定

| 项 | 约定 |
| --- | --- |
| ID | 字符串（ULID 26 位），如 `"01JBX..."`；不要用自增数字，前端一律按 `string` 处理 |
| 时间 | ISO 8601 带时区，如 `"2026-11-11T10:12:00+08:00"` |
| 金额 | number，单位**元**，两位小数（`3268.00`）；不是分，不是字符串 |
| 保额 | number，单位**元**；不适用为 `null` |
| 证件号 | 只返回 `idMasked`（如 `4403**********8821`），**永不返回完整号** |
| 空值 | 统一 `null`，不用 `""` 表示缺失（字符串字段为空串即"无此项"） |
| 枚举 | 大写，与数据库一致；例外见下 |

> **唯一的例外**：`message.role` 用 `user | assistant | tool` 小写，
> 与《前端.md》第五节的聊天消息结构一致。
>
> **stage 大小写差异**：接口返回 `COLLECTING / RECOMMENDED / IDENTITY / QUOTED /
> UNDERWRITING / PAYING / ISSUED / ABORTED`；前端内部的 `FlowStageKey` 是小写
> （`collect/recommend/...`）。接入时需在 `utils/` 加一层映射，不要改 UI 常量。

### 1.9 权限矩阵

| 接口组 | USER | AGENT |
| --- | --- | --- |
| `auth/*` | ✔ | ✔ |
| `conversations/*`、`messages`、`files`、`plans`、`quotations`、`underwritings`、`payments`、`policies` | 仅本人 | 本人 + 归属客户（`agent_account_id`） |
| `orders/*` | 仅本人 | 归属客户 |
| `workbench/*` | ✘ | ✔ |
| `handoffs/{ticketId}/messages`、`close` | ✘ | ✔（仅被指派的坐席） |
| `knowledge/terms` | ✔ | ✔ |

### 1.10 限流

默认 60 次/分钟/用户；上传 10 次/分钟；发消息 20 次/分钟。超限返回 429 + `Retry-After`。

---

## 2. 枚举字典

| 枚举 | 取值 |
| --- | --- |
| `ConversationStage` | `COLLECTING` `RECOMMENDED` `IDENTITY` `QUOTED` `UNDERWRITING` `PAYING` `ISSUED` `ABORTED` |
| `ConversationStatus` | `ACTIVE` `HANDOFF` `CLOSED` |
| `OrderStatus` | `COLLECTING` `RECOMMENDING` `IDENTITY` `QUOTED` `UNDERWRITING` `PAY_PENDING` `ISSUED` `FAILED` `CANCELLED` |
| `PlanStatus` | `PROPOSED` `CONFIRMED` `SUPERSEDED` |
| `PlanSource` | `RECOMMEND` `REVISE` `REUSE_LAST_YEAR` |
| `QuotationStatus` | `QUOTING` `QUOTED` `FAILED` `EXPIRED` |
| `UnderwritingStatus` | `PENDING` `PASS` `REJECT` `NEED_MATERIAL` |
| `PaymentStatus` | `CREATED` `PENDING` `SUCCESS` `FAILED` `CLOSED` `REFUNDED` |
| `HandoffStatus` | `REQUESTED` `QUEUED` `SERVING` `CLOSED` `CANCELLED` |
| `MessageRole` | `user` `assistant` `tool` |
| `MessageType` | `text` `image` `plan` `status` `handoff` |
| `FileCategory` | `LICENSE_MAIN` `LICENSE_SUB` `IDCARD_FRONT` `IDCARD_BACK` `INSPECTION_PHOTO` `INSPECTION_VIDEO` `CHAT_IMAGE` |
| `CoverageCode` | `CI`（交强险）`TPL`（三者）`VEHICLE_DAMAGE`（车损）`DEDUCTIBLE`（不计免赔）… |
| `UserRole` | `USER` `AGENT` |

**状态流转由后端裁决**，前端只渲染。前端拿到未知枚举值时应原样显示并降级为中性样式，不要崩。

---

## 3. 接口总览

| # | 方法 | 路径 | 用途 | 权限 |
| --- | --- | --- | --- | --- |
| 1 | POST | `/api/auth/login` | 登录 | 公开 |
| 2 | POST | `/api/auth/refresh` | 续期 | 公开 |
| 3 | POST | `/api/auth/logout` | 登出 | 登录 |
| 4 | GET | `/api/auth/profile` | 当前身份 | 登录 |
| 5 | GET | `/api/conversations` | 会话列表 | 登录 |
| 6 | POST | `/api/conversations` | 新建/复用会话 | 登录 |
| 7 | GET | `/api/conversations/{conversationId}` | 会话详情 | 本人/归属业务员 |
| 8 | PATCH | `/api/conversations/{conversationId}` | 关闭会话 | 本人 |
| 9 | GET | `/api/conversations/{conversationId}/messages` | 历史消息 | 同上 |
| 10 | POST | `/api/conversations/{conversationId}/messages` | 发消息（含 AI 回复） | 本人 |
| 11 | GET | `/api/conversations/{conversationId}/stream` | SSE 流式（二期） | 本人 |
| 12 | POST | `/api/files` | 上传图片 | 登录 |
| 13 | GET | `/api/files/{fileId}/url` | 重新签发访问地址 | 上传人 |
| 14 | DELETE | `/api/files/{fileId}` | 删除 | 上传人 |
| 15 | POST | `/api/ocr/license` | 行驶证识别 | 登录 |
| 16 | POST | `/api/ocr/idcard` | 身份证识别 | 登录 |
| 17 | POST | `/api/conversations/{conversationId}/vehicle-profile/confirm` | 保存车辆资料 | 本人 |
| 18 | POST | `/api/conversations/{conversationId}/applicant/confirm` | 保存投保人资料 | 本人 |
| 19 | GET | `/api/vehicles/lookup` | 续保查询 | 登录 |
| 20 | GET | `/api/vehicles/{vehicleId}` | 车辆详情 | 本人/归属业务员 |
| 21 | POST | `/api/plans/recommend` | 生成推荐方案 | 本人 |
| 22 | POST | `/api/plans/{planId}/revise` | 自然语言改方案 | 本人 |
| 23 | POST | `/api/plans/{planId}/confirm` | 确认方案 | 本人 |
| 24 | GET | `/api/plans/{planId}` | 方案详情 | 本人 |
| 25 | GET | `/api/conversations/{conversationId}/plans` | 方案版本列表 | 本人 |
| 26 | POST | `/api/quotations` | 发起报价 | 本人 |
| 27 | GET | `/api/quotations/{quotationId}` | 报价详情 | 本人 |
| 28 | POST | `/api/underwritings` | 提交核保 | 本人 |
| 29 | GET | `/api/underwritings/{caseId}` | 核保详情 | 本人 |
| 30 | POST | `/api/underwritings/{caseId}/materials` | 补验车材料 | 本人 |
| 31 | POST | `/api/payments` | 创建支付 | 本人 |
| 32 | GET | `/api/payments/{paymentId}` | 支付状态 | 本人 |
| 33 | POST | `/api/payments/{paymentId}/close` | 关闭支付 | 本人 |
| 34 | GET | `/api/policies` | 保单列表 | 本人 |
| 35 | GET | `/api/policies/{policyId}` | 保单明细 | 本人 |
| 36 | GET | `/api/orders` | 订单列表 | 本人 |
| 37 | GET | `/api/orders/{orderId}` | 订单详情 | 本人 |
| 38 | GET | `/api/workbench/orders` | 客户订单列表 | AGENT |
| 39 | POST | `/api/workbench/batches` | 批量操作 | AGENT |
| 40 | GET | `/api/workbench/batches/{jobId}` | 批量任务进度 | AGENT |
| 41 | GET | `/api/workbench/batches/{jobId}/items` | 批量逐条结果 | AGENT |
| 42 | POST | `/api/handoffs` | 转人工 | 本人 |
| 43 | GET | `/api/handoffs/{ticketId}` | 接管详情 | 本人/坐席 |
| 44 | GET | `/api/handoffs/{ticketId}/events` | 接管事件流 | 本人/坐席 |
| 45 | POST | `/api/handoffs/{ticketId}/messages` | 坐席回复 | AGENT |
| 46 | POST | `/api/handoffs/{ticketId}/close` | 结束接管 | AGENT |
| 47 | GET | `/api/knowledge/terms` | 术语查询 | 登录 |

**内部接口**（前端不调用）：`POST /api/payments/callback` 支付回调、
`GET /api/internal/files/{fileId}/content` 供 OCR 服务取图。

---

## 4. 接口详解

> 记法：`→` 后为 `data` 字段；「前置」为后端必须校验的业务条件。

### 4.1 认证

**`POST /api/auth/login`**
请求：`username`、`password`。**不接受前端传 `role`** —— 角色由账号决定。
→ `accessToken`、`refreshToken`、`expiresIn`、`user{ userId, userName, role, agentId, agentNo }`
错误：401 `AUTH_INVALID_CREDENTIALS`；423 账号锁定。
注：`agentId` 是需求里"登录自带业务员 ID"，即 `account.parent_agent_id`。

> ⚠️ 前端影响：登录页当前有"普通用户/业务员"切换，接真实鉴权后该切换必须移除或降级为纯展示，
> 否则用户能自选角色，属越权。

**`POST /api/auth/refresh`** → `accessToken`、`expiresIn`。错误 401。

**`POST /api/auth/logout`** → 无。幂等。

**`GET /api/auth/profile`** → 同 login 的 `user`。

### 4.2 会话

**`GET /api/conversations`** 查询：`status`、`cursor`、`limit`
→ `items[]`：`conversationId`、`plateNumber`、`stage`、`status`、`lastMessagePreview`、
`lastMessageAt`、`hasHandoff`、`updatedAt`

**`POST /api/conversations`** 请求：`plateNumber`（可空，空则由 Agent 首轮对话确定）
→ 会话对象。规则：同一用户 + 同号牌且存在 `ACTIVE` 会话时**复用**，否则新建。
错误：409 `CONVERSATION_LIMIT`（单用户并发会话上限，待定）。

**`GET /api/conversations/{conversationId}`**
→ `conversationId`、`plateNumber`、`stage`、`status`、`vehicle`、`vehicleProfile`、
`applicantMasked`、`activePlanId`、`activeQuotationId`、`activePaymentId`、`policyId`、
`handoff{ ticketId, status, queuePosition }`、`messageCount`、`updatedAt`
一次给全，避免前端进会话后串行拉 5 个接口。

**`PATCH /api/conversations/{conversationId}`** 请求：`status`（仅允许 `CLOSED`）
→ 会话对象。关闭后不可再发消息（409 `CONVERSATION_CLOSED`）。

### 4.3 消息

**`GET /api/conversations/{conversationId}/messages`**
查询：`cursor`、`limit`（默认 30）、`direction`（`backward` 默认）
→ `items[]`（**倒序返回，前端插到顶部**）、`nextCursor`、`hasMore`
`Message`：`messageId`、`conversationId`、`role`、`messageType`、`content`、
`payload`、`createdAt`

**`POST /api/conversations/{conversationId}/messages`**
请求：`content`、`clientMsgId`（必填）、`messageType`（默认 `text`）、`fileIds[]`（图片消息）
→ `userMessage`、`assistantMessage`（同步返回 AI 回复；二期 SSE 时可为 `null`）、
`conversation{ stage, status }`、`handoff{ ticketId, status }`
错误：409 会话已关闭；429 限流；502 Agent 不可用（此时 `assistantMessage` 为一条
`messageType=status`、`statusCode=AGENT_UNAVAILABLE` 的降级消息，前端照常渲染）。
**幂等**：同 `clientMsgId` 重复提交返回首次结果，不重复落库。

**`GET /api/conversations/{conversationId}/stream`**（二期，SSE）
事件：`message.delta`、`message.done`、`stage.changed`、`handoff.created`、`heartbeat`。
本期先同步返回，前端不要提前实现。

### 4.4 文件

**`POST /api/files`** `multipart/form-data`
字段：`file`、`category`、`conversationId`（可选）
→ `fileId`、`url`、`expiresAt`、`size`、`mimeType`、`sha256`
限制：JPG/PNG/WebP，≤10MB（前端 `utils/file.ts` 已校验，服务端必须二次校验）。
错误：413 `FILE_TOO_LARGE`、415 `FILE_TYPE_UNSUPPORTED`。
**关键**：返回的 `url` 是短期签名地址（建议 10 分钟），前端只保存 `fileId`；
后续 OCR / 报价 / 核保都用 `fileId` 引用，**不回传二进制**。

**`GET /api/files/{fileId}/url`** → `url`、`expiresAt`（预览过期后重新取）。

**`DELETE /api/files/{fileId}`** → 无。已被业务引用的文件返回 409 `FILE_IN_USE`。

### 4.5 OCR 与资料确认

**`POST /api/ocr/license`** 请求：`fileId`、`side`（`main`|`sub`）
→ `fileId`、`side`、`recognizedAt`、`provider`、
`fields[]：{ key, label, value, confidence, abnormal }`、`missingFields[]`
`key` 固定为：`plateNumber` `vin` `engineNo` `ownerName` `registerDate` `usageNature`。
错误：422 `OCR_NO_FIELDS`（整张图无有效字段）；502 `UPSTREAM_OCR_ERROR`。
**约定**：识别失败**不要 5xx 到底**，能出部分字段就返回 200 + `abnormal=true`。

**`POST /api/ocr/idcard`** 请求：`fileId`、`side`（`front`|`back`）
→ `name`、`idMasked`、`validTo`、`abnormalFields[]`
**不返回完整证件号**；如需入库由后端自行识别并加密。

**`POST /api/conversations/{conversationId}/vehicle-profile/confirm`**
请求：`fields{ plateNumber, vin, engineNo, ownerName, registerDate, usageNature }`、
`correctedFields[]`、`source`（`OCR`|`MANUAL`）、`ocrFileId`
→ `vehicle{...}`、`missingFields[]`、`stage`
前置：缺字段不阻止保存，但要回 `missingFields` 让前端高亮；进入报价前必须为空。
错误：422 `VEHICLE_PROFILE_INCOMPLETE`（可选：仅当 `strict=true`）。

**`POST /api/conversations/{conversationId}/applicant/confirm`**
请求：`name`、`idNo`、`idType`、`correctedFields[]`
→ `applicantMasked{ name, idMasked, idType }`、`stage`
错误：422 `ID_NO_INVALID`。`idNo` 只在传输中出现一次，不回显。

### 4.6 车辆

**`GET /api/vehicles/lookup`** 查询：`plateNumber`、`vin`（至少一个）
→ `matched`（布尔）、`vehicle`、`lastPolicy{ policyNo, effectiveFrom, effectiveTo, totalPremium, items[] }`、`lastPlan`
**未命中必须 200 + `matched:false`**，不要 404（前端会把 404 当异常）。

**`GET /api/vehicles/{vehicleId}`** → `vehicle`（同 4.5 的字段集）。

### 4.7 方案

**`POST /api/plans/recommend`** 请求：`conversationId`、`mode`（`RECOMMEND`|`REUSE_LAST_YEAR`）
→ `Plan`（见下）。前置：车辆资料已确认（`missingFields` 为空），否则 422。
`Plan`：`planId`、`conversationId`、`version`、`parentPlanId`、`insuranceCompany`、
`companyCode`、`totalPremium`、`status`、`source`、`changedItems[]`、
`items[]：{ itemId, coverageCode, coverageName, coverageAmount, premium, required }`、
`confirmedAt`、`createdAt`

**`POST /api/plans/{planId}/revise`**
请求：`instruction`（自然语言原文）、`clientRequestId`
→ **新版** `Plan`（`version + 1`，`parentPlanId` 指向旧版），
`changedItems[]：{ field, label, from, to }` 供气泡里说明"改了什么"
错误：409 `PLAN_ALREADY_CONFIRMED`（已确认的方案不能再改，需先新建版本）；
422 `INSTRUCTION_UNPARSABLE`（Agent 无法解析时，返回给用户追问）。

**`POST /api/plans/{planId}/confirm`** → `planId`、`status=CONFIRMED`、`confirmedAt`
副作用：同会话其它 `PROPOSED` 方案置为 `SUPERSEDED`。

**`GET /api/plans/{planId}`** → `Plan`
**`GET /api/conversations/{conversationId}/plans`** → `items[]`（按 `version` 倒序）

### 4.8 报价

**`POST /api/quotations`** 请求：`conversationId`、`planId`（`Idempotency-Key` 必填）
→ `quotationId`、`status`、`totalPremium`、`validUntil`、
`items[]`、`failReason`
前置：`plan.status == CONFIRMED`，否则 409 `PLAN_NOT_CONFIRMED`；
身份证已确认，否则 422 `APPLICANT_NOT_CONFIRMED`。
失败也要落库：`status=FAILED` + `failReason`，HTTP 200（业务失败不等于接口失败）。
上游异常才用 502。

**`GET /api/quotations/{quotationId}`** → 同上；`EXPIRED` 由后端按 `validUntil` 计算。

### 4.9 核保

**`POST /api/underwritings`** 请求：`quotationId`
→ `caseId`、`status`、`reason`、`requiredMaterials[]`、`decidedAt`
`status` 三态：`PASS` / `REJECT` / `NEED_MATERIAL`。
`requiredMaterials` 取值：`INSPECTION_PHOTO`、`INSPECTION_VIDEO`。
前置：报价 `QUOTED` 且未过期，否则 409 `QUOTATION_EXPIRED`。

**`GET /api/underwritings/{caseId}`** → 同上。
**`POST /api/underwritings/{caseId}/materials`** 请求：`fileIds[]`、`materialType`
→ 更新后的 `case`（`NEED_MATERIAL` 补完回 `PENDING`）。

### 4.10 支付

**`POST /api/payments`** 请求：`quotationId`、`channel`（`WECHAT`|`ALIPAY`|`MOCK`）
→ `paymentId`、`status`、`amount`、`payUrl`、`expiresAt`
前置：核保 `PASS`，否则 409 `UNDERWRITING_NOT_PASSED`；
资金状态已成功则 409 `PAYMENT_ALREADY_PAID`。
前端拿到 `payUrl` 后整页跳转，不要内嵌 iframe。

**`GET /api/payments/{paymentId}`** → `paymentId`、`status`、`amount`、`paidAt`、`policyId`
回跳后前端**只**轮询这个接口（建议 2s 间隔、60s 超时），`SUCCESS` 才展示已支付/已出单。

**`POST /api/payments/{paymentId}/close`** → 关闭未支付订单，幂等。

### 4.11 保单

**`GET /api/policies`** 查询：`cursor`、`limit`
→ `items[]：{ policyId, policyNo, plateNumber, effectiveFrom, effectiveTo, status, issuedAt }`

**`GET /api/policies/{policyId}`**
→ `policyNo`、`applicant{ name, idType, idMasked }`、`insured{...}`、`vehicle{...}`、
`items[]：{ coverageName, coverageAmount, premium }`、`totalPremium`、
`effectiveFrom`、`effectiveTo`、`terms[]`、`issuedAt`、`status`
前置：只有支付确认成功才存在保单记录；未出单返回 404 `POLICY_NOT_FOUND`。

### 4.12 订单

**`GET /api/orders`** 查询：`status`、`cursor`、`limit`
→ `items[]：{ orderId, orderNo, plateNumber, status, totalPremium, conversationId, policyId, updatedAt }`

**`GET /api/orders/{orderId}`** → 订单 + `plan` / `quotation` / `underwriting` / `payment` 摘要
（一次返回，前端不拼装）。

### 4.13 业务员工作台

**`GET /api/workbench/orders`** 查询：`status`、`keyword`（车牌/客户）、`cursor`、`limit`
→ 同订单列表 DTO + `customerName`、`agentName`

**`POST /api/workbench/batches`** 请求：`action`（`QUOTE`|`UNDERWRITING`|`PAYMENT`|`HANDOFF`）、`orderIds[]`
→ `jobId`、`accepted`、`skipped`（附跳过原因）、`status`
前置：`orderIds` ≤ 100；跨业务员订单返回 403。
**异步语义**：立即返回 `jobId`，前端轮询 40 号接口看进度。

**`GET /api/workbench/batches/{jobId}`** → `jobId`、`action`、`totalCount`、`successCount`、`failCount`、`status`、`finishedAt`
**`GET /api/workbench/batches/{jobId}/items`** → `items[]：{ orderId, plateNumber, status, errorMsg }`
—— 验收要求"批量操作有结果反馈"，这条不能省。

### 4.14 人工接管

**`POST /api/handoffs`** 请求：`conversationId`、`reason`
→ `ticketId`、`status`、`queuePosition`、`requestedAt`
前置：已有未关闭工单返回 409 `HANDOFF_ALREADY_OPEN`（前端应跳详情而不是重复申请）。
副作用：会话 `status=HANDOFF`，AI 暂停自动回复。

**`GET /api/handoffs/{ticketId}`** → `ticketId`、`status`、`reason`、`queuePosition`、
`assignedAgentName`、`requestedAt`、`acceptedAt`、`closedAt`

**`GET /api/handoffs/{ticketId}/events`** → `items[]：{ eventId, eventType, payload, actorType, createdAt }`
前端时间线只按这个渲染，不要自己按 status 拼。

**`POST /api/handoffs/{ticketId}/messages`**（AGENT）
请求：`content` → 落 `message` 表（`sender_type=HUMAN_AGENT`），前端通过消息接口看到。

**`POST /api/handoffs/{ticketId}/close`**（AGENT）请求：`reason` → `status=CLOSED`，会话回到 `ACTIVE`。

### 4.15 知识库

**`GET /api/knowledge/terms`** 查询：`q`、`limit`（默认 10）
→ `items[]：{ termId, term, definition, synonyms[] }`
用于聊天里对"三者""车损"这类口语的解释；查不到返回空数组，不要 404。

---

## 5. 典型报文

**登录**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "accessToken": "eyJhbGciOi...",
    "refreshToken": "rft_...",
    "expiresIn": 7200,
    "user": { "userId": "acc_01JBX", "userName": "张三", "role": "USER", "agentId": "acc_01JBY", "agentNo": "A1007" }
  }
}
```

**发消息（同步返回 AI 回复）**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userMessage": {
      "messageId": "msg_01JC1", "conversationId": "conv_01JBZ", "role": "user",
      "messageType": "text", "content": "把三者险提到 200 万", "payload": null,
      "createdAt": "2026-11-11T10:20:01+08:00"
    },
    "assistantMessage": {
      "messageId": "msg_01JC2", "conversationId": "conv_01JBZ", "role": "assistant",
      "messageType": "plan", "content": "已调整为 v2，三者险保额 200 万。",
      "payload": { "planId": "pln_01JC0", "version": 2 },
      "createdAt": "2026-11-11T10:20:03+08:00"
    },
    "conversation": { "stage": "RECOMMENDED", "status": "ACTIVE" }
  }
}
```

**上传**

```json
{
  "code": 200, "message": "success",
  "data": {
    "fileId": "fil_01JC3", "url": "https://oss.example.com/...?sign=...&expires=1762...",
    "expiresAt": "2026-11-11T10:30:00+08:00", "size": 184320, "mimeType": "image/jpeg",
    "sha256": "9f2c..."
  }
}
```

**行驶证 OCR**

```json
{
  "code": 200, "message": "success",
  "data": {
    "fileId": "fil_01JC3", "side": "main", "provider": "aliyun-ocr",
    "recognizedAt": "2026-11-11T10:21:00+08:00",
    "fields": [
      { "key": "plateNumber", "label": "号牌号码", "value": "粤B8K2M7", "confidence": 0.98, "abnormal": false },
      { "key": "vin", "label": "车辆识别代号", "value": null, "confidence": 0, "abnormal": true }
    ],
    "missingFields": ["vin"]
  }
}
```

**方案（v2，含改动说明）**

```json
{
  "code": 200, "message": "success",
  "data": {
    "planId": "pln_01JC0", "conversationId": "conv_01JBZ", "version": 2,
    "parentPlanId": "pln_01JBQ", "insuranceCompany": "中国平安", "companyCode": "PINGAN",
    "totalPremium": 3486.00, "status": "PROPOSED", "source": "REVISE",
    "changedItems": [
      { "field": "items.TPL.coverageAmount", "label": "三者险保额", "from": 1000000, "to": 2000000 }
    ],
    "items": [
      { "itemId": "it_1", "coverageCode": "CI", "coverageName": "交强险", "coverageAmount": 122000, "premium": 950.00, "required": true },
      { "itemId": "it_2", "coverageCode": "TPL", "coverageName": "第三者责任险", "coverageAmount": 2000000, "premium": 1500.00, "required": false }
    ],
    "confirmedAt": null, "createdAt": "2026-11-11T10:20:03+08:00"
  }
}
```

**报价**

```json
{
  "code": 200, "message": "success",
  "data": {
    "quotationId": "qtn_01JC4", "status": "QUOTED", "totalPremium": 3486.00,
    "validUntil": "2026-11-18T23:59:59+08:00",
    "items": [{ "coverageCode": "CI", "coverageName": "交强险", "coverageAmount": 122000, "premium": 950.00 }],
    "failReason": null
  }
}
```

**核保需补材料**

```json
{
  "code": 200, "message": "success",
  "data": {
    "caseId": "uw_01JC5", "status": "NEED_MATERIAL",
    "reason": "车辆使用性质与历史保单不一致，需补充验车照片。",
    "requiredMaterials": ["INSPECTION_PHOTO"], "decidedAt": "2026-11-11T10:25:00+08:00"
  }
}
```

**创建支付**

```json
{
  "code": 200, "message": "success",
  "data": {
    "paymentId": "pay_01JC6", "status": "PENDING", "amount": 3486.00,
    "payUrl": "https://pay.example.com/qr?token=...", "expiresAt": "2026-11-11T11:25:00+08:00"
  }
}
```

**错误**

```json
{
  "code": 409, "message": "方案尚未确认，无法发起报价", "data": null,
  "errorCode": "PLAN_NOT_CONFIRMED", "requestId": "0f7a-..."
}
```

---

## 6. 七阶段调用时序

| 阶段 | 前端动作 | 接口 |
| --- | --- | --- |
| 准备 | 登录 | 1 → 4 |
| ① 收集信息 | 建会话/选会话 | 6 / 5 → 7 |
| ① 收集信息 | 上传行驶证正副页 | 12 ×2 → 15 ×2 |
| ① 收集信息 | 确认车辆资料 | 17 |
| ① 收集信息（老用户） | 续保查询 | 19（`matched=true` 可跳过 OCR） |
| ② 智能推荐 | 生成方案 / 对话改方案 | 21 / 10（Agent 调 22） |
| ② 智能推荐 | 确认方案 | 23 |
| ③ 身份证 | 上传正反面 + 识别 + 确认 | 12 → 16 → 18 |
| ④ 报价 | 发起报价 | 26 → 27 |
| ⑤ 核保 | 提交核保 / 补材料 | 28 → 29，需要时 30 |
| ⑥ 支付 | 创建支付 → 跳第三方 → 回跳轮询 | 31 → 32 |
| ⑦ 保单 | 查看保单明细 | 35 |
| 任意时刻 | 转人工 | 42 → 43 → 44 |

**前端铁律**：页面只在 `status/ stage` 满足条件时展示"已支付""已出单"，
本地不推断；`PAY_PENDING` 不显示成功态，`SUCCESS` 才显示。

---

## 7. 与前端占位符的映射（18 处）

| 占位符 ID | 对应接口 |
| --- | --- |
| `auth-login` | 1、2、3、4 |
| `conversation-list` | 5、6、7、8 |
| `chat-history` | 9 |
| `chat-send` | 10 |
| `agent-stream` | 11（二期） |
| `license-upload` | 12、13、14 |
| `ocr-license` | 15、17 |
| `vehicle-query` | 19、20 |
| `plan-recommend` | 21、24、25 |
| `plan-revise` | 22、23 |
| `idcard-upload` | 16、18 |
| `quote` | 26、27 |
| `underwriting` | 28、29、30 |
| `payment` | 31、32、33 |
| `policy` | 34、35 |
| `agent-workbench` | 38、39、40、41 |
| `handoff` | 42、43、44、45、46 |
| `knowledge-base` | 47 |

> 订单页（36、37）与保单列表（34）目前是静态占位页面，没有独立的占位符条目；
> 接入时按上表一并替换，`placeholders.ts` 不必新增条目。

---

## 8. 与数据表的映射

| 接口 | 主要表 |
| --- | --- |
| 1–4 认证 | `account` |
| 5–8 会话 | `conversation`、`vehicle`、`insurance_order` |
| 9–11 消息 | `message`、`message_attachment`、`agent_session` |
| 12–14 文件 | `file_object` |
| 15–16 OCR | `file_object`、`ocr_record` |
| 17 车辆确认 | `vehicle`、`vehicle_profile`、`ocr_record` |
| 18 身份确认 | `applicant`、`ocr_record` |
| 19–20 车辆 | `vehicle`、`policy`、`insurance_plan` |
| 21–25 方案 | `insurance_plan`、`plan_item`、`insurance_order` |
| 26–27 报价 | `quotation`、`quotation_item`、`insurance_order` |
| 28–30 核保 | `underwriting_case`、`underwriting_material` |
| 31–33 支付 | `payment_order`、`idempotency_key` |
| 34–35 保单 | `policy`、`policy_item`、`applicant`、`vehicle` |
| 36–37 订单 | `insurance_order` |
| 38–41 工作台 | `insurance_order`、`batch_job`、`batch_job_item` |
| 42–46 接管 | `handoff_ticket`、`handoff_event`、`message` |
| 47 知识库 | `kb_term` |
| 回调 | `payment_order`、`audit_log` |

---

## 9. 前端接入顺序（建议）

| 波次 | 接口 | 替换的占位符 |
| --- | --- | --- |
| 1 | 1、4 | `auth-login`（并移除角色的客户端选择） |
| 2 | 12、15、17 | `license-upload`、`ocr-license` → 打通"上传→识别→修正→确认" |
| 3 | 9、10 | `chat-history`、`chat-send` |
| 4 | 21–23 | `plan-recommend`、`plan-revise`（接 C 已自测的能力） |
| 5 | 26–30 | `quote`、`underwriting` |
| 6 | 31–35、38–41、42–46、47 | 其余 |

**接接口时必须同时做的三件事**：删掉对应 `PlaceholderBlock`、把静态占位数据换成真实数据、
在 `placeholders.ts` 与 `AGENTS.md` 里删除该条目（18 → 0 是最终状态）。

---

## 10. 待确认（B / 团队拍板）

| # | 问题 | 建议 |
| --- | --- | --- |
| 1 | 登录是否允许客户端指定角色 | **不允许**，角色由账号决定；前端移除切换 |
| 2 | 发消息同步返回还是要 SSE | 先同步（C 的 FastAPI 已就绪），二期 SSE |
| 3 | 报价/核保/支付是否异步 | 同步返回首次状态 + `GET` 轮询终态 |
| 4 | 支付回跳地址与回调地址 | 由 B 提供，需备案域名 |
| 5 | 保单是否提供 PDF 下载 | 建议 `GET /api/policies/{policyId}/file` 302 到签名 URL |
| 6 | 报价有效期 | 建议 7 天，写入 `quotation.valid_until` |
| 7 | 业务员可见范围 | 按 `agent_account_id`；跨机构需额外授权 |
| 8 | 批量上限 | 建议 100，超出 400 |
| 9 | `Idempotency-Key` 由谁生成 | 前端生成 UUID v4 |
| 10 | 错误码表是否一次定死 | 建议按本手册第 1.5 节，后续只增不改 |

---

## 附录 A：与现有前端代码的差异（接入前必须改）

| 位置 | 现状 | 需改为 |
| --- | --- | --- |
| `types/index.ts` | `QuoteResult.quotationId`（已改） | — |
| `data/flow.ts` | `FlowStageKey` 小写 | 增加 `StageMap`：`COLLECTING→collect` 等 |
| `LoginPage.tsx` | 有角色切换并写入本地身份 | 调 `POST /api/auth/login`，角色取返回值 |
| `types/index.ts` | `UnderwritingResult.requiredMaterials` 用短横线值 | 改为 `INSPECTION_PHOTO` / `INSPECTION_VIDEO` |
| `types/index.ts` | `PlanItem.required` 布尔 | 与接口 `required` 一致（保持不变） |

> 这些改动在接口真正落地时一起做，避免现在先改一遍、接口再变一遍。
