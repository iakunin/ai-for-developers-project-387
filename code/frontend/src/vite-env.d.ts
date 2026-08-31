/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** Базовый URL бэкенда, реализующего контракт. По умолчанию — Prism на localhost:4010. */
  readonly VITE_API_BASE_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
