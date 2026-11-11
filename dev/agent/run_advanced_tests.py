r"""进阶边界测试脚本。

覆盖 README 中 run_demo.py 未包含的场景：
  1. 核保补件（粤B12999 车牌尾号 999 触发补验车）
  2. 人工暂停（发送"人工"后自动操作暂停）
  3. 用户隔离（user-2 访问 user-1 的会话）
  4. 批量权限（/ai/batch 仅 salesperson 角色可用）
  5. 续保方案复用（"查询去年方案"）

运行前请确保服务已启动，且为全新进程（demo 业务状态存内存，重启即清空，
旧会话会因 owner+plate 复用导致状态冲突）。
运行：.\.venv\Scripts\python.exe run_advanced_tests.py
"""

import json
import sys

import httpx

BASE_URL = "http://127.0.0.1:8000"


def make_headers(user_id: str = "user-1", role: str = "user") -> dict:
    return {
        "Authorization": "Bearer change-this-local-token",
        "X-User-Id": user_id,
        "X-Salesperson-Id": "sales-1",
        "X-Role": role,
    }


def print_json(title: str, obj):
    print(f"\n--- {title} ---")
    if isinstance(obj, dict):
        print(json.dumps(obj, ensure_ascii=False, indent=2))
    else:
        print(obj)


def get_pending(data):
    """兼容两种返回结构：chat 返回 conversation.pending，上传/确认返回顶层 pending。"""
    if not isinstance(data, dict):
        return None
    conv = data.get("conversation")
    if isinstance(conv, dict) and conv.get("pending"):
        return conv["pending"]
    return data.get("pending")


def confirm_pending(client, cid, data, headers, label=""):
    """若响应含 pending.token，则调用确认接口消费，保证单次仅一个待确认操作。"""
    pending = get_pending(data)
    if not pending or not pending.get("token"):
        return None
    res = client.post(
        f"/ai/conversations/{cid}/confirm",
        headers=headers,
        json={"token": pending["token"]},
    )
    print(f"   [确认 {label} token] status={res.status_code}")
    return res


RESULTS = []


def record(name: str, ok: bool, detail: str = ""):
    RESULTS.append((name, ok))
    mark = "PASS" if ok else "FAIL"
    print(f"[{mark}] {name}")
    if detail:
        print(f"        {detail}")


# ---------------------------------------------------------------------------
# 用例 1：核保补件测试
# ---------------------------------------------------------------------------
def test_supplement(client):
    print("\n" + "=" * 70)
    print("用例 1：核保补件测试（粤B12999，尾号 999 触发补验车）")
    print("=" * 70)
    h = make_headers()
    plate = "粤B12999"

    # 1) 建会话 + 拿方案
    res = client.post("/ai/chat", headers=h, json={"plate": plate, "message": "推荐方案"})
    data = res.json()
    print_json("建立会话 + 推荐方案", data)
    cid = (data.get("conversation") or {}).get("id")
    record("1.1 建立会话并返回方案", res.status_code == 200 and bool(cid), f"cid={cid}")
    confirm_pending(client, cid, data, h, "方案")

    # 2) 上传 4 种标准材料并逐次确认
    for kind in ["license_front", "license_back", "id_front", "id_back"]:
        res = client.post(
            f"/ai/conversations/{cid}/materials/{kind}",
            headers=h,
            files={"file": (f"{kind}.jpg", b"fake-image-data", "image/jpeg")},
        )
        if res.status_code != 200:
            record(f"1.2 上传 {kind}", False, f"status={res.status_code}, body={res.text}")
            continue
        confirm_pending(client, cid, res.json(), h, kind)
    record("1.2 上传 4 种材料并确认", True)

    # 3) 报价 -> 自动核保 -> 因缺 inspection 进入补件状态
    res = client.post("/ai/chat", headers=h, json={"conversation_id": cid, "message": "报价"})
    data = res.json()
    print_json("报价后（应提示补 inspection）", data)
    conv = data.get("conversation") or {}
    state = conv.get("state")
    required = conv.get("required_materials") or []
    ok = res.status_code == 200 and state == "supplement" and "inspection" in required
    record("1.3 报价触发核保补件状态", ok, f"state={state}, required_materials={required}")

    # 4) 上传 inspection 验车材料（supplement 不产生 token，无需确认）
    res = client.post(
        f"/ai/conversations/{cid}/materials/inspection",
        headers=h,
        files={"file": ("inspection.jpg", b"fake-image-data", "image/jpeg")},
    )
    data = res.json()
    print_json("上传 inspection 验车材料", data)
    record("1.4 上传 inspection 成功", res.status_code == 200, f"status={res.status_code}")

    # 5) 再次核保 -> approved
    res = client.post("/ai/chat", headers=h, json={"conversation_id": cid, "message": "核保"})
    data = res.json()
    print_json("核保结果", data)
    state = (data.get("conversation") or {}).get("state")
    record("1.5 核保状态变为 approved", res.status_code == 200 and state == "approved", f"state={state}")

    return cid


