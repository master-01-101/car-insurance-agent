import httpx
import json
import os

# 基础配置
base_url = "http://127.0.0.1:8000"
headers = {
    "Authorization": "Bearer change-this-local-token",
    "X-User-Id": "user-1",
    "X-Salesperson-Id": "sales-1",
    "X-Role": "user"
}

def print_json(title, data):
    print(f"\n=== {title} ===")
    print(json.dumps(data, indent=2, ensure_ascii=False))

# 1. 发起对话并确认 (对应 README 步骤 1-2)
print("1. 发起对话...")
res = httpx.post(f"{base_url}/ai/chat", headers=headers, json={"plate": "粤B12345", "message": "推荐方案"})
data = res.json()
print_json("发起对话返回", data)
cid = data["conversation"]["id"]
token = data["conversation"]["pending"]["token"]

print("\n2. 确认方案...")
httpx.post(f"{base_url}/ai/conversations/{cid}/confirm", headers=headers, json={"token": token})
print("方案已确认")

# 3. 修改方案并确认 (对应 README 步骤 4)
print("\n3. 发送消息修改三者险到200万...")
res_mod = httpx.post(f"{base_url}/ai/chat", headers=headers, json={"conversation_id": cid, "message": "三者改到200万"})
data_mod = res_mod.json()
print_json("修改方案返回", data_mod)
token_mod = data_mod["conversation"]["pending"]["token"]

print("\n4. 确认修改...")
httpx.post(f"{base_url}/ai/conversations/{cid}/confirm", headers=headers, json={"token": token_mod})
print("修改已确认")

# 5. 模拟上传材料
print("\n5. 开始上传4种材料...")
os.makedirs("mock_files", exist_ok=True)
for kind in ["license_front", "license_back", "id_front", "id_back"]:
    fake_file_path = f"mock_files/{kind}.jpg"
    with open(fake_file_path, "wb") as f:
        f.write(b"fake image data")
        
    with open(fake_file_path, "rb") as f:
        files = {"file": (f"{kind}.jpg", f, "image/jpeg")}
        url = f"{base_url}/ai/conversations/{cid}/materials/{kind}"
        
        res_upload = httpx.post(url, headers=headers, files=files)
        upload_data = res_upload.json()
        
        # 把上传接口的完整返回打印出来，方便排查结构
        print_json(f"上传 {kind} 完整返回", upload_data)
        
        # 兼容两种可能的返回结构
        pending = upload_data.get("conversation", {}).get("pending")
        if not pending:
             pending = upload_data.get("pending") 
             
        if pending and "token" in pending:
            # 发送确认请求
            confirm_res = httpx.post(f"{base_url}/ai/conversations/{cid}/confirm", headers=headers, json={"token": pending["token"]})
            print(f"✅ 已确认 {kind} 的 token，确认结果状态码: {confirm_res.status_code}")
        else:
            print(f"⚠️ 未找到 {kind} 的 pending，无需确认")

# 6. 发送报价请求
print("\n6. 发送报价请求...")
res_quote = httpx.post(f"{base_url}/ai/chat", headers=headers, json={"conversation_id": cid, "message": "报价"})
quote_data = res_quote.json()
print_json("报价返回", quote_data)

# 如果报价成功，看看是否有待确认的核保结果
if "conversation" in quote_data:
    pending_quote = quote_data["conversation"].get("pending")
    if pending_quote and "token" in pending_quote:
        httpx.post(f"{base_url}/ai/conversations/{cid}/confirm", headers=headers, json={"token": pending_quote["token"]})
        print("✅ 报价结果已确认")

# 7. 发送支付 (对应 README 步骤 7)
print("\n7. 发送支付请求...")
res_pay = httpx.post(f"{base_url}/ai/chat", headers=headers, json={"conversation_id": cid, "message": "支付"})
print_json("支付返回", res_pay.json())

# 8. 模拟支付成功 (对应 README 步骤 8)
print("\n8. 模拟第三方支付成功...")
res_pay_demo = httpx.post(f"{base_url}/demo/{cid}/pay", headers=headers)
print_json("模拟支付返回", res_pay_demo.json())

# 9. 获取保单 (对应 README 步骤 8)
print("\n9. 发送保单请求...")
res_policy = httpx.post(f"{base_url}/ai/chat", headers=headers, json={"conversation_id": cid, "message": "保单"})
print_json("保单返回", res_policy.json())

print("\n🎉 演示流程全部跑完！")