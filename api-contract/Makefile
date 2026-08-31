.DEFAULT_GOAL := help

.PHONY: help install install-ci build watch check

help: ## Show this help
	@grep -E '^[a-z-]+:.*?## ' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "%-12s %s\n", $$1, $$2}'

install: ## Install dependencies
	npm install

install-ci: ## Install dependencies from the lockfile (CI)
	npm ci

build: ## Compile main.tsp -> openapi/openapi.yaml
	npm run build

watch: ## Recompile on every change to main.tsp
	npm run watch

check: build ## Everything that must pass before the work is done
