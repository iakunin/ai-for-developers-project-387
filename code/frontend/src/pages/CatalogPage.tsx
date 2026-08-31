import { Link } from 'react-router-dom'

import { useEventTypes, useOwner } from '@/api/queries'
import { OwnerCard } from '@/components/OwnerCard'
import { QueryState } from '@/components/QueryState'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader } from '@/components/ui/card'
import { formatDuration } from '@/lib/datetime'

export function CatalogPage() {
  const owner = useOwner()
  const eventTypes = useEventTypes()

  return (
    <div className="mx-auto w-full max-w-6xl space-y-6 px-6 py-10">
      <Card>
        <CardHeader className="gap-5">
          <QueryState isPending={owner.isPending} error={owner.error}>
            {owner.data && <OwnerCard owner={owner.data} />}
          </QueryState>

          <div>
            <h1 className="text-3xl font-extrabold tracking-tight">Выберите тип события</h1>
            <p className="mt-2 text-sm text-muted-foreground">
              Нажмите на карточку, чтобы открыть календарь и выбрать удобный слот.
            </p>
          </div>
        </CardHeader>
      </Card>

      <QueryState isPending={eventTypes.isPending} error={eventTypes.error}>
        {eventTypes.data?.length === 0 ? (
          <Card>
            <CardContent className="pt-6 text-sm text-muted-foreground">
              Пока нет ни одного типа события. Создайте его в админке.
            </CardContent>
          </Card>
        ) : (
          <div className="grid gap-6 md:grid-cols-2">
            {eventTypes.data?.map((eventType) => (
              <Link
                key={eventType.id}
                to={`/book/${encodeURIComponent(eventType.id)}`}
                className="rounded-2xl focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              >
                <Card className="h-full transition-colors hover:border-primary/60">
                  <CardContent className="flex items-start justify-between gap-4 pt-6">
                    <div>
                      <h2 className="text-lg font-bold">{eventType.title}</h2>
                      <p className="mt-2 text-sm text-muted-foreground">{eventType.description}</p>
                    </div>
                    <Badge>{formatDuration(eventType.durationMinutes)}</Badge>
                  </CardContent>
                </Card>
              </Link>
            ))}
          </div>
        )}
      </QueryState>
    </div>
  )
}
