# 车险智能体与 Java 后端接口需求

版本：对接草案 v1.0｜日期：2026-11-10｜提供方：智能体侧 C｜接收方：Java 后端 B

## 1. 范围、分工和现状

本文回答：智能体完整业务需要 Java 提供哪些接口，每个接口传什么、返回什么、由谁发起。路径、字段和枚举为建议契约，由 B 统一确认后，C 修改适配层。本文不是已经实现的 Java API，也不要求 B 原样照搬路径。

职责约定：

- A：页面、聊天输入、材料上传交互、资料/方案确认按钮和支付跳转。
- B：登录、权限、MySQL、文件存储、消息持久化、方案版本、订单、报价/核保/支付/出单对接、人工平台业务记录。
- C：FastAPI + LangGraph、多轮意图理解、推荐与修改决策、追问、OCR 调用及结果标准化、知识检索、调用业务工具。AI 不直接连接 MySQL。
- OCR 流程由 C 开发。B 保存文件及识别结果，不能把“保存 OCR 结果”误解成由 B 实现 OCR 识别。

现状：C 已完成多轮方案修改和 LangGraph 迁移测试；业务与 OCR 仍为 demo。C 当前真实模式预留 `/ai-tools/{operation}` 网关调用，但未与 B 联调。聊天记录仍在进程内存；本稿提出的持久化、结构化报价、异步状态等需要继续实现。前端 `/api` 清单同样为待确认草案。

## 2. 调用方向与一次请求的边界

| 场景 | 发起方 → 接收方 | 谁落库 |
|---|---|---|
| 登录、会话列表、历史消息、上传文件 | A → B | B |
| 发消息后的智能体执行 | B → C | B 保存用户消息及 C 返回的回复 |
| 智能体获取上下文/修改方案/发起业务动作 | C → B 的内部业务接口 | B |
| 文件识别 | B → C 的 OCR 接口；C → OCR 服务商 | B 保存 C 返回的结构化结果 |
| 用户点击确认 | A → B | B 校验并确认，不允许模型替用户确认 |
| 核保/支付结果通知 | 外部平台 → B，或 B 主动查单 | B |
| 人工处理/恢复 | 人工平台或授权客服 → B | B；C 下次读取最新状态 |

默认采用普通 HTTP：B 先保存用户消息并提交事务，再调用 C；C 按需调用 B 的内部接口，最后把回复返回 B；B 保存助手消息再响应前端。B 等待 C 时不得持有数据库事务或会话锁，否则 C 回调 B 容易相互等待。Java 内部工具接口不能再调用聊天入口，避免递归。

消息只由 B 的入口编排层保存一次。C 不再调用一个“保存同一条 AI 回复”的接口重复写入。下面提供消息保存接口的等价服务能力，供 B 内部调用；如果未来改成 C 异步回调写消息，应另立版本并关闭入口层重复保存。

## 3. 通用契约

### 3.1 路径和身份

- 前端网关前缀建议 `/api/v1`，仅供 C 使用的 Java 内部接口前缀建议 `/internal/ai/v1`。以下第 5～11 节省略内部前缀。
- B → C 路径统一列在第 12 节。这些新路径是建议，当前 C 接口映射列在第 14 节。
- 内部请求必须有服务身份认证，并携带 B 签发的、可验证的委托上下文：`operatorId`（实际操作者）、`customerId`（代办客户）、`salespersonId`、`role`、`requestId`、`expiresAt`。建议用签名凭证或 B 可验证的短期引用，不接受浏览器自行声明身份。
- 客户 ID 只用于选定业务对象，不能作为权限证明；B 每次校验客户/车辆/会话归属、业务员授权范围。模型不得自行选择或修改委托身份。
- 读取“当前用户”需同时返回操作者与所办理客户；业务员不等于投保人。

### 3.2 数据类型与返回包

本文：`string` 为字符串，`int` 为整数，`bool` 为布尔值，`datetime` 为带时区 ISO 8601；字段后 `?` 表示可选/可空，其余必填。ID 全部按 string 传输。金额/保额用十进制字符串，单位元，例如 `"3000000.00"`，币种为 `"CNY"`；未知保费为 null，不是零。

成功包：`{code: "OK", message: "success", data: <下文对象>, requestId: string}`。错误包：`{code: <业务码>, message: <可读说明>, data: {retryable: bool, ...}, requestId: string}`。这是内部接口建议；前端草案使用数值 code=200，B 可以统一改成该规范或在网关转换，但不能让调用方自行猜测。

HTTP：GET 成功 200，创建 201，异步受理 202；401 未认证，403 无权限，404 对象不可用/不存在，409 状态或版本冲突，422 字段校验失败，429 限流，502/503 上游失败。历史未命中用成功包 `matched:false`。OCR 业务识别不出用结果状态 failed/needs_review，服务调用不可达用错误包，不能混淆。

### 3.3 幂等与版本

