import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { HttpResponse, http } from 'msw'
import { describe, expect, it } from 'vitest'

import type { Booking, EventType, Owner } from '@/api/types'
import { server } from '@/test/server'
import { renderPage } from '@/test/utils'
import { AdminPage } from './AdminPage'

const API = 'http://localhost:4010/api'

const owner: Owner = { id: 'owner-1', name: 'Tota' }

const eventTypes: EventType[] = [
  {
    id: 'intro',
    title: 'Встреча 15 минут',
    description: 'Короткий тип события для быстрого слота.',
    durationMinutes: 15,
  },
]

const bookings: Booking[] = [
  {
    id: 'booking-1',
    eventTypeId: 'intro',
    start: '2026-03-31T09:00:00Z',
    end: '2026-03-31T09:15:00Z',
    guestName: 'Мария',
    guestEmail: 'maria@example.com',
    createdAt: '2026-03-01T00:00:00Z',
  },
]

function arrange() {
  server.use(
    http.get(`${API}/owner`, () => HttpResponse.json(owner)),
    http.get(`${API}/event-types`, () => HttpResponse.json(eventTypes)),
    http.get(`${API}/bookings`, () => HttpResponse.json(bookings)),
  )

  return renderPage(<AdminPage />, { path: '/admin', route: '/admin' })
}

describe('AdminPage', () => {
  it('показывает предстоящие встречи и типы событий владельца', async () => {
    arrange()

    expect(await screen.findByText('Календарь владельца: Tota')).toBeVisible()
    expect(await screen.findByText('Мария · maria@example.com')).toBeVisible()
    expect(await screen.findByText('Встреча 15 минут')).toBeVisible()
    expect(await screen.findByText('15 мин')).toBeVisible()
  })

  it('создаёт тип события с заданным владельцем идентификатором', async () => {
    const user = userEvent.setup()
    let submitted: EventType | undefined

    arrange()
    server.use(
      http.post(`${API}/event-types`, async ({ request }) => {
        submitted = (await request.json()) as EventType
        return HttpResponse.json(submitted, { status: 201 })
      }),
    )

    await user.type(await screen.findByLabelText('Идентификатор'), 'deep-dive')
    await user.type(screen.getByLabelText('Название'), 'Встреча 60 минут')
    await user.type(screen.getByLabelText('Описание'), 'Длинный разговор.')
    await user.clear(screen.getByLabelText('Длительность, минут'))
    await user.type(screen.getByLabelText('Длительность, минут'), '60')
    await user.click(screen.getByRole('button', { name: 'Создать' }))

    expect(await screen.findByText('Тип события создан.')).toBeVisible()
    expect(submitted).toEqual({
      id: 'deep-dive',
      title: 'Встреча 60 минут',
      description: 'Длинный разговор.',
      durationMinutes: 60,
    })
  })

  it('показывает сообщение по коду event_type_id_taken', async () => {
    const user = userEvent.setup()

    arrange()
    server.use(
      http.post(`${API}/event-types`, () =>
        HttpResponse.json(
          { code: 'event_type_id_taken', message: 'Already exists' },
          { status: 409 },
        ),
      ),
    )

    await user.type(await screen.findByLabelText('Идентификатор'), 'intro')
    await user.type(screen.getByLabelText('Название'), 'Дубль')
    await user.click(screen.getByRole('button', { name: 'Создать' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Тип события с таким идентификатором уже существует.',
    )
  })
})
