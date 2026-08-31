import type { ApiErrorBody, ErrorCode } from './types'

/**
 * Ошибка обращения к API. Решения на стороне UI принимаются по `code`,
 * а не по тексту `message` — так требует контракт.
 */
export class ApiError extends Error {
  readonly status: number
  readonly code: ErrorCode | 'network_error' | 'unknown_error'

  constructor(status: number, code: ApiError['code'], message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
  }
}

const DEFAULT_BASE_URL = 'http://localhost:4010'

export function apiBaseUrl(): string {
  const configured = import.meta.env.VITE_API_BASE_URL?.trim()
  return (configured || DEFAULT_BASE_URL).replace(/\/+$/, '')
}

function isApiErrorBody(value: unknown): value is ApiErrorBody {
  if (typeof value !== 'object' || value === null) return false
  const body = value as Record<string, unknown>
  return typeof body.code === 'string' && typeof body.message === 'string'
}

async function toApiError(response: Response): Promise<ApiError> {
  let body: unknown
  try {
    body = await response.json()
  } catch {
    body = undefined
  }

  if (isApiErrorBody(body)) {
    return new ApiError(response.status, body.code, body.message)
  }
  return new ApiError(response.status, 'unknown_error', `Запрос завершился с кодом ${response.status}.`)
}

async function executeFetch(path: string, init?: RequestInit): Promise<Response> {
  try {
    return await fetch(`${apiBaseUrl()}${path}`, {
      ...init,
      headers: {
        Accept: 'application/json',
        ...(init?.body ? { 'Content-Type': 'application/json' } : {}),
        ...init?.headers,
      },
    })
  } catch {
    throw new ApiError(0, 'network_error', 'Не удалось связаться с сервером.')
  }
}

export async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await executeFetch(path, init)

  if (!response.ok) {
    throw await toApiError(response)
  }

  return (await response.json()) as T
}

export async function requestVoid(path: string, init?: RequestInit): Promise<void> {
  const response = await executeFetch(path, init)

  if (!response.ok) {
    throw await toApiError(response)
  }
}