- 所有有副作用的 POST/PATCH 携带 `Idempotency-Key`；同一次操作的网络重试复用相同键。不同动作使用不同键，例如 requestId + 动作 + 目标 ID + 操作序号。
- 同键同参数返回首次结果；同键不同参数返回 409 `IDEMPOTENCY_CONFLICT`。B 需将外部动作与业务记录关联，超时先查单，不重复收费/出单。
- 有版本字段的修改传 `expectedVersion:int`；B 原子检查，不一致返回 409 `VERSION_CONFLICT` 和 `currentVersion`。同一幂等请求重放先识别原结果，不因版本已推进而报错。
- 会话 version、方案 version、订单 version 各自独立；GET 不增加版本。方案变更后旧确认失效，已报价方案不能直接覆盖。
- 金额、身份和关键材料确认均由 B 校验；模型输出只是请求参数，不是可信业务结果。

## 4. 通用返回对象

以下对象供各接口引用，避免同一字段出现多种定义。

### Conversation

`{id, customerId, salespersonId, plateNumber?:string, vin?:string, title:string, stage:string, handlingMode:"ai"|"human", version:int, vehicleId?:string, confirmedPlanId?:string, pendingConfirmationId?:string, activeOrderId?:string, lastMessageAt?:datetime}`。

stage：collecting / ready / quoting / quoted / underwriting / supplement / underwriting_rejected / approved / paying / paid / issuing / issued / closed。ready 表示该次报价所需资料和方案全部确认，不是确认任意一个字段就 ready。handlingMode 独立表示人工接管，不覆盖业务阶段。

### Message

`{id, conversationId, seq:int, role:"user"|"assistant"|"human_agent"|"system", content:string, requestId:string, replyToMessageId?:string, status:"processing"|"completed"|"failed", relatedPlanId?:string, createdAt:datetime}`。seq 会话内递增，分页返回明确顺序。

### Vehicle

`{id, plateNumber?:string, vin?:string, ownerNameMasked?:string, vehicleType?:string, useNature?:string, brandModel?:string, registrationDate?:date, issueDate?:date, approvedPassengers?:int, version:int, confirmed:bool, missingFields:string[]}`。完整车主姓名、地址、发动机号可由受控 OCR/确认数据保存，默认不进入聊天上下文。

### Plan / PlanItem

Plan：`{id, conversationId, version:int, previousPlanId?:string, insurerCode:string, insurerName:string, source:"recommend"|"history"|"modify", status:"draft"|"pending_confirmation"|"confirmed"|"superseded"|"cancelled", items:PlanItem[], estimatedTotal?:string, currency:"CNY", priceSource:"demo"|"estimate"|"insurer_quote"|"pending_requote", confirmedAt?:datetime}`。

PlanItem：`{coverageCode:string, coverageName:string, selected:bool, sumInsured?:string, estimatedPremium?:string, terms?:object}`。当前险种至少 compulsory、third_party_liability、vehicle_damage；未投保险种的保额/保费应清空。正式保险公司代码由 B 产品目录提供，不让模型虚构。

### Confirmation

`{id, conversationId, kind:"plan"|"vehicle_profile"|"party_profile", targetId:string, targetVersion:int, status:"pending"|"confirmed"|"expired"|"superseded"|"cancelled", displayData:object, missingFields:string[], abnormalFields:string[], expiresAt:datetime, confirmationToken?:string}`。

displayData 为脱敏后的展示数据。confirmationToken 只在 B 面向已授权用户签发时返回，普通 AI 上下文不返回；令牌绑定客户、会话、目标版本、有效期和展示内容摘要。数据库可只存摘要，刷新需要重签并使旧令牌失效。

### Order

`{id, conversationId, customerId, planId, planVersion:int, vehicleVersion:int, partyVersions:object, status:string, version:int, activeQuoteId?:string, activeUnderwritingId?:string, activePaymentId?:string, policyIds:string[], amountPayable?:string, currency:"CNY", createdAt:datetime}`。

订单引用确认后的资料快照与方案版本，status 使用 draft / quoting / quoted / underwriting / need_material / rejected / approved / paying / paid / issuing / issued / cancelled。金额未知时为空。会话汇总状态由 B 根据业务事实计算，C 不直接提交任意 stage。

## 5. 用户、车辆、历史投保和会话（C → Java）

