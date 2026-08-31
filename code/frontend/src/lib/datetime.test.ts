import { describe, expect, it } from 'vitest'

import { addMonths, dayKey, isSameMonth, monthGrid, startOfMonth } from './datetime'

describe('dayKey', () => {
  it('дополняет месяц и день до двух знаков', () => {
    expect(dayKey(new Date(2026, 2, 5))).toBe('2026-03-05')
  })
})

describe('monthGrid', () => {
  it('строит 6 недель, начиная с понедельника', () => {
    const grid = monthGrid(new Date(2026, 2, 1))

    expect(grid).toHaveLength(42)
    expect(grid[0].getDay()).toBe(1)
    // Март 2026 начинается в воскресенье, поэтому первая неделя — из февраля.
    expect(dayKey(grid[0])).toBe('2026-02-23')
    expect(dayKey(grid[41])).toBe('2026-04-05')
  })

  it('включает все дни выбранного месяца', () => {
    const grid = monthGrid(new Date(2026, 2, 1))
    const marchDays = grid.filter((day) => day.getMonth() === 2)

    expect(marchDays).toHaveLength(31)
  })
})

describe('навигация по месяцам', () => {
  it('сдвигает месяц через границу года', () => {
    expect(dayKey(addMonths(new Date(2026, 11, 15), 1))).toBe('2027-01-01')
    expect(dayKey(addMonths(new Date(2026, 0, 15), -1))).toBe('2025-12-01')
  })

  it('startOfMonth сбрасывает число', () => {
    expect(dayKey(startOfMonth(new Date(2026, 2, 31)))).toBe('2026-03-01')
  })

  it('isSameMonth сравнивает месяц вместе с годом', () => {
    expect(isSameMonth(new Date(2026, 2, 1), new Date(2026, 2, 31))).toBe(true)
    expect(isSameMonth(new Date(2026, 2, 1), new Date(2025, 2, 1))).toBe(false)
  })
})
