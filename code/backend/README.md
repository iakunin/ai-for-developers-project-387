# Call Calendar — backend

The application API from step 4. It implements
[`api-contract/openapi/openapi.yaml`](../../api-contract/openapi/openapi.yaml) — the contract stays
the single source of truth — and can also serve the built frontend from the same jar.

## Stack

- **Java 25** + **Gradle 9.7.1** (wrapper committed)
- **Spring Boot 4.1.1** — Spring Web MVC, with virtual threads enabled
  (`spring.threads.virtual.enabled: true`)
- **Lombok** — build-time only, never reaches the runtime image. Used for `@RequiredArgsConstructor`,
  `@Slf4j` and `@Getter` on our own classes; never on the generated contract models
- **Spotless** with `google-java-format` — formatting

## Types from the contract

Models are generated from the contract at build time — the backend never hand-writes a domain type.
The `openapi-generator` Gradle plugin runs the `spring` generator in models-only mode with builders
enabled, so call sites read `Booking.builder()....build()`. Generated sources land in
`build/generated/openapi/src/main/java`, under package `dev.iakunin.callcalendar.contract.model`, and
are not committed — `make build` (or any target that depends on it) regenerates them from
`api-contract/openapi/openapi.yaml`.

## Quick start

```bash
make run   # starts on port 8080
```

The frontend can then point at it instead of the Prism mock — see
[`code/frontend/README.md`](../frontend/README.md#working-against-a-real-backend).

## Storage

All data lives in memory, behind `EventTypeRepository` and `BookingRepository`. Nothing is persisted:
every restart starts from the seeded event types again. Step 4 of the project explicitly allows this.

## Slot generation

`GET /api/event-types/{id}/slots` returns **free slots only**; busy intervals, when a client needs them,
come from `GET /api/bookings`. Calendar busyness is shared across all event types — two bookings can never
be created for the same time, even for different event types.

The grid itself is backend policy, driven entirely by configuration rather than constants:

- Working days and hours default to Monday–Friday, 09:00–18:00, `Europe/Moscow`.
- Each event type steps the grid by its own `durationMinutes` — a slot only appears when it, and the
  full duration of the meeting, fit inside one working day.
- The window is 14 days starting from today, in the owner's timezone.

## Configuration

Every rule the contract leaves to the backend lives under `call-calendar` in `application.yml`, never
as a constant in code:

```yaml
call-calendar:
  owner:
    id: owner
    name: Максим Якунин
  schedule:
    timezone: Europe/Moscow
    working-days: [MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY]
    open: "09:00"
    close: "18:00"
  booking:
    window-days: 14
  cors:
    allowed-origins:
      - http://localhost:5173
  seed:
    event-types:
      - id: intro-call
        title: Знакомство
        description: Короткий созвон, чтобы познакомиться и обсудить задачу в общих чертах.
        duration-minutes: 15
      - id: consultation
        title: Консультация
        description: Разговор по конкретному вопросу с ответами и рекомендациями.
        duration-minutes: 30
      - id: deep-dive
        title: Разбор задачи
        description: Подробный разбор задачи или кода с планом дальнейших шагов.
        duration-minutes: 60
```

| Key | Meaning | Default |
| --- | --- | --- |
| `owner.id` / `owner.name` | The single predefined profile returned by `GET /api/owner` | `owner` / Максим Якунин |
| `schedule.timezone` | Timezone the working hours and slot grid are computed in | `Europe/Moscow` |
| `schedule.working-days` | Days of the week the owner takes meetings | Mon–Fri |
| `schedule.open` / `schedule.close` | Daily working hours | `09:00` / `18:00` |
| `booking.window-days` | Length of the booking window, starting today | `14` |
| `cors.allowed-origins` | Origins allowed to call the API (the frontend dev server) | `http://localhost:5173` |
| `seed.event-types` | Event types the in-memory repository starts with | three defaults, see above |

## Errors

Decisions are made on the machine-readable `code` from `ApiError`, never on the `message` text. Every
`ErrorCode` the contract defines (spec §6.3), the status it carries, and the operation it arises from:

| `code` | HTTP status | Arises from |
| --- | --- | --- |
| `validation_failed` | 400 | `POST /api/event-types`, `POST /api/bookings` — invalid or unparsable request body |
| `event_type_not_found` | 400 | `POST /api/bookings` — `eventTypeId` does not exist (contract-mandated: `BadRequest`, not `NotFound`, for this operation) |
| `event_type_not_found` | 404 | `GET /api/event-types/{id}`, `GET /api/event-types/{id}/slots` — the event type does not exist |
| `event_type_id_taken` | 409 | `POST /api/event-types` — `id` already exists |
| `slot_not_available` | 400 | `POST /api/bookings` — `start` does not fall on a free slot's start |
| `outside_booking_window` | 400 | `POST /api/bookings` — `start` falls outside the 14-day window |
| `slot_taken` | 409 | `POST /api/bookings` — the time is already booked, under any event type |

**Statuses the contract does not define carry Spring's default error body, not `ApiError`.** A 404 for
an unmapped path, 405, 415, 406 and 500 all return Spring's own `{timestamp, status, error, path}`
shape, with no `code` field. A client must branch on the HTTP status for anything the contract does
not list, and must not assume `code` is present there. Only the statuses the contract documents for a
given operation (400/404/409, as tabulated above) are guaranteed to carry a proper `ApiError`.

## Serving the frontend

When the built frontend is present under `classpath:/static/` (as it is inside the Docker image), the
backend serves it: any path that is not an API path (anything under `/api`) and does
not match a built file falls back to `index.html`, so client-side routes like `/book/:eventTypeId`
resolve correctly on a hard reload. During local development that directory is empty, so this handler
finds nothing and the jar stays API-only.

The fallback is unconditional — it is not gated on whether the path looks like a file (e.g. by
checking for a dot in the last segment). Client routes can themselves contain a dot (an event-type
slug such as `consultation-2.0`), so an extension-based heuristic would misroute those. The practical
consequence: if a stale `index.html` ever referenced a hashed asset that no longer exists, the browser
would receive that HTML fallback instead of a 404, and report `SyntaxError: Unexpected token '<'` when
trying to parse it as JavaScript. In this project that skew cannot happen — `index.html` and its hashed
assets are built and shipped together in one Docker image, so they never go out of sync — but it is
worth knowing if that error is ever seen while debugging a deployment.

## Commands

Every command is a target in the [`Makefile`](Makefile):

```bash
make help    # list every target
make run     # run the application on port 8080
make build   # generate contract models, compile and package
make test    # tests
make lint    # check formatting (Spotless)
make format  # apply formatting
make clean   # remove build output
make check   # lint + test + build
```

Before considering work done: `make check` must pass.
