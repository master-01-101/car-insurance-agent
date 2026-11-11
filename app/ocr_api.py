"""供可信 Java 服务调用的证件 OCR 接口；本模块不保存业务数据。"""

import io
import os
import secrets
from datetime import datetime, timezone
from typing import Literal
from urllib.parse import urlsplit

import httpx
from fastapi import APIRouter, Depends, Header, HTTPException
from PIL import Image, ImageOps, UnidentifiedImageError
from pydantic import BaseModel, Field

from .providers import recognize


router = APIRouter(prefix="/internal/ai/ocr", tags=["内部 OCR"])

MAX_BYTES = 10 * 1024 * 1024
MAX_PIXELS = 25_000_000

IMAGE_TYPES = {
    "image/jpeg": "JPEG",
    "image/png": "PNG",
    "image/webp": "WEBP",
}


class VehicleOcrRequest(BaseModel):
    requestId: str = Field(min_length=1)
    conversationId: str = Field(min_length=1)
    materialId: str = Field(min_length=1)
    side: Literal[
        "front", "back", "main", "sub",
        "license_front", "license_back",
    ]
    readUrl: str = Field(min_length=1)
    expiresAt: datetime
    mimeType: Literal["image/jpeg", "image/png", "image/webp"]
    sizeBytes: int = Field(gt=0, le=MAX_BYTES)
    expectedPlateNumber: str | None = None
    otherPagePlateNumber: str | None = None


class OcrField(BaseModel):
    key: str
    value: str | int | float | bool | None
    confidence: float | None = None
    abnormal: bool = False


class OcrWarning(BaseModel):
    code: str
    message: str


class VehicleOcrResponse(BaseModel):
    requestId: str
    conversationId: str
    materialId: str
    side: Literal["front", "back"]
    provider: str
    status: Literal["succeeded", "needs_review", "failed"]
    fields: list[OcrField]
    missingFields: list[str]
    warnings: list[OcrWarning]
    recognizedAt: datetime
    handoffRequired: bool
    errorCode: str | None = None
    detectedPlateNumber: str | None = None
    message: str | None = None


class IdCardOcrRequest(BaseModel):
    requestId: str = Field(min_length=1)
    conversationId: str = Field(min_length=1)
    materialId: str = Field(min_length=1)
    side: Literal["front", "back", "id_front", "id_back"]
    readUrl: str = Field(min_length=1)
    expiresAt: datetime
    mimeType: Literal["image/jpeg", "image/png", "image/webp"]
    sizeBytes: int = Field(gt=0, le=MAX_BYTES)


class IdCardOcrResponse(BaseModel):
    requestId: str
    conversationId: str
    materialId: str
    side: Literal["front", "back"]
    provider: str
    status: Literal["succeeded", "needs_review", "failed"]
    fields: list[OcrField]
    missingFields: list[str]
    warnings: list[OcrWarning]
    recognizedAt: datetime
    handoffRequired: bool
    errorCode: str | None = None
    message: str | None = None


async def service_auth(authorization: str = Header()):
    token = os.getenv("SERVICE_TOKEN", "")

    if not token or not secrets.compare_digest(
        authorization,
        "Bearer " + token,
    ):
        raise HTTPException(401, "服务认证失败")


def validate_read_url(url: str, expires_at: datetime):
    if (
        expires_at.tzinfo is None
        or expires_at <= datetime.now(timezone.utc)
    ):
        raise HTTPException(
            422,
            "图片读取地址已过期或缺少时区",
        )

    try:
        parts = urlsplit(url)
        hostname = parts.hostname
    except ValueError as exc:
        raise HTTPException(
            422,
            "图片读取地址格式不正确",
        ) from exc

    allowed_hosts = {
        host.strip().lower()
        for host in os.getenv("OCR_READ_HOSTS", "").split(",")
        if host.strip()
    }

    if not allowed_hosts:
        raise HTTPException(503, "未配置 OCR_READ_HOSTS")

    # 本地 demo 可以显式允许 HTTP；真实环境使用 HTTPS。
    local_http = (
        os.getenv("MODE", "demo") == "demo"
        and os.getenv("OCR_ALLOW_HTTP") == "1"
        and parts.scheme == "http"
    )

    if (
        (parts.scheme != "https" and not local_http)
        or not hostname
        or parts.username
        or parts.password
    ):
        raise HTTPException(
            422,
            "图片读取地址必须是可信 HTTPS 地址",
        )

    if hostname.lower() not in allowed_hosts:
        raise HTTPException(
            422,
            "图片读取地址不属于允许的文件服务",
        )