| 编号/接口 | 请求参数 | data 返回 | 发起与触发 |
|---|---|---|---|
| J01 `GET /context` | 无业务参数，使用委托身份 | `{operator:{id,role,displayName},customer:{id,displayNameMasked},salesperson:{id},permissions:string[]}` | C 初始化/校验权限；B → C 时已带可信上下文则可省略本次读取 |
| J02 `GET /vehicles` | query `plateNumber?:string, vin?:string, cursor?:string, limit?:int`，limit 默认20、最大100 | `{items:Vehicle[],nextCursor?:string}` | C 查询该客户授权车辆；无匹配返回空数组 |
| J03 `GET /vehicles/{vehicleId}` | path vehicleId | Vehicle | C 读取车辆详情 |
| J04 `POST /renewal-lookups` | body `{plateNumber?:string,vin?:string,policyYear?:int}`；车牌/VIN 至少一项 | `{lookupId,status:"processing"|"completed"|"failed",matched?:bool,candidates?:[{sourcePolicyRef,policyYear,vehicle:Vehicle,planSnapshot:Plan,requiresReconfirmation:bool}],pollAfterMs?:int,error?:{code,message}}` | C 老客户查去年资料；B 查询本地或外部授权来源 |
| J05 `GET /renewal-lookups/{lookupId}` | path lookupId | 同 J04 | C 或 B 查询异步历史结果；completed 时 matched 必填 |
| J06 `POST /conversations/resolve` | `{plateNumber?:string,vin?:string,forceNew?:bool}`；至少一项，forceNew 默认 false | `{conversation:Conversation,created:bool}` | C 识别到车辆后创建/定位会话；也可由 B 消息入口先调用同一业务服务 |
| J07 `GET /conversations` | query `cursor?:string,limit?:int,stage?:string`，默认20、最大100 | `{items:Conversation[],nextCursor?:string}` | C 确有查询需求时调用；常规列表由 A → B |
| J08 `GET /conversations/{conversationId}` | path conversationId | Conversation | C 查询基础会话 |
| J09 `GET /conversations/{conversationId}/context` | path ID；query `historyTurns?:int`，默认6、最大20；`beforeMessageSeq?:int` 用于排除本次输入 | `{conversation,vehicle?:Vehicle,parties:[{role,confirmed,missingFields}],confirmedPlan?:Plan,pendingPlan?:Plan,pendingConfirmation?:Confirmation,materials:[{id,kind,status}],order?:Order,recentMessages:Message[],allowedActions:string[],missingFields:string[]}` | C 每次图执行前读取一致的业务上下文 |
| J10 `GET /conversations/{conversationId}/messages` | query `beforeSeq?:int,limit?:int`，默认20、最大100 | `{items:Message[],nextBeforeSeq?:int,hasMore:bool}`；items 按 seq 升序 | C 补查对话；常规历史页面由 A → B |

J06 仅复用该客户相同车辆、相同办理周期的活动会话；不能把同一车牌永久映射为一个会话。车辆身份冲突返回 409，不能自动覆盖。请求中同时给出车牌和 VIN 时需验证是否指向同车。

一条消息识别出两辆车时，目标需求是分别创建/定位两个会话，返回前端切换目标；当前 C 会拒绝多车牌消息，这部分待补。车牌尚未知时临时标题可用“车辆办理中”，确认后更新为车牌。

## 6. 消息保存、文件和 OCR

### 6.1 Java 自己发起的持久化能力

下面路径可作为 B 内部应用服务而不开放 HTTP；若开放，只允许指定的消息入口服务/OCR 编排服务调用。

| 编号/接口 | 请求参数 | data 返回 | 谁发起 |
|---|---|---|---|
| J11 `POST /conversations/{id}/messages` | `{requestId:string,role:string,content:string,replyToMessageId?:string,relatedPlanId?:string,status:string}`；role/状态由服务端确定 | `{message:Message}` | B：调用 C 前存 user；收到回复后存 assistant。C 默认不调用 |
| J12 `POST /conversations/{id}/ocr-results` | `{materialId:string,recognitionRequestId:string,documentType:"vehicle_license"|"id_card",side:"main"|"sub"|"front"|"back",partyRole?:"applicant"|"insured"|"vehicle_owner",provider:string,status:"succeeded"|"needs_review"|"failed",fields:OcrField[],missingFields:string[],warnings:[{code,message}],recognizedAt:datetime,error?:{code,message}}` | `{ocrResultId,materialId,status,version:int,needsReview:bool,nextAction:"confirm"|"upload"|"handoff"}` | B：接到 C 的 OCR 返回后保存；C 默认不再二次保存 |

OcrField：`{key:string,value:string|number|bool|null,confidence:number|null,abnormal:bool,reasonCode?:string}`。confidence 若服务商不提供用 null，不伪造；身份证完整字段仅 C → B 受控内部传输、B 加密保存，不直接回传浏览器或送给聊天模型。`(materialId, recognitionRequestId)` 去重，允许同材料重新识别保留多次结果。

J11 assistant 消息关联 replyToMessageId；同一请求同一消息槽去重。若 C 超时但业务写入已成功，B 先查请求/业务结果再恢复回复，不能重新执行全流程造成重复方案/订单。

### 6.2 文件和识别结果查询（C → Java）

