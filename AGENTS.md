# AGENTS.md

> 面向在本仓库工作的 AI 代理与开发者：**先读本文，再动代码**。
> 本文记录项目的技术栈、目录结构、以及全部「未导入」占位符的位置。
> 需求原文：`前端.md`（前端任务书）、`车险出单需求.md`（业务需求）。

---

## 1. 项目是什么

车险出单智能体的前端。业务流程共七个阶段：
**收集信息 → 智能推荐 → 身份证上传 → 报价 → 核保 → 支付 → 保单**。

本仓库**只包含前端 UI 骨架**：页面、布局、路由、角色权限、组件与样式已完成；
**所有后端 API 与 Agent（智能体）能力都未导入**，对应位置一律用占位符
「此地方未导入」标注，绝不伪造数据或假装成功。

交付物位置：

| 内容 | 路径 |
| --- | --- |
| 前端工程 | `frontend/` |
| 启动与页面说明 | `frontend/README.md` |
| 界面自检截图（11 张） | `screenshots/` |
| 本文（占位符 / 技术栈 / 结构） | `AGENTS.md` |

---

## 2. 硬性约束（违反即视为交付缺陷）

1. **不得伪造后端数据。** 不写假接口、不造假报价/核保/支付结果、不在前端计算保费。
   `code`/状态一律以后端返回为准。
2. **占位符只有一个数据源**：`frontend/src/api/placeholders.ts`。页面里不要硬编码
   「未实现」「待开发」等文案，改用 `<PlaceholderBlock id="…"/>`、
   `<PlaceholderInline id="…"/>` 或 `useNotImported()`。
3. **严禁紫色系配色**（violet / purple / fuchsia / 色相 260–300）。全站是海军蓝 + 冷蓝 + 中性灰。
4. **页面背景固定**为 `linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%)`
   （`frontend/src/styles/tokens.css` 的 `--app-background-image`），不得替换。
5. **风格保持简洁**（Minimalism / Swiss）：细描边、克制阴影、大量留白、
   图标只用 SVG（`@ant-design/icons`），不要用 emoji 当图标。
6. **不得在页面组件里散落硬编码接口 URL**；接口统一收在 `src/api/`。
7. 改动占位符后，必须同步本文第 5 节的清单。

---

## 3. 技术栈

| 项目项 | 选型 | 版本（已安装） |
| --- | --- | --- |
| 框架 | React + TypeScript | react 18.3.1 / typescript 5.9.3 |
| 构建 | Vite | 6.4.5 |
| UI 组件 | Ant Design（中文 locale） | antd 5.29.3 |
| 图标 | @ant-design/icons | 5.6.1 |
| 路由 | react-router-dom | 6.30.6 |
| 请求 | axios（统一实例，尚未发真实请求） | 1.21.0 |
| 日期 | dayjs（预留给后续接口格式化） | 1.11.23 |
| 样式 | 原生 CSS + CSS 变量（无 CSS-in-JS、无 Tailwind） | — |
| 开发自检 | puppeteer-core（仅 devDependency，用于截图） | 25.16.0 |

包管理器：npm。脚本见 `frontend/package.json`：`dev` / `build` / `typecheck` / `preview`。

---

## 4. 项目结构

```
Project/
├─ AGENTS.md                  # 本文件（占位符清单、技术栈、结构）
├─ 前端.md / 车险出单需求.md    # 需求原文
├─ screenshots/               # 界面自检截图（01-login … 09-nav-drawer）
└─ frontend/
   ├─ index.html
   ├─ vite.config.ts          # @ → src 别名；忽略 .npm-cache/.build-tmp/*.tmpdir 的 watch
   ├─ tsconfig.json           # strict + noUnusedLocals/Parameters
   ├─ .env.example            # VITE_API_BASE_URL
   ├─ scripts/screenshot.mjs  # 无头 Edge 打开各路由并截图
   └─ src/
      ├─ main.tsx             # ConfigProvider(zhCN, antdTheme) + AntApp + Store + Router
      ├─ api/
      │   ├─ placeholders.ts  # ★ 未导入能力注册表（18 条，唯一数据源）
      │   ├─ notImported.ts   # NotImportedError / notImported(id)
      │   ├─ http.ts          # axios 实例、token 注入、统一响应解包
      │   └─ index.ts         # 按模块的接口骨架（全部当前 reject NotImportedError）
      ├─ components/
      │   ├─ PlaceholderBlock.tsx    # 块级/行内占位符 + 弹窗详情
      │   ├─ useNotImported.tsx      # 点击未导入功能时弹说明的 hook
      │   ├─ ChatBubble.tsx          # text/image/plan/status/handoff 五种消息
      │   ├─ PlanCard.tsx            # 保险方案卡片（含保额/保费/合计）
      │   ├─ StatusCard.tsx          # 业务状态卡片（info/success/warning/danger）
      │   ├─ StatusTag.tsx           # 订单状态标签
      │   ├─ ChatComposer.tsx        # 输入区 + 快捷操作 chips
      │   ├─ ConversationList.tsx    # 会话列表（标题=车牌号）
      │   ├─ UploadTile.tsx          # 行驶证/身份证上传位
      │   ├─ FlowSteps.tsx           # 右侧栏阶段步骤
      │   ├─ FlowOverview.tsx        # 入口页七阶段概览
      │   ├─ PageHeader.tsx / BrandMark.tsx
      ├─ data/
      │   ├─ flow.ts          # 七阶段定义 + 阶段状态占位
      │   └─ samples.ts       # ★ 静态占位数据（非业务数据，接口导入后删除）
      ├─ layouts/AppLayout.tsx       # 侧边栏 + 顶栏 + 内容区 + 移动端抽屉
      ├─ pages/                      # 8 个页面（见第 6 节）
      ├─ router/
      │   ├─ index.tsx        # 路由表
      │   └─ guards.tsx       # RequireAuth / RequireRole
      ├─ store/AppStore.tsx   # 登录态、会话、当前车辆/阶段、移动端导航
      ├─ styles/
      │   ├─ tokens.css       # ★ 设计令牌（配色、间距、圆角、阴影、渐变）
      │   └─ global.css       # 布局与组件样式 + 响应式
      ├─ theme/antdTheme.ts   # Ant Design token 映射
      ├─ types/index.ts       # 与后端约定的类型草案
      └─ utils/format.ts, file.ts
```

---

## 5. 占位符清单（未导入能力）

共 **18** 处。`frontend/src/api/placeholders.ts` 为唯一数据源，
`id` 即组件 `<PlaceholderBlock id="…"/>` 的取值。

| # | 占位 ID | 未导入能力 | 界面位置 | 补齐方 | 预期接口（待确认） |
| --- | --- | --- | --- | --- | --- |
| 1 | `auth-login` | 登录与身份鉴权、token 下发、角色判定 | 登录页、路由守卫 | 成员B | `POST /api/auth/login`、`GET /api/auth/profile` |
| 2 | `conversation-list` | 会话列表、新建会话、按车牌号分组 | 侧边栏「会话 · 按车牌号」 | 成员B | `GET /api/conversations`、`POST /api/conversations`、`PATCH /api/conversations/{id}` |
| 3 | `chat-history` | 历史消息、分页、已读 | 聊天页消息区顶部 | 成员B | `GET /api/conversations/{id}/messages?cursor=&limit=` |
| 4 | `chat-send` | 用户消息提交、Agent 回复 | 聊天输入区 | 成员C | `POST /api/conversations/{id}/messages` |
| 5 | `agent-stream` | 流式输出（SSE） | 输入区「流式输出」按钮 | 成员B + C | `GET /api/conversations/{id}/stream` |
| 6 | `license-upload` | 行驶证正/副页上传、预览、删除重传 | 右侧栏「车辆资料」 | 成员B | `POST /api/files`、`DELETE /api/files/{fileId}` |
| 7 | `ocr-license` | 行驶证 OCR 识别与字段回填 | 右侧栏「OCR 结果确认」 | 成员B + C | `POST /api/ocr/license` |
| 8 | `vehicle-query` | 老用户按车牌/车架号查去年保单 | 车险入口页 | 成员B | `GET /api/vehicles/lookup?plateNumber=&vin=` |
| 9 | `plan-recommend` | 智能推荐方案（公司/险种/保额/价格） | 聊天气泡内的方案卡片 | 成员C | `POST /api/plans/recommend` |
| 10 | `plan-revise` | 自然语言修改方案、确认方案 | 方案卡片按钮、「修改方案」chip | 成员C | `POST /api/plans/{planId}/revise` |
| 11 | `idcard-upload` | 身份证正反面上传与识别 | 右侧栏「身份证影像」 | 成员B + C | `POST /api/files`、`POST /api/ocr/idcard` |
| 12 | `quote` | 报价、报价明细、有效期 | 右侧栏「业务动作」、订单页 | 成员B | `POST /api/quotes`、`GET /api/quotes/{quoteId}` |
| 13 | `underwriting` | 核保通过/失败/补验车材料 | 右侧栏「业务动作」 | 成员B | `POST /api/underwriting`、`POST /api/underwriting/{caseId}/materials` |
| 14 | `payment` | 第三方支付下单、跳转、回调校验 | 右侧栏「业务动作」、保单页 | 成员B | `POST /api/payments`、`GET /api/payments/{paymentId}`、`POST /api/payments/callback` |
| 15 | `policy` | 保单明细（投保人/车辆/险种/生效期/条款） | 保单页 | 成员B | `GET /api/policies/{policyId}` |
| 16 | `agent-workbench` | 业务员批量报价/核保/支付/转人工 | 业务员工作台 | 成员B | `GET /api/workbench/orders`、`POST /api/workbench/batch` |
| 17 | `handoff` | 人工接管：申请、排队、坐席回复、状态同步 | 聊天气泡、人工接管页 | 成员B + C | `POST /api/handoff`、`GET /api/handoff/{ticketId}` |
| 18 | `knowledge-base` | 行业术语知识库查询 | 车险入口页、聊天右侧栏 | 成员C | `GET /api/knowledge/terms?q=` |