async def fetch_image(url: str, expected_size: int) -> bytes:
    try:
        async with httpx.AsyncClient(
            timeout=20,
            follow_redirects=False,
            trust_env=False,
        ) as client:
            async with client.stream("GET", url) as response:
                if response.status_code != 200:
                    raise HTTPException(502, "图片服务返回失败")

                chunks = []
                total = 0

                async for chunk in response.aiter_bytes():
                    total += len(chunk)

                    if total > MAX_BYTES:
                        raise HTTPException(413, "图片超过 10MB")

                    chunks.append(chunk)

    except httpx.HTTPError as exc:
        raise HTTPException(
            502,
            "图片服务连接失败或超时",
        ) from exc

    if total != expected_size:
        raise HTTPException(
            422,
            "图片大小与 Java 提供的文件元数据不一致",
        )

    return b"".join(chunks)


def prepare_image(
    data: bytes,
    mime_type: str,
) -> tuple[bytes, str]:
    try:
        with Image.open(io.BytesIO(data)) as image:
            if image.format != IMAGE_TYPES[mime_type]:
                raise HTTPException(
                    422,
                    "图片内容与 mimeType 不一致",
                )

            if image.width * image.height > MAX_PIXELS:
                raise HTTPException(413, "图片像素过大")

            image.load()

            if mime_type != "image/webp":
                return data, mime_type

            output = io.BytesIO()

            ImageOps.exif_transpose(image).convert("RGB").save(
                output,
                format="PNG",
            )

            converted = output.getvalue()

            if len(converted) > MAX_BYTES:
                raise HTTPException(
                    413,
                    "WebP 转换后的图片超过 10MB",
                )

            return converted, "image/png"

    except (UnidentifiedImageError, OSError) as exc:
        raise HTTPException(
            422,
            "无法读取图片内容",
        ) from exc

    except Image.DecompressionBombError as exc:
        raise HTTPException(
            413,
            "图片像素过大",
        ) from exc


def normalized_plate(value: object) -> str | None:
    if value is None:
        return None

    plate = str(value).strip().replace(" ", "").upper()
    return plate or None


@router.post(
    "/vehicle-license",
    response_model=VehicleOcrResponse,
)
async def vehicle_license_ocr(
    req: VehicleOcrRequest,
    _=Depends(service_auth),
):
    side = (
        "front"
        if req.side in ("front", "main", "license_front")
        else "back"
    )

    validate_read_url(req.readUrl, req.expiresAt)

    raw_image = await fetch_image(
        req.readUrl,
        req.sizeBytes,
    )

    image, mime_type = prepare_image(
        raw_image,
        req.mimeType,
    )

    kind = (
        "license_front"
        if side == "front"
        else "license_back"
    )

    result = await recognize(
        kind,
        image,
        mime_type,
    )

    fields = result.get("fields")

    if (
        not isinstance(fields, dict)
        or not isinstance(result.get("needs_review"), bool)
    ):
        raise HTTPException(
            502,
            "OCR 返回结构不正确",
        )

    missing = result.get("missing_fields") or []

    if not isinstance(missing, list):
        missing = []

    recognized_plate = normalized_plate(
        fields.get("plate_number")
    )
    expected = normalized_plate(
        req.expectedPlateNumber
    )
    other = normalized_plate(
        req.otherPagePlateNumber
    )

    if (
        not recognized_plate
        and "plate_number" not in missing
    ):
        missing.append("plate_number")

    needs_review = (
        result["needs_review"]
        or bool(missing)
    )

    mismatch = (
        not needs_review
        and recognized_plate
        and (
            (expected and expected != recognized_plate)
            or (other and other != recognized_plate)
        )
    )

    if mismatch:
        status = "needs_review"
        error_code = "VEHICLE_PLATE_MISMATCH"
        handoff = False
        message = (
            "识别出另一车牌，请由 Java 查询或创建"
            "该车辆会话并提示补齐对应材料。"
        )

    elif needs_review:
        status = "needs_review"
        error_code = "OCR_REVIEW_REQUIRED"
        handoff = True
        message = (
            "字段缺失或无法可靠识别，"
            "请核对并转人工处理。"
        )

    else:
        status = "succeeded"
        error_code = None
        handoff = False
        message = None

    warnings = []

    if missing:
        warnings.append(
            OcrWarning(
                code="MISSING_FIELDS",
                message="OCR 存在缺失字段",
            )
        )

    if mismatch:
        warnings.append(
            OcrWarning(
                code="VEHICLE_PLATE_MISMATCH",
                message="车牌与当前车辆不一致",
            )
        )

    return VehicleOcrResponse(
        requestId=req.requestId,
        conversationId=req.conversationId,
        materialId=req.materialId,
        side=side,
        provider=str(
            result.get("provider")
            or os.getenv("OCR_MODE")
            or "unknown"
        ),
        status=status,
        fields=[
            OcrField(key=key, value=value)
            for key, value in fields.items()
        ],
        missingFields=[
            str(item)
            for item in missing
        ],
        warnings=warnings,
        recognizedAt=datetime.now(timezone.utc),
        handoffRequired=handoff,
        errorCode=error_code,
        detectedPlateNumber=(
            recognized_plate if mismatch else None
        ),
        message=message,
    )


