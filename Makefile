.DEFAULT_GOAL := help

.PHONY: help docker-build docker-run

help: ## Show this help
	@grep -E '^[a-z-]+:.*?## ' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "%-14s %s\n", $$1, $$2}'

docker-build: ## Build the whole application as one image
	docker build -t call-calendar .

docker-run: ## Run the built image on http://localhost:8080
	docker run --rm -p 8080:8080 call-calendar
