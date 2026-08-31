// Renders reports/manifest.json as a markdown table. In CI it goes into $GITHUB_STEP_SUMMARY,
// so the team sees the scores without downloading the artifact; locally it just prints.
import { readFileSync } from 'node:fs'

const CATEGORIES = [
  ['performance', 'Performance'],
  ['accessibility', 'Accessibility'],
  ['best-practices', 'Best practices'],
  ['seo', 'SEO'],
]

const configUrl = new URL('../lighthouserc.json', import.meta.url)
const manifestUrl = new URL('../reports/manifest.json', import.meta.url)

// The thresholds live in lighthouserc.json — the same numbers the assertions warn on. Copying
// them here would let the table and the audit disagree about what counts as a regression.
const config = JSON.parse(readFileSync(configUrl, 'utf8'))
const thresholdOf = (category) =>
  config.ci.assert.assertions[`categories:${category}`]?.[1]?.minScore ?? 0

let manifest
try {
  manifest = JSON.parse(readFileSync(manifestUrl, 'utf8'))
} catch {
  console.error('reports/manifest.json не найден. Сначала выполните `make audit`.')
  process.exit(1)
}

// Lighthouse CI runs every URL several times; the representative run is its own choice of the
// median one, so the table shows the same number the assertions were checked against.
const runs = manifest.filter((entry) => entry.isRepresentativeRun)

const cell = (category, score) => {
  const mark = score < thresholdOf(category) ? '⚠️' : '✅'
  return `${mark} ${Math.round(score * 100)}`
}

// Пока порог у всех категорий один, называем его одним числом; если какой-то опустят или
// поднимут отдельно, подпись перечислит категории поимённо, а не оставит четыре числа подряд.
const describeThresholds = () => {
  const percent = (id) => Math.round(thresholdOf(id) * 100)
  const values = new Set(CATEGORIES.map(([id]) => percent(id)))
  if (values.size === 1) return String([...values][0])
  return CATEGORIES.map(([id, title]) => `${title} — ${percent(id)}`).join(', ')
}

const lines = [
  `| Страница | ${CATEGORIES.map(([, title]) => title).join(' | ')} |`,
  `| --- | ${CATEGORIES.map(() => '---').join(' | ')} |`,
  ...runs.map((entry) => {
    const path = new URL(entry.url).pathname
    const scores = CATEGORIES.map(([id]) => cell(id, entry.summary[id]))
    return `| \`${path}\` | ${scores.join(' | ')} |`
  }),
  '',
  `⚠️ — ниже порога: ${describeThresholds()}.`,
]

console.log(lines.join('\n'))
