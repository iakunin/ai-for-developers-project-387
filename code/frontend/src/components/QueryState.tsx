import type { ReactNode } from 'react'

import { errorMessage } from '@/api/errorMessages'
import { Alert } from '@/components/ui/alert'
import { Skeleton } from '@/components/ui/skeleton'

type QueryStateProps = {
  isPending: boolean
  error: unknown
  children: ReactNode
}

/** Единая подача состояний загрузки и ошибки для страниц. */
export function QueryState({ isPending, error, children }: QueryStateProps) {
  if (isPending) {
    return (
      <div className="space-y-3" aria-busy="true">
        <Skeleton className="h-24 w-full" />
        <Skeleton className="h-24 w-full" />
      </div>
    )
  }

  if (error) {
    return <Alert variant="destructive">{errorMessage(error)}</Alert>
  }

  return <>{children}</>
}
