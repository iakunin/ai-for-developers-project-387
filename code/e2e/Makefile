.DEFAULT_GOAL := help

.PHONY: help install install-ci browsers image test test-ui report typecheck check

help: ## Show this help
	@grep -E '^[a-z-]+:.*?## ' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "%-12s %s\n", $$1, $$2}'

install: ## Install dependencies
	npm install

install-ci: ## Install dependencies from the lockfile (CI)
	npm ci

browsers: ## Install the Chromium build Playwright drives
	npm run browsers

image: ## Build the application image the tests run against
	$(MAKE) -C ../.. docker-build

test: image ## Run the e2e scenarios
	npm run test

test-ui: image ## Run the e2e scenarios in the Playwright UI
	npm run test:ui

report: ## Open the last HTML report
	npm run report

typecheck: ## Type check the tests
	npm run typecheck

check: typecheck test ## Everything that must pass before the work is done
