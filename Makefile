.PHONY: help build run test clean docker-up docker-down install

help: ## Показать помощь
	@echo "Доступные команды:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

install: ## Установить зависимости
	mvn clean install -DskipTests

build: ## Собрать проект
	mvn clean package

run: ## Запустить приложение
	mvn spring-boot:run

test: ## Запустить тесты
	mvn test

docker-up: ## Запустить Docker сервисы
	docker-compose up -d

docker-down: ## Остановить Docker сервисы
	docker-compose down

docker-clean: ## Очистить Docker volumes
	docker-compose down -v

docker-logs: ## Показать логи Docker
	docker-compose logs -f

docker-build: ## Собрать Docker образ приложения
	docker build -t search-learning-platform:latest .

all: docker-up install build ## Полная сборка и запуск

dev: docker-up run ## Режим разработки

clean: docker-down ## Очистить проект
	mvn clean
	rm -rf target/

status: ## Статус сервисов
	@echo "=== Docker Services ==="
	@docker-compose ps
	@echo "\n=== Application Health ==="
	@curl -s http://localhost:8080/actuator/health || echo "Application not running"
	@echo "\n=== Solr Health ==="
	@curl -s http://localhost:8983/solr/admin/ping || echo "Solr not running"
	@echo "\n=== OpenSearch Health ==="
	@curl -s http://localhost:9200/_cluster/health || echo "OpenSearch not running"
	@echo "\n=== TypeSense Health ==="
	@curl -s http://localhost:8108/health || echo "TypeSense not running"


