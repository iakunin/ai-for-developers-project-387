import { expect, test } from '@playwright/test'

test('приложение отдаётся из собранного образа', async ({ page }) => {
  await page.goto('/')

  await expect(page.getByRole('heading', { level: 1, name: 'Calendar' })).toBeVisible()
  // Ссылка «Записаться» есть и в шапке, и в основном контенте — уточняем до одной,
  // иначе локатор упадёт со strict mode violation.
  await expect(page.getByRole('main').getByRole('link', { name: 'Записаться' })).toBeVisible()
})
