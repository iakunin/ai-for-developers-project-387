import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { HttpResponse, http } from 'msw'
import { describe, expect, it } from 'vitest'

import type { Booking, EventType, Owner, Slot } from '@/api/types'
import { server } from '@/test/server'
import { renderPage } from '@/test/utils'
import { BookingPage } from './BookingPage'

const API = 'http://localhost:4010/api'

const owner: Owner = { id: 'owner-1', name: 'Tota' }

const eventType: EventType = {
  id: 'intro',
  title: 'Встреча 15 минут',
  description: 'Короткий тип события для быстрого слота.',
  durationMinutes: 15,
}

const FREE: Slot[] = [
  { start: '2026-03-31T09:45:00Z', end: '2026-03-31T10:00:00Z' },
  { start: '2026-03-31T10:00:00Z', end: '2026-03-31T10:15:00Z' },
]

const BOOKED: Booking = {
  id: 'booking-1',
  eventTypeId: 'intro',
  start: '2026-03-31T09:00:00Z',
  end: '2026-03-31T09:15:00Z',
  guestName: 'Занятый гость',
  guestEmail: 'busy@example.com',
  createdAt: '2026-03-01T00:00:00Z',
}

function arrange({ slots = FREE, bookings = [BOOKED] }: { slots?: Slot[]; bookings?: Booking[] } = {}) {
  server.use(
    http.get(`${API}/owner`, () => HttpResponse.json(owner)),
    http.get(`${API}/event-types/intro`, () => HttpResponse.json(eventType)),
    http.get(`${API}/event-types/intro/slots`, () => HttpResponse.json(slots)),
    http.get(`${API}/bookings`, () => HttpResponse.json(bookings)),
  )

  return renderPage(<BookingPage />, { path: '/book/:eventTypeId', route: '/book/intro' })
}

describe('BookingPage', () => {
  it('показывает тип события, владельца и свободные слоты', async () => {
    arrange()

    expect(await screen.findByRole('heading', { name: 'Встреча 15 минут', level: 1 })).toBeVisible()
    expect(await screen.findByText('Tota')).toBeVisible()
    expect(await screen.findAllByText('Свободно')).toHaveLength(2)
  })

  it('помечает занятым время из /api/bookings — контракт отдаёт только свободные слоты', async () => {
    arrange()

    expect(await screen.findByText('Занято')).toBeVisible()
    // Занятая строка не должна быть кликабельной.
    expect(screen.queryByRole('button', { name: /Занято/ })).not.toBeInTheDocument()
  })

  it('проводит гостя от выбора слота до подтверждения', async () => {
    const user = userEvent.setup()
    let submitted: unknown

    arrange()
    server.use(
      http.post(`${API}/bookings`, async ({ request }) => {
        submitted = await request.json()
        return HttpResponse.json(
          {
            ...BOOKED,
            id: 'booking-2',
            start: FREE[0].start,
            end: FREE[0].end,
            guestName: 'Мария',
            guestEmail: 'maria@example.com',
          },
          { status: 201 },
        )
      }),
    )

    const slots = await screen.findAllByRole('button', { name: /Свободно/ })
    await user.click(slots[0])
    await user.click(screen.getByRole('button', { name: 'Продолжить' }))

    await user.type(screen.getByLabelText('Имя'), 'Мария')
    await user.type(screen.getByLabelText('Email'), 'maria@example.com')
    await user.click(screen.getByRole('button', { name: 'Забронировать' }))

    expect(await screen.findByText('Встреча забронирована')).toBeVisible()
    expect(submitted).toEqual({
      eventTypeId: 'intro',
      start: FREE[0].start,
      guestName: 'Мария',
      guestEmail: 'maria@example.com',
    })
  })

  it('на 409 slot_taken показывает ошибку и возвращает к выбору слота', async () => {
    const user = userEvent.setup()

    arrange()
    server.use(
      http.post(`${API}/bookings`, () =>
        HttpResponse.json(
          { code: 'slot_taken', message: 'Slot already booked' },
          { status: 409 },
        ),
      ),
    )

    const slots = await screen.findAllByRole('button', { name: /Свободно/ })
    await user.click(slots[0])
    await user.click(screen.getByRole('button', { name: 'Продолжить' }))

    await user.type(screen.getByLabelText('Имя'), 'Мария')
    await user.type(screen.getByLabelText('Email'), 'maria@example.com')
    await user.click(screen.getByRole('button', { name: 'Забронировать' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Это время только что заняли. Выберите другой слот.',
    )
    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Продолжить' })).toBeDisabled()
    })
  })

  it('сообщает, когда в выбранный день нет свободных слотов', async () => {
    arrange({ slots: [], bookings: [] })

    expect(await screen.findByText('В этот день нет свободных слотов.')).toBeVisible()
  })
})
