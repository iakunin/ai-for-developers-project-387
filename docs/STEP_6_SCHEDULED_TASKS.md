# Проект: Календарь звонков (продолжение)

> Источник: https://ru.hexlet.io/projects/387/members/53685?step=5
> Шаг 5 — Проверка

## Регулярные задачи по расписанию

Добавим повторяющуюся задачу: она запускается по расписанию и при необходимости вручную. Ночью агент выполняет проверку через Lighthouse CLI, а утром команда смотрит отчет и решает, нужны ли правки.

## Ссылки

- [OpenCode GitHub - Schedule Example](https://opencode.ai/docs/github/#schedule-example)
- [GitHub Actions - Events that trigger workflows](https://docs.github.com/en/actions/using-workflows/events-that-trigger-workflows)
- [Lighthouse CI - Getting started](https://github.com/GoogleChrome/lighthouse-ci/blob/main/docs/getting-started.md)

## Задачи

1. Добавьте повторяющуюся задачу с запуском по расписанию.
2. Добавьте возможность запускать ту же задачу вручную.
3. Настройте запуск Lighthouse CLI и генерацию отчета.
4. Убедитесь, что отчет сохраняется и его можно посмотреть утром.
5. По итогам отчета фиксируйте, какие правки нужны в проекте.

## Результат шага

В проекте работает регулярная проверка с утренним отчетом: команда получает понятный результат и принимает решения о правках.
