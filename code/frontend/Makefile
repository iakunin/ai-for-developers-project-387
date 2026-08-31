.DEFAULT_GOAL := help

.PHONY: help install install-ci dev mock test test-watch lint typecheck build preview api-types check

help: ## Show this help
	@grep -E '^[a-z-]+:.*?## ' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "%-12s %s\n", $$1, $$2}'

install: ## Install dependencies
	npm install

install-ci: ## Install dependencies from the lockfile (CI)
	npm ci

dev: ## Dev server
	npm run dev

mock: ## Prism mock of the API from the contract (port 4010)
	npm run mock

test: ## Tests
	npm run test

test-watch: ## Tests in watch mode
	npm run test:watch

lint: ## Linter
	npm run lint

typecheck: ## Type check only
	npm run typecheck

build: ## Type check and production build
	npm run build

preview: ## Preview the built app
	npm run preview

api-types: ## Regenerate types from the contract
	npm run api:types

check: lint test build ## Everything that must pass before the work is done
