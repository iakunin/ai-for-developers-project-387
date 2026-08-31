import { expect, test } from '@playwright/test'

import {
  bookAs,
  busyRow,
  freeSlot,
  openEventType,
  pickFirstFreeSlot,
  selectDay,
  selectedDayNumber,
  startTimeOf,
} from '../support/booking'

test('Сценарий 1: бронирование звонка от главной до подтверждения', async ({ page }) => {
  await page.goto('/')
  // «Записаться» есть и в шапке (NavLink), и в герое главной страницы. Без сужения до
  // <main> локатор падает со strict mode violation.
  await page.getByRole('main').getByRole('link', { name: 'Записаться' }).click()

  await expect(page.getByRole('heading', { name: 'Выберите тип события' })).toBeVisible()
  await page.getByRole('link', { name: /Знакомство/ }).click()

  // На странице бронирования два заголовка с названием типа события: h1 и h2 в карточке слева.
  await expect(page.getByRole('heading', { level: 1, name: 'Знакомство' })).toBeVisible()

  const slotLabel = await pickFirstFreeSlot(page)
  const day = await selectedDayNumber(page)

  await page.getByRole('button', { name: 'Продолжить' }).click()
  await page.getByLabel('Имя').fill('Гость Основной')
  await page.getByLabel('Email').fill('e2e-booking@example.com')
  await page.getByRole('button', { name: 'Забронировать' }).click()

  await expect(page.getByText('Встреча забронирована')).toBeVisible()
  await expect(page.getByText('Знакомство')).toBeVisible()
  // Экран подтверждения форматирует момент как «31 августа в 15:30», список слотов — как
  // «09:00 - 09:15», поэтому сверяем только время начала.
  await expect(page.getByRole('main')).toContainText(startTimeOf(slotLabel))
  // Проверка дня не может быть подстрочным поиском самого числа: те же цифры встречаются и во
  // времени («09:31»), а без границы слова число дня совпало бы и внутри чужого числа —
  // «1» нашёлся бы в «31 августа». Якорим число выбранного дня границей слова спереди и
  // кириллическим названием месяца следом за ним — так, как «31 августа» реально выглядит
  // на экране подтверждения.
  await expect(page.getByRole('main')).toContainText(new RegExp(`\\b${day}\\s+[а-яё]+`))
  await expect(page.getByRole('main')).toContainText('Гость Основной')
  await expect(page.getByRole('main')).toContainText('e2e-booking@example.com')
})

test('Сценарий 2: забронированный слот больше не предлагается', async ({ page }) => {
  await openEventType(page, 'intro-call')
  const day = await selectedDayNumber(page)
  const slotLabel = await pickFirstFreeSlot(page)
  const startTime = startTimeOf(slotLabel)

  await bookAs(page, 'Гость Второй', 'e2e-slot-taken@example.com')

  // Свежая загрузка страницы перечитывает и слоты, и бронирования.
  await openEventType(page, 'intro-call')
  await selectDay(page, day)

  await expect(busyRow(page, startTime)).toBeVisible()
  await expect(freeSlot(page, startTime)).toHaveCount(0)
})
