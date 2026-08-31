import { useState } from 'react'

import { errorMessage } from '@/api/errorMessages'
import { useBookings, useCancelBooking, useCreateEventType, useEventTypes, useOwner } from '@/api/queries'
import { QueryState } from '@/components/QueryState'
import { Alert } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { formatDateTime, formatDuration } from '@/lib/datetime'

export function AdminPage() {
  const owner = useOwner()
  const bookings = useBookings()
  const eventTypes = useEventTypes()

  return (
    <div className="mx-auto w-full max-w-6xl space-y-6 px-6 py-10">
      <div>
        <h1 className="text-3xl font-extrabold tracking-tight">Админка</h1>
        <QueryState isPending={owner.isPending} error={owner.error}>
          <p className="mt-2 text-sm text-muted-foreground">
            Календарь владельца: {owner.data?.name}
          </p>
        </QueryState>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Предстоящие встречи</CardTitle>
        </CardHeader>
        <CardContent>
          <QueryState isPending={bookings.isPending} error={bookings.error}>
            {bookings.data?.length === 0 ? (
              <p className="text-sm text-muted-foreground">Пока нет ни одной записи.</p>
            ) : (
              <ul className="divide-y divide-border">
                {bookings.data?.map((booking) => (
                  <li
                    key={booking.id}
                    className="flex flex-wrap items-center justify-between gap-3 py-3 text-sm"
                  >
                    <div>
                      <p className="font-medium">{formatDateTime(booking.start)}</p>
                      <p className="text-muted-foreground">
                        {booking.guestName} · {booking.guestEmail}
                      </p>
                    </div>
                    <div className="flex items-center gap-2">
                      <Badge>{booking.eventTypeId}</Badge>
                      <CancelBookingButton bookingId={booking.id} />
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </QueryState>
        </CardContent>
      </Card>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Типы событий</CardTitle>
          </CardHeader>
          <CardContent>
            <QueryState isPending={eventTypes.isPending} error={eventTypes.error}>
              {eventTypes.data?.length === 0 ? (
                <p className="text-sm text-muted-foreground">Типы событий ещё не созданы.</p>
              ) : (
                <ul className="divide-y divide-border">
                  {eventTypes.data?.map((eventType) => (
                    <li key={eventType.id} className="flex items-start justify-between gap-3 py-3">
                      <div>
                        <p className="text-sm font-medium">{eventType.title}</p>
                        <p className="text-sm text-muted-foreground">{eventType.description}</p>
                        <p className="mt-1 text-xs text-muted-foreground">id: {eventType.id}</p>
                      </div>
                      <Badge>{formatDuration(eventType.durationMinutes)}</Badge>
                    </li>
                  ))}
                </ul>
              )}
            </QueryState>
          </CardContent>
        </Card>

        <EventTypeForm />
      </div>
    </div>
  )
}

function EventTypeForm() {
  const createEventType = useCreateEventType()
  const [id, setId] = useState('')
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [durationMinutes, setDurationMinutes] = useState('30')

  return (
    <Card>
      <CardHeader>
        <CardTitle>Новый тип события</CardTitle>
      </CardHeader>
      <CardContent>
        <form
          className="space-y-4"
          onSubmit={(event) => {
            event.preventDefault()
            createEventType.mutate(
              {
                id: id.trim(),
                title: title.trim(),
                description: description.trim(),
                durationMinutes: Number(durationMinutes),
              },
              {
                onSuccess: () => {
                  setId('')
                  setTitle('')
                  setDescription('')
                  setDurationMinutes('30')
                },
              },
            )
          }}
        >
          {createEventType.error && (
            <Alert variant="destructive">{errorMessage(createEventType.error)}</Alert>
          )}
          {createEventType.isSuccess && !createEventType.error && (
            <Alert>Тип события создан.</Alert>
          )}

          <div className="space-y-2">
            <Label htmlFor="eventTypeId">Идентификатор</Label>
            <Input
              id="eventTypeId"
              required
              value={id}
              onChange={(event) => setId(event.target.value)}
              placeholder="intro-call"
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="eventTypeTitle">Название</Label>
            <Input
              id="eventTypeTitle"
              required
              value={title}
              onChange={(event) => setTitle(event.target.value)}
              placeholder="Встреча 30 минут"
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="eventTypeDescription">Описание</Label>
            <Textarea
              id="eventTypeDescription"
              value={description}
              onChange={(event) => setDescription(event.target.value)}
              placeholder="Базовый тип события для бронирования."
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="eventTypeDuration">Длительность, минут</Label>
            <Input
              id="eventTypeDuration"
              type="number"
              min={1}
              required
              value={durationMinutes}
              onChange={(event) => setDurationMinutes(event.target.value)}
            />
          </div>

          <Button type="submit" disabled={createEventType.isPending}>
            {createEventType.isPending ? 'Создаём…' : 'Создать'}
          </Button>
        </form>
      </CardContent>
    </Card>
  )
}

function CancelBookingButton({ bookingId }: { bookingId: string }) {
  const cancelBooking = useCancelBooking()

  return (
    <div className="flex items-center gap-2">
      {cancelBooking.isError && (
        <Alert variant="destructive">{errorMessage(cancelBooking.error)}</Alert>
      )}
      <Button
        variant="destructive"
        size="sm"
        disabled={cancelBooking.isPending}
        onClick={() => {
          if (window.confirm('Отменить это бронирование?')) {
            cancelBooking.mutate(bookingId)
          }
        }}
      >
        {cancelBooking.isPending ? 'Отмена…' : 'Отменить'}
      </Button>
    </div>
  )
}
