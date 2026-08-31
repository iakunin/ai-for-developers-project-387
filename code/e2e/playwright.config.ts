import { defineConfig, devices } from '@playwright/test'

/**
 * Тесты идут против собранного образа приложения: фронтенд лежит на classpath бэкенда,
 * поэтому всё отвечает с одного origin и CORS в проверках не участвует.
 */
export default defineConfig({
  testDir: './tests',

  // Хранилище бэкенда — в памяти и общее на весь прогон. Параллельные воркеры
  // перемешали бы бронирования непредсказуемым образом.
  fullyParallel: false,
  workers: 1,

  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? [['github'], ['html', { open: 'never' }]] : 'list',

  use: {
    baseURL: 'http://localhost:8080',
    // Фронтенд показывает моменты времени в часовом поясе браузера, а рабочее расписание
    // бэкенда настроено в Europe/Moscow. Без фиксации раннер на UTC сдвинул бы всё на три часа.
    locale: 'ru-RU',
    timezoneId: 'Europe/Moscow',
    trace: 'on-first-retry',
  },

  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],

  webServer: {
    // Образ собирает `make image`; здесь он только запускается. Сборка внутри webServer
    // не уложилась бы в таймаут запуска.
    command: 'docker run --rm -p 8080:8080 call-calendar',
    // Не `/`: SPA-фолбэк отдаёт index.html раньше, чем поднимется контекст приложения.
    // 200 на /api/owner означает, что бэкенд действительно готов.
    url: 'http://localhost:8080/api/owner',
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
  },
})