@router.post(
    "/id-card",
    response_model=IdCardOcrResponse,
)
async def id_card_ocr(
    req: IdCardOcrRequest,
    _=Depends(service_auth),
):
    side = (
        "front"
        if req.side in ("front", "id_front")
        else "back"
    )
    kind = "id_front" if side == "front" else "id_back"

    validate_read_url(req.readUrl, req.expiresAt)

    raw_image = await fetch_image(
        req.readUrl,
        req.sizeBytes,
    )
    image, mime_type = prepare_image(
        raw_image,
        req.mimeType,
    )

    result = await recognize(
        kind,
        image,
        mime_type,
    )

    fields = result.get("fields")

    if (
        not isinstance(fields, dict)
        or not isinstance(result.get("needs_review"), bool)
    ):
        raise HTTPException(
            502,
            "OCR 返回结构不正确",
        )

    required = (
        ("name", "id_number")
        if side == "front"
        else ("issuing_authority", "valid_to")
    )

    provider_missing = result.get("missing_fields") or []

    if not isinstance(provider_missing, list):
        raise HTTPException(
            502,
            "OCR 缺失字段结构不正确",
        )

    missing = list(
        dict.fromkeys(
            [str(item) for item in provider_missing]
            + [key for key in required if not fields.get(key)]
        )
    )

    warnings = []

    if missing:
        warnings.append(
            OcrWarning(
                code="MISSING_FIELDS",
                message="OCR 存在缺失字段",
            )
        )

    provider_warnings = result.get("warnings") or []

    if not isinstance(provider_warnings, list):
        raise HTTPException(
            502,
            "OCR 警告结构不正确",
        )

    if any(
        str(item).startswith("image_status:")
        for item in provider_warnings
    ):
        warnings.append(
            OcrWarning(
                code="IMAGE_STATUS",
                message="图片状态异常或无法判断",
            )
        )

    if "idcard_number_check_failed" in provider_warnings:
        warnings.append(
            OcrWarning(
                code="ID_NUMBER_CHECK_FAILED",
                message="证件号码校验异常",
            )
        )

    needs_review = (
        result["needs_review"]
        or bool(missing or warnings)
    )

    return IdCardOcrResponse(
        requestId=req.requestId,
        conversationId=req.conversationId,
        materialId=req.materialId,
        side=side,
        provider=str(
            result.get("provider")
            or os.getenv("OCR_MODE")
            or "unknown"
        ),
        status=(
            "needs_review"
            if needs_review
            else "succeeded"
        ),
        fields=[
            OcrField(key=key, value=value)
            for key, value in fields.items()
        ],
        missingFields=missing,
        warnings=warnings,
        recognizedAt=datetime.now(timezone.utc),
        handoffRequired=needs_review,
        errorCode=(
            "OCR_REVIEW_REQUIRED"
            if needs_review
            else None
        ),
        message=(
            "身份证信息需要核对，请转人工处理。"
            if needs_review
            else None
        ),
    )