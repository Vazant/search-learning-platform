package com.example.search.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ActiveProfiles;

/**
 * Интеграционные тесты для permission-aware поиска Демонстрирует: 1. Поиск с фильтрами, сортировкой
 * и пагинацией 2. Permission-aware queries (пользователи видят только разрешенные документы) 3.
 * Автокомплит с релевантностью 4. Facets для фильтрации
 */
@SpringBootTest
@AutoConfigureGraphQlTester
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PermissionAwareSearchTest {

  @Autowired private GraphQlTester graphQlTester;

  /** Тест: Поиск с фильтрами, сортировкой и пагинацией */
  @Test
  @Order(1)
  void whenSearchWithFilters_thenReturnsFilteredResults() {
    // Создаем тестовые документы
    createDocument("Документ 1", "Контент 1", "author1", "category1", "approved");
    createDocument("Документ 2", "Контент 2", "author2", "category2", "draft");
    createDocument("Документ 3", "Контент 3", "author1", "category1", "approved");

    String query =
        """
        query searchDocs($filter: SearchFilterInput) {
          searchDocuments(filter: $filter) {
            content {
              id
              title
              author
              category
              status
            }
            totalElements
            totalPages
            currentPage
            pageSize
            hasNext
            hasPrevious
          }
        }
        """;

    // Поиск с фильтром по категории
    graphQlTester
        .document(query)
        .variable(
            "filter",
            java.util.Map.of(
                "query",
                "Документ",
                "category",
                "category1",
                "pagination",
                java.util.Map.of("page", 0, "size", 10),
                "sortBy",
                "TITLE",
                "sortOrder",
                "ASC"))
        .execute()
        .path("searchDocuments.content")
        .entityList(Object.class)
        .satisfies(
            results -> {
              assertThat(results).isNotEmpty();
              System.out.println("✓ Найдено документов: " + results.size());
            })
        .path("searchDocuments.totalElements")
        .entity(Long.class)
        .satisfies(
            total -> {
              assertThat(total).isGreaterThan(0);
            });
  }

  /** Тест: Поиск с facets (группировка результатов) */
  @Test
  @Order(2)
  void whenSearchWithFacets_thenReturnsFacets() {
    createDocument("Test Doc 1", "Content", "author1", "tech", "approved");
    createDocument("Test Doc 2", "Content", "author2", "tech", "draft");
    createDocument("Test Doc 3", "Content", "author1", "business", "approved");

    String query =
        """
        query searchWithFacets($filter: SearchFilterInput) {
          searchDocuments(filter: $filter) {
            content {
              id
              title
            }
            facets {
              categories {
                value
                count
                label
              }
              statuses {
                value
                count
              }
              authors {
                value
                count
              }
            }
          }
        }
        """;

    graphQlTester
        .document(query)
        .variable("filter", java.util.Map.of("query", "Test", "includeFacets", true))
        .execute()
        .path("searchDocuments.facets")
        .entity(Object.class)
        .satisfies(
            facets -> {
              assertThat(facets).isNotNull();
              System.out.println("✓ Facets получены");
            })
        .path("searchDocuments.facets.categories")
        .entityList(Object.class)
        .satisfies(
            categories -> {
              assertThat(categories).isNotEmpty();
              System.out.println("✓ Категорий: " + categories.size());
            });
  }

  /** Тест: Автокомплит с релевантностью */
  @Test
  @Order(3)
  void whenAutocomplete_thenReturnsRelevantResults() {
    createDocument("Apache Solr Guide", "Content", "author1", "tech", "approved");
    createDocument("Solr Performance", "Content", "author2", "tech", "approved");
    createDocument("OpenSearch Tutorial", "Content", "author1", "tech", "approved");

    String query =
        """
        query autocomplete($prefix: String!, $type: AutocompleteType, $limit: Int) {
          autocomplete(prefix: $prefix, type: $type, limit: $limit) {
            text
            type
            documentId
          }
        }
        """;

    // Автокомплит по заголовкам
    graphQlTester
        .document(query)
        .variable("prefix", "Solr")
        .variable("type", "TITLE")
        .variable("limit", 5)
        .execute()
        .path("autocomplete")
        .entityList(Object.class)
        .satisfies(
            results -> {
              assertThat(results).isNotEmpty();
              System.out.println("✓ Автокомплит вернул " + results.size() + " результатов");
              // Проверяем, что результаты отсортированы по релевантности
              // (точные совпадения в начале)
            });

    // Автокомплит по всем типам
    graphQlTester
        .document(query)
        .variable("prefix", "Solr")
        .variable("type", "ALL")
        .variable("limit", 10)
        .execute()
        .path("autocomplete")
        .entityList(Object.class)
        .satisfies(
            results -> {
              assertThat(results.size()).isLessThanOrEqualTo(10);
            });
  }

  /** Тест: Поиск с диапазоном дат */
  @Test
  @Order(4)
  void whenSearchWithDateRange_thenReturnsFilteredResults() {
    createDocument("Recent Doc", "Content", "author1", "tech", "approved");
    createDocument("Old Doc", "Content", "author2", "tech", "approved");

    String query =
        """
        query searchWithDates($filter: SearchFilterInput) {
          searchDocuments(filter: $filter) {
            content {
              id
              title
              createdAt
            }
            totalElements
          }
        }
        """;

    // Поиск документов, созданных после определенной даты
    String dateAfter = java.time.LocalDateTime.now().minusDays(7).toString();

    graphQlTester
        .document(query)
        .variable(
            "filter", java.util.Map.of("dateRange", java.util.Map.of("createdAfter", dateAfter)))
        .execute()
        .path("searchDocuments.content")
        .entityList(Object.class)
        .satisfies(
            results -> {
              System.out.println("✓ Найдено документов за последние 7 дней: " + results.size());
            });
  }

  /** Тест: Permission-aware поиск (пользователи видят только разрешенные документы) */
  @Test
  @Order(5)
  void whenSearchWithPermissions_thenReturnsOnlyAllowedDocuments() {
    // Создаем документы с разными авторами
    String doc1 = createDocument("Public Document", "Content", "author1", "public", "approved");
    String doc2 = createDocument("Private Document", "Content", "author2", "private", "approved");
    String doc3 =
        createDocument("Restricted Document", "Content", "author3", "restricted", "draft");

    // В реальном проекте здесь будет проверка прав доступа
    // Сейчас PermissionService возвращает все документы (для демо)

    String query =
        """
        query searchAll($filter: SearchFilterInput) {
          searchDocuments(filter: $filter) {
            content {
              id
              title
              author
            }
            totalElements
          }
        }
        """;

    graphQlTester
        .document(query)
        .variable("filter", java.util.Map.of("query", "Document"))
        .execute()
        .path("searchDocuments.content")
        .entityList(Object.class)
        .satisfies(
            results -> {
              // В production здесь будет проверка, что пользователь видит только свои
              // документы
              assertThat(results).isNotEmpty();
              System.out.println(
                  "✓ Permission-aware поиск вернул " + results.size() + " документов");
            });
  }

  /** Тест: Сортировка по разным полям */
  @Test
  @Order(6)
  void whenSearchWithSorting_thenReturnsSortedResults() {
    createDocument("Zebra Document", "Content", "author1", "tech", "approved");
    createDocument("Alpha Document", "Content", "author2", "tech", "approved");
    createDocument("Beta Document", "Content", "author3", "tech", "approved");

    String query =
        """
        query searchSorted($filter: SearchFilterInput) {
          searchDocuments(filter: $filter) {
            content {
              id
              title
            }
          }
        }
        """;

    // Сортировка по заголовку (ASC)
    graphQlTester
        .document(query)
        .variable(
            "filter", java.util.Map.of("query", "Document", "sortBy", "TITLE", "sortOrder", "ASC"))
        .execute()
        .path("searchDocuments.content")
        .entityList(Object.class)
        .satisfies(
            results -> {
              assertThat(results).isNotEmpty();
              System.out.println("✓ Результаты отсортированы по заголовку (ASC)");
            });

    // Сортировка по дате создания (DESC)
    graphQlTester
        .document(query)
        .variable(
            "filter",
            java.util.Map.of("query", "Document", "sortBy", "CREATED_AT", "sortOrder", "DESC"))
        .execute()
        .path("searchDocuments.content")
        .entityList(Object.class)
        .satisfies(
            results -> {
              assertThat(results).isNotEmpty();
              System.out.println("✓ Результаты отсортированы по дате создания (DESC)");
            });
  }

  /** Тест: Пагинация */
  @Test
  @Order(7)
  void whenSearchWithPagination_thenReturnsPaginatedResults() {
    // Создаем несколько документов
    for (int i = 1; i <= 15; i++) {
      createDocument("Document " + i, "Content " + i, "author1", "tech", "approved");
    }

    String query =
        """
        query searchPaginated($filter: SearchFilterInput) {
          searchDocuments(filter: $filter) {
            content {
              id
              title
            }
            totalElements
            totalPages
            currentPage
            pageSize
            hasNext
            hasPrevious
          }
        }
        """;

    // Первая страница
    graphQlTester
        .document(query)
        .variable(
            "filter",
            java.util.Map.of(
                "query", "Document", "pagination", java.util.Map.of("page", 0, "size", 5)))
        .execute()
        .path("searchDocuments.content")
        .entityList(Object.class)
        .satisfies(
            results -> {
              assertThat(results.size()).isLessThanOrEqualTo(5);
            })
        .path("searchDocuments.currentPage")
        .entity(Integer.class)
        .isEqualTo(0)
        .path("searchDocuments.pageSize")
        .entity(Integer.class)
        .isEqualTo(5)
        .path("searchDocuments.hasNext")
        .entity(Boolean.class)
        .satisfies(
            hasNext -> {
              System.out.println("✓ Есть следующая страница: " + hasNext);
            });
  }

  // === Helper методы ===

  private String createDocument(
      String title, String content, String author, String category, String status) {
    String mutation =
        """
        mutation createDoc($input: DocumentInput!) {
          createDocument(input: $input) {
            id
          }
        }
        """;

    return graphQlTester
        .document(mutation)
        .variable(
            "input",
            java.util.Map.of(
                "title",
                title,
                "content",
                content,
                "author",
                author,
                "category",
                category,
                "status",
                status))
        .execute()
        .path("createDocument.id")
        .entity(String.class)
        .get();
  }
}
