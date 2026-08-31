import { describe, expect, it } from 'vitest'

import type { Booking, Slot } from '@/api/types'
import { dayKeyOf } from './datetime'
import { dayTimeline, firstDayWithSlots, freeSlotCountByDay } from './slots'

function slot(start: string, end: string): Slot {
  return { start, end }
}

function booking(start: string, end: string, id = start): Booking {
  return {
    id,
    eventTypeId: 'intro',
    start,
    end,
    guestName: 'Гость',
    guestEmail: 'guest@example.com',
    createdAt: '2026-03-01T00:00:00Z',
  }
}

describe('freeSlotCountByDay', () => {
  it('считает свободные слоты по локальным дням', () => {
    const slots = [
      slot('2026-03-31T09:00:00Z', '2026-03-31T09:15:00Z'),
      slot('2026-03-31T09:15:00Z', '2026-03-31T09:30:00Z'),
      slot('2026-04-01T09:00:00Z', '2026-04-01T09:15:00Z'),
    ]

    const counts = freeSlotCountByDay(slots)

    expect(counts.get(dayKeyOf('2026-03-31T09:00:00Z'))).toBe(2)
    expect(counts.get(dayKeyOf('2026-04-01T09:00:00Z'))).toBe(1)
  })

  it('для пустого списка не возвращает дней', () => {
    expect(freeSlotCountByDay([]).size).toBe(0)
  })
})

describe('firstDayWithSlots', () => {
  it('берёт самый ранний слот независимо от порядка в списке', () => {
    const slots = [
      slot('2026-04-02T12:00:00Z', '2026-04-02T12:15:00Z'),
      slot('2026-03-31T09:00:00Z', '2026-03-31T09:15:00Z'),
    ]

    expect(firstDayWithSlots(slots)?.toISOString()).toBe('2026-03-31T09:00:00.000Z')
  })

  it('возвращает undefined, когда свободных слотов нет', () => {
    expect(firstDayWithSlots([])).toBeUndefined()
  })
})

describe('dayTimeline', () => {
  it('склеивает свободные слоты и бронирования одного дня по возрастанию времени', () => {
    const day = dayKeyOf('2026-03-31T09:00:00Z')
    const slots = [
      slot('2026-03-31T09:45:00Z', '2026-03-31T10:00:00Z'),
      slot('2026-03-31T10:00:00Z', '2026-03-31T10:15:00Z'),
    ]
    const bookings = [booking('2026-03-31T09:00:00Z', '2026-03-31T09:15:00Z')]

    const timeline = dayTimeline(day, slots, bookings)

    expect(timeline).toEqual([
      { status: 'busy', start: '2026-03-31T09:00:00Z', end: '2026-03-31T09:15:00Z' },
      { status: 'free', start: '2026-03-31T09:45:00Z', end: '2026-03-31T10:00:00Z' },
      { status: 'free', start: '2026-03-31T10:00:00Z', end: '2026-03-31T10:15:00Z' },
    ])
  })

  it('показывает бронирования любых типов событий — занятость календаря общая', () => {
    const day = dayKeyOf('2026-03-31T09:00:00Z')
    const bookings = [
      booking('2026-03-31T11:00:00Z', '2026-03-31T11:30:00Z'),
      { ...booking('2026-03-31T08:00:00Z', '2026-03-31T08:15:00Z'), eventTypeId: 'other' },
    ]

    const timeline = dayTimeline(day, [], bookings)

    expect(timeline.map((entry) => entry.start)).toEqual([
      '2026-03-31T08:00:00Z',
      '2026-03-31T11:00:00Z',
    ])
  })

  it('отбрасывает записи других дней', () => {
    const day = dayKeyOf('2026-03-31T09:00:00Z')
    const slots = [slot('2026-04-01T09:00:00Z', '2026-04-01T09:15:00Z')]
    const bookings = [booking('2026-04-01T10:00:00Z', '2026-04-01T10:15:00Z')]

    expect(dayTimeline(day, slots, bookings)).toEqual([])
  })
})
