import type { Booking, Slot } from '@/api/types'
import { dayKeyOf } from './datetime'

/**
 * Строка в колонке «Статус слотов».
 *
 * Контракт отдаёт по `/api/event-types/{id}/slots` только СВОБОДНЫЕ слоты, поэтому
 * занятые интервалы мы берём из `/api/bookings`. Пересечений между списками быть не
 * может: слот, пересекающийся с любым бронированием, в выдачу слотов не попадает.
 */
export type TimelineEntry =
  | { status: 'free'; start: string; end: string }
  | { status: 'busy'; start: string; end: string }

/** Количество свободных слотов на каждый локальный день. */
export function freeSlotCountByDay(slots: Slot[]): Map<string, number> {
  const counts = new Map<string, number>()
  for (const slot of slots) {
    const key = dayKeyOf(slot.start)
    counts.set(key, (counts.get(key) ?? 0) + 1)
  }
  return counts
}

/** Первый день (по возрастанию `start`), в котором есть свободные слоты. */
export function firstDayWithSlots(slots: Slot[]): Date | undefined {
  let earliest: Date | undefined
  for (const slot of slots) {
    const start = new Date(slot.start)
    if (!earliest || start < earliest) earliest = start
  }
  return earliest
}

/**
 * Свободные и занятые интервалы выбранного дня одним списком,
 * отсортированным по возрастанию `start`.
 */
export function dayTimeline(day: string, slots: Slot[], bookings: Booking[]): TimelineEntry[] {
  const free: TimelineEntry[] = slots
    .filter((slot) => dayKeyOf(slot.start) === day)
    .map((slot) => ({ status: 'free', start: slot.start, end: slot.end }))

  const busy: TimelineEntry[] = bookings
    .filter((booking) => dayKeyOf(booking.start) === day)
    .map((booking) => ({ status: 'busy', start: booking.start, end: booking.end }))

  return [...free, ...busy].sort((a, b) => Date.parse(a.start) - Date.parse(b.start))
}
