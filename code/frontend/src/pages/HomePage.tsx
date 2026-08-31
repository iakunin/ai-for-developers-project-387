import { ArrowRight } from 'lucide-react'
import { Link } from 'react-router-dom'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

const FEATURES = [
  'Выбор типа события и удобного времени для встречи.',
  'Быстрое бронирование с подтверждением и дополнительными заметками.',
  'Управление типами встреч и просмотр предстоящих записей в админке.',
]

export function HomePage() {
  return (
    <div className="flex-1 bg-[radial-gradient(120%_120%_at_0%_0%,#dbe6fb_0%,#f1f5f9_45%,#fdece0_100%)]">
      <div className="mx-auto grid max-w-6xl items-start gap-12 px-6 py-16 lg:grid-cols-2">
        <div>
          <span className="inline-flex rounded-full bg-card px-4 py-2 text-xs font-bold tracking-wider text-muted-foreground uppercase shadow-sm">
            Быстрая запись на звонок
          </span>

          <h1 className="mt-6 text-6xl font-extrabold tracking-tight">Calendar</h1>

          <p className="mt-4 max-w-md text-lg text-muted-foreground">
            Забронируйте встречу за минуту: выберите тип события и удобное время.
          </p>

          <Button asChild size="lg" className="mt-8">
            <Link to="/book">
              Записаться
              <ArrowRight aria-hidden />
            </Link>
          </Button>
        </div>

        <Card>
          <CardHeader>
            <CardTitle>Возможности</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3 text-sm text-muted-foreground">
            {FEATURES.map((feature) => (
              <p key={feature}>• {feature}</p>
            ))}
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
