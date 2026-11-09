import axios, { AxiosError, type AxiosRequestConfig } from 'axios'
import type { ApiResponse } from '@/types'

/**
 * 统一请求层。
 * 约定的响应结构见《前端.md》第五节：
 *   { "code": 200, "message": "success", "data": {} }
 *
 * 注意：后端尚未导入，本文件只提供骨架；页面当前不会发起任何真实请求。
 */

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api'

/** token 存储键，登录接口导入后由成员 B 约定字段 */
export const TOKEN_STORAGE_KEY = 'chexian.token'

export const http = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15_000,
  headers: { 'Content-Type': 'application/json' },
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_STORAGE_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiResponse<unknown>>) => {
    const message =
      error.response?.data?.message ?? error.message ?? '请求失败，请稍后重试'
    return Promise.reject(new Error(message))
  },
)

/**
 * 解包统一响应结构；code !== 200 视为业务失败。
 * 页面只按服务端返回的业务状态渲染，不自行推断成功与否。
 */
export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await http.request<ApiResponse<T>>(config)
  const body = response.data
  if (body?.code !== 200) {
    throw new Error(body?.message || '业务处理失败')
  }
  return body.data
}