| 编号/接口 | 请求参数 | data 返回 | 谁发起 |
|---|---|---|---|
| J13 `GET /materials/{materialId}` | path ID | `{id,conversationId,kind,mimeType,sizeBytes:int,status,revision:int,partyRole?:string}` | C 校验文件归属、用途和版本 |
| J14 `POST /materials/{materialId}/read-access` | `{purpose:"ocr",ttlSeconds?:int}`；实际时长由 B 限制 | `{materialId,readUrl:string,expiresAt:datetime,mimeType,sizeBytes:int}` | C 获取/刷新短期读取权限；B 调 C 时已带有效 URL 则不用再次获取 |
| J15 `GET /conversations/{id}/materials` | query `kind?:string,includeSuperseded?:bool` | `{items:[J13返回对象]}` | C 检查是否缺行驶证副页、身份证背面或验车材料 |
| J16 `GET /ocr-results/{ocrResultId}` | path ID | `{id,materialId,status,version,fields:OcrField[],missingFields,warnings}` | C 处理字段问题时按权限读取，默认脱敏；原始数据由 B 的确认服务使用 |

上传由 A → B 的 `POST /api/v1/files`（multipart：file、conversationId、kind、partyRole?）完成，B 返回 `{fileId,materialId,kind,mimeType,sizeBytes,status}`。kind 使用 license_front/license_back/id_front/id_back/inspection；行驶证 main/sub 分别映射 front/back。该接口不是 C 必须提供的文件存储接口。

C 只读取 B 签发并校验来源的文件地址，限制下载大小、类型、超时和重定向；不接收用户任意 URL。前端文件格式和 C 当前支持格式尚未对齐，须统一后联调。

### 6.3 资料待确认、用户确认（分清调用者）

| 编号/接口 | 请求参数 | data 返回 | 发起与规则 |
|---|---|---|---|
| J17 `POST /conversations/{id}/profile-drafts` | `{kind:"vehicle_profile"|"party_profile",ocrResultIds:string[],partyRole?:string,expectedVersion:int}` | `{draftId,draftVersion:int,displayFields:OcrField[],missingFields:string[],abnormalFields:string[],canConfirm:bool}` | C 或 B 的 OCR 编排层，根据已保存结果构建资料草稿；每次链路选定唯一调用者 |
| J18 `POST /conversations/{id}/confirmations` | `{kind:string,targetId:string,targetVersion:int,expectedVersion:int}` | Confirmation（内部不给模型 token） | C 创建资料/方案待确认事项；B 校验目标归属与完整性 |
| J19 `GET /conversations/{id}/confirmations/current` | 无 body | `{confirmation:Confirmation|null}` | C 查询待确认状态；A → B 展示时由 B 另行签发 token |
| J20 `POST /conversations/{id}/confirmations/{confirmationId}/confirm` | `{confirmationToken:string,targetVersion:int,expectedVersion:int}` | `{confirmationId,status:"confirmed",conversation:Conversation,vehicle?:Vehicle,party?:{role,nameMasked,idMasked,confirmed},plan?:Plan}` | 仅 B 的已认证用户确认入口发起；AI 不能自主消费令牌 |
| J21 `POST /conversations/{id}/confirmations/{confirmationId}/cancel` | `{expectedVersion:int,reason?:string}` | `{confirmationId,status:"cancelled",conversation:Conversation}` | 用户明确取消时 C → B；或用户点击取消由 A → B。只取消待确认，不等于退保/撤销订单 |

正常识别 → J17 → J18 → 展示给用户 → 用户点击后 J20；确认成功 B 原子保存资料并推进状态。不要在 OCR 一完成时直接写成“用户已确认”。跨页冲突、必填字段缺失不能放行。

OCR 错误按原需求追问并创建人工任务；此时不要自动确认。若产品允许人工修正，授权客服通过独立资料修订服务写入新 draftVersion，重新确认；C 不能凭猜测补齐身份证号。J20 不接受用户携带一份全新字段值绕过已展示的确认版本。

## 7. 方案推荐、创建、修改与确认（C → Java）

| 编号/接口 | 请求参数 | data 返回 | 触发 |
|---|---|---|---|
| J22 `GET /insurance-products` | query `vehicleId?:string,insurerCode?:string` | `{catalogVersion:string,insurers:[{code,name}],coverages:[{code,name,required:bool,allowedLimits?:string[],constraints?:object}]}` | C 获取可选公司、险种和保额规则；禁止模型虚构不可投产品 |
| J23 `POST /conversations/{id}/plan-candidates` | `{vehicleId?:string,preferences?:{budget?:string,thirdPartyLimit?:string,vehicleDamage?:bool,insurerCode?:string},sourcePolicyRef?:string}` | `{candidates:Plan[],catalogVersion:string,missingFields:string[]}` | C 请求 B 提供可投方案候选/历史方案转换；B 可按规则生成候选，C 负责结合对话解释和选择 |
| J24 `POST /conversations/{id}/plans` | `{insurerCode:string,items:[{coverageCode,selected:bool,sumInsured?:string}],source:"recommend"|"history",sourcePolicyRef?:string,catalogVersion:string,expectedVersion:int}` | `{plan:Plan,conversation:Conversation}` | C 将选定方案保存成新 draft；Java 校验产品和保额，价格由受信数据提供 |
| J25 `POST /plans/{planId}/revisions` | `{expectedPlanVersion:int,expectedConversationVersion:int,changes:{insurerCode?:string,items?:[{coverageCode,selected?:bool,sumInsured?:string|null}]},reason?:string}` | `{plan:Plan,conversation:Conversation,changedItems:[{field,before,after}],requiresRequote:bool}` | C 理解“改三者为300万”后创建新版本；不是原地覆盖旧版本 |
| J26 `GET /plans/{planId}` | path ID | Plan | C 查询明确版本 |
| J27 `GET /conversations/{id}/plans` | query `cursor?:string,limit?:int` | `{items:Plan[],nextCursor?:string}` | C 查询方案历史，默认20、最大100 |

