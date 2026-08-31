# Call Calendar — frontend

The application UI from step 3. All data and actions go through the API described in
[`api-contract/openapi/openapi.yaml`](../../api-contract/openapi/openapi.yaml) — the contract stays
the single source of truth.

## Stack

- **Vite** + **React** + **TypeScript**
- **Tailwind CSS** + **shadcn/ui** (components live in `src/components/ui` and belong to the project)
- **TanStack Query** — fetching, caching and query invalidation
- **React Router** — routing
- **Vitest** + **Testing Library** + **MSW** — tests
- **Prism** — a mock of the API from the contract, for development

## Quick start

```bash
make install

# 1. Start the contract-based mock API (port 4010)
make mock

# 2. In another terminal — the dev server
make dev
```

The app opens at http://localhost:5173 and talks to Prism at http://localhost:4010 by default.

### Working against a real backend

The base URL is set through the `VITE_API_BASE_URL` variable (see `.env.example`):

```bash
cp .env.example .env
# VITE_API_BASE_URL=http://localhost:8080
make dev
```

## Pages

| Route | Role | What it shows |
| --- | --- | --- |
| `/` | guest | Home: description and a link to book |
| `/book` | guest | Owner profile and the list of event types |
| `/book/:eventTypeId` | guest | Calendar, slot status, guest form, confirmation |
| `/admin` | owner | Upcoming bookings, event types and creating them |

## How the booking page works with the contract

`GET /api/event-types/{id}/slots` returns **free slots only**. To render the full "slot status" column,
busy intervals are taken from `GET /api/bookings` — calendar busyness is shared across all event types,
so bookings of any type show up in the list. The two lists cannot overlap: a slot that intersects a
booking never appears in the slots response.

Decisions about errors are made on the machine-readable `code`, never on the `message` text.
`slot_taken` and `slot_not_available` additionally refetch the slots and send the guest back to
picking a time.

The contract carries instants in UTC; they are displayed to the user in the browser's time zone.

## Types from the contract

`src/api/schema.d.ts` is generated from the OpenAPI spec and committed to the repository. The
frontend defines no domain types of its own — `src/api/types.ts` only re-exports the contract
schemas.

```bash
make api-types   # regenerate after the contract changes
```

CI (`.github/workflows/frontend.yml`) fails when the generated types have drifted from the contract.

## Commands

Every command is a target in the [`Makefile`](Makefile):

```bash
make help          # list every target
make install       # install dependencies
make dev           # dev server
make build         # type check and production build
make preview       # preview the built app
make test          # tests
make test-watch    # tests in watch mode
make lint          # linter
make typecheck     # type check only
make mock          # Prism mock of the API from the contract
make api-types     # regenerate types from the contract
make check         # lint + test + build
```

Before considering work done: `make check` must pass.
