from app.ocr_normalize import normalize_license

normal = {
    "words_result": {
        "号牌号码": {"words": "粤B67890"},
        "车辆识别代号": {"words": "LSV12345678901234"},
        "所有人": {"words": "测试用户"},
        "车辆类型": {"words": "小型汽车"},
        "使用性质": {"words": "非营运"},
    }
}

result = normalize_license(normal, "front")
assert result["fields"]["plate_number"] == "粤B67890"
assert result["fields"]["vin"] == "LSV12345678901234"
assert result["needs_review"] is False

missing_vin = {
    "words_result": {
        "号牌号码": {"words": "粤B67890"},
        "所有人": {"words": "测试用户"},
        "使用性质": {"words": "非营运"},
    }
}

result = normalize_license(missing_vin, "front")
assert result["needs_review"] is True
assert "vin" in result["missing_fields"]

print("PASS: 行驶证字段转换与缺失字段检测")