方案创建或修改后由 J18 生成待确认事项，用户确认用 J20。J25 只改显式字段，未出现字段保持原值；selected=false 清空对应保额/报价。若“那个不要了”有歧义，C 只追问，不调用修改接口。

三者险修改请求示例（金额是保额）：

```json
{
  "expectedPlanVersion": 2,
  "expectedConversationVersion": 8,
  "changes": {
    "items": [{"coverageCode":"third_party_liability","sumInsured":"3000000.00"}]
  },
  "reason": "用户将三者险保额调整为三百万元"
}
```

J25 新版本应返回 estimatedTotal=null、priceSource=pending_requote，并使旧 pending 失效。已有订单绑定旧方案时需返回 409 `ORDER_BOUND_PLAN` 和 orderId，用户明确重建报价流程后创建新订单；不改动已支付订单和已出单保单。

J23 是规则候选能力，不是要求 B 再开发一个语言模型。若 B 不提供候选生成，C 可根据 J22 和明确规则生成建议，再由 J24 验证保存；双方需确定一条实现路线。任何“推荐价格”均标明估算或模拟，正式保费只认报价结果。

## 8. 订单、报价与核保（C → Java）

| 编号/接口 | 请求参数 | data 返回 | 触发 |
|---|---|---|---|
| J28 `POST /orders` | `{conversationId,planId,planVersion:int,vehicleVersion:int,partyVersions:object,expectedConversationVersion:int}` | `{order:Order,conversation:Conversation}` | 用户明确发起报价后 C 创建订单；B 校验已确认方案、材料和身份，保存不可变快照 |
| J29 `GET /orders/{orderId}` | path ID | Order | C 查询订单汇总 |
| J30 `GET /orders` | query `conversationId?:string,status?:string,cursor?:string,limit?:int` | `{items:Order[],nextCursor?:string}`，默认20、最大100 | C 查询历史；页面/工作台由 A → B |
| J31 `POST /orders/{orderId}/quotes` | `{expectedOrderVersion:int}` | QuoteResult | C 提交报价；B 对接保险公司，不接受模型提供正式保费 |
| J32 `GET /quotes/{quoteId}` | path ID | QuoteResult | C 或 B 查询报价结果 |
| J33 `POST /orders/{orderId}/underwritings` | `{quoteId,expectedOrderVersion:int,previousCaseId?:string}` | UnderwritingResult | 报价成功后 C 衔接核保；补件后通过 previousCaseId 关联原核保 |
| J34 `GET /underwritings/{caseId}` | path ID | UnderwritingResult | C 或 B 获取核保状态 |
| J35 `POST /underwritings/{caseId}/materials` | `{materialIds:string[],expectedCaseVersion:int}` | `{caseId,version:int,acceptedMaterialIds:string[],missingMaterials:string[],canResubmit:bool}` | C 用户已补传后挂接材料；核保重提用 J33，Java 防重复提交 |

QuoteResult：`{quoteId,orderId,planId,planVersion:int,status:"processing"|"succeeded"|"failed"|"expired",items:[{coverageCode,coverageName,sumInsured?:string,premium:string}],totalPremium?:string,taxAmount?:string,payableAmount?:string,currency:"CNY",validUntil?:datetime,externalQuoteNo?:string,error?:{code,message},pollAfterMs?:int,orderVersion:int}`。成功时明细、应付金额和有效期须完整，其他阶段可以空。税费与保费分开，避免总额计算歧义。

UnderwritingResult：`{caseId,orderId,quoteId,version:int,status:"processing"|"passed"|"rejected"|"need_material",reasonCode?:string,reason?:string,requiredMaterials:[{kind:"inspection_photo"|"inspection_video",minCount:int,instructions:string}],externalCaseNo?:string,pollAfterMs?:int,orderVersion:int}`。requiredMaterials 在 need_material 时给出，其他状态为空数组。

报价返回 processing 时本次图返回“报价处理中”，不能立即假定成功并提交核保。建议首次由 C 提交，后续由 B 后台任务/平台回调更新状态；报价首次成功后由 B 的幂等业务任务调用核保应用服务（不重入聊天入口）。同步报价成功则 C 可调用 J33。B 必须对同一订单/报价防重复核保，确保同步/异步路径不会双提交。

