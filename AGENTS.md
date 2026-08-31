# AGENTS.md

The "Call Calendar" project (Hexlet, project 386). A learning project built **Design First**: the API
contract is fixed first, then each part of the application is implemented independently against it.

## Repository layout

| Path             | What it is |
|------------------| --- |
| `api-contract/`  | TypeSpec description (`main.tsp`) and the OpenAPI spec generated from it |
| `code/frontend/` | Application UI (Vite + React + TypeScript + Tailwind + shadcn/ui) |
| `code/backend/`  | Application API (Java 25 + Gradle + Spring Boot, in-memory storage) |
| `code/e2e/`      | End-to-end scenarios (Playwright, against the built Docker image) |
| `docs/`          | Project step descriptions |

## Commands go through the Makefile

**Every command MUST be run through the `Makefile` of the part you are working in.** Never call the
underlying tool directly (`npm run build`, `npx vitest`, `oxlint`, …) — the Makefile is the single
entry point, and only its targets are documented and kept in sync.

- [`api-contract/Makefile`](api-contract/Makefile) — the contract
- [`code/frontend/Makefile`](code/frontend/Makefile) — the UI
- [`code/backend/Makefile`](code/backend/Makefile) — the API
- [`code/e2e/Makefile`](code/e2e/Makefile) — the end-to-end scenarios

`make help` lists every target, and `make check` runs everything that must pass before the work is
done. When a command is missing, add a target instead of running it by hand.

## The contract is the single source of truth

`api-contract/openapi/openapi.yaml` describes the external behaviour of the application. Both parts
of the application follow it and never rely on each other's internal implementation.

**Two files are generated and MUST NOT be edited by hand:**

- `api-contract/openapi/openapi.yaml` — generated from `api-contract/main.tsp`
- `code/frontend/src/api/schema.d.ts` — generated from `openapi.yaml`

After any change to `main.tsp`, regenerate both files and commit them together with the contract:

```bash
cd api-contract && make build        # main.tsp -> openapi.yaml
cd code/frontend && make api-types   # openapi.yaml -> schema.d.ts
```

CI (`.github/workflows/api-contract.yml` and `.github/workflows/frontend.yml`) fails when the
generated files have drifted from their sources.

Two more files are generated and MUST NOT be edited by hand — release-please owns them, and it
rewrites them on every release PR:

- `CHANGELOG.md` — built from the Conventional Commits history
- `version.txt` — the version release-please proposes

## Domain rules

These rules are set by the contract; behaviour can only be changed by changing the contract.

- There is no registration and no authentication. The calendar owner is a single predefined profile,
  and guests book without an account.
- The booking window is 14 days starting from the current date.
- Calendar busyness is shared across all event types: two bookings cannot be created for the same
  time, even for different event types.
- `GET /api/event-types/{id}/slots` returns **free slots only**. Busy intervals, when the UI needs them,
  come from `GET /api/bookings`.

## Frontend (`code/frontend`)

See [`code/frontend/README.md`](code/frontend/README.md) for details. Key conventions:

- Never hand-write domain types: `src/api/types.ts` only re-exports the schemas from the generated
  `schema.d.ts`.
- Decisions about errors are made on the machine-readable `code` from `ApiError`, never on the
  `message` text.
- The contract carries instants in UTC; they are displayed to the user in the browser's time zone.
- The shadcn/ui components live in `src/components/ui` and belong to the project — edit them freely.
- Derive state during render instead of synchronising it with `useEffect` + `setState` (the linter
  catches this with the `react(set-state-in-effect)` rule).

Commands MUST be run through [`code/frontend/Makefile`](code/frontend/Makefile), never as `npm run`
directly:

```bash
cd code/frontend
make help          # list every target
make install       # install dependencies
make dev           # dev server
make mock          # Prism mock of the API from the contract (port 4010)
make test          # tests (Vitest + Testing Library + MSW)
make lint          # oxlint
make build         # type check and production build
make check         # lint + test + build
```

Before considering work done: `make check` must pass.

## Backend (`code/backend`)

See [`code/backend/README.md`](code/backend/README.md) for details. Key conventions:

- Never hand-write domain types: models are generated from the contract at build time and are not
  committed.
- Storage is in memory, behind `EventTypeRepository` and `BookingRepository`; all data resets on
  restart.
- Slot-generation policy (working days and hours, the booking window) comes from configuration, never
  from constants in code.
- Decisions about errors are made on `ErrorCode`, never on the `message` text.

## E2E (`code/e2e`)

The user scenarios are fixed in [`docs/E2E_SCENARIOS.md`](docs/E2E_SCENARIOS.md); every scenario
there is covered by a Playwright test whose title quotes the scenario name verbatim, so the
document and the suite cannot drift apart silently.

Key conventions:

- The tests run against the **built Docker image**, not against the dev servers. That is the
  artifact that ships: the frontend sits on the backend classpath, so everything answers from
  one origin and CORS never enters the picture.
- `code/e2e` depends on nothing inside `code/frontend` or `code/backend` — only on the HTTP
  surface of the running application.
- Locators are role- and label-based. The rendered DOM is already accessible; do not add
  `data-testid` attributes to the frontend for the tests' benefit.
- Backend storage is in memory and shared across the whole run, so **no test may hardcode a
  date or a time**, assume an empty calendar, or assert a total count. Each test reads the
  slots the UI is currently offering and uses its own guest email — generated with `Date.now()`
  where a test asserts a count, since `reuseExistingServer` lets a local container outlive a
  single run and a static email would accumulate rows across runs.
- The suite runs single-worker (`fullyParallel: false`, `workers: 1`) for the same reason:
  parallel workers would interleave bookings against that shared storage unpredictably. Do not
  parallelise the run.
- The browser context pins `locale: 'ru-RU'` and `timezoneId: 'Europe/Moscow'`. The frontend
  renders instants in the browser's zone while the backend's working hours are configured in
  Moscow time; without pinning, a CI runner on UTC shifts every displayed time by three hours.
- Locally, `reuseExistingServer` attaches to whatever already answers on `:8080`. After changing
  frontend or backend source, stop any container on `:8080` before `make test`, or the run
  reuses the stale one and validates the old image.

Commands MUST be run through [`code/e2e/Makefile`](code/e2e/Makefile):

```bash
cd code/e2e
make help          # list every target
make install       # install dependencies
make browsers      # install the Chromium build Playwright drives
make image         # build the application image the tests run against
make test          # run the scenarios (builds the image first)
make test-ui       # run them in the Playwright UI
make report        # open the last HTML report
make check         # typecheck + test
```

Before considering work done: `make check` must pass.

## Commit messages

All commits MUST follow the [Conventional Commits 1.0.0](https://www.conventionalcommits.org/en/v1.0.0/) specification.

Since [release-please](https://github.com/googleapis/release-please) generates `CHANGELOG.md`
and the version from this history, a commit that does not follow the specification is silently
dropped from the changelog. This applies to agent-authored commits exactly as it does to
hand-written ones.

Format:

```
<type>[optional scope][optional !]: <description>

[optional body]

[optional footer(s)]
```

Rules:

- `type` is one of: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `chore`, `revert`.
- `description` is in the imperative mood, lowercase, no trailing period.
- Use a scope when the change is confined to one area, e.g. `feat(api-contract): ...`.
- Breaking changes are marked with `!` after the type/scope and/or a `BREAKING CHANGE: <explanation>` footer.

Examples:

```
feat(api-contract): add booking cancellation endpoint
fix: reject overlapping bookings across event types
docs: add step 3 frontend task description
refactor(frontend)!: drop legacy slots response shape
```
