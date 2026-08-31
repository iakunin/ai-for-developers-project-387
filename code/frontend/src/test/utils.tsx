import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import type { ReactElement } from 'react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'

import { Layout } from '@/components/Layout'

/** Рендерит страницу внутри роутера и клиента запросов, как в приложении. */
export function renderPage(
  element: ReactElement,
  { path = '/', route = '/' }: { path?: string; route?: string } = {},
) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[route]}>
        <Routes>
          <Route element={<Layout />}>
            <Route path={path} element={element} />
          </Route>
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}