当前 C demo 为同步报价后立即核保，真实异步分支需要改造。只查询状态不触发新报价；明确区分 rejected 与 need_material。对外失败原因需可读且脱敏。

## 9. 支付、支付核验与保单

| 编号/接口 | 请求参数 | data 返回 | 发起与规则 |
|---|---|---|---|
| J36 `POST /orders/{orderId}/payments` | `{quoteId,underwritingId,expectedOrderVersion:int}` | PaymentResult | C 在用户明确支付意图后发起，或 A 支付按钮经 B 调同一服务。B 校验核保通过、金额和报价有效期 |
| J37 `GET /payments/{paymentId}` | path ID | PaymentResult（必要时仅返回状态摘要） | C 查询；支付回跳后的页面由 A → B 轮询 |
| J38 `POST /orders/{orderId}/policy-issuance` | `{paymentId,expectedOrderVersion:int}` | `{orderId,status:"issuing"|"issued"|"failed",policyIds:string[],pollAfterMs?:int,error?:{code,message}}` | 默认 B 验证支付成功后自动发起；不由聊天模型决定是否真实出单 |
| J39 `GET /orders/{orderId}/policies` | path ID | `{orderId,status:"issuing"|"issued"|"failed",items:PolicyDetail[],pollAfterMs?:int,error?:{code,message}}` | C 查询/整理保单明细，B 可供前端直接查询 |
| J40 `GET /policies/{policyId}` | path ID | PolicyDetail | C 查看具体保单 |

PaymentResult：`{paymentId,orderId,status:"pending"|"success"|"failed"|"closed"|"refunded",amount:string,currency:"CNY",payUrl?:string,expiresAt?:datetime,paidAt?:datetime,verifiedAt?:datetime,pollAfterMs?:int,orderVersion:int}`。payUrl 为已批准平台地址，不发送给语言模型；A 可直接展示按钮。返回地址由 B 白名单配置，不允许模型提供任意 redirect URL。

PolicyDetail：`{policyId,policyNumber,orderId,insurerCode,insurerName,policyType:"compulsory"|"commercial",status:"issued"|"cancelled"|"expired",applicant:{nameMasked,idMasked},insured:{nameMasked,idMasked},vehicle:{plateNumber,vinMasked,brandModel?},items:[{coverageCode,coverageName,sumInsured?:string,premium:string}],totalPremium:string,currency:"CNY",effectiveAt:datetime,expiresAt:datetime,terms:[{code,version,title,summary}],documentFileId?:string,issuedAt:datetime}`。只对已获得正式保单的项目返回完整 PolicyDetail；出单中用 J39 外层状态表达，不编造 policyNumber。

支付平台回调：外部平台 → B（如 `POST /callbacks/payments/{provider}`），参数/验签按所选平台协议实现，不由 C 设计一个通用伪签名。B 验签并核对商户、订单、金额、币种后原子更新；重复通知去重。浏览器回跳/用户说“付了”均不能视为支付成功。支付成功但出单失败需保留 paid 事实并显示“出单处理中/失败”，不得自动再次收款。

一笔订单可能对应交强险、商业险多张保单，所以返回数组。正式保单详情来自 Java/保险公司，AI 只解释，不生成虚构承保记录。

## 10. 人工接管、术语与批量

| 编号/接口 | 请求参数 | data 返回 | 发起与规则 |
|---|---|---|---|
| J41 `POST /conversations/{id}/handoffs` | `{reasonCode:"ocr_conflict"|"user_request"|"business_failure",reason:string,summary:string,relatedMaterialIds?:string[],relatedMessageIds?:string[],expectedVersion:int}` | `{handoffId,status:"pending"|"accepted",handlingMode:"human",externalTicketId?:string,conversation:Conversation}` | C 识别异常或用户要求人工时调用；Java 建工单并暂停自动办理 |
| J42 `GET /handoffs/{handoffId}` | path ID | `{handoffId,conversationId,version:int,status:"pending"|"accepted"|"resolved"|"cancelled",assignedAgentId?:string,resolution?:string,updatedAt:datetime}` | C 查询人工处理进度 |
| J43 `POST /handoffs/{handoffId}/resolve` | `{expectedVersion:int,resolution:string,resumeAi:bool}` | `{handoffId,status:"resolved",conversation:Conversation}` | 仅授权人工平台/客服 → B；C 不得自行结束人工任务 |
| J44 `GET /knowledge/terms` | query `q:string,limit?:int`，默认5、最大20 | `{items:[{id,term,aliases:string[],definition,example?:string,source?:string,version:int}]}` | C 查询术语；当前本地词库可暂保留，此接口非首轮必须 |
| J45 `POST /batch-jobs` | `{items:[{customerId,conversationId?:string,plateNumber?:string,vin?:string,message:string}],clientRequestId:string}`，1～30项 | `{batchId,status:"pending",items:[{itemId,status:"pending"}]}` | 业务员 A → B，B 校验每个客户的代办权限后逐项调用 C |
| J46 `GET /batch-jobs/{batchId}` | path ID | `{batchId,status:"pending"|"running"|"completed"|"partial_failed"|"failed",items:[{itemId,customerId,conversationId?:string,status,requestId,error?:{code,message},replyMessageId?:string}]}` | 前端 → B 查询；无需让模型轮询整个批次 |

