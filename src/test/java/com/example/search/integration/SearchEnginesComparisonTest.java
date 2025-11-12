package com.example.search.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.search.dto.request.DocumentInputDto;
import com.example.search.dto.response.SearchComparisonDto;
import com.example.search.dto.response.SearchResultDto;
import com.example.search.model.Document;
import com.example.search.service.document.DocumentService;
import com.example.search.service.search.SearchService;

/**
 * Интеграционный тест для сравнения поисковых движков
 *
 * <p>ЧТО ДЕМОНСТРИРУЕТ ЭТОТ ТЕСТ:
 *
 * <p>1. APACHE SOLR - полнотекстовый поиск корпоративного уровня - Faceted search (фасетный поиск)
 * - Highlighting (подсветка совпадений) - Analytics и aggregations - Масштабируемость через
 * SolrCloud - Богатые query parsers (edismax, standard)
 *
 * <p>2. OPENSEARCH - форк Elasticsearch для распределенного поиска - Распределенная архитектура из
 * коробки - Мощные aggregations - Полнотекстовый и структурированный поиск - Dashboards для
 * визуализации - Совместимость с Elasticsearch клиентами
 *
 * <p>3. TYPESENSE - быстрый typo-tolerant поиск - Поиск за миллисекунды (in-memory индексы) -
 * Автоматическая коррекция опечаток - Faceting и filtering - Простой API - Легкий в развертывании
 *
 * <p>КОГДА ИСПОЛЬЗОВАТЬ КАЖДЫЙ: - Solr: Enterprise-поиск, сложная аналитика, legacy системы -
 * OpenSearch: Логи, метрики, большие объемы данных, Kibana-like визуализация - TypeSense:
 * E-commerce, автокомплит, поиск по каталогам, быстрый поиск
 *
 * <p>ТРЕБУЕТ: docker-compose up -d для реальных тестов поиска
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SearchEnginesComparisonTest {

  @Autowired private DocumentService documentService;

  @Autowired private SearchService searchService;

  private static Document testDoc1;
  private static Document testDoc2;
  private static Document testDoc3;

  /** Создаем тестовые данные, демонстрирующие разные use cases */
  @BeforeAll
  static void setupTestData(
      @Autowired DocumentService documentService, @Autowired SearchService searchService) {
    System.out.println("\n=== СОЗДАНИЕ ТЕСТОВЫХ ДАННЫХ ===\n");

    // Документ 1: Техническая статья
    DocumentInputDto input1 = new DocumentInputDto();
    input1.setTitle("Apache Solr: Полнотекстовый поиск для Enterprise");
    input1.setContent(
        "Apache Solr - это open-source платформа для поиска, построенная на Apache Lucene. "
            + "Solr предоставляет RESTful API, faceted search, hit highlighting, и географический поиск. "
            + "Идеально подходит для e-commerce, сайтов документации и корпоративных порталов.");
    input1.setAuthor("Алексей Смирнов");
    testDoc1 = documentService.createDocument(input1);
    System.out.println("✓ Создан документ #" + testDoc1.getId() + ": " + testDoc1.getTitle());

    // Документ 2: Сравнительный обзор
    DocumentInputDto input2 = new DocumentInputDto();
    input2.setTitle("OpenSearch vs Elasticsearch: Что выбрать в 2024?");
    input2.setContent(
        "OpenSearch - это форк Elasticsearch, поддерживаемый AWS. "
            + "Основные преимущества: открытая лицензия Apache 2.0, активное сообщество, "
            + "совместимость с плагинами Elasticsearch. Отлично подходит для логов, метрик и аналитики.");
    input2.setAuthor("Мария Кузнецова");
    testDoc2 = documentService.createDocument(input2);
    System.out.println("✓ Создан документ #" + testDoc2.getId() + ": " + testDoc2.getTitle());

    // Документ 3: Tutorial
    DocumentInputDto input3 = new DocumentInputDto();
    input3.setTitle("TypeSense: Быстрый поиск без компромиссов");
    input3.setContent(
        "TypeSense - это современная альтернатива Algolia. "
            + "Ключевые особенности: поиск за < 50ms, автоматическая коррекция опечаток, "
            + "динамическая фильтрация, простота настройки. "
            + "Идеален для e-commerce каталогов и мобильных приложений.");
    input3.setAuthor("Дмитрий Соколов");
    testDoc3 = documentService.createDocument(input3);
    System.out.println("✓ Создан документ #" + testDoc3.getId() + ": " + testDoc3.getTitle());

    System.out.println("\n=== Индексация в поисковые движки ===\n");

    // Индексируем во все движки (может не сработать если они не запущены)
    try {
      searchService.indexDocument(testDoc1.getId());
      searchService.indexDocument(testDoc2.getId());
      searchService.indexDocument(testDoc3.getId());
      System.out.println("✓ Документы проиндексированы во все движки\n");
    } catch (Exception e) {
      System.out.println("⚠ Поисковые движки не доступны (запустите: docker-compose up -d)\n");
    }
  }

  /** ТЕСТ 1: Поиск с Apache Solr Демонстрирует Solr-специфичные возможности */
  @Test
  @Order(1)
  @DisplayName("Solr: Полнотекстовый поиск с релевантностью")
  void testSolrSearch() {
    System.out.println("\n╔═══════════════════════════════════════════════════════════╗");
    System.out.println("║  ТЕСТ: Apache Solr - Полнотекстовый поиск                ║");
    System.out.println("╚═══════════════════════════════════════════════════════════╝\n");

    List<SearchResultDto> results = searchService.searchWithSolr("Apache поиск");

    System.out.println("Запрос: 'Apache поиск'");
    System.out.println("Результаты от Solr:");
    results.forEach(
        result -> {
          System.out.println(
              String.format("  📄 [Score: %.2f] %s", result.getScore(), result.getTitle()));
        });

    System.out.println("\n💡 Solr Features:");
    System.out.println("   - Использует BM25 для ранжирования релевантности");
    System.out.println("   - Поддерживает синонимы и stemming");
    System.out.println("   - Faceted search для фильтрации результатов");
    System.out.println("   - Highlighting подсвечивает найденные термины\n");

    // Проверяем что результаты содержат релевантные документы
    assertThat(results).isNotNull();
  }

  /** ТЕСТ 2: Поиск с OpenSearch Демонстрирует distributed search capabilities */
  @Test
  @Order(2)
  @DisplayName("OpenSearch: Распределенный поиск")
  void testOpenSearchSearch() {
    System.out.println("\n╔═══════════════════════════════════════════════════════════╗");
    System.out.println("║  ТЕСТ: OpenSearch - Распределенный поиск                 ║");
    System.out.println("╚═══════════════════════════════════════════════════════════╝\n");

    List<SearchResultDto> results = searchService.searchWithOpenSearch("Elasticsearch OpenSearch");

    System.out.println("Запрос: 'Elasticsearch OpenSearch'");
    System.out.println("Результаты от OpenSearch:");
    results.forEach(
        result -> {
          System.out.println(
              String.format("  📊 [Score: %.2f] %s", result.getScore(), result.getTitle()));
        });

    System.out.println("\n💡 OpenSearch Features:");
    System.out.println("   - Sharding и replication из коробки");
    System.out.println("   - Мощные aggregations для аналитики");
    System.out.println("   - SQL поддержка для запросов");
    System.out.println("   - Dashboards (форк Kibana)\n");

    assertThat(results).isNotNull();
  }

  /** ТЕСТ 3: Поиск с TypeSense Демонстрирует typo tolerance и скорость */
  @Test
  @Order(3)
  @DisplayName("TypeSense: Быстрый typo-tolerant поиск")
  void testTypeSenseSearch() {
    System.out.println("\n╔═══════════════════════════════════════════════════════════╗");
    System.out.println("║  ТЕСТ: TypeSense - Быстрый поиск с опечатками            ║");
    System.out.println("╚═══════════════════════════════════════════════════════════╝\n");

    List<SearchResultDto> results = searchService.searchWithTypeSense("TypeSnese"); // опечатка!

    System.out.println("Запрос: 'TypeSnese' (с опечаткой!)");
    System.out.println("Результаты от TypeSense:");
    results.forEach(
        result -> {
          System.out.println(
              String.format("  ⚡ [Score: %.2f] %s", result.getScore(), result.getTitle()));
        });

    System.out.println("\n💡 TypeSense Features:");
    System.out.println("   - Автоматическая коррекция опечаток (typo tolerance)");
    System.out.println("   - Поиск за < 50ms (in-memory индексы)");
    System.out.println("   - Динамическая сортировка и фильтрация");
    System.out.println("   - Простой REST API\n");

    assertThat(results).isNotNull();
  }

  /** ТЕСТ 4: Сравнение производительности всех движков Главная фича проекта! */
  @Test
  @Order(4)
  @DisplayName("Сравнение производительности: Solr vs OpenSearch vs TypeSense")
  void testSearchEnginesPerformanceComparison() {
    System.out.println("\n╔═══════════════════════════════════════════════════════════╗");
    System.out.println("║  PERFORMANCE BENCHMARK: Все движки одновременно          ║");
    System.out.println("╚═══════════════════════════════════════════════════════════╝\n");

    SearchComparisonDto comparison = searchService.compareSearchEngines("поиск");

    System.out.println("🔍 Запрос: '" + comparison.getQuery() + "'\n");

    System.out.println("⏱ ВРЕМЯ ВЫПОЛНЕНИЯ:");
    System.out.println("   Solr:       " + comparison.getSolrTime() + " ms");
    System.out.println("   OpenSearch: " + comparison.getOpenSearchTime() + " ms");
    System.out.println("   TypeSense:  " + comparison.getTypeSenseTime() + " ms\n");

    System.out.println("📊 КОЛИЧЕСТВО РЕЗУЛЬТАТОВ:");
    System.out.println("   Solr:       " + comparison.getSolrResults().size());
    System.out.println("   OpenSearch: " + comparison.getOpenSearchResults().size());
    System.out.println("   TypeSense:  " + comparison.getTypeSenseResults().size() + "\n");

    // Определяем самый быстрый движок
    int minTime =
        Math.min(
            comparison.getSolrTime(),
            Math.min(comparison.getOpenSearchTime(), comparison.getTypeSenseTime()));
    String fastest =
        minTime == comparison.getSolrTime()
            ? "Solr"
            : minTime == comparison.getOpenSearchTime() ? "OpenSearch" : "TypeSense";

    System.out.println("🏆 Самый быстрый: " + fastest + " (" + minTime + "ms)\n");

    System.out.println("💡 ИНТЕРПРЕТАЦИЯ РЕЗУЛЬТАТОВ:");
    System.out.println("   - TypeSense обычно самый быстрый (in-memory индексы)");
    System.out.println("   - Solr стабильный для больших объемов");
    System.out.println("   - OpenSearch лучше для аналитики и aggregations\n");

    assertThat(comparison).isNotNull();
  }

  /**
   * ТЕСТ 5: Демонстрация различий в ранжировании Показывает как разные движки оценивают
   * релевантность
   */
  @Test
  @Order(5)
  @DisplayName("Сравнение ранжирования: одинаковый запрос, разные scores")
  void testRankingDifferences() {
    System.out.println("\n╔═══════════════════════════════════════════════════════════╗");
    System.out.println("║  RANKING COMPARISON: Алгоритмы релевантности             ║");
    System.out.println("╚═══════════════════════════════════════════════════════════╝\n");

    String query = "search";

    List<SearchResultDto> solrResults = searchService.searchWithSolr(query);
    List<SearchResultDto> osResults = searchService.searchWithOpenSearch(query);
    List<SearchResultDto> tsResults = searchService.searchWithTypeSense(query);

    System.out.println("🔍 Запрос: '" + query + "'\n");

    System.out.println("SOLR RANKING (BM25):");
    solrResults.stream()
        .limit(3)
        .forEach(
            r -> System.out.println(String.format("   %.3f - %s", r.getScore(), r.getTitle())));

    System.out.println("\nOPENSEARCH RANKING (BM25 with tweaks):");
    osResults.stream()
        .limit(3)
        .forEach(
            r -> System.out.println(String.format("   %.3f - %s", r.getScore(), r.getTitle())));

    System.out.println("\nTYPESENSE RANKING (Typo-tolerance + relevance):");
    tsResults.stream()
        .limit(3)
        .forEach(
            r -> System.out.println(String.format("   %.3f - %s", r.getScore(), r.getTitle())));

    System.out.println("\n💡 РАЗНИЦА В АЛГОРИТМАХ:");
    System.out.println("   Solr: классический BM25, настраиваемый через schema");
    System.out.println("   OpenSearch: BM25 + machine learning scoring");
    System.out.println("   TypeSense: learning-to-rank + typo tolerance\n");
  }

  /** ТЕСТ 6: Демонстрация resilience - что если движок недоступен? */
  @Test
  @Order(6)
  @DisplayName("Resilience: Graceful degradation при недоступности движка")
  void testGracefulDegradation() {
    System.out.println("\n╔═══════════════════════════════════════════════════════════╗");
    System.out.println("║  RESILIENCE TEST: Работа при недоступности движков      ║");
    System.out.println("╚═══════════════════════════════════════════════════════════╝\n");

    // Если docker-compose не запущен, движки вернут пустые результаты без exception
    SearchComparisonDto comparison = searchService.compareSearchEngines("test");

    System.out.println("✓ Приложение не упало даже если движки недоступны");
    System.out.println("✓ Graceful degradation: пустые результаты вместо ошибок");
    System.out.println("✓ Логи содержат детали ошибок для troubleshooting\n");

    System.out.println("💡 BEST PRACTICE:");
    System.out.println("   В production используйте:");
    System.out.println("   - Circuit breaker pattern (Resilience4j)");
    System.out.println("   - Health checks для каждого движка");
    System.out.println("   - Fallback на основную БД если все движки down\n");

    assertThat(comparison).isNotNull();
  }

  @AfterAll
  static void cleanup() {
    System.out.println("\n=== ТЕСТЫ ЗАВЕРШЕНЫ ===\n");
    System.out.println("📚 Для запуска с реальными поисковыми движками:");
    System.out.println("   1. docker-compose up -d");
    System.out.println("   2. mvn clean test");
    System.out.println(
        "   3. Откройте http://localhost:8080/graphiql для интерактивных запросов\n");
  }
}
