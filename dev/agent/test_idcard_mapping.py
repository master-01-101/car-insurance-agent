"""测试身份证 OCR 字段转换；不调用百度接口。"""

from app.ocr_idcard import map_idcard_result


def cell(value):
    return {"words": value}


front = map_idcard_result(
    "id_front",
    {
        "image_status": "normal",
        "idcard_number_type": 1,
        "words_result": {
            "姓名": cell("测试甲"),
            "公民身份号码": cell("110101200001010019"),
            "住址": cell("测试地址"),
        },
    },
)

assert front["fields"]["name"] == "测试甲"
assert front["fields"]["id_number"] == "110101200001010019"
assert front["needs_review"] is False
print("PASS: 身份证正面字段转换")


back = map_idcard_result(
    "id_back",
    {
        "image_status": "normal",
        "words_result": {
            "签发机关": cell("测试机关"),
            "签发日期": cell("20200101"),
            "失效日期": cell("20300101"),
        },
    },
)

assert back["fields"]["issuing_authority"] == "测试机关"
assert back["fields"]["valid_to"] == "20300101"
assert back["needs_review"] is False
print("PASS: 身份证反面字段转换")


reversed_side = map_idcard_result(
    "id_front",
    {
        "image_status": "reversed_side",
        "idcard_number_type": 1,
        "words_result": {
            "姓名": cell("测试甲"),
            "公民身份号码": cell("110101200001010019"),
        },
    },
)

assert reversed_side["needs_review"] is True
print("PASS: 正反面颠倒标记核对")


missing = map_idcard_result(
    "id_front",
    {
        "image_status": "normal",
        "idcard_number_type": 0,
        "words_result": {
            "姓名": cell("测试甲"),
        },
    },
)

assert missing["needs_review"] is True
assert "id_number" in missing["missing_fields"]
print("PASS: 证件号码缺失或校验异常标记核对")