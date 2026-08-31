.DEFAULT_GOAL := help

.PHONY: help run build test lint format clean check

help: ## Show this help
	@grep -E '^[a-z-]+:.*?## ' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "%-12s %s\n", $$1, $$2}'

run: ## Run the application on port 8080
	./gradlew bootRun

build: ## Generate contract models, compile and package
	./gradlew build -x test

test: ## Tests
	./gradlew test

lint: ## Check formatting
	./gradlew spotlessCheck

format: ## Apply formatting
	./gradlew spotlessApply

clean: ## Remove build output
	./gradlew clean

check: lint test build ## Everything that must pass before the work is done
