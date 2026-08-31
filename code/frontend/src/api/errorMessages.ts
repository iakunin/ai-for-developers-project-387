import { ApiError } from './client'

/** Человекочитаемые тексты для машиночитаемых кодов ошибок контракта. */
const MESSAGES: Record<ApiError['code'], string> = {
  validation_failed: 'Проверьте заполненные поля и попробуйте ещё раз.',
  event_type_not_found: 'Такой тип события не найден.',
  event_type_id_taken: 'Тип события с таким идентификатором уже существует.',
  slot_not_available: 'Этот слот больше не доступен. Выберите другое время.',
  outside_booking_window: 'Время выходит за окно записи — доступны ближайшие 14 дней.',
  slot_taken: 'Это время только что заняли. Выберите другой слот.',
  network_error: 'Не удалось связаться с сервером. Проверьте, что бэкенд запущен.',
  unknown_error: 'Что-то пошло не так. Попробуйте ещё раз.',
}

export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) return MESSAGES[error.code]
  return MESSAGES.unknown_error
}

/** Коды, после которых список слотов устарел и его нужно перезапросить. */
export function isStaleSlotError(error: unknown): boolean {
  return error instanceof ApiError && (error.code === 'slot_taken' || error.code === 'slot_not_available')
}
