import { ArrowLeft, ArrowRight } from 'lucide-react'
import { useState } from 'react'

import { Button } from '@/components/ui/button'
import { addMonths, dayKey, formatMonth, isSameMonth, monthGrid, WEEKDAY_LABELS } from '@/lib/datetime'
import { cn } from '@/lib/utils'

type MonthCalendarProps = {
  /** Первый показываемый месяц. */
  initialMonth: Date
  /** Количество свободных слотов по ключу локального дня. */
  freeSlotsByDay: Map<string, number>
  selectedDay: string | undefined
  onSelectDay: (day: string) => void
}

export function MonthCalendar({
  initialMonth,
  freeSlotsByDay,
  selectedDay,
  onSelectDay,
}: MonthCalendarProps) {
  const [month, setMonth] = useState(initialMonth)
  const days = monthGrid(month)

  return (
    <div>
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-bold">Календарь</h2>
        <div className="flex gap-2">
          <Button
            variant="outline"
            size="icon"
            aria-label="Предыдущий месяц"
            onClick={() => setMonth(addMonths(month, -1))}
          >
            <ArrowLeft aria-hidden />
          </Button>
          <Button
            variant="outline"
            size="icon"
            aria-label="Следующий месяц"
            onClick={() => setMonth(addMonths(month, 1))}
          >
            <ArrowRight aria-hidden />
          </Button>
        </div>
      </div>

      <p className="mt-5 text-sm text-muted-foreground">{formatMonth(month)}</p>

      <div className="mt-4 grid grid-cols-7 gap-1.5">
        {WEEKDAY_LABELS.map((label) => (
          <div key={label} className="pb-1 text-center text-xs font-medium text-muted-foreground">
            {label}
          </div>
        ))}

        {days.map((day) => {
          const key = dayKey(day)
          const freeCount = freeSlotsByDay.get(key) ?? 0
          const isAvailable = freeCount > 0
          const isSelected = key === selectedDay

          return (
            <button
              key={key}
              type="button"
              disabled={!isAvailable}
              aria-pressed={isSelected}
              onClick={() => onSelectDay(key)}
              className={cn(
                'flex h-12 flex-col items-center justify-center rounded-lg border text-sm transition-colors',
                isAvailable
                  ? 'border-border bg-card hover:border-primary/60'
                  : 'cursor-not-allowed border-transparent bg-secondary text-muted-foreground',
                !isSameMonth(day, month) && 'opacity-60',
                isSelected && 'border-primary bg-primary/10 font-semibold text-foreground',
              )}
            >
              <span>{day.getDate()}</span>
              {isAvailable && (
                <span className="text-[10px] text-muted-foreground">{freeCount} св.</span>
              )}
            </button>
          )
        })}
      </div>
    </div>
  )
}
