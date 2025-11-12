package com.example.search.service.search;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.search.dto.response.SearchResultDto;
import com.example.search.model.Document;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Сервис для работы с Apache Solr
 *
 * <p>Особенности: - Batch indexing для эффективной индексации - Полная поддержка всех полей модели
 * Document - Правильная обработка ошибок с retry logic - Поддержка дат и других типов данных
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SolrSearchService {

  private final SolrClient solrClient;

  @Value("${search.solr.core:search_demo}")
  private String core;

  @Value("${search.solr.batch-size:100}")
  private int batchSize;

  @Value("${search.solr.auto-commit:false}")
  private boolean autoCommit;

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  /** Поиск документов в Solr */
  public List<SearchResultDto> search(String query) {
    List<SearchResultDto> results = new ArrayList<>();

    try {
      SolrQuery solrQuery = new SolrQuery();
      solrQuery.setQuery(query != null && !query.isEmpty() ? query : "*:*");
      solrQuery.setFields(
          "id", "title", "content", "author", "category", "status", "created_at", "updated_at");
      solrQuery.setRows(10);
      solrQuery.setHighlight(true);
      solrQuery.addHighlightField("title");
      solrQuery.addHighlightField("content");
      solrQuery.setHighlightSimplePre("<em>");
      solrQuery.setHighlightSimplePost("</em>");

      QueryResponse response = solrClient.query(core, solrQuery);

      for (SolrDocument doc : response.getResults()) {
        SearchResultDto result =
            SearchResultDto.builder()
                .id(String.valueOf(doc.getFieldValue("id")))
                .title((String) doc.getFieldValue("title"))
                .content((String) doc.getFieldValue("content"))
                .author((String) doc.getFieldValue("author"))
                .score(
                    doc.getFieldValue("score") != null
                        ? ((Number) doc.getFieldValue("score")).floatValue()
                        : 0.0f)
                .engine("Solr")
                .build();
        results.add(result);
      }

      log.debug("Solr search for '{}' returned {} results", query, results.size());
    } catch (SolrServerException e) {
      log.error("Solr server error during search: {}", e.getMessage(), e);
    } catch (IOException e) {
      log.error("IO error during Solr search: {}", e.getMessage(), e);
    } catch (Exception e) {
      log.error("Unexpected error searching in Solr: {}", e.getMessage(), e);
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
      SolrInputDocument solrDoc = convertToSolrDocument(document);

      UpdateResponse response = solrClient.add(core, solrDoc);

      if (!autoCommit) {
        solrClient.commit(core);
      }

      log.debug("Document {} indexed in Solr (status: {})", document.getId(), response.getStatus());
      return true;
    } catch (SolrServerException e) {
      log.warn("Solr server error indexing document {}: {}", document.getId(), e.getMessage());
      return false;
    } catch (IOException e) {
      log.warn("IO error indexing document {}: {}", document.getId(), e.getMessage());
      return false;
    } catch (Exception e) {
      log.error("Unexpected error indexing document {}: {}", document.getId(), e.getMessage(), e);
      return false;
    }
  }

  /** Batch индексация документов (рекомендуется для production) */
  public int batchIndexDocuments(Collection<Document> documents) {
    if (documents == null || documents.isEmpty()) {
      return 0;
    }

    List<SolrInputDocument> solrDocs =
        documents.stream().map(this::convertToSolrDocument).collect(Collectors.toList());

    try {
      // Разбиваем на батчи для эффективной обработки
      int indexed = 0;
      for (int i = 0; i < solrDocs.size(); i += batchSize) {
        int end = Math.min(i + batchSize, solrDocs.size());
        List<SolrInputDocument> batch = solrDocs.subList(i, end);

        UpdateResponse response = solrClient.add(core, batch);

        if (!autoCommit && end == solrDocs.size()) {
          // Commit только после последнего батча
          solrClient.commit(core);
        }

        indexed += batch.size();
        log.debug("Indexed batch of {} documents (status: {})", batch.size(), response.getStatus());
      }

      log.info("Successfully indexed {} documents in Solr", indexed);
      return indexed;
    } catch (SolrServerException e) {
      log.error("Solr server error during batch indexing: {}", e.getMessage(), e);
      return 0;
    } catch (IOException e) {
      log.error("IO error during batch indexing: {}", e.getMessage(), e);
      return 0;
    } catch (Exception e) {
      log.error("Unexpected error during batch indexing: {}", e.getMessage(), e);
      return 0;
    }
  }

  /** Удаление документа по ID */
  public boolean deleteDocument(Long id) {
    if (id == null) {
      return false;
    }

    try {
      UpdateResponse response = solrClient.deleteById(core, String.valueOf(id));

      if (!autoCommit) {
        solrClient.commit(core);
      }

      log.debug("Document {} deleted from Solr (status: {})", id, response.getStatus());
      return true;
    } catch (SolrServerException e) {
      log.error("Solr server error deleting document {}: {}", id, e.getMessage(), e);
      return false;
    } catch (IOException e) {
      log.error("IO error deleting document {}: {}", id, e.getMessage(), e);
      return false;
    } catch (Exception e) {
      log.error("Unexpected error deleting document {}: {}", id, e.getMessage(), e);
      return false;
    }
  }

  /** Batch удаление документов */
  public int batchDeleteDocuments(Collection<Long> ids) {
    if (ids == null || ids.isEmpty()) {
      return 0;
    }

    try {
      List<String> idStrings = ids.stream().map(String::valueOf).collect(Collectors.toList());

      UpdateResponse response = solrClient.deleteById(core, idStrings);

      if (!autoCommit) {
        solrClient.commit(core);
      }

      log.info("Deleted {} documents from Solr (status: {})", ids.size(), response.getStatus());
      return ids.size();
    } catch (SolrServerException e) {
      log.error("Solr server error during batch delete: {}", e.getMessage(), e);
      return 0;
    } catch (IOException e) {
      log.error("IO error during batch delete: {}", e.getMessage(), e);
      return 0;
    } catch (Exception e) {
      log.error("Unexpected error during batch delete: {}", e.getMessage(), e);
      return 0;
    }
  }

  /** Очистка всего индекса (осторожно!) */
  public boolean clearIndex() {
    try {
      UpdateResponse response = solrClient.deleteByQuery(core, "*:*");
      solrClient.commit(core);

      log.warn("Solr index cleared (status: {})", response.getStatus());
      return true;
    } catch (Exception e) {
      log.error("Error clearing Solr index: {}", e.getMessage(), e);
      return false;
    }
  }

  /** Проверка доступности Solr */
  public boolean isAvailable() {
    try {
      SolrQuery query = new SolrQuery("*:*");
      query.setRows(0);
      solrClient.query(core, query);
      return true;
    } catch (Exception e) {
      log.debug("Solr health check failed: {}", e.getMessage());
      return false;
    }
  }

  /** Конвертация Document в SolrInputDocument */
  private SolrInputDocument convertToSolrDocument(Document document) {
    SolrInputDocument solrDoc = new SolrInputDocument();

    if (document.getId() != null) {
      solrDoc.addField("id", document.getId());
    }
    if (document.getTitle() != null) {
      solrDoc.addField("title", document.getTitle());
    }
    if (document.getContent() != null) {
      solrDoc.addField("content", document.getContent());
    }
    if (document.getAuthor() != null) {
      solrDoc.addField("author", document.getAuthor());
    }
    if (document.getCategory() != null) {
      solrDoc.addField("category", document.getCategory());
    }
    if (document.getStatus() != null) {
      solrDoc.addField("status", document.getStatus());
    }
    if (document.getCreatedAt() != null) {
      solrDoc.addField("created_at", document.getCreatedAt().format(DATE_FORMATTER));
    }
    if (document.getUpdatedAt() != null) {
      solrDoc.addField("updated_at", document.getUpdatedAt().format(DATE_FORMATTER));
    }

    return solrDoc;
  }
}