> 接口路径来自《前端.md》第四节「团队建议」，**尚未与后端确认**；
> 真实保险公司报价/核保、第三方支付、人工接管平台接口需项目方另行提供。

### 占位符的三种呈现

| 用法 | 组件 | 说明 |
| --- | --- | --- |
| 块级 | `<PlaceholderBlock id="quote" />` | 虚线框 + 徽标「此地方未导入」+ 说明 + 接口标签 |
| 行内 | `<PlaceholderInline id="chat-send" />` | 单行提示条，用于输入区等窄空间 |
| 交互 | `const notImported = useNotImported(); onClick={() => notImported('quote')}` | 弹出完整说明弹窗 |

### 新增 / 移除占位符

**新增**：在 `placeholders.ts` 的 `SPECS` 追加条目（title/desc/endpoints/owner/ui）→ 页面引用
`PlaceholderBlock` 或 `useNotImported` → 同步本文表格与数量（第 5 节标题里的 18）。

**移除（接口已导入）**：
1. 在 `src/api/index.ts` 把对应方法的 `notImported('…')` 换成
   `request<T>({ url, method, data })`（`http.ts` 已处理 token 与解包）；
2. 页面改用真实数据 + loading/error 状态，删掉对应 `PlaceholderBlock`；
3. 从 `placeholders.ts` 与本文表格中删除该条目；
4. 若数据来自 `data/samples.ts`，同时删除引用。

---

## 6. 路由与角色权限

| 路由 | 页面组件 | 权限 |
| --- | --- | --- |
| `/login` | `pages/LoginPage.tsx` | 公开（不套用 AppLayout） |
| `/` | `pages/EntryPage.tsx` | 登录用户 |
| `/chat` | `pages/ChatPage.tsx` | 登录用户 |
| `/orders` | `pages/OrdersPage.tsx` | 登录用户 |
| `/policy`、`/policy/:policyId` | `pages/PolicyPage.tsx` | 登录用户 |
| `/handoff` | `pages/HandoffPage.tsx` | 登录用户 |
| `/workbench` | `pages/WorkbenchPage.tsx` | 仅 `user.role === 'agent'` |
| `*` | `pages/NotFoundPage.tsx` | — |

- 未登录访问受保护路由 → 重定向 `/login`（带回跳地址）。
- 普通用户访问 `/workbench` → 403 页面（`ForbiddenPage`）。
- 角色菜单：`AppLayout` 中「业务员工作台」仅对业务员显示。
- **当前登录态是本地演示**：点登录只把身份写入 `localStorage('chexian.auth.demo')`，
  不校验密码、不发请求。真实鉴权见占位符 `auth-login`。

---

