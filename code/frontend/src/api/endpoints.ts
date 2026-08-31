import { request } from './client'
import type { Booking, BookingCreate, EventType, Owner, Slot } from './types'

/** Профиль владельца календаря. */
export function getOwner(): Promise<Owner> {
  return request<Owner>('/api/owner')
}

/** Список типов событий. */
export function listEventTypes(): Promise<EventType[]> {
  return request<EventType[]>('/api/event-types')
}

/** Тип события по идентификатору. */
export function getEventType(id: string): Promise<EventType> {
  return request<EventType>(`/api/event-types/${encodeURIComponent(id)}`)
}

/** Свободные слоты типа события в окне записи (14 дней). */
export function listSlots(id: string): Promise<Slot[]> {
  return request<Slot[]>(`/api/event-types/${encodeURIComponent(id)}/slots`)
}

/** Предстоящие встречи по всем типам событий. */
export function listBookings(): Promise<Booking[]> {
  return request<Booking[]>('/api/bookings')
}

/** Создает бронирование гостя на выбранный свободный слот. */
export function createBooking(payload: BookingCreate): Promise<Booking> {
  return request<Booking>('/api/bookings', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

/** Создает тип события. Идентификатор задает владелец. */
export function createEventType(payload: EventType): Promise<EventType> {
  return request<EventType>('/api/event-types', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

/** Отменяет (удаляет) бронирование по идентификатору. */
export function cancelBooking(id: string): Promise<void> {
  return request<void>(`/api/bookings/${encodeURIComponent(id)}`, {
    method: 'DELETE',
  })
}
