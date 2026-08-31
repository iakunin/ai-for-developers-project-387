import { useState } from 'react'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'

export type GuestDetails = {
  guestName: string
  guestEmail: string
}

type GuestFormProps = {
  pending: boolean
  onBack: () => void
  onSubmit: (guest: GuestDetails) => void
}

export function GuestForm({ pending, onBack, onSubmit }: GuestFormProps) {
  const [guestName, setGuestName] = useState('')
  const [guestEmail, setGuestEmail] = useState('')

  return (
    <form
      className="space-y-4"
      onSubmit={(event) => {
        event.preventDefault()
        onSubmit({ guestName: guestName.trim(), guestEmail: guestEmail.trim() })
      }}
    >
      <div className="space-y-2">
        <Label htmlFor="guestName">Имя</Label>
        <Input
          id="guestName"
          required
          value={guestName}
          onChange={(event) => setGuestName(event.target.value)}
          placeholder="Как к вам обращаться"
        />
      </div>

      <div className="space-y-2">
        <Label htmlFor="guestEmail">Email</Label>
        <Input
          id="guestEmail"
          type="email"
          required
          value={guestEmail}
          onChange={(event) => setGuestEmail(event.target.value)}
          placeholder="you@example.com"
        />
      </div>

      <div className="flex gap-3 pt-2">
        <Button type="button" variant="outline" onClick={onBack} disabled={pending}>
          Назад
        </Button>
        <Button type="submit" disabled={pending}>
          {pending ? 'Отправляем…' : 'Забронировать'}
        </Button>
      </div>
    </form>
  )
}
