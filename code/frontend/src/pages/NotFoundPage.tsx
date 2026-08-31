import { Link } from 'react-router-dom'

import { Button } from '@/components/ui/button'

export function NotFoundPage() {
  return (
    <div className="mx-auto w-full max-w-2xl px-6 py-24 text-center">
      <h1 className="text-3xl font-extrabold tracking-tight">Страница не найдена</h1>
      <p className="mt-3 text-muted-foreground">Возможно, ссылка устарела.</p>
      <Button asChild className="mt-8">
        <Link to="/">На главную</Link>
      </Button>
    </div>
  )
}
