package com.example.search.config.data;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.example.search.constants.DocumentConstants;
import com.example.search.dto.request.DocumentInputDto;
import com.example.search.service.document.DocumentService;
import com.example.search.service.search.SearchService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@RequiredArgsConstructor
@Slf4j
@Profile("!test")
public class DataSeeder {
  private final DocumentService documentService;
  private final SearchService searchService;

  @Bean
  CommandLineRunner initDatabase() {
    return args -> {
      log.info("🌱 Инициализация демонстрационных данных...");
      if (!documentService.getAllDocuments().isEmpty()) {
        log.info("⚠️  В базе уже есть документы. Пропускаем создание.");
        return;
      }

      create(
          "Apache Solr: Введение в полнотекстовый поиск",
          "Apache Solr - это мощная open-source платформа для полнотекстового поиска, построенная"
              + " на Apache Lucene. Solr предоставляет RESTful HTTP/JSON API, faceted search, hit"
              + " highlighting, географический поиск, и поддержку rich документов.",
          "Алексей Иванов",
          DocumentConstants.CATEGORY_DOCUMENT,
          DocumentConstants.STATUS_APPROVED);
      create(
          "OpenSearch vs Elasticsearch: Полное сравнение",
          "OpenSearch - это форк Elasticsearch, созданный AWS после изменения лицензии ES. Основные"
              + " преимущества: Apache 2.0 лицензия, активное сообщество, совместимость с"
              + " большинством Elasticsearch плагинов.",
          "Мария Петрова",
          DocumentConstants.CATEGORY_DOCUMENT,
          DocumentConstants.STATUS_APPROVED);
      create(
          "TypeSense: Современная альтернатива Algolia",
          "TypeSense - это typo-tolerant поисковый движок, оптимизированный для скорости. Ключевые"
              + " особенности: поиск за < 50ms, автоматическая коррекция опечаток, динамическая"
              + " фильтрация и faceting.",
          "Дмитрий Соколов",
          DocumentConstants.CATEGORY_DOCUMENT,
          DocumentConstants.STATUS_APPROVED);
      create(
          "GraphQL для Java разработчиков",
          "GraphQL - это язык запросов для вашего API и среда выполнения для этих запросов. Spring"
              + " for GraphQL интегрируется с Spring Boot и предоставляет аннотации @QueryMapping,"
              + " @MutationMapping.",
          "Сергей Кузнецов",
          DocumentConstants.CATEGORY_DOCUMENT,
          DocumentConstants.STATUS_DRAFT);
      create(
          "Docker и Microservices: Best Practices",
          "Containerization с Docker позволяет упаковать приложение со всеми зависимостями. Docker"
              + " Compose упрощает оркестрацию multi-container приложений.",
          "Анна Смирнова",
          DocumentConstants.CATEGORY_ATTACHMENT,
          DocumentConstants.STATUS_PENDING);
      create(
          "CI/CD с GitHub Actions",
          "GitHub Actions - это платформа для автоматизации CI/CD прямо в GitHub. Основные"
              + " концепции: workflows, jobs, actions, secrets для credentials.",
          "Игорь Волков",
          DocumentConstants.CATEGORY_DOCUMENT,
          DocumentConstants.STATUS_APPROVED);
      create(
          "E-commerce поиск: Требования и решения",
          "Для e-commerce критичны: typo tolerance, faceted navigation, autocomplete,"
              + " персонализация результатов, и скорость < 100ms.",
          "Елена Новикова",
          DocumentConstants.CATEGORY_TASK,
          DocumentConstants.STATUS_APPROVED);
      create(
          "Log Analysis с OpenSearch",
          "OpenSearch отлично подходит для centralized logging. Архитектура: Fluentd/Logstash"
              + " собирают логи → OpenSearch индексирует → Dashboards визуализирует.",
          "Владимир Попов",
          DocumentConstants.CATEGORY_DOCUMENT,
          DocumentConstants.STATUS_APPROVED);
      create(
          "Search as a Service: SaaS vs Self-hosted",
          "Выбор между SaaS (Algolia, Elastic Cloud) и self-hosted (Solr, OpenSearch, TypeSense):"
              + " SaaS плюсы - zero ops, SLA, автоскейлинг.",
          "Ольга Лебедева",
          DocumentConstants.CATEGORY_APPROVAL,
          DocumentConstants.STATUS_PENDING);
      create(
          "Java 17 LTS: Новые возможности",
          "Java 17 - это Long Term Support release с поддержкой до 2029. Ключевые features:"
              + " Records, Sealed Classes, Pattern Matching, Text Blocks, Switch Expressions.",
          "Николай Морозов",
          DocumentConstants.CATEGORY_DOCUMENT,
          DocumentConstants.STATUS_APPROVED);
      create(
          "Spring Boot 3: Миграция и новые возможности",
          "Spring Boot 3 требует Java 17+ и использует Jakarta EE вместо Java EE. Основные"
              + " изменения: поддержка GraalVM Native Image, улучшенная производительность.",
          "Алексей Иванов",
          DocumentConstants.CATEGORY_DOCUMENT,
          DocumentConstants.STATUS_APPROVED);
      create(
          "REST API Design: Best Practices",
          "REST API должен следовать принципам: использование правильных HTTP методов, стандартные"
              + " коды ответов, версионирование через URL или заголовки.",
          "Мария Петрова",
          DocumentConstants.CATEGORY_DOCUMENT,
          DocumentConstants.STATUS_APPROVED);
      create(
          "PostgreSQL vs MySQL: Выбор базы данных",
          "PostgreSQL - объектно-реляционная СУБД с поддержкой JSON, массивов, полнотекстового"
              + " поиска. MySQL - популярная реляционная СУБД.",
          "Дмитрий Соколов",
          DocumentConstants.CATEGORY_DOCUMENT,
          DocumentConstants.STATUS_DRAFT);
      create(
          "Kubernetes для начинающих",
          "Kubernetes - оркестратор контейнеров для автоматизации развертывания, масштабирования и"
              + " управления. Основные концепции: Pod, Service, Deployment.",
          "Сергей Кузнецов",
          DocumentConstants.CATEGORY_DOCUMENT,
          DocumentConstants.STATUS_APPROVED);
      create(
          "Тестирование REST API с JUnit и MockMvc",
          "Spring Boot Test предоставляет MockMvc для тестирования контроллеров без запуска"
              + " сервера. JUnit 5 и AssertJ упрощают написание читаемых тестов.",
          "Анна Смирнова",
          DocumentConstants.CATEGORY_DOCUMENT,
          DocumentConstants.STATUS_APPROVED);
      create(
          "Рефакторинг legacy кода",
          "Рефакторинг старого кода требует осторожности: сначала покрыть тестами, затем постепенно"
              + " улучшать. Основные техники: извлечение методов.",
          "Игорь Волков",
          DocumentConstants.CATEGORY_TASK,
          DocumentConstants.STATUS_PENDING);
      create(
          "Мониторинг приложений с Prometheus и Grafana",
          "Prometheus собирает метрики через pull модель, Grafana визуализирует данные. Spring Boot"
              + " Actuator предоставляет готовые метрики.",
          "Елена Новикова",
          DocumentConstants.CATEGORY_DOCUMENT,
          DocumentConstants.STATUS_APPROVED);
      create(
          "Security в Spring Boot приложениях",
          "Spring Security предоставляет аутентификацию и авторизацию из коробки. Основные"
              + " компоненты: UserDetailsService, AuthenticationProvider, SecurityFilterChain.",
          "Владимир Попов",
          DocumentConstants.CATEGORY_DOCUMENT,
          DocumentConstants.STATUS_APPROVED);
      create(
          "Redis: Кэширование и очереди",
          "Redis - это in-memory структура данных, используемая как база данных, кэш и message"
              + " broker. Основные use cases: кэширование результатов запросов.",
          "Алексей Иванов",
          DocumentConstants.CATEGORY_DOCUMENT,
          DocumentConstants.STATUS_APPROVED);
      create(
          "MongoDB: NoSQL база данных",
          "MongoDB - документо-ориентированная NoSQL база данных. Хранит данные в формате BSON."
              + " Идеально подходит для: гибких схем, больших объемов данных.",
          "Мария Петрова",
          DocumentConstants.CATEGORY_DOCUMENT,
          DocumentConstants.STATUS_APPROVED);
      create(
          "RabbitMQ: Message Queue система",
          "RabbitMQ - это message broker, реализующий протокол AMQP. Используется для асинхронной"
              + " обработки задач, decoupling микросервисов.",
          "Дмитрий Соколов",
          DocumentConstants.CATEGORY_DOCUMENT,
          DocumentConstants.STATUS_PENDING);
      create(
          "Nginx: Reverse Proxy и Load Balancer",
          "Nginx - высокопроизводительный веб-сервер и reverse proxy. Используется для:"
              + " статического контента, load balancing между серверами.",
          "Сергей Кузнецов",
          DocumentConstants.CATEGORY_DOCUMENT,
          DocumentConstants.STATUS_APPROVED);
      create(
          "Git: Версионирование кода",
          "Git - распределенная система контроля версий. Основные команды: commit, push, pull,"
              + " branch, merge, rebase. Git Flow - популярная модель ветвления.",
          "Анна Смирнова",
          DocumentConstants.CATEGORY_DOCUMENT,
          DocumentConstants.STATUS_APPROVED);
      create(
          "Code Review: Best Practices",
          "Code Review - процесс проверки кода перед merge. Цели: найти баги, улучшить качество"
              + " кода, поделиться знаниями.",
          "Игорь Волков",
          DocumentConstants.CATEGORY_TASK,
          DocumentConstants.STATUS_APPROVED);

      log.info("✅ Создано {} документов", documentService.getAllDocuments().size());
      try {
        searchService.reindexAll();
      } catch (Exception e) {
        log.warn("⚠ Поисковые движки недоступны");
      }
      log.info("🚀 Приложение готово!");
    };
  }

  private void create(String title, String content, String author, String category, String status) {
    DocumentInputDto input = new DocumentInputDto();
    input.setTitle(title);
    input.setContent(content);
    input.setAuthor(author);
    input.setCategory(category);
    input.setStatus(status);
    documentService.createDocument(input);
  }
}
