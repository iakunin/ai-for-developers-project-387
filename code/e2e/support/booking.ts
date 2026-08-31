import { expect, type Locator, type Page } from '@playwright/test'

/**
 * Локаторы опираются на роли и подписи — размётка страниц уже доступна, менять исходники
 * фронтенда для тестов не нужно.
 *
 * Доступное имя кнопки свободного слота — «09:00 - 09:15 Свободно»;
 * занятый интервал — не кнопка, а элемент списка с текстом «Занято».
 * Доступное имя дня календаря — «31 5 св.», и «N св.» появляется только у дней со слотами,
 * поэтому регулярка ниже выбирает ровно доступные для выбора дни.
 */

const DAY_WITH_SLOTS = /^\d{1,2} \d+ св\.$/

/** «09:00 - 09:15» -> «09:00». Список слотов и экран подтверждения форматируют время по-разному. */
export function startTimeOf(slotLabel: string): string {
  const match = slotLabel.match(/\d{2}:\d{2}/)
  if (!match) {
    throw new Error(`Не удалось разобрать время слота: "${slotLabel}"`)
  }
  return match[0]
}

/** Кнопка свободного слота, начинающегося в указанное время. */
export function freeSlot(page: Page, startTime: string): Locator {
  return page.getByRole('button', {
    name: new RegExp(`^${startTime} - \\d{2}:\\d{2} Свободно$`),
  })
}

/**
 * Строка «Занято», начинающаяся с указанного времени. Якорим начало текста, а не просто
 * ищем подстроку: соседние занятые интервалы делят границу («13:00 - 13:15» и «13:15 - 13:30»),
 * поэтому подстрочный поиск «13:15» находит обе строки.
 */
export function busyRow(page: Page, timeText: string): Locator {
  return page
    .getByRole('listitem')
    .filter({ hasText: 'Занято' })
    .filter({ hasText: new RegExp(`^${timeText}`) })
}

/**
 * Открывает страницу бронирования типа события и дожидается слотов.
 * День выбирать не нужно: страница сама выбирает ближайший день со свободными слотами.
 */
export async function openEventType(page: Page, eventTypeId: string): Promise<void> {
  await page.goto(`/book/${eventTypeId}`)
  await expect(page.getByRole('button', { name: /Свободно/ }).first()).toBeVisible()
}

/** Подпись первого свободного слота выбранного дня, например «09:00 - 09:15». */
export async function firstFreeSlotLabel(page: Page): Promise<string> {
  const slot = page.getByRole('button', { name: /Свободно/ }).first()
  const text = (await slot.textContent()) ?? ''
  return text.replace('Свободно', '').trim()
}

/** Выбирает первый свободный слот и возвращает его подпись. */
export async function pickFirstFreeSlot(page: Page): Promise<string> {
  const label = await firstFreeSlotLabel(page)
  const slot = freeSlot(page, startTimeOf(label))
  await slot.click()
  await expect(slot).toHaveAttribute('aria-pressed', 'true')
  return label
}

/**
 * Номер дня, выбранного в календаре, например «31». Кнопка дня — это два соседних <span>
 * (номер дня и «N св.»), без разделителя между ними в textContent: день 1 с 31 слотом даёт
 * «131 св.». Поэтому номер дня берём из первого <span>, а не из текста всей кнопки.
 */
export async function selectedDayNumber(page: Page): Promise<string> {
  const selected = page.getByRole('button', { name: DAY_WITH_SLOTS, pressed: true })

  // Кнопка дня состоит из двух соседних span-ов, и textContent склеивает их без разделителя:
  // день 1 с 31 свободным слотом читается как «131 св.», откуда `^\d{1,2}` достаёт «13».
  // Поэтому номер дня берём из первого span-а, а не разбираем текст кнопки целиком.
  const text = (await selected.locator('span').first().textContent()) ?? ''
  const match = text.trim().match(/^\d{1,2}$/)
  if (!match) {
    throw new Error(`Не удалось разобрать номер выбранного дня: "${text}"`)
  }
  return match[0]
}

/**
 * Выбирает день по его номеру. Календарь открывается на месяце ближайшего дня со слотами,
 * поэтому нужный день изредка оказывается в следующем месяце.
 */
export async function selectDay(page: Page, dayNumber: string): Promise<void> {
  const day = page.getByRole('button', { name: new RegExp(`^${dayNumber} \\d+ св\\.$`) })

  if ((await day.count()) === 0) {
    await page.getByRole('button', { name: 'Следующий месяц' }).click()
  }

  // Если кнопки дня нет и в соседнем месяце, вероятная причина — на этом дне закончились
  // свободные слоты между открытием страницы и повторным выбором дня (он пропадает из
  // календаря без «N св.»), а не то, что день ищется не в том месяце.
  if ((await day.count()) === 0) {
    throw new Error(`День ${dayNumber} не найден в календаре: похоже, на нём закончились свободные слоты`)
  }

  await day.click()
  await expect(day).toHaveAttribute('aria-pressed', 'true')
  await expect(page.getByRole('button', { name: /Свободно/ }).first()).toBeVisible()
}

/** Проходит форму гостя и подтверждает бронирование. */
export async function bookAs(page: Page, guestName: string, guestEmail: string): Promise<void> {
  await page.getByRole('button', { name: 'Продолжить' }).click()
  await page.getByLabel('Имя').fill(guestName)
  await page.getByLabel('Email').fill(guestEmail)
  await page.getByRole('button', { name: 'Забронировать' }).click()
  await expect(page.getByText('Встреча забронирована')).toBeVisible()
}