J43 的 expectedVersion 对应工单独立 version，恢复阶段依据保存的业务阶段而不是重置 collecting。人工平台不可用时 B 仍需可靠保留待派发工单和暂停状态，异步补发，避免“说已转人工却无记录”。

批量必须分别生成委托上下文、幂等键和事务；一项失败不取消其他项。当前 C 的 `/ai/batch` 同步最多 30 条且共用 Principal，不能直接当作跨客户授权批量服务；应改造或由 B 拆成单客户调用。

## 11. 异步、错误与恢复的统一约定

- 异步受理返回对象 ID、processing 状态和 `pollAfterMs`。建议初值 2000ms，具体由 B/上游约束确定。页面等待一段时间可停止转圈，但不能把未知状态写成失败；稍后按对象 ID 查询。
- 页面订单/支付轮询由 A → B；外部平台状态同步由 B 后台任务负责；C 一次请求做有限查询后返回“处理中”，不长时间阻塞模型流程。
- 错误码至少包括：FORBIDDEN、CONVERSATION_NOT_FOUND、VERSION_CONFLICT、IDEMPOTENCY_CONFLICT、CONFIRMATION_EXPIRED、MATERIALS_MISSING、OCR_REVIEW_REQUIRED、PLAN_NOT_CONFIRMED、ORDER_BOUND_PLAN、QUOTE_EXPIRED、UNDERWRITING_NOT_PASSED、PAYMENT_UNVERIFIED、HUMAN_HANDLING、UPSTREAM_UNAVAILABLE。
- 缺字段错误的 data 返回 `{missingFields:string[]}`；版本冲突返回 `{currentVersion:int}`；需人工返回 `{handoffId?:string}`。C 不靠解析中文错误文案分支。
- Java 的业务阶段为权威。人工接管期间拒绝自动业务写操作，允许必要查询与保存聊天；确认、支付等显式动作也必须检查当前权限/阶段。
- 建议 B 的消息入口增加 `GET /api/v1/requests/{requestId}` 返回 `{status:"processing"|"completed"|"failed",conversationId,replyMessageId?,error?}`，让前端网络断开后查询原请求，避免重发整条指令。

## 12. Java 需要调用 C 的接口（建议新增/调整）

### C01 `POST /internal/agent/chat`

发起者：B。请求：

```json
{
  "requestId": "req_demo_001",
  "conversationId": "conv_demo_001",
  "userMessageId": "msg_demo_001",
  "userMessageSeq": 15,
  "message": "把三者险改成三百万，车损险保留"
}
```

身份来自内部委托凭证，不在 message 中识别。C 用 J09 加 beforeMessageSeq=15 获取此前上下文，本次 message 单独加入一次，防止用户消息重复输入模型。C 返回：

```json
{
  "requestId": "req_demo_001",
  "conversationId": "conv_demo_001",
  "reply": "三者险保额已调整为300万元，车损险保留，请核对后确认。",
  "intent": "modify",
  "conversationVersion": 9,
  "actions": [
    {"type":"confirm_plan","planId":"plan_demo_003","confirmationId":"confirm_demo_003"}
  ],
  "relatedPlanId": "plan_demo_003"
}
```

actions 是结构化页面动作，不允许携带任意脚本或自由生成 URL。B 验证引用对象，保存回复再返回前端；确认令牌由 B 另行签发。C 不保存同一条回复。已成功的业务工具调用应有幂等记录；同一 requestId 重试要读回已完成结果，不能再次修改方案。

### C02 `POST /internal/agent/ocr`

发起者：B。请求：`{requestId,conversationId,materialId,documentType:"vehicle_license"|"id_card",side:"main"|"sub"|"front"|"back",partyRole?:string,readUrl,expiresAt:datetime,mimeType,sizeBytes:int}`。

响应：`{requestId,materialId,documentType,side,partyRole?,provider,status:"succeeded"|"needs_review"|"failed",fields:OcrField[],missingFields:string[],warnings:[{code,message}],recognizedAt:datetime,error?:{code,message},needsHandoff:bool,reply?:string}`。

B 调 C 识别并通过 J12 落库。正常结果由 B OCR 编排层调用 J17/J18 建立待确认；需人工时由 B 依据 C 的结构化结果调用 J41 并保存提示消息，避免 C/B 双建工单。聊天中的用户主动转人工仍由 C → J41。识别结果不得假装是正式资料。

### C03 `POST /internal/agent/events`（可选，第一版可以不实现）

发起者：B。请求：`{eventId,eventType:"confirmation_completed"|"underwriting_updated"|"payment_verified"|"handoff_resumed",conversationId,entityId,version:int}`。返回 `{eventId,accepted:bool,reply?:string}`。

