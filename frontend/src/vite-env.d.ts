/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** 后端 API 基础地址，未配置时默认 /api（由 Vite 代理或同源网关转发） */
  readonly VITE_API_BASE_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
