import type { Owner } from '@/api/types'
import { cn } from '@/lib/utils'

/** Аватар владельца: контракт не передаёт изображение, поэтому рисуем плейсхолдер. */
function OwnerAvatar({ className }: { className?: string }) {
  return (
    <div className={cn('overflow-hidden rounded-2xl bg-secondary', className)} aria-hidden>
      <svg viewBox="0 0 64 64" className="size-full">
        <circle cx="32" cy="24" r="15" fill="#f2b98d" />
        <path d="M4 64a28 22 0 0 1 56 0Z" fill="#127f74" />
      </svg>
    </div>
  )
}

export function OwnerCard({ owner, className }: { owner: Owner; className?: string }) {
  return (
    <div className={cn('flex items-center gap-4', className)}>
      <OwnerAvatar className="size-14 shrink-0" />
      <div>
        <p className="font-bold">{owner.name}</p>
        <p className="text-sm text-muted-foreground">Host</p>
      </div>
    </div>
  )
}