# ---------------------------------------------------------------------------
# 用例 2：人工暂停测试
# ---------------------------------------------------------------------------
def test_manual_pause(client):
    print("\n" + "=" * 70)
    print("用例 2：人工暂停测试")
    print("=" * 70)
    h = make_headers()

    # 直接对新车牌发"人工"，会话创建后即进入人工接管
    res = client.post("/ai/chat", headers=h, json={"plate": "粤B23456", "message": "人工"})
    data = res.json()
    print_json("发送\"人工\"", data)
    conv = data.get("conversation") or {}
    cid = conv.get("id")
    state = conv.get("state")
    reply = data.get("reply")
    record("2.1 发送\"人工\"后状态为 human", res.status_code == 200 and state == "human",
           f"state={state}, reply={reply}")

    # 暂停后再发"报价"，应只提示人工接管，不再自动触发
    res2 = client.post("/ai/chat", headers=h, json={"conversation_id": cid, "message": "报价"})
    data2 = res2.json()
    print_json("暂停后发送\"报价\"", data2)
    state2 = (data2.get("conversation") or {}).get("state")
    reply2 = data2.get("reply")
    no_trigger = res2.status_code == 200 and state2 == "human" and "人工接管" in (reply2 or "")
    record("2.2 暂停后不再自动触发报价", no_trigger, f"state={state2}, reply={reply2}")


# ---------------------------------------------------------------------------
# 用例 3：用户隔离测试
# ---------------------------------------------------------------------------
def test_user_isolation(client):
    print("\n" + "=" * 70)
    print("用例 3：用户隔离测试")
    print("=" * 70)
    h1 = make_headers("user-1")

    # user-1 建会话
    res = client.post("/ai/chat", headers=h1, json={"plate": "粤B34567", "message": "推荐方案"})
    data = res.json()
    cid = (data.get("conversation") or {}).get("id")
    print_json("user-1 建立会话", data)
    record("3.1 user-1 建立会话成功", res.status_code == 200 and bool(cid), f"cid={cid}")

    h2 = make_headers("user-2")

    # user-2 读取 user-1 的会话
    res_get = client.get(f"/ai/conversations/{cid}", headers=h2)
    print_json("user-2 读取会话", {"status": res_get.status_code, "body": res_get.text})
    record("3.2 user-2 读取会话被拒绝(403/404)", res_get.status_code in (403, 404),
           f"status={res_get.status_code}")

    # user-2 尝试确认 user-1 的会话
    res_confirm = client.post(
        f"/ai/conversations/{cid}/confirm", headers=h2, json={"token": "dummy-token"}
    )
    print_json("user-2 尝试确认", {"status": res_confirm.status_code, "body": res_confirm.text})
    record("3.3 user-2 确认被拒绝(403/404)", res_confirm.status_code in (403, 404),
           f"status={res_confirm.status_code}")


# ---------------------------------------------------------------------------
# 用例 4：批量权限测试
# ---------------------------------------------------------------------------
def test_batch_permission(client):
    print("\n" + "=" * 70)
    print("用例 4：批量权限测试（/ai/batch）")
    print("=" * 70)
    payload = {"items": [{"plate": "粤B22222", "message": "推荐"}]}

    # user 角色 -> 403
    res_user = client.post("/ai/batch", headers=make_headers("user-1", "user"), json=payload)
    print_json("user 角色调用批量", {"status": res_user.status_code, "body": res_user.text})
    record("4.1 user 角色调用批量返回 403", res_user.status_code == 403,
           f"status={res_user.status_code}")

    # salesperson 角色 -> 200
    res_sp = client.post("/ai/batch", headers=make_headers("user-1", "salesperson"), json=payload)
    try:
        body = res_sp.json()
    except Exception:
        body = {"status": res_sp.status_code, "body": res_sp.text}
    print_json("salesperson 角色调用批量", body)
    ok_sp = res_sp.status_code == 200 and isinstance(body.get("results"), list) and len(body["results"]) > 0
    record("4.2 salesperson 角色调用批量成功", ok_sp, f"status={res_sp.status_code}")


# ---------------------------------------------------------------------------
# 用例 5：续保方案复用测试
# ---------------------------------------------------------------------------
def test_renewal(client):
    print("\n" + "=" * 70)
    print("用例 5：续保方案复用测试（粤B12345）")
    print("=" * 70)
    h = make_headers()

    res = client.post("/ai/chat", headers=h, json={"plate": "粤B12345", "message": "查询去年方案"})
    data = res.json()
    print_json("查询去年方案返回", data)
    conv = data.get("conversation") or {}
    pending = get_pending(data)
    ok = res.status_code == 200 and pending is not None and pending.get("kind") == "plan"
    record("5.1 返回续保方案（plan 待确认）", ok,
           f"state={conv.get('state')}, pending.kind={pending.get('kind') if pending else None}")
    if pending:
        print_json("续保方案内容（复用去年方案）", pending.get("value"))
        print("   [提示] demo 复用去年方案，但仍需重新上传并确认材料（README 第 9 步）")


def main():
    with httpx.Client(base_url=BASE_URL, timeout=20) as client:
        # 启动前健康检查
        try:
            health = client.get("/health")
            print_json("服务健康检查", health.json() if health.status_code == 200 else health.text)
        except httpx.HTTPError as exc:
            print(f"[FAIL] 无法连接服务 {BASE_URL}：{exc}")
            sys.exit(1)

        test_supplement(client)
        test_manual_pause(client)
        test_user_isolation(client)
        test_batch_permission(client)
        test_renewal(client)

    passed = sum(1 for _, ok in RESULTS if ok)
    total = len(RESULTS)
    print("\n" + "=" * 70)
    print(f"总结：{passed}/{total} 项通过")
    for name, ok in RESULTS:
        print(f"   [{'PASS' if ok else 'FAIL'}] {name}")
    print("=" * 70)
    if passed != total:
        print("存在失败用例，请检查上面的 FAIL 项与响应内容。")
        sys.exit(1)
    print("全部用例通过。")


if __name__ == "__main__":
    main()
