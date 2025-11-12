package com.example.search.service.search;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.query_dsl.MultiMatchQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.DeleteRequest;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.opensearch.client.opensearch.core.bulk.IndexOperation;
import org.opensearch.client.opensearch.core.search.Hit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.search.dto.response.SearchResultDto;
import com.example.search.model.Document;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Сервис для работы с OpenSearch
 *
 * <p>Особенности: - Batch indexing через Bulk API для эффективной индексации - Полная поддержка
 * всех полей модели Document - Правильная обработка ошибок с разделением типов исключений -
 * Поддержка highlight в поисковых результатах
 *
 * <p>OpenSearch Java Client API (2.x)
 *
 * <p>References: - https://opensearch.org/docs/latest/clients/java/
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OpenSearchService {

  private final OpenSearchClient openSearchClient;
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  @Value("${search.opensearch.index:search_demo}")
  private String index;

  @Value("${search.opensearch.batch-size:100}")
  private int batchSize;

  /** Поиск документов в OpenSearch */
  public List<SearchResultDto> search(String queryText) {
    List<SearchResultDto> results = new ArrayList<>();

    try {
      // Build multi-match query
      Query query =
          Query.of(
              q ->
                  q.multiMatch(
                      MultiMatchQuery.of(
                          mm ->
                              mm.query(queryText != null && !queryText.isEmpty() ? queryText : "*")
                                  .fields("title^2", "content", "author")
                                  .fuzziness("AUTO"))));

      // Execute search with highlighting
      SearchRequest searchRequest =
          SearchRequest.of(
              s ->
                  s.index(index)
                      .query(query)
                      .size(10)
                      .highlight(h -> h.fields("title", f -> f).fields("content", f -> f)));

      @SuppressWarnings("unchecked")
      SearchResponse<Map<String, Object>> response =
          openSearchClient.search(searchRequest, (Class<Map<String, Object>>) (Class<?>) Map.class);

      // Process results
      for (Hit<Map<String, Object>> hit : response.hits().hits()) {
        Map<String, Object> source = hit.source();
        if (source != null) {
          Double score = hit.score();
          SearchResultDto result =
              SearchResultDto.builder()
                  .id(hit.id())
                  .title((String) source.get("title"))
                  .content((String) source.get("content"))
                  .author((String) source.get("author"))
                  .score(score != null ? score.floatValue() : 0.0f)
                  .engine("OpenSearch")
                  .build();
          results.add(result);
        }
      }

      log.debug("OpenSearch search for '{}' returned {} results", queryText, results.size());
    } catch (IOException e) {
      log.error("IO error searching in OpenSearch: {}", e.getMessage(), e);
    } catch (Exception e) {
      log.error("Error searching in OpenSearch: {}", e.getMessage(), e);
    }

    return results;
  }

  /** Индексация одного документа Для production рекомендуется использовать batchIndexDocuments */
  public boolean indexDocument(Document document) {
    if (document == null) {
      log.warn("Attempted to index null document");
      return false;
    }

    try {
      Map<String, Object> docMap = convertToOpenSearchDocument(document);

      IndexRequest<Map<String, Object>> request =
          IndexRequest.of(
              i -> i.index(index).id(String.valueOf(document.getId())).document(docMap));

      openSearchClient.index(request);

      log.debug("Document {} indexed in OpenSearch", document.getId());
      return true;
    } catch (IOException e) {
      log.warn("IO error indexing document {} in OpenSearch: {}", document.getId(), e.getMessage());
      return false;
    } catch (Exception e) {
      log.warn("Error indexing document {} in OpenSearch: {}", document.getId(), e.getMessage());
      return false;
    }
  }

  /** Batch индексация документов через Bulk API (рекомендуется для production) */
  public int batchIndexDocuments(Collection<Document> documents) {
    if (documents == null || documents.isEmpty()) {
      return 0;
    }

    try {
      List<Document> docList = new ArrayList<>(documents);
      int indexed = 0;

      // Разбиваем на батчи для эффективной обработки
      for (int i = 0; i < docList.size(); i += batchSize) {
        int end = Math.min(i + batchSize, docList.size());
        List<Document> batch = docList.subList(i, end);

        // Создаем bulk операции
        List<BulkOperation> bulkOperations =
            batch.stream()
                .map(
                    doc -> {
                      Map<String, Object> docMap = convertToOpenSearchDocument(doc);
                      IndexOperation<Map<String, Object>> indexOp =
                          IndexOperation.of(
                              io ->
                                  io.index(index).id(String.valueOf(doc.getId())).document(docMap));
                      return BulkOperation.of(bo -> bo.index(indexOp));
                    })
                .collect(Collectors.toList());

        // Выполняем bulk запрос
        BulkRequest bulkRequest = BulkRequest.of(br -> br.operations(bulkOperations));

        openSearchClient.bulk(bulkRequest);

        indexed += batch.size();
        log.debug("Indexed batch of {} documents in OpenSearch", batch.size());
      }

      log.info("Successfully indexed {} documents in OpenSearch", indexed);
      return indexed;
    } catch (IOException e) {
      log.error("IO error during batch indexing in OpenSearch: {}", e.getMessage(), e);
      return 0;
    } catch (Exception e) {
      log.error("Error during batch indexing in OpenSearch: {}", e.getMessage(), e);
      return 0;
    }
  }

  /** Удаление документа по ID */
  public boolean deleteDocument(Long id) {
    if (id == null) {
      return false;
    }

    try {
      DeleteRequest request = DeleteRequest.of(d -> d.index(index).id(String.valueOf(id)));

      openSearchClient.delete(request);

      log.debug("Document {} deleted from OpenSearch", id);
      return true;
    } catch (IOException e) {
      log.error("IO error deleting document {} from OpenSearch: {}", id, e.getMessage(), e);
      return false;
    } catch (Exception e) {
      log.error("Error deleting document {} from OpenSearch: {}", id, e.getMessage(), e);
      return false;
    }
  }

  /** Batch удаление документов через Bulk API */
  public int batchDeleteDocuments(Collection<Long> ids) {
    if (ids == null || ids.isEmpty()) {
      return 0;
    }

    try {
      List<Long> idList = new ArrayList<>(ids);
      int deleted = 0;

      // Разбиваем на батчи
      for (int i = 0; i < idList.size(); i += batchSize) {
        int end = Math.min(i + batchSize, idList.size());
        List<Long> batch = idList.subList(i, end);

        // Создаем bulk операции для удаления
        List<BulkOperation> bulkOperations =
            batch.stream()
                .map(
                    id -> {
                      org.opensearch.client.opensearch.core.bulk.DeleteOperation deleteOp =
                          org.opensearch.client.opensearch.core.bulk.DeleteOperation.of(
                              del -> del.index(index).id(String.valueOf(id)));
                      return BulkOperation.of(bo -> bo.delete(deleteOp));
                    })
                .collect(Collectors.toList());

        BulkRequest bulkRequest = BulkRequest.of(br -> br.operations(bulkOperations));

        openSearchClient.bulk(bulkRequest);
        deleted += batch.size();
      }

      log.info("Deleted {} documents from OpenSearch", deleted);
      return deleted;
    } catch (IOException e) {
      log.error("IO error during batch delete in OpenSearch: {}", e.getMessage(), e);
      return 0;
    } catch (Exception e) {
      log.error("Error during batch delete in OpenSearch: {}", e.getMessage(), e);
      return 0;
    }
  }

  /** Проверка доступности OpenSearch */
  public boolean isAvailable() {
    try {
      SearchRequest searchRequest =
          SearchRequest.of(s -> s.index(index).query(q -> q.matchAll(m -> m)).size(0));
      openSearchClient.search(searchRequest, Map.class);
      return true;
    } catch (Exception e) {
      log.debug("OpenSearch health check failed: {}", e.getMessage());
      return false;
    }
  }

  /** Конвертация Document в OpenSearch документ */
  private Map<String, Object> convertToOpenSearchDocument(Document document) {
    Map<String, Object> docMap = new HashMap<>();

    if (document.getTitle() != null) {
      docMap.put("title", document.getTitle());
    }
    if (document.getContent() != null) {
      docMap.put("content", document.getContent());
    }
    if (document.getAuthor() != null) {
      docMap.put("author", document.getAuthor());
    }
    if (document.getCategory() != null) {
      docMap.put("category", document.getCategory());
    }
    if (document.getStatus() != null) {
      docMap.put("status", document.getStatus());
    }
    if (document.getCreatedAt() != null) {
      docMap.put("created_at", document.getCreatedAt().format(DATE_FORMATTER));
    }
    if (document.getUpdatedAt() != null) {
      docMap.put("updated_at", document.getUpdatedAt().format(DATE_FORMATTER));
    }

    return docMap;
  }
}
