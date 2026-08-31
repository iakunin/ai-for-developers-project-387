# Changelog

## [0.2.0](https://github.com/iakunin/ai-for-developers-project-387/compare/call-calendar-v0.1.0...call-calendar-v0.2.0) (2026-08-31)


### Features

* add opencode workflows ([3fb9fa7](https://github.com/iakunin/ai-for-developers-project-387/commit/3fb9fa7845f0b4efa0154fda43b549ec54d05753))
* init commit ([3284945](https://github.com/iakunin/ai-for-developers-project-387/commit/32849456feccc05fdf1e450d8d5dcf23a9835398))
* **lighthouse:** add the Lighthouse CI part ([23e7fc7](https://github.com/iakunin/ai-for-developers-project-387/commit/23e7fc702fccc6b588a6a78a2b33bc6030c9a1af))
* **lighthouse:** render the report as a markdown summary ([38d4fb4](https://github.com/iakunin/ai-for-developers-project-387/commit/38d4fb430728b313c22da84a3e67b01be82e3273))

## 0.1.0 (2026-08-31)


### ⚠ BREAKING CHANGES

* every endpoint moved under `/api`. `GET /owner` is now `GET /api/owner`, and likewise for `/event-types` and `/bookings`. Clients pinned to the old paths break, and inside the combined Docker image the old paths fall through to the SPA and return `index.html` rather than a 404.

### Features

* **backend:** add calendar configuration properties and clock bean ([5d70829](https://github.com/iakunin/ai-for-developers-project-386/commit/5d708294f241ab190c578ed472be0a5a0e65d856))
* **backend:** add in-memory storage behind repository interfaces ([38ebdeb](https://github.com/iakunin/ai-for-developers-project-386/commit/38ebdebe6402466e201db525832a86b730e5d085))
* **backend:** enforce booking and event type rules ([e23d3c3](https://github.com/iakunin/ai-for-developers-project-386/commit/e23d3c363832fe661ff1f3bbfc46f55e722fb2d8))
* **backend:** expose the contract operations over http ([bb04c04](https://github.com/iakunin/ai-for-developers-project-386/commit/bb04c042d41c436da80384855c849b634abded2f))
* **backend:** generate free slots from the configured working schedule ([3e0ab10](https://github.com/iakunin/ai-for-developers-project-386/commit/3e0ab10a8e7875b7d1265036cd00fac0f2c86047))
* **backend:** seed demo event types and allow the dev frontend origin ([4364fe2](https://github.com/iakunin/ai-for-developers-project-386/commit/4364fe24acd11767af68b15415d59bbb5c424d84))
* **backend:** serve the built frontend with an spa fallback ([45c2c3d](https://github.com/iakunin/ai-for-developers-project-386/commit/45c2c3d578b48bfa05b74ebd94f26319cfe53742))
* **frontend:** add api client typed from the openapi contract ([1a8065e](https://github.com/iakunin/ai-for-developers-project-386/commit/1a8065ee69fc24a3532e21b9df51d63bc9238fc5))
* **frontend:** add home, booking and admin pages ([abe0ac0](https://github.com/iakunin/ai-for-developers-project-386/commit/abe0ac03ffcc94d032ef8354ee2f4f63af69e959))
* **frontend:** scaffold vite + react + typescript project ([c5aefd9](https://github.com/iakunin/ai-for-developers-project-386/commit/c5aefd9292138ef211a4dc7d1bf4ccaa61be8516))


### Bug Fixes

* **backend:** drop unenforced bean validation and harden seeding ([8ea24c2](https://github.com/iakunin/ai-for-developers-project-386/commit/8ea24c2177ae8b2220141d50b8115691a261847c))
* **backend:** stop labelling server faults as validation errors ([b73f892](https://github.com/iakunin/ai-for-developers-project-386/commit/b73f89249d0240040e25a114736fa7878e286ff0))
* **backend:** stop packaging two jars into the docker image ([916f724](https://github.com/iakunin/ai-for-developers-project-386/commit/916f724fc2a33342b631f58350b541a33b23bdef))
* **backend:** switch to spring generator and test wire format over HTTP ([048e7da](https://github.com/iakunin/ai-for-developers-project-386/commit/048e7da74d9e1f16296889d725f4a3d19db92988))
* **ci:** keep the first release pre-1.0 and allow release-please to label its PR ([36e14b9](https://github.com/iakunin/ai-for-developers-project-386/commit/36e14b955deea32e9822b4abb0109931d5e33363))
* **ci:** pin the first release-please version to 0.1.0 ([bb728b0](https://github.com/iakunin/ai-for-developers-project-386/commit/bb728b0a777ccd91db72ab7049049585e1aea31a))
* **e2e:** anchor the booking date assertion to whole day numbers ([44b6a95](https://github.com/iakunin/ai-for-developers-project-386/commit/44b6a954cc3199b8d3626e32f7b922b5ae4a3c02))
* **e2e:** fail selectDay with a clear error when a day runs out of slots ([4132ed3](https://github.com/iakunin/ai-for-developers-project-386/commit/4132ed34150c66d44b4432c7decd3ca5af6f8fa0))
* **e2e:** read the calendar day number from its own span ([8171956](https://github.com/iakunin/ai-for-developers-project-386/commit/8171956a05054c775ce152faf7d74111081e0e60))
* **e2e:** typecheck in CI, assert the booking date, and harden retries ([1d4960b](https://github.com/iakunin/ai-for-developers-project-386/commit/1d4960bb3cff9016b40effe9a4cbc515f05e1179))


### Code Refactoring

* prefix every api path with /api ([6efc3c5](https://github.com/iakunin/ai-for-developers-project-386/commit/6efc3c5198d560b5c7804210d6d7dbb5e7e721e7))