## 7. 数据契约（草案，需与成员 B / C 确认）

统一响应（《前端.md》第五节）：

```json
{ "code": 200, "message": "success", "data": {} }
```

聊天消息：

```json
{
  "conversationId": "会话ID",
  "messageId": "消息ID",
  "role": "user | assistant | tool",
  "content": "消息文本",
  "messageType": "text | image | plan | status | handoff",
  "createdAt": "时间"
}
```

七阶段枚举（`src/data/flow.ts`、`types/index.ts`）：
`collect | recommend | identity | quote | underwriting | payment | policy`。

其它类型（会话、车辆、OCR 字段、方案、报价、核保、支付、保单、工单）见
`frontend/src/types/index.ts`，未确认字段已在注释中标注。

---

## 8. 设计系统（已落地）

来源：`ui-ux-pro-max` 生成的 **Minimalism & Swiss Style** 设计系统，按需求改为
「海军蓝 + 冷蓝」并强制排除紫色。

| 令牌 | 值 | 用途 |
| --- | --- | --- |
| `--color-primary` | `#0f172a` | 标题、主文字、深色面板 |
| `--color-accent` | `#0369a1` | 主按钮、链接、当前阶段 |
| `--color-accent-soft` | `#e0f2fe` | 激活态、车牌标签 |
| `--color-background` | `#f8fafc` | 回退底色 |
| `--app-background-image` | `linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%)` | **页面背景（需求指定）** |
| `--color-border` | `#e2e8f0` | 分隔线、描边 |
| `--color-muted-foreground` | `#475569` | 次要文字 |
| `--color-destructive` / `success` / `warning` | `#dc2626` / `#15803d` / `#b45309` | 状态色 |

- 圆角 8/12/16px，阴影仅 3 级且很轻；间距 4→48px（8 基准）。
- 字体栈：`Inter` → 系统 sans → `PingFang SC` / `Microsoft YaHei`（未下载 Web Font，避免离线卡顿）。
- 断点：1280 / 1180（聊天右侧栏收起，改抽屉）/ 960（侧边栏收进抽屉）/ 640。
- 无障碍：可见焦点环、`prefers-reduced-motion` 降级、图标按钮有 `aria-label`、
  正文对比度 ≥ 4.5:1。

---

## 9. 静态占位数据（重要）

`frontend/src/data/samples.ts` 里的会话、消息、方案、订单、保单、工单**都不是业务数据**，
只用于确认布局；引用处同时显示「静态占位」标签，且不参与任何计算。

接口导入后，应删除该文件及全部引用，改用 `src/api/` 的真实调用。

---

## 10. 本地命令与验证

```bash
cd frontend
npm install
npm run dev          # http://localhost:5173（/login 起步）
npm run typecheck    # 必须 0 error
npm run build        # 必须成功，产物 dist/
node scripts/screenshot.mjs   # 需 dev 在跑；输出到 ../screenshots
```

环境注意（本机已踩过）：

- npm 默认缓存目录可能不可写：`npm install --cache ./.npm-cache`。
- 构建时 esbuild 清理临时文件可能被拒：
  PowerShell 下先 `$env:TEMP = "$PWD\.build-tmp"; $env:TMP = $env:TEMP`。
- Vite 的 watcher 已忽略 `.npm-cache`、`.build-tmp`、`*.tmpdir`、`screenshots`，
  否则编辑器临时目录会触发 `EBUSY` 并让 dev server 退出。
- dev server 只监听 `localhost`（IPv6），截图脚本用 `http://localhost:5173`，
  不要写 `127.0.0.1`。

改动后的最低验证：`npm run typecheck` + `npm run build` + 截图脚本无控制台错误。

---

## 11. 当前限制与后续工作

- 除「登录（本地演示）」外，**没有任何可用业务动作**；所有业务按钮都会弹出占位说明。
- 报价、核保、支付、出单状态一律未展示为成功——只有后端确认后才能显示。
- 未实现：SSE 流式、图片上传进度/预览、OCR 字段编辑校验、方案版本对比、分页、
  批量操作结果回执、人工接管实时推送、知识库检索。
- 建议下一步：先与成员 B 对齐统一响应与错误码，再按第 5 节表格自下而上逐个替换
  `notImported`，每替换一个就删一个占位符，保持「界面无假数据」这条底线。
