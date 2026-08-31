import { screen } from '@testing-library/react'
import { HttpResponse, http } from 'msw'
import { describe, expect, it } from 'vitest'

import type { EventType, Owner } from '@/api/types'
import { server } from '@/test/server'
import { renderPage } from '@/test/utils'
import { CatalogPage } from './CatalogPage'

const API = 'http://localhost:4010/api'

const owner: Owner = { id: 'owner-1', name: 'Tota' }

const eventTypes: EventType[] = [
  {
    id: 'intro',
    title: 'Встреча 15 минут',
    description: 'Короткий тип события для быстрого слота.',
    durationMinutes: 15,
  },
  {
    id: 'standard',
    title: 'Встреча 30 минут',
    description: 'Базовый тип события для бронирования.',
    durationMinutes: 30,
  },
]

function arrange(types: EventType[] = eventTypes) {
  server.use(
    http.get(`${API}/owner`, () => HttpResponse.json(owner)),
    http.get(`${API}/event-types`, () => HttpResponse.json(types)),
  )

  return renderPage(<CatalogPage />, { path: '/book', route: '/book' })
}

describe('CatalogPage', () => {
  it('показывает владельца и карточки типов событий с длительностью', async () => {
    arrange()

    expect(await screen.findByText('Tota')).toBeVisible()
    expect(await screen.findByText('Встреча 15 минут')).toBeVisible()
    expect(await screen.findByText('15 мин')).toBeVisible()
    expect(await screen.findByText('Встреча 30 минут')).toBeVisible()
    expect(await screen.findByText('30 мин')).toBeVisible()
  })

  it('ведёт с карточки на календарь этого типа события', async () => {
    arrange()

    const link = await screen.findByRole('link', { name: /Встреча 15 минут/ })
    expect(link).toHaveAttribute('href', '/book/intro')
  })

  it('подсказывает создать тип события, когда список пуст', async () => {
    arrange([])

    expect(
      await screen.findByText('Пока нет ни одного типа события. Создайте его в админке.'),
    ).toBeVisible()
  })

  it('показывает ошибку, когда бэкенд недоступен', async () => {
    server.use(
      http.get(`${API}/owner`, () => HttpResponse.json(owner)),
      http.get(`${API}/event-types`, () => HttpResponse.error()),
    )
    renderPage(<CatalogPage />, { path: '/book', route: '/book' })

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Не удалось связаться с сервером. Проверьте, что бэкенд запущен.',
    )
  })
})
