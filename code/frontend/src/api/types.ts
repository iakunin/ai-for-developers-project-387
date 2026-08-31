import type { components } from './schema'

/**
 * Домменные типы фронтенда — это ровно схемы из контракта.
 * Своих определений мы не заводим, чтобы `api-contract/openapi/openapi.yaml`
 * оставался единственным источником правды.
 */
export type Owner = components['schemas']['Owner']
export type EventType = components['schemas']['EventType']
export type Slot = components['schemas']['Slot']
export type Booking = components['schemas']['Booking']
export type BookingCreate = components['schemas']['BookingCreate']
export type ApiErrorBody = components['schemas']['ApiError']
export type ErrorCode = components['schemas']['ErrorCode']
