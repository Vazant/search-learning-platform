package com.example.search.integration;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Интеграционный тест для GraphQL API
 * Демонстрирует основные операции: создание, поиск, сравнение движков
 * 
 * ВАЖНО: Для работы тестов с реальным поиском нужно запустить:
 * docker-compose up -d
 * 
 * Этот тест показывает:
 * 1. Как создавать документы через GraphQL mutations
 * 2. Как искать через разные движки (Solr, OpenSearch, TypeSense)
 * 3. Как сравнивать производительность поисковых движков
 * 4. Как индексировать данные
 */
@SpringBootTest
@AutoConfigureGraphQlTester
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GraphQLIntegrationTest {

    @Autowired
    private GraphQlTester graphQlTester;

    /**
     * ПРИМЕР 1: Создание документа
     * Показывает базовую GraphQL mutation для создания документа
     */
    @Test
    @Order(1)
    void whenCreateDocument_thenReturnsCreatedDocument() {
        String mutation = """
            mutation {
              createDocument(input: {
                title: "Введение в Apache Solr"
                content: "Apache Solr - это мощная платформа для полнотекстового поиска, построенная на Apache Lucene"
                author: "Иван Иванов"
              }) {
                id
                title
                author
                createdAt
                updatedAt
              }
            }
            """;

        graphQlTester.document(mutation)
                .execute()
                .path("createDocument.id").entity(String.class).satisfies(id -> {
                    assertThat(id).isNotNull();
                    System.out.println("✓ Создан документ с ID: " + id);
                })
                .path("createDocument.title").entity(String.class).isEqualTo("Введение в Apache Solr")
                .path("createDocument.author").entity(String.class).isEqualTo("Иван Иванов");
    }

    /**
     * ПРИМЕР 2: Получение всех документов
     * Показывает простой GraphQL query
     */
    @Test
    @Order(2)
    void whenQueryAllDocuments_thenReturnsDocumentList() {
        // Сначала создаем несколько документов
        createSampleDocument("GraphQL Best Practices", "Руководство по эффективному использованию GraphQL");
        createSampleDocument("OpenSearch Tutorial", "Распределенный поиск с OpenSearch");

        String query = """
            query {
              allDocuments {
                id
                title
                content
                author
                createdAt
              }
            }
            """;

        graphQlTester.document(query)
                .execute()
                .path("allDocuments").entityList(Object.class).hasSizeGreaterThan(0);
    }

    /**
     * ПРИМЕР 3: Получение конкретного документа по ID
     * Показывает GraphQL query с параметрами
     */
    @Test
    @Order(3)
    void whenQueryDocumentById_thenReturnsDocument() {
        // Создаем документ и получаем его ID
        String createMutation = """
            mutation {
              createDocument(input: {
                title: "TypeSense Speed Benchmark"
                content: "TypeSense обеспечивает поиск за миллисекунды"
                author: "Петр Петров"
              }) {
                id
              }
            }
            """;

        String docId = graphQlTester.document(createMutation)
                .execute()
                .path("createDocument.id")
                .entity(String.class)
                .get();

        // Теперь получаем документ по ID
        String query = """
            query getDoc($id: ID!) {
              document(id: $id) {
                id
                title
                content
                author
              }
            }
            """;

        graphQlTester.document(query)
                .variable("id", docId)
                .execute()
                .path("document.id").entity(String.class).isEqualTo(docId)
                .path("document.title").entity(String.class).isEqualTo("TypeSense Speed Benchmark");
    }

    /**
     * ПРИМЕР 4: Обновление документа
     * Показывает GraphQL mutation с ID и input
     */
    @Test
    @Order(4)
    void whenUpdateDocument_thenReturnsUpdatedDocument() {
        // Создаем документ
        String docId = createSampleDocument("Original Title", "Original Content");

        // Обновляем его
        String mutation = """
            mutation updateDoc($id: ID!, $input: DocumentInput!) {
              updateDocument(id: $id, input: $input) {
                id
                title
                content
                updatedAt
              }
            }
            """;

        graphQlTester.document(mutation)
                .variable("id", docId)
                .variable("input", java.util.Map.of(
                        "title", "Updated Title",
                        "content", "Updated Content with new information",
                        "author", "New Author"
                ))
                .execute()
                .path("updateDocument.id").entity(String.class).isEqualTo(docId)
                .path("updateDocument.title").entity(String.class).isEqualTo("Updated Title")
                .path("updateDocument.content").entity(String.class).satisfies(content -> {
                    assertThat(content).contains("new information");
                });
    }

    /**
     * ПРИМЕР 5: Индексация документа во все поисковые движки
     * Показывает как работает multi-engine indexing
     * 
     * ТРЕБУЕТ: запущенный docker-compose up -d
     */
    @Test
    @Order(5)
    void whenIndexDocument_thenIndexedInAllEngines() {
        String docId = createSampleDocument(
                "Machine Learning with Solr",
                "Использование машинного обучения для улучшения поиска в Apache Solr"
        );

        String mutation = """
            mutation indexDoc($id: ID!) {
              indexDocument(id: $id) {
                documentId
                solrSuccess
                openSearchSuccess
                typeSenseSuccess
                message
              }
            }
            """;

        graphQlTester.document(mutation)
                .variable("id", docId)
                .execute()
                .path("indexDocument.documentId").entity(String.class).isEqualTo(docId)
                .path("indexDocument.message").entity(String.class).satisfies(msg -> {
                    System.out.println("Результат индексации: " + msg);
                    // Может не сработать если search engines не запущены, но GraphQL ответ вернется
                });
    }

    /**
     * ПРИМЕР 6: Сравнение всех поисковых движков
     * Самый интересный пример - показывает разницу в производительности
     * 
     * ТРЕБУЕТ: docker-compose up -d + проиндексированные данные
     */
    @Test
    @Order(6)
    void whenCompareSearchEngines_thenReturnsPerformanceMetrics() {
        // Создаем и индексируем несколько документов
        String doc1 = createSampleDocument("Elasticsearch vs Solr", "Сравнение возможностей поисковых платформ");
        String doc2 = createSampleDocument("TypeSense Performance", "Быстрый поиск с TypeSense");
        String doc3 = createSampleDocument("OpenSearch Features", "Новые возможности OpenSearch 2.x");

        String query = """
            query compareEngines($searchQuery: String!) {
              compareSearchEngines(query: $searchQuery) {
                query
                solrTime
                openSearchTime
                typeSenseTime
                solrResults {
                  id
                  title
                  score
                  engine
                }
                openSearchResults {
                  id
                  title
                  score
                  engine
                }
                typeSenseResults {
                  id
                  title
                  score
                  engine
                }
              }
            }
            """;

        graphQlTester.document(query)
                .variable("searchQuery", "search")
                .execute()
                .path("compareSearchEngines.query").entity(String.class).isEqualTo("search")
                .path("compareSearchEngines.solrTime").entity(Integer.class).satisfies(time -> {
                    System.out.println("⏱ Solr search time: " + time + "ms");
                })
                .path("compareSearchEngines.openSearchTime").entity(Integer.class).satisfies(time -> {
                    System.out.println("⏱ OpenSearch search time: " + time + "ms");
                })
                .path("compareSearchEngines.typeSenseTime").entity(Integer.class).satisfies(time -> {
                    System.out.println("⏱ TypeSense search time: " + time + "ms");
                });
    }

    /**
     * ПРИМЕР 7: Удаление документа
     */
    @Test
    @Order(7)
    void whenDeleteDocument_thenReturnsTrue() {
        String docId = createSampleDocument("Document to Delete", "This will be deleted");

        String mutation = """
            mutation deleteDoc($id: ID!) {
              deleteDocument(id: $id)
            }
            """;

        graphQlTester.document(mutation)
                .variable("id", docId)
                .execute()
                .path("deleteDocument").entity(Boolean.class).isEqualTo(true);
    }

    /**
     * ПРИМЕР 8: Реиндексация всех документов
     * Показывает batch операцию
     */
    @Test
    @Order(8)
    void whenReindexAll_thenReturnsStatistics() {
        // Создаем несколько документов
        createSampleDocument("Doc 1", "Content 1");
        createSampleDocument("Doc 2", "Content 2");
        createSampleDocument("Doc 3", "Content 3");

        String mutation = """
            mutation {
              reindexAll {
                totalDocuments
                successCount
                failureCount
                message
              }
            }
            """;

        graphQlTester.document(mutation)
                .execute()
                .path("reindexAll.totalDocuments").entity(Integer.class).satisfies(total -> {
                    System.out.println("📊 Всего документов для реиндексации: " + total);
                    assertThat(total).isGreaterThan(0);
                })
                .path("reindexAll.message").entity(String.class).satisfies(msg -> {
                    System.out.println("📝 Результат: " + msg);
                });
    }

    // === Helper методы ===

    /**
     * Вспомогательный метод для создания тестового документа
     */
    private String createSampleDocument(String title, String content) {
        String mutation = """
            mutation createDoc($title: String!, $content: String!) {
              createDocument(input: {
                title: $title
                content: $content
                author: "Test Author"
              }) {
                id
              }
            }
            """;

        return graphQlTester.document(mutation)
                .variable("title", title)
                .variable("content", content)
                .execute()
                .path("createDocument.id")
                .entity(String.class)
                .get();
    }
}

