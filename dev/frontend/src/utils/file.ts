/**
 * 文件校验工具（前端侧基础校验）
 * 真实的上传约束（尺寸、格式白名单、单用户配额）需与成员 B 的上传接口对齐，
 * 前端校验只做提前拦截，不能替代服务端校验。
 */

export const IMAGE_MAX_SIZE_MB = 10
export const IMAGE_MAX_SIZE_BYTES = IMAGE_MAX_SIZE_MB * 1024 * 1024
export const ACCEPTED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp'] as const

export interface FileValidation {
  ok: boolean
  reason?: string
}

export function validateImageFile(file: File): FileValidation {
  if (!ACCEPTED_IMAGE_TYPES.includes(file.type as (typeof ACCEPTED_IMAGE_TYPES)[number])) {
    return { ok: false, reason: '仅支持 JPG / PNG / WebP 图片' }
  }
  if (file.size > IMAGE_MAX_SIZE_BYTES) {
    return { ok: false, reason: `单张图片不能超过 ${IMAGE_MAX_SIZE_MB}MB` }
  }
  if (file.size === 0) {
    return { ok: false, reason: '文件为空，请重新选择' }
  }
  return { ok: true }
}

export function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}
