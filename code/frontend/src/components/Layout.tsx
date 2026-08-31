import { CalendarClock } from 'lucide-react'
import { Link, NavLink, Outlet } from 'react-router-dom'

import { cn } from '@/lib/utils'

const NAV_ITEMS = [
  { to: '/book', label: 'Записаться' },
  { to: '/admin', label: 'Админка' },
]

function Header() {
  return (
    <header className="border-b border-border bg-card/70 backdrop-blur">
      <div className="mx-auto flex h-[60px] max-w-6xl items-center justify-between px-6">
        <Link to="/" className="flex items-center gap-2 font-bold tracking-tight">
          <CalendarClock className="size-5 text-primary" aria-hidden />
          Calendar
        </Link>

        <nav className="flex items-center gap-1">
          {NAV_ITEMS.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                cn(
                  'rounded-lg px-3 py-1.5 text-sm transition-colors',
                  isActive
                    ? 'bg-secondary font-medium text-foreground'
                    : 'text-muted-foreground hover:text-foreground',
                )
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </div>
    </header>
  )
}

export function Layout() {
  return (
    <div className="flex min-h-dvh flex-col">
      <Header />
      <main className="flex flex-1 flex-col">
        <Outlet />
      </main>
    </div>
  )
}