用途是生成可选通知文案，而不是让 C 再次执行支付/出单。Java 先提交业务事务，再投递事件；C 按 eventId 去重。第一版可由 B/前端直接显示状态，C 下一条聊天重新读 J09 即可，不必为每个事件新增模型调用。

## 13. 建议的首轮联调顺序

| 轮次 | Java 需要先提供 | C 配合 | 验收 |
|---|---|---|---|
| 1：对话落库 | J01、J06、J09～J11，以及可信调用入口 | 改为读取 Java 历史，移除内存作为权威上下文 | 重启后历史可查、指代追问能继续、不越权 |
| 2：方案闭环 | J22/J24～J27、J18～J21；J23可选 | 保留现有真实模型语义，映射方案格式 | 200万→300万→追问→取消车损→显式确认，旧令牌失效 |
| 3：真实 OCR | 文件上传、J12～J21、J41/J42 | 实现 C02、接 OCR 服务商、异常标记 | 行驶证/身份证识别、确认入库、异常转人工 |
| 4：业务闭环 | J28～J40、平台回调和幂等 | 接异步状态与查询，不编造金额/保单 | 报价→核保/补件→支付核验→真实/明确模拟出单 |
| 5：补齐需求 | 历史投保、批量、恢复人工、术语接口 | VIN 开场、多车辆路由、跨客户委托适配 | 完整需求逐项验收 |

真实服务尚未接通时，各结果明确 `source=demo`（可在通用返回 data 增加 dataSource 字段），不可把模拟价格、支付成功或保单号当真。独立 OCR 配置与业务 demo 开关需拆开，避免为了试 OCR 提前启用尚未实现的真实业务接口。

## 14. 与当前代码的差异及适配方案

| 当前 C 代码 | 本稿目标 | 需要修改 |
|---|---|---|
| `POST /ai/chat` 收 message、conversation_id、plate | C01 + Java 统一会话/消息 | 适配 camelCase、requestId、消息 ID、委托身份 |
| `GET /ai/conversations/{cid}` 转业务 get | Java J08/J09 作为权威读取 | C 调 J09，B 向前端返回需要的会话结构 |
| `POST /ai/conversations/{cid}/confirm` 在图中调用业务 confirm | 用户确认落在 B 的 J20 | C 保留转发兼容或移除前端依赖；模型不获得确认权限 |
| `/materials/{kind}` 收 multipart，并保存/识别/转人工 | B 存文件→C02→B 保存结果和确认事项 | 拆分上传、识别、保存职责，补 readUrl 输入 |
| `business.call` 调 `/ai-tools/{operation}`，返回裸对象 | 本稿 Java 资源接口，统一包裹响应 | 改 business.py 适配层：拆包、字段/金额/状态映射、错误码、幂等 |
| 当前 plan 是 insurer/third_party_liability/vehicle_damage/compulsory | Plan + items + 独立版本 | 映射险种和金额、使用产品目录代码 |
| 当前 quote 后立即 underwriting | 真实报价可能异步 | 增加 processing 分支与状态读取，避免重复核保 |
| 当前 payment.status=paid；policy 动作兼出单 | 正式支付 success；B 自动核验后出单，C 只查 | 更新节点，避免聊天查询触发新的出单 |
| 内存最近六轮，取消/人工接管会清空 | Java 历史持久化，模型上下文按规则筛选 | 不删除审计历史；必要时用上下文边界标记排除过期轮次 |
| `/ai/batch` 同步共用一个身份 | B 逐客户授权批处理 | 明确 operator/customer 区分和逐项身份 |

B 可选择先实现原 `/ai-tools/{operation}` 作为兼容网关，或按本稿资源接口实现后由 C 适配；不需要同时维护两套独立业务实现。若保留网关，它只是转到同一 Java 应用服务的薄适配层。无论哪种路径，都以最终统一契约为准。

## 15. 请 B 确认的关键点

1. 最终路径、命名、错误包及金额格式；内部响应与前端 code=200 的转换位置。
2. Java 保存消息和 OCR 结果的唯一责任，C 不重复落库。
3. 内部认证及客户代办授权方式；会话 ID 全链路统一，不另造不必要的 session_id。
4. 选择 Java 提供候选方案还是 C 按产品目录生成候选；正式价格只来自业务报价。
5. 明确确认令牌、版本冲突、旧方案关联订单的处理，以及幂等键重试规则。
6. 报价成功后的核保、支付成功后的出单由谁推进；按本文默认 B 可靠推进异步链路、C 发起同步用户动作。
7. OCR 文件格式、临时读取地址、字段脱敏、材料到个人角色的绑定。

B 确认后，建议把这份草案转成唯一一份团队 OpenAPI 契约和固定 JSON 示例，A/B/C 的 AI 都读取同一版本。此时再修改 C 适配层，避免各自按不同字段继续开发。
