const LOCALE = 'ru-RU'

/**
 * Контракт передаёт моменты времени в UTC (`format: date-time`).
 * Пользователю мы показываем их в часовом поясе браузера.
 */

const timeFormat = new Intl.DateTimeFormat(LOCALE, { hour: '2-digit', minute: '2-digit' })
const dayFormat = new Intl.DateTimeFormat(LOCALE, { weekday: 'long', day: 'numeric', month: 'long' })
const monthFormat = new Intl.DateTimeFormat(LOCALE, { month: 'long', year: 'numeric' })
const dateTimeFormat = new Intl.DateTimeFormat(LOCALE, {
  day: 'numeric',
  month: 'long',
  hour: '2-digit',
  minute: '2-digit',
})

/** Ключ локального дня в формате YYYY-MM-DD. Служит идентификатором дня в календаре. */
export function dayKey(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

/** Ключ локального дня для ISO-момента из контракта. */
export function dayKeyOf(isoInstant: string): string {
  return dayKey(new Date(isoInstant))
}

export function formatTime(isoInstant: string): string {
  return timeFormat.format(new Date(isoInstant))
}

/** «09:00 - 09:15» */
export function formatTimeRange(startIso: string, endIso: string): string {
  return `${formatTime(startIso)} - ${formatTime(endIso)}`
}

/** «вторник, 31 марта» */
export function formatDay(date: Date): string {
  return dayFormat.format(date)
}

/** «март 2026 г.» */
export function formatMonth(date: Date): string {
  return monthFormat.format(date)
}

/** «31 марта, 09:00» */
export function formatDateTime(isoInstant: string): string {
  return dateTimeFormat.format(new Date(isoInstant))
}

export function formatDuration(minutes: number): string {
  return `${minutes} мин`
}

/** Первое число месяца, к которому принадлежит дата. */
export function startOfMonth(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), 1)
}

export function addMonths(date: Date, months: number): Date {
  return new Date(date.getFullYear(), date.getMonth() + months, 1)
}

export function isSameMonth(a: Date, b: Date): boolean {
  return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth()
}

/**
 * Сетка месяца, начинающаяся с понедельника: 6 недель по 7 дней,
 * с добавочными днями соседних месяцев — как в макете.
 */
export function monthGrid(month: Date): Date[] {
  const first = startOfMonth(month)
  // getDay(): 0 — воскресенье. Переводим в понедельник-первый.
  const leading = (first.getDay() + 6) % 7
  const start = new Date(first.getFullYear(), first.getMonth(), 1 - leading)

  return Array.from({ length: 42 }, (_, index) => {
    return new Date(start.getFullYear(), start.getMonth(), start.getDate() + index)
  })
}

export const WEEKDAY_LABELS = ['Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб', 'Вс'] as const
