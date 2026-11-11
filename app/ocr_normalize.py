def normalize_license(raw: dict, side: str) -> dict:
    """将百度行驶证结果转换成项目统一格式。"""
    if side not in ("front", "back"):
        raise ValueError("side 必须为 front 或 back")

    words_result = raw.get("words_result")
    if not isinstance(words_result, dict):
        return {
            "fields": {},
            "needs_review": True,
            "missing_fields": ["words_result"],
        }

    def read(chinese_name: str) -> str:
        item = words_result.get(chinese_name)
        if not isinstance(item, dict):
            return ""
        return str(item.get("words") or "").strip()

    if side == "front":
        fields = {
            "plate_number": read("号牌号码"),
            "vin": read("车辆识别代号"),
            "owner_name": read("所有人"),
            "vehicle_type": read("车辆类型"),
            "use_nature": read("使用性质"),
            "brand_model": read("品牌型号"),
            "registration_date": read("注册日期"),
        }
        required = ("plate_number", "vin", "owner_name", "use_nature")
    else:
        fields = {
            "plate_number": read("号牌号码"),
            "approved_passengers": read("核定载人数"),
            "inspection_record": read("检验记录"),
        }
        # 部分副页不一定能读出车牌；先保留识别结果供两页核对。
        required = ()

    missing = [name for name in required if not fields[name]]

    # 不把没有识别出来的空字符串当作车辆的真实资料。
    fields = {name: value for name, value in fields.items() if value}

    return {
        "fields": fields,
        "needs_review": bool(missing),
        "missing_fields": missing,
    }