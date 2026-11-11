# 前端 API 对接清单（草案 v0.2）

> 用途：成员 B（后端）、成员 C（FastAPI / Agent）与前端对齐接口。
> **状态：全部为建议契约，未与后端确认。** 本仓库当前只有前端，`/api` 下没有任何实现。
> 权威占位符登记：`frontend/src/api/placeholders.ts`（18 条）；类型草案：`frontend/src/types/index.ts`。
>
> 「现状」一列的含义：**未实现** = 前端方法直接 `reject(NotImportedError)`，不发请求。

---

## 0. 总体约定（前端已按此假设编码）

| 项 | 前端现状 | 需要确认 |
| --- | --- | --- |
| 网关 | 单一 `baseURL = VITE_API_BASE_URL ?? '/api'`（`src/api/http.ts`） | 前端**不直连** FastAPI |
| 鉴权 | `Authorization: Bearer <token>`，token 读 `localStorage['chexian.token']` | 目前无登录接口，token 为空 |
| 响应 | `{ code, message, data }`，`code !== 200` 视为业务失败并抛错 | 错误码表 |
| 身份 | 不在 body 里放 `userId` 作为授权依据 | 由后端从 token 解出 |
| 会话 | `conversationId` 走路径参数 | 与 Agent 侧 `session_id` 的映射 |
| 时间 | ISO 8601 带时区（`2026-11-09T10:12:00+08:00`） | — |
| 文件 | JPG / PNG / WebP，单张 ≤ 10MB（`src/utils/file.ts` 已实现前端校验） | 服务端二次校验 + 真实上限 |

---

## 1. 图片上传：谁保存、C 如何拿到图片

### 结论（建议方案 A，推荐）

```
浏览器 ──multipart──> 成员B: POST /api/files ──> 对象存储
                          │
                          └─返回 { fileId, url?, expiresAt? }
浏览器只保存 fileId；OCR / 报价 / 核保全部用 fileId 引用
成员B 调 C 的 OCR 时附「短期签名 URL」，C 不直连存储、不持密钥
```

**前端永远不回传图片二进制**：上传成功后只持有 `fileId`；预览用本地 `objectURL`，
服务端 `url` 是短期签名地址，过期后重新签发。

### 三个方案对比

| 方案 | 链路 | 优点 | 风险 |
| --- | --- | --- | --- |
| **A（推荐）** B 存文件 + B 转发给 C | 前端 → B → C | 单一鉴权、密钥不外泄、C 无需存储凭据 | B 需实现转发（流量过 B） |
| B 前端直传对象存储（预签名） | 前端 → OSS；fileId 给 B | B 不扛流量 | 前端需处理 CORS/预签名续签；多一条链路 |
| C 前端直接把图发给 FastAPI | 前端 → C | C 最快拿到图 | **不推荐**：双通道、双鉴权、密钥/跨域暴露、审计断裂 |

### 建议契约

```http
POST /api/files                (multipart/form-data)
  file        : 二进制
  category    : license-main | license-sub | idcard-front | idcard-back | inspection
  conversationId? : string
  vehicleId?      : string
── 201 ────────────────────────────────────────────────
{ "code":200, "message":"success", "data":{
    "fileId":"f_01H...", "url":"https://...signed...", "expiresAt":"2026-11-09T11:12:00+08:00",
    "size":184320, "mimeType":"image/jpeg"
}}

DELETE /api/files/{fileId}      → 204
GET    /api/files/{fileId}      → 302 到签名 URL（或鉴权后直接返回二进制）
```

若改走直传，需要额外两个接口：
`POST /api/files/presign` → `{ fileId, uploadUrl, headers, expiresAt }`；
`POST /api/files/{fileId}/complete` → 校验并入库。

### 需要成员 C 回答

1. 行驶证正/副页是**两张独立图**还是允许一张图里双页？（影响 `category` 与 OCR 入参）
2. 旋转 / 纠偏 / 裁边由谁做？（建议 C 侧做，前端只做大小与格式校验）
3. 单张图识别失败时，C 返回什么？（建议 200 + `fields[].abnormal`，不要 500）

---

## 2. OCR 结果经用户确认后，资料保存到哪个接口

**原则：OCR 服务（C）只读不写库；用户「确认」这一步必须落到成员 B 的接口。**
前端不直接写车辆或身份资料（《前端.md》：前端不访问数据库）。

### 建议契约

```http
POST /api/ocr/license
{ "fileId":"f_...", "side":"main" }
── 200 ────────────────────────────────────────────────
{ "code":200, "data":{
    "fileId":"f_...",
    "recognizedAt":"2026-11-09T10:20:00+08:00",
    "provider":"<服务商>",
    "fields":[
      { "key":"plateNumber","label":"号牌号码","value":"粤B8K2M7","confidence":0.98,"abnormal":false },
      { "key":"vin","label":"车辆识别代号","value":null,"confidence":0,"abnormal":true }
    ]
}}
```

```http
POST /api/ocr/idcard
{ "fileId":"f_...", "side":"front" }
── 200 ────────────────────────────────────────────────
{ "code":200, "data":{ "name":"张三", "idMasked":"4403**********8821", "validTo":"2035-06-01" } }
```

> 身份证**不要回传完整证件号**。前端只展示 `idMasked`；完整号码如需入库，由后端接收并加密。
> 需要前端回传完整号时，必须先在需求里改掉「页面不暴露完整证件号」这条。

### 确认保存（两种写法，需成员 B 拍板）

```http
POST /api/conversations/{conversationId}/vehicle-profile/confirm
{
  "fields": { "plateNumber":"粤B8K2M7", "vin":"LSV...", "usageNature":"非营运" },
  "correctedFields": ["vin"],          // 用户手动改过的字段，便于审计/回传 Agent
  "source": "ocr",
  "ocrFileId": "f_..."
}
── 200 ── { "vehicle":{...}, "missingFields":[...], "stage":"recommend" }
```

```http
POST /api/conversations/{conversationId}/applicant/confirm
{ "name":"张三", "idNo":"<服务端使用>", "idType":"居民身份证", "correctedFields":[] }
── 200 ── { "applicantMasked":{ "name":"张三","idMasked":"4403**********8821" }, "stage":"quote" }
```

**关键要求**（否则前端无法正确渲染）：

- 必须返回**保存后的最新资料**，不要只回 `204`；
- 必须返回 `abnormal` / `missingFields`，前端据此高亮缺失字段并阻止进入下一步；
- 确认后的资料要能被 `GET /api/conversations/{conversationId}` 重新读到（刷新不丢）。

### 需要成员 B 拍板的数据模型问题

- 车辆资料挂在 **conversation / vehicle / order** 哪个资源上？
- 同一会话里换了车牌（第二个车牌自动开新会话）时，旧资料如何隔离？
- 用户确认两次（改字段后再次确认）是否保留历史版本？

---

## 3. 可联调接口盘点（现状：全部未实现）

> 直接回答：「目前**没有**任何可以联调的业务接口」。前端 18 处占位，`src/api/index.ts`
> 所有方法一律 `reject(NotImportedError)`，不会发出网络请求。唯一"能用"的是登录，
> 且只是本地写入身份、不发请求。

| 能力 | 前端方法 | 建议接口 | 现状 |
| --- | --- | --- | --- |
| 登录 | `authApi.login` | `POST /api/auth/login`、`GET /api/auth/profile` | 未实现 |
| 会话列表 | `conversationApi.list/create` | `GET/POST /api/conversations` | 未实现 |
| 历史消息 | `chatApi.history` | `GET /api/conversations/{conversationId}/messages` | 未实现 |
| 发消息 | `chatApi.send` | `POST /api/conversations/{conversationId}/messages` | 未实现 |
| 流式输出 | `chatApi.stream` | `GET /api/conversations/{conversationId}/stream`（SSE） | 未实现 |
| 上传 | `documentApi.upload*` | `POST /api/files` | 未实现 |
| OCR | `ocrApi.*` | `POST /api/ocr/license`、`/idcard` | 未实现 |
| 资料确认 | `vehicleApi.confirmProfile`、`applicantApi.confirm` | 见第 2 节 | 未实现 |
| 历史续保查询 | `vehicleApi.lookup` | `GET /api/vehicles/lookup` | 未实现 |
| 方案推荐 | `planApi.recommend` | `POST /api/plans/recommend` | 未实现 |
| 方案修改 | `planApi.revise` | `POST /api/plans/{planId}/revise` | 未实现 |
| 方案确认 | `planApi.confirm` | `POST /api/plans/{planId}/confirm` | 未实现 |
| 报价 | `quoteApi.create/detail` | `POST /api/quotations`、`GET /api/quotations/{quotationId}` | 未实现 |
| 核保 | `underwritingApi.submit` | `POST /api/underwritings` | 未实现 |
| 补验车材料 | `underwritingApi.uploadMaterials` | `POST /api/underwritings/{caseId}/materials` | 未实现 |
| 支付 | `paymentApi.create/detail` | `POST /api/payments`、`GET /api/payments/{paymentId}` | 未实现 |
| 保单 | `policyApi.detail/list` | `GET /api/policies/{policyId}` | 未实现 |
| 业务员批量 | `workbenchApi.*` | `GET /api/workbench/orders`、`POST /api/workbench/batches` | 未实现 |
| 人工接管 | `handoffApi.*` | `POST /api/handoffs` | 未实现 |
| 知识库 | `knowledgeApi.lookupTerm` | `GET /api/knowledge/terms?q=` | 未实现 |

### 各接口的前端硬性期望

- **方案修改**：必须返回新 `version`（`v1 → v2`）。前端只渲染服务端确认后的版本，
  不做本地重算；另建议返回 `changedItems[]`，便于气泡里说明"改了什么"。
