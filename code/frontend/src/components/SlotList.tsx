import type { TimelineEntry } from '@/lib/slots'
import { formatTimeRange } from '@/lib/datetime'
import { cn } from '@/lib/utils'

type SlotListProps = {
  entries: TimelineEntry[]
  selectedStart: string | undefined
  onSelect: (start: string) => void
}

export function SlotList({ entries, selectedStart, onSelect }: SlotListProps) {
  if (entries.length === 0) {
    return <p className="py-6 text-sm text-muted-foreground">В этот день нет свободных слотов.</p>
  }

  return (
    <ul className="max-h-80 space-y-2 overflow-y-auto pr-1">
      {entries.map((entry) => {
        const label = formatTimeRange(entry.start, entry.end)

        if (entry.status === 'busy') {
          return (
            <li
              key={entry.start}
              className="flex items-center justify-between rounded-lg bg-secondary px-4 py-3 text-sm text-muted-foreground"
            >
              <span>{label}</span>
              <span className="font-semibold">Занято</span>
            </li>
          )
        }

        const isSelected = entry.start === selectedStart

        return (
          <li key={entry.start}>
            <button
              type="button"
              aria-pressed={isSelected}
              onClick={() => onSelect(entry.start)}
              className={cn(
                'flex w-full items-center justify-between rounded-lg border bg-card px-4 py-3 text-sm transition-colors',
                isSelected ? 'border-primary bg-primary/10' : 'border-border hover:border-primary/60',
              )}
            >
              <span>{label}</span>
              <span className="font-semibold">Свободно</span>
            </button>
          </li>
        )
      })}
    </ul>
  )
}
