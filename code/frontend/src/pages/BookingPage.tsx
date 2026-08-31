import { CheckCircle2 } from 'lucide-react'
import { useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'

import { errorMessage, isStaleSlotError } from '@/api/errorMessages'
import { useBookings, useCreateBooking, useEventType, useOwner, useSlots } from '@/api/queries'
import type { Booking } from '@/api/types'
import { GuestForm, type GuestDetails } from '@/components/GuestForm'
import { MonthCalendar } from '@/components/MonthCalendar'
import { OwnerCard } from '@/components/OwnerCard'
import { QueryState } from '@/components/QueryState'
import { SlotList } from '@/components/SlotList'
import { Alert } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import {
  dayKey,
  formatDateTime,
  formatDuration,
  formatDay,
  formatTimeRange,
  startOfMonth,
} from '@/lib/datetime'
import { dayTimeline, firstDayWithSlots, freeSlotCountByDay } from '@/lib/slots'

type Step = 'pick' | 'details'

export function BookingPage() {
  const { eventTypeId = '' } = useParams()

  const owner = useOwner()
  const eventType = useEventType(eventTypeId)
  const slots = useSlots(eventTypeId)
  const bookings = useBookings()
  const createBooking = useCreateBooking(eventTypeId)

  const [pickedStep, setPickedStep] = useState<Step>('pick')
  const [pickedDay, setPickedDay] = useState<string>()
  const [pickedStart, setPickedStart] = useState<string>()
  const [confirmed, setConfirmed] = useState<Booking>()

  const slotData = useMemo(() => slots.data ?? [], [slots.data])
  const freeSlotsByDay = useMemo(() => freeSlotCountByDay(slotData), [slotData])
  const earliestDay = useMemo(() => firstDayWithSlots(slotData), [slotData])

  // Пока гость не выбрал день сам, показываем первый доступный.
  const selectedDay = pickedDay ?? (earliestDay ? dayKey(earliestDay) : undefined)

  // Выбранный слот мог исчезнуть после обновления списка: тогда выбор считается снятым,
  // а форма гостя — недоступной.
  const selectedSlot = slotData.find((slot) => slot.start === pickedStart)
  const selectedStart = selectedSlot?.start
  const step: Step = selectedStart ? pickedStep : 'pick'

  const timeline = useMemo(
    () => (selectedDay ? dayTimeline(selectedDay, slotData, bookings.data ?? []) : []),
    [selectedDay, slotData, bookings.data],
  )

  function handleSubmit(guest: GuestDetails) {
    if (!selectedStart) return

    createBooking.mutate(
      { eventTypeId, start: selectedStart, ...guest },
      {
        onSuccess: (booking) => setConfirmed(booking),
        onError: (error) => {
          // Слот уже заняли: возвращаем гостя к выбору времени.
          if (isStaleSlotError(error)) setPickedStart(undefined)
        },
      },
    )
  }

  if (confirmed && eventType.data) {
    return (
      <div className="mx-auto w-full max-w-2xl px-6 py-16">
        <Card>
          <CardHeader className="items-start gap-4">
            <CheckCircle2 className="size-8 text-primary" aria-hidden />
            <CardTitle className="text-2xl">Встреча забронирована</CardTitle>
          </CardHeader>
          <CardContent className="space-y-2 text-sm text-muted-foreground">
            <p>
              <span className="font-medium text-foreground">{eventType.data.title}</span> —{' '}
              {formatDateTime(confirmed.start)}
            </p>
            <p>
              {confirmed.guestName} · {confirmed.guestEmail}
            </p>
            <Button asChild className="mt-6">
              <Link to="/book">Записаться ещё</Link>
            </Button>
          </CardContent>
        </Card>
      </div>
    )
  }

  return (
    <div className="mx-auto w-full max-w-6xl px-6 py-10">
      <QueryState isPending={eventType.isPending} error={eventType.error}>
        <h1 className="text-4xl font-extrabold tracking-tight">{eventType.data?.title}</h1>
      </QueryState>

      <div className="mt-8 grid gap-6 lg:grid-cols-3">
        <Card>
          <CardHeader className="gap-5">
            <QueryState isPending={owner.isPending} error={owner.error}>
              {owner.data && <OwnerCard owner={owner.data} />}
            </QueryState>

            {eventType.data && (
              <div>
                <div className="flex items-start justify-between gap-3">
                  <h2 className="text-xl font-bold">{eventType.data.title}</h2>
                  <Badge>{formatDuration(eventType.data.durationMinutes)}</Badge>
                </div>
                <p className="mt-2 text-sm text-muted-foreground">{eventType.data.description}</p>
              </div>
            )}

            <div className="space-y-3">
              <SummaryField
                label="Выбранная дата"
                value={selectedDay ? formatDay(new Date(`${selectedDay}T00:00:00`)) : 'Дата не выбрана'}
              />
              <SummaryField
                label="Выбранное время"
                value={
                  selectedSlot
                    ? formatTimeRange(selectedSlot.start, selectedSlot.end)
                    : 'Время не выбрано'
                }
              />
            </div>
          </CardHeader>
        </Card>

        <Card>
          <CardContent className="pt-6">
            <QueryState isPending={slots.isPending} error={slots.error}>
              <MonthCalendar
                initialMonth={startOfMonth(earliestDay ?? new Date())}
                freeSlotsByDay={freeSlotsByDay}
                selectedDay={selectedDay}
                onSelectDay={(day) => {
                  setPickedDay(day)
                  setPickedStart(undefined)
                }}
              />
            </QueryState>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>{step === 'pick' ? 'Статус слотов' : 'Ваши данные'}</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            {createBooking.error && (
              <Alert variant="destructive">{errorMessage(createBooking.error)}</Alert>
            )}

            {step === 'pick' ? (
              <>
                <QueryState
                  isPending={slots.isPending || bookings.isPending}
                  error={slots.error ?? bookings.error}
                >
                  <SlotList
                    entries={timeline}
                    selectedStart={selectedStart}
                    onSelect={setPickedStart}
                  />
                </QueryState>

                <div className="flex gap-3">
                  <Button asChild variant="outline">
                    <Link to="/book">Назад</Link>
                  </Button>
                  <Button disabled={!selectedStart} onClick={() => setPickedStep('details')}>
                    Продолжить
                  </Button>
                </div>
              </>
            ) : (
              <GuestForm
                pending={createBooking.isPending}
                onBack={() => setPickedStep('pick')}
                onSubmit={handleSubmit}
              />
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  )
}

function SummaryField({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg bg-secondary px-4 py-3">
      <p className="text-sm text-muted-foreground">{label}</p>
      <p className="mt-0.5 text-sm font-medium first-letter:uppercase">{value}</p>
    </div>
  )
}
