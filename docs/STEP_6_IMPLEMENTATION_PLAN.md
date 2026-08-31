# Step 6 — Nightly Lighthouse audit: implementation plan

## Goal

A recurring job runs at night, audits the built application with Lighthouse CI, stores a report the
team can open in the morning, and records the fixes the report calls for as a GitHub issue.

Maps to the five tasks in `docs/STEP_6_SCHEDULED_TASKS.md`:

| Task | Where it is satisfied |
| --- | --- |
| 1. Recurring task on a schedule | `on.schedule` in `.github/workflows/lighthouse.yml` |
| 2. Same task manually | `on.workflow_dispatch` in the same workflow |
| 3. Lighthouse CLI run + report generation | `code/lighthouse/` (`@lhci/cli autorun`) |
| 4. Report is stored and viewable in the morning | artifact upload (30 days) + job summary table |
| 5. Needed fixes are recorded | opencode step opens a GitHub issue with the findings |

## Decisions (agreed)

- **Target**: the Docker image the project ships (`make -C ../.. docker-build`), started on
  `:8080` — the same artifact `code/e2e` tests. No deployment exists, and the frontend only serves
  real data from behind the backend.
- **Tooling**: `@lhci/cli autorun` (collect → assert → upload), the tool the step's own link
  describes. Multiple runs per URL, budgets in config, HTML + JSON on the filesystem.
- **Report delivery**: artifact + GitHub job summary + an opencode step that turns the JSON into a
  Russian issue with concrete fixes.
- **Gating**: assertions at `warn` level. The nightly job stays green; the report says what
  regressed. A cron that goes red on ordinary score noise gets muted within a week.

## New part: `code/lighthouse/`

Mirrors `code/e2e` — its own `package.json`, `Makefile`, `README.md`, depends on nothing inside
`code/frontend` or `code/backend`, only on the HTTP surface of the running image.

```
code/lighthouse/
  Makefile
  README.md
  package.json
  package-lock.json
  lighthouserc.json
  scripts/summary.mjs
  .gitignore          # .lighthouseci/ and reports/
```

### `lighthouserc.json`

```jsonc
{
  "ci": {
    "collect": {
      "url": [
        "http://localhost:8080/",
        "http://localhost:8080/book",
        "http://localhost:8080/admin"
      ],
      "numberOfRuns": 3,
      "startServerCommand": "docker run --rm --name call-calendar-lighthouse -p 8080:8080 call-calendar",
      "startServerReadyPattern": "Started CallCalendarApplication",
      "startServerReadyTimeout": 120000,
      "settings": { "chromeFlags": "--no-sandbox --headless=new" }
    },
    "assert": {
      "assertions": {
        "categories:performance":  ["warn", { "minScore": 0.9 }],
        "categories:accessibility":["warn", { "minScore": 0.9 }],
        "categories:best-practices":["warn",{ "minScore": 0.9 }],
        "categories:seo":          ["warn", { "minScore": 0.9 }]
      }
    },
    "upload": {
      "target": "filesystem",
      "outputDir": "./reports",
      "reportFilenamePattern": "%%PATHNAME%%-%%DATETIME%%.%%EXTENSION%%"
    }
  }
}
```

Notes:
- `/book/:eventTypeId` is deliberately out of scope for now: the id is dynamic and would need a
  pre-flight `GET /api/event-types`. Three static routes cover the layout, the list and the admin
  screen. Add the booking page later if the report shows the other pages are clean.
- `startServerReadyPattern` matches Spring's own startup line. `code/e2e` waits on
  `GET /api/owner` instead; if the pattern proves brittle, the Makefile can wait on that URL and
  `startServerCommand` drops out.
- Leftover container guard: `make audit` force-removes `call-calendar-lighthouse` before starting,
  so a killed run cannot leave `:8080` occupied.

### `Makefile`

```make
help         # list targets
install      # npm install
install-ci   # npm ci
image        # $(MAKE) -C ../.. docker-build
audit        # image + lhci autorun
summary      # render reports/manifest.json as a markdown table
report       # open the newest HTML report locally
check        # audit
```

Every command in the workflow goes through these targets — nothing calls `npx lhci` directly.

### `scripts/summary.mjs`

Reads `reports/manifest.json` (LHCI writes it for the filesystem target) and prints a markdown
table — URL × performance / accessibility / best-practices / SEO, plus a ✅/⚠️ per row. `make
summary` in CI appends it to `$GITHUB_STEP_SUMMARY`; run locally it just prints.

## Workflow: `.github/workflows/lighthouse.yml`

