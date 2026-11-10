# 车险出单智能体 · 前端

车险出单智能体的前端 UI 骨架。**当前只有界面与交互结构，后端 API 与 Agent 能力一律未接入**，
涉及这些能力的位置都用「此地方未导入」占位符标注，并集中登记在
`src/api/placeholders.ts`。

> 需求依据：仓库根目录 `前端.md`、`车险出单需求.md`。
> 面向协作方的工程说明：仓库根目录 `AGENTS.md`。

## 技术栈

| 类别 | 选型 |
| --- | --- |
| 框架 | React 18 + TypeScript 5 + Vite 6 |
| UI 组件 | Ant Design 5（中文 locale） |
| 路由 | react-router-dom 6 |
| 请求 | Axios（统一实例 + 响应解包，尚未发出真实请求） |
| 样式 | 原生 CSS + CSS 变量设计令牌（无 CSS-in-JS） |

## 启动

```bash
npm install
npm run dev        # http://localhost:5173
```

其它命令：

```bash
npm run typecheck  # tsc --noEmit
npm run build      # 类型检查 + 生产构建，产物在 dist/
npm run preview    # 预览构建产物
node scripts/screenshot.mjs   # 需先启动 dev，截图输出到仓库根 screenshots/
```

浏览器打开后会跳转到 `/login`。当前登录不校验密码，选择「普通用户 / 业务员」后
点登录即可进入对应视图（身份只写入 localStorage，属演示行为）。

## 页面与角色

| 路由 | 页面 | 权限 |
| --- | --- | --- |
| `/login` | 登录 | 公开 |
| `/` | 车险入口（财险、人寿险显示暂未开放） | 登录用户 |
| `/chat` | 办理会话（聊天 + 右侧流程/资料栏） | 登录用户 |
| `/orders` | 订单 | 登录用户 |
| `/policy`、`/policy/:policyId` | 保单明细 | 登录用户 |
| `/handoff` | 人工接管 | 登录用户 |
| `/workbench` | 业务员工作台（批量操作） | 仅 `role === 'agent'` |

权限由 `src/router/guards.tsx` 控制，非业务员访问 `/workbench` 会看到 403 页面。

## 目录结构

```
src/
  api/          接口层：placeholders.ts（占位符注册表）、http.ts（Axios 实例）、index.ts（模块化接口骨架）
  components/   聊天气泡、方案卡片、状态卡片、上传位、流程步骤、占位符组件等
  data/         flow.ts（七阶段定义）、samples.ts（静态占位数据）
  layouts/      AppLayout（侧边栏 + 顶栏 + 内容区）
  pages/        登录、车险入口、聊天、订单、保单、业务员工作台、人工接管、404/403
  router/       路由表与角色守卫
  store/        登录态、当前会话、当前车辆/阶段（Context）
  styles/       tokens.css（设计令牌）、global.css（布局与组件样式）
  theme/        antdTheme.ts（Ant Design 主题映射）
  types/        与后端约定的 TypeScript 类型草案
  utils/        金额/时间格式化、图片校验
```

## 设计要求（已落实，改动时请保持）

- 页面背景固定为 `linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%)`（`tokens.css` → `--app-background-image`）。
- **严禁使用紫色系配色**（violet / purple / fuchsia / 色相 260–300 一律禁止）。
  全站配色为海军蓝 `#0f172a`、冷蓝 `#0369a1`、中性灰与红/绿/琥珀功能色。
- 风格：极简 / Swiss，克制阴影、细描边、大量留白，图标全部使用 SVG（`@ant-design/icons`，不使用 emoji）。

## 占位符约定

不要在页面里直接写「未实现」之类文案。新增后端/Agent 依赖时：

1. 在 `src/api/placeholders.ts` 的 `SPECS` 中追加一条（标题、说明、预期接口、补齐方、界面位置）；
2. 页面用 `<PlaceholderBlock id="…" />`、`<PlaceholderInline id="…" />`
   或 `useNotImported()` 弹出说明；
3. 同步更新根目录 `AGENTS.md` 的占位符清单。

## 常见问题

- **`npm install` 报缓存目录不可写**：把缓存指到工作区内，例如
  `npm install --cache ./.npm-cache`。
- **`npm run build` 报 esbuild 临时文件 `Access is denied`**：把临时目录指到工作区内再构建，
  PowerShell 下：`$env:TEMP = "$PWD\.build-tmp"; $env:TMP = $env:TEMP; npm run build`。
- **dev 时改文件导致 watcher 报 `EBUSY`**：`vite.config.ts` 已忽略 `.npm-cache`、`.build-tmp`、
  `*.tmpdir` 与 `screenshots`，如仍出现请检查是否新增了临时目录。

## 尚未包含

真实鉴权、会话与消息持久化、图片上传/OCR、方案生成与修改、报价、核保、支付、
保单、业务员批量接口、人工接管平台、行业术语知识库。详见 `AGENTS.md`。
