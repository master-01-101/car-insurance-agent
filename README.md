# 车险 AI 服务

基于 ai.docx 的可运行参考实现。Python 3.11+。默认 demo 全流程可运行，真实模式需要成员 B 实现约定业务网关，并接入模型、OCR。尚未经真实保险平台联调，不能直接作为生产出单系统。

## Windows 启动

解压后在本目录打开 PowerShell。

```powershell
python -m venv .venv
.\.venv\Scripts\python -m pip install -r requirements.txt
Copy-Item .env.example .env
.\.venv\Scripts\python -m uvicorn app.main:app --reload --host 127.0.0.1 --port 8000
```

浏览器打开 http://127.0.0.1:8000/docs 。所有业务接口填写四个请求头。

| 头 | 演示值 |
|---|---|
| Authorization | Bearer change-this-local-token |
| X-User-Id | user-1 |
| X-Salesperson-Id | sales-1 |
| X-Role | user 或 salesperson |

身份头只允许可信业务后端提供。前端通过成员 B 登录，B 校验用户再调用 AI 服务。不要把 SERVICE_TOKEN 放进前端。

## 演示步骤

1. POST /ai/chat，正文 `{"plate":"粤B12345","message":"推荐方案"}`。
2. 保存返回的 conversation.id。方案在 conversation.pending.value 中，确认令牌在 pending.token 中。
3. POST /ai/conversations/{cid}/confirm，正文 `{"token":"刚才返回的token"}`。
4. POST /ai/chat，正文 `{"conversation_id":"会话ID","message":"三者改到200万"}`，查看新方案，再确认新的 token。
5. 逐个调用 materials 接口上传 license_front、license_back、id_front、id_back。每次上传后确认该次返回的 pending.token。一次仅保留一个待确认操作。
6. 同一会话发送“报价”，材料齐全并确认后得到模拟报价和核保结果。
7. 发送“支付”，得到待支付记录。模拟支付地址为空。
8. 调用 POST /demo/{cid}/pay 模拟第三方确认支付成功，再发送“保单”。
9. 使用粤B12999测试核保补件，上传 inspection 后发送“核保”。发送“人工”后自动操作暂停。
10. 发送“查询去年方案”测试续保方案复用，demo 仍要求重新上传并确认材料。

材料、报价、投保人和保单在 demo 中全是测试数据。模拟 OCR 不解析图片内容。模拟业务状态仅存在进程内，重启清空，demo 只启动一个 worker。

## 模型与真实接口

将 MODE 改为 real，填写模型和业务网关配置。MODEL_URL 使用 OpenAI 兼容 chat/completions 的根地址，MODEL_NAME 为实际支持工具调用的模型。模型只输出受约束的意图，动作由服务端控制。用户确认通过带 token 的确认接口执行，普通聊天中的“确认”只提示界面确认。

OCR_URL 接收 multipart file 和 kind，返回 `{"fields":{"name":"...","id_number":"..."},"needs_review":false}`。字段缺失或冲突返回 needs_review=true。该适配接口应完成字段映射、证件类型校验、日期检查和跨影像一致性校验。行驶证 VIN 等字段应按实际证件布局提取，不能硬编码在副页。AI 收到异常后追问并触发人工。

业务网关契约见 BACKEND.md。原始文件交业务网关保存，AI 不直接写业务数据库。真实支付跳转链接、每项保费、起止时间、条款和保单身份信息都由后端返回。

## 知识库

app/terms.json 是可编辑词条，审核人当前标记为待团队审核。默认使用精确同义词检索。配置 QDRANT_URL 和 EMBEDDING_* 后执行 `python -m app.rag`，初始化 Qdrant 集合。模型理解时会使用向量检索结果。相似度阈值 0.7 需结合你们选用的嵌入模型校准。真实向量服务尚未联调。

## 测试

```powershell
.\.venv\Scripts\python -m tests.smoke
```

测试覆盖模拟端到端、方案修改、材料确认、报价前置、用户口头支付不能直接出单、不同用户隔离、人工暂停和批量权限。真实模式仍需接口超时、幂等重试、并发版本冲突、OCR 错误样本和真实模型工具输出联调。

## 文件职责

- app/main.py 提供聊天、材料上传、用户确认、会话查询和业务员批量入口。
- app/providers.py 提供模型 Function Calling 和 OCR 接口适配。
- app/business.py 提供模拟业务逻辑和真实网关适配。
- app/models.py 定义输入和模型意图结构。
- app/knowledge.py 与 app/rag.py 提供术语匹配和向量检索。
- tests/smoke.py 提供可直接执行的演示验收。

## 与任务书仍需对齐的边界

本包覆盖任务书主流程，但真实业务能力取决于 B 的实现。VIN 单独发起查询、纠正已有会话车牌、多车在同一消息中自动拆分、人工处理完恢复自动流程尚未实现；当前需要先按车牌建会话，一次消息办理一辆车，业务员可使用批量接口。材料缺失只按类型检查，真实字段有效性由 OCR 适配器和 B 校验。没有单独网页，使用 /docs 联调。业务操作日志和审计持久化由 B 承担，AI 服务不落盘敏感内容。批量每项独立返回结果，确认仍逐车执行。任何真实接口超时都需先查状态再重试。
