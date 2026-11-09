import { getPlaceholder, type PlaceholderId } from './placeholders'

/**
 * 调用尚未导入的后端 / Agent 能力时抛出的错误。
 * 页面捕获后可据此展示对应占位符，而不是弹「网络错误」。
 */
export class NotImportedError extends Error {
  readonly placeholderId: PlaceholderId

  constructor(placeholderId: PlaceholderId) {
    const spec = getPlaceholder(placeholderId)
    super(`此地方未导入：${spec.title}`)
    this.name = 'NotImportedError'
    this.placeholderId = placeholderId
  }
}

/**
 * 显式返回「未导入」的拒绝结果。
 * 用于 api/ 下尚未联调的接口封装：既不发送请求，也不伪造数据。
 */
export function notImported(placeholderId: PlaceholderId): Promise<never> {
  return Promise.reject(new NotImportedError(placeholderId))
}
