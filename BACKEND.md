# 成员 B 对接约定

这是本项目定义的适配契约，需由团队实现或按现有 API 修改 app/business.py。

所有工具 POST {BUSINESS_URL}/ai-tools/{operation}，使用 BUSINESS_TOKEN 鉴权。

请求含 principal（user_id、salesperson_id、role）、conversation_id、arguments、idempotency_key。

B 必须校验身份范围、阶段、参数、原子更新和幂等键。会话按用户与车牌关联。所有会话类响应返回同一结构。

```json
{"id":"c1","owner":"u1","salesperson_id":"s1","plate":"粤B12345","title":"粤B12345","state":"collecting","materials":{},"plan":null,"pending":null,"policy":null,"payment":null,"version":0}
```

| operation | arguments / 返回 |
|---|---|
| open | plate，创建或查询会话，返回完整会话 |
| get | 空参数，返回完整会话 |
| history | 按会话车辆查去年方案，返回方案对象，查无数据需返回业务错误 |
| recommend | 返回可投方案，价格来自真实报价或明确待报价 |
| stage | kind、value，保存待确认数据并签发唯一 token，返回完整会话 |
| confirm | token，原子消费 token，校验并保存方案或资料，返回完整会话 |
| upload_material | kind、content_type、base64，返回 material_id |
| quote | 检查方案与材料确认后报价，返回完整会话 |
| underwriting | 提交或查询核保，返回完整会话及 required_materials |
| supplement | material_id，保存验车材料，返回完整会话 |
| payment | 幂等创建支付单或查询现有支付单，返回 payment.id/status/url |
| policy | 查询或触发幂等出单，返回完整会话，未出单不得标记 issued |
| handoff | reason、summary 或 material_id，接入人工平台并暂停自动动作 |
| cancel | 清除当前待确认操作，返回完整会话 |

state 可用 collecting、ready、quoted、supplement、approved、paying、paid、issued、human。pending 为 null 或 {token,kind,value}。每次真实业务变更增加 version，读操作不增加。支付回调由 B 验签并持久化，AI 仅查询状态。报价后核保可能异步，B 应返回当前状态，后续客户端查询/发消息刷新。HTTP 失败不能伪装成功。上游 4xx 当前统一映射为 AI 的 502，需要根据团队错误码进一步改进提示。

同版本相同操作使用相同幂等键；B 必须使幂等结果与业务更新同事务提交。业务成功但网络中断后，重试应返回已有结果。并发实例的最终一致性由 B 保证，本地 asyncio.Lock 仅保护单进程。
