import { expect, test } from '@playwright/test'

import {
  bookAs,
  busyRow,
  firstFreeSlotLabel,
  freeSlot,
  openEventType,
  selectDay,
  selectedDayNumber,
  startTimeOf,
} from '../support/booking'

test('Сценарий 3: занятость общая для всех типов событий', async ({ page }) => {
  // Сетка «Консультации» (30 мин) — подмножество сетки «Знакомства» (15 мин): обе строятся
  // шагом длительности от начала рабочего дня. Поэтому слот, свободный в «Консультации»,
  // гарантированно свободен и в «Знакомстве» в то же время, каким бы поздним ни был запуск.
  await openEventType(page, 'consultation')
  const day = await selectedDayNumber(page)
  const startTime = startTimeOf(await firstFreeSlotLabel(page))

  await openEventType(page, 'intro-call')
  await selectDay(page, day)
  const introSlot = freeSlot(page, startTime)
  await expect(introSlot).toHaveCount(1)
  await introSlot.click()
  await bookAs(page, 'Гость Третий', 'e2e-shared-busy@example.com')

  await openEventType(page, 'consultation')
  await selectDay(page, day)

  await expect(busyRow(page, startTime)).toBeVisible()
  await expect(freeSlot(page, startTime)).toHaveCount(0)
})
