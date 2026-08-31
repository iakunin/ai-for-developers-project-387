import { expect, test } from '@playwright/test'

import { bookAs, openEventType, pickFirstFreeSlot, startTimeOf } from '../support/booking'

test('Сценарий 4: бронирование видно в админке', async ({ page }) => {
  // Email генерируется на каждую попытку (а не на загрузку модуля): контейнер переиспользуется
  // между прогонами, и CI повторяет упавший тест (`retries: 1`) — фиксированный email совпал бы
  // со строками, оставленными предыдущими прогонами или предыдущей попыткой этого же теста.
  const GUEST_EMAIL = `e2e-admin-${Date.now()}@example.com`

  await openEventType(page, 'intro-call')
  const slotLabel = await pickFirstFreeSlot(page)

  await bookAs(page, 'Гость Админский', GUEST_EMAIL)

  await page.goto('/admin')
  await expect(page.getByRole('heading', { name: 'Админка' })).toBeVisible()

  // Email уникален для этого прогона, поэтому строка находится однозначно и проверка
  // не зависит от того, сколько бронирований оставили предыдущие прогоны.
  const row = page.getByRole('listitem').filter({ hasText: GUEST_EMAIL })
  await expect(row).toHaveCount(1)
  await expect(row).toContainText('Гость Админский')
  await expect(row).toContainText('intro-call')
  await expect(row).toContainText(startTimeOf(slotLabel))
})