- **报价**：返回 `status` + `validUntil` + `items`；前端不判断成功与否，只看 `status`。
- **核保**：三态 `pass | reject | need-material`，失败时给 `reason`（要能看懂），
  需补材料时给 `requiredMaterials: ['inspection-photo' | 'inspection-video']`。
- **支付**：`create` 返回 `payUrl` 由前端跳转第三方；回跳后前端**只**轮询
  `GET /api/payments/{paymentId}`，`status === 'success'` 才展示"已支付 / 已出单"。
- **历史查询**：命中/未命中建议用 `200 + matched:false`，不要用 404 —— 前端会把 404 当异常。
- **异步**：报价/核保/支付若是异步，请明确"轮询间隔 + 超时 + 终态"，前端按此实现 loading。

---

## 4. 前端是否统一通过后端调 AI 服务

**是。前端只认一个网关 `/api`，不直连 FastAPI。**

```
浏览器 ──/api/*──> 成员B 网关（鉴权、校验、落库、审计）
                        └──内部调用──> 成员C FastAPI（方案/多轮/追问）
```

| 传递项 | 方式 | 说明 |
| --- | --- | --- |
| 登录凭证 | `Authorization: Bearer <token>` | 前端拦截器已实现（`src/api/http.ts`） |
| 用户 ID | **后端从 token 解出**，前端不传 | 前端传会被篡改，导致越权 |
| 角色 | 同上；前端只用于菜单/路由展示 | `/workbench` 前端也做拦截，但**以后端为准** |
| 业务员 ID | 登录接口下发到 `AuthUser.agentId`，仅展示 | 批量操作的 operator 由后端注入 |
| 会话 ID | 路径参数 `{conversationId}` | 后端映射为 Agent 的 `session_id` |
| 追踪 | 建议 `X-Request-Id`（由 B 生成并透传） | 前端暂未发送，需要时我加 |

建议后端调 C 时带：`X-Internal-Token`（服务间鉴权）、`X-Request-Id`、
`X-On-Behalf-Of: <userId>`（审计与限流），C 侧不要信任这些头以外的身份来源。

---

## 5. OCR 服务商与额度

- 团队**未选定服务商**，本仓库没有任何配置、密钥或额度信息 —— 我这边无法同步。
- 前端侧约束（已实现，改服务商时需同步）：
  - 图片 ≤ 10MB、JPG/PNG/WebP（`IMAGE_MAX_SIZE_MB`、`ACCEPTED_IMAGE_TYPES`）；
  - 身份证只展示脱敏号；
  - OCR 结果需要 `confidence`，前端用来高亮低置信字段（目前 UI 预留了"异常字段"样式）。
- 建议：厂商密钥只存在于服务端（B 或 C 侧均可，但不能进前端）。
- 选定后请给我三样东西，我据此改提示文案与校验：单图大小/格式上限、是否支持正副页、
  返回字段清单与中文 label。

---

## 6. 待拍板问题清单

| # | 问题 | 建议默认值 | 决策人 |
| --- | --- | --- | --- |
| 1 | 文件存哪、谁转发给 OCR | 方案 A（B 存 + B 转发） | B |
| 2 | 车辆/身份资料挂哪个资源 | 挂 `conversation` 下的 `vehicle` / `applicant` | B |
| 3 | 确认接口形态 | `POST .../confirm`（显式动作，便于审计） | B |
| 4 | 报价/核保/支付是否异步 | 同步返回首次状态 + `GET` 轮询终态 | B |
| 5 | 错误码表 | 401/403/409/422 + 业务 `code` | B |
| 6 | 幂等 | 上传、报价、支付带 `requestId` | B |
| 7 | OCR 服务商 | 待选 | 项目方 |
| 8 | 身份证完整号是否回传 | **不回传**，只回 `idMasked` | B + 需求 |
| 9 | `conversationId` ↔ Agent `session_id` | 一对一，B 侧维护映射 | B + C |
| 10 | 本期是否做 SSE 流式 | 先普通 HTTP，二期 SSE | B + C |

---

## 7. 最小联调路径（建议按此顺序替换占位符）

| 步骤 | 依赖 | 替换占位符 | 可演示的能力 |
| --- | --- | --- | --- |
| 1 | B：`POST /api/auth/login` | `auth-login` | 真实登录、角色隔离 |
| 2 | B：`POST /api/files` | `license-upload` | 真正的上传 / 预览 / 删除 |
| 3 | B + C：`POST /api/ocr/license` | `ocr-license` | 上传 → 识别 → 修正 → 确认闭环 |
| 4 | B：资料确认接口 | — | 资料落库、刷新不丢 |
| 5 | C：方案 + 修改 | `plan-recommend`、`plan-revise` | 自然语言改方案（C 已自测） |
| 6 | B：报价 / 核保 / 支付 / 保单 | `quote`、`underwriting`、`payment`、`policy` | 业务闭环 |

前 4 步打通，就能演示「真实 OCR + 确认」；第 5 步可直接接 C 已跑通的 Agent 能力。
