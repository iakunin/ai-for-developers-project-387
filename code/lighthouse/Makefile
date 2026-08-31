.DEFAULT_GOAL := help

.PHONY: help install install-ci image audit report check

help: ## Show this help
	@grep -E '^[a-z-]+:.*?## ' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "%-12s %s\n", $$1, $$2}'

install: ## Install dependencies
	npm install

install-ci: ## Install dependencies from the lockfile (CI)
	npm ci

image: ## Build the application image the audit runs against
	$(MAKE) -C ../.. docker-build

# Lighthouse CI starts and stops the container itself. A run killed halfway leaves it behind,
# and the next start then fails on the busy port — so clear it out first.
audit: image ## Audit the built image with Lighthouse CI
	@docker rm -f call-calendar-lighthouse >/dev/null 2>&1 || true
	npm run audit

report: ## Open the newest HTML report
	@f=$$(ls -t reports/*.html | head -1); echo "$$f"; open "$$f" 2>/dev/null || xdg-open "$$f"

check: audit ## Everything that must pass before the work is done
