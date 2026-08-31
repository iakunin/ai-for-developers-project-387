# Календарь звонков (продолжение)


[![hexlet-check](https://github.com/iakunin/ai-for-developers-project-387/actions/workflows/hexlet-check.yml/badge.svg)](https://github.com/iakunin/ai-for-developers-project-387/actions)

Интегрируйте работу агентов в GitHub проект

Учебный проект Хекслета: https://ru.hexlet.io/programs/ai-for-developers
Как это должно работать: https://files.hexlet.app/a/2ipc5m

## Стек

- Разное

## Установка

<!-- Опишите установку: клонирование, зависимости, переменные окружения -->

```bash
git clone https://github.com/iakunin/ai-for-developers-project-387.git
cd ai-for-developers-project-387
```

## Использование

<!-- Добавьте примеры запуска и запись asciinema — именно это смотрит работодатель -->

## Conventional Commits

Все коммиты должны соответствовать спецификации [Conventional Commits 1.0.0](https://www.conventionalcommits.org/en/v1.0.0/). История коммитов используется [release-please](https://github.com/googleapis/release-please) для автоматической генерации `CHANGELOG.md` и `version.txt` — коммит, не соответствующий спецификации, молча опускается из changelog.

**Формат:**

```
<type>[optional scope][optional !]: <description>

[optional body]

[optional footer(s)]
```

**Правила:**

- `type` — один из: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `chore`, `revert`.
- `description` — в повелительном наклонении, в нижнем регистре, без точки в конце.
- Скоуп используется, когда изменение ограничено одной областью, например `feat(api-contract): ...`.
- Breaking changes помечаются `!` после type/scope и/или футером `BREAKING CHANGE: <explanation>`.

**Примеры:**

```
feat(api-contract): add booking cancellation endpoint
fix: reject overlapping bookings across event types
docs: add step 3 frontend task description
refactor(frontend)!: drop legacy slots response shape
```

CI-пайплайн [`.github/workflows/conventional-commits.yml`](.github/workflows/conventional-commits.yml) проверяет каждый коммит и название PR на соответствие этим правилам. Подробности, включая протокол для AI-агентов в workflows, см. в [`AGENTS.md`](AGENTS.md).

---

<details>
<summary>Автоматические тесты Хекслета</summary>

Тесты запускаются на каждый коммит. За запуск отвечает файл `.github/workflows/hexlet-check.yml` — не удаляйте и не переименовывайте ни его, ни репозиторий.

</details>

## О Хекслете

[Хекслет](https://ru.hexlet.io/) — школа программирования: авторские программы обучения с практикой, поддержкой наставников и реальными проектами, которые остаются в резюме. Этот репозиторий — один из таких проектов.