```yaml
name: lighthouse

on:
  schedule:
    - cron: '0 0 * * *'   # 03:00 Europe/Moscow
  workflow_dispatch:

permissions:
  contents: read
  issues: write

concurrency:
  group: lighthouse
  cancel-in-progress: false
```

Steps, in order:

1. `actions/checkout@v6` (`persist-credentials: false`, as in `opencode-review.yml`).
2. `actions/setup-node@v7`, node 22, npm cache keyed on `code/lighthouse/package-lock.json`.
3. `make install-ci`.
4. `make image` — kept as its own step so the slowest part of the job stays readable in the log.
5. `make audit`.
6. `make summary >> "$GITHUB_STEP_SUMMARY"` — `if: always()`.
7. `actions/upload-artifact@v4`, `lighthouse-report`, path `code/lighthouse/reports/`,
   `retention-days: 30`, `if: always()`.
8. opencode step (see below), `if: always()`.

Two constraints worth stating up front, both GitHub's:
- `schedule` only fires on the **default branch**, so the workflow file has to be on `main` before
  the first nightly run. `workflow_dispatch` is how it gets tested before then.
- GitHub disables cron on a repository with 60 days of no activity, and scheduled runs are queued
  best-effort — a 00:00 UTC job routinely starts several minutes late. Neither matters here.

## The agent step (task 5)

Same shape as `opencode-review.yml`: install the pinned CLI (1.18.25), run
`opencode run --auto --model opencode/big-pickle "$PROMPT"` with `OPENCODE_API_KEY` and
`GITHUB_TOKEN`.

Prompt contract:
- Read `code/lighthouse/reports/manifest.json` and the newest `*.report.json` per URL.
- **Open an issue only when there is something to fix** — any category below its threshold, or a
  failing audit worth acting on. A green night produces no issue; the artifact and the job summary
  already prove the check ran.
- Issue title `Lighthouse: отчёт за <YYYY-MM-DD>`, label `lighthouse`, body in Russian: the scores
  table, then a prioritised list of concrete fixes naming the audit and the affected route, then a
  link to the run (`$GITHUB_SERVER_URL/$GITHUB_REPOSITORY/actions/runs/$GITHUB_RUN_ID`).
- Ground every recommendation in an audit present in the JSON; no invented findings.

The `lighthouse` label has to exist (like `lgtm` / `needs-changes` do). A first step creates it if
missing: `gh label create lighthouse --color ededed --force`.

## Documentation

- `code/lighthouse/README.md` — what it audits, how to run it locally (needs Chrome installed),
  why warn-level assertions, why the container and not a dev server.
- `AGENTS.md` — add `code/lighthouse/` to the repository layout table, add its Makefile to the
  Makefile list, and add a short section next to the E2E one.

## Task decomposition

Each item is one commit, Conventional Commits, in this order:

1. `feat(lighthouse): add the Lighthouse CI part` — `code/lighthouse/` scaffolding: `package.json`,
   lockfile, `Makefile`, `lighthouserc.json`, `.gitignore`.
   **Verify**: `cd code/lighthouse && make install && make audit` produces `reports/manifest.json`
   with four scores per URL and exits 0.
2. `feat(lighthouse): render the report as a markdown summary` — `scripts/summary.mjs` + the
   `summary` target.
   **Verify**: `make summary` prints a table matching the scores in `manifest.json`.
3. `ci: run Lighthouse nightly and on demand` — the workflow up to and including the artifact
   upload.
   **Verify**: `workflow_dispatch` run on the branch is green, the job summary shows the table, the
   artifact downloads and its HTML opens.
4. `ci: file a GitHub issue with the Lighthouse findings` — the opencode step and the label step.
   **Verify**: a manual run on a state with a sub-threshold score opens exactly one Russian issue
   labelled `lighthouse` whose findings match the JSON; a green run opens none.
5. `docs: describe the nightly Lighthouse check` — `code/lighthouse/README.md` and the `AGENTS.md`
   entries.
   **Verify**: `AGENTS.md` layout table and Makefile list name the new part.

## Assumptions to confirm while implementing

- Cron `0 0 * * *` = 03:00 MSK. Change the minute/hour if the team wants the report earlier.
- Score threshold 0.9 for all four categories, warn-level. The first real run tells us whether that
  is the right bar for this app; adjust once and leave it.
- Artifact retention 30 days (e2e uses 7 — a nightly trend is worth keeping longer).

## Out of scope

- Deploying the app anywhere.
- Auditing `/book/:eventTypeId`.
- Trend storage across runs (LHCI server / `temporary-public-storage`).
- Making any code change the first report suggests — that is the next task, driven by the issue.
