package com.example.search.service.search;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.typesense.api.Client;
import org.typesense.api.FieldTypes;
import org.typesense.model.CollectionSchema;
import org.typesense.model.Field;
import org.typesense.model.SearchParameters;
import org.typesense.model.SearchResult;

import com.example.search.dto.response.SearchResultDto;
import com.example.search.model.Document;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Сервис для работы с TypeSense
 *
 * <p>Особенности: - Batch indexing для эффективной индексации - Полная поддержка всех полей модели
 * Document - Правильная обработка ошибок - Автоматическое создание коллекции при старте
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TypeSenseService {

  private final Client typeSenseClient;
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  @Value("${search.typesense.collection:search_demo}")
  private String collection;

  @Value("${search.typesense.batch-size:100}")
  private int batchSize;

  @PostConstruct
  public void init() {
    try {
      // Проверяем, существует ли коллекция
      typeSenseClient.collections(collection).retrieve();
      log.debug("TypeSense collection '{}' already exists", collection);
    } catch (Exception e) {
      // Создаем коллекцию, если её нет
      try {
        List<Field> fields = new ArrayList<>();
        fields.add(new Field().name("id").type(FieldTypes.STRING));
        fields.add(new Field().name("title").type(FieldTypes.STRING));
        fields.add(new Field().name("content").type(FieldTypes.STRING));
        fields.add(new Field().name("author").type(FieldTypes.STRING).optional(true));
        fields.add(new Field().name("category").type(FieldTypes.STRING).optional(true));
        fields.add(new Field().name("status").type(FieldTypes.STRING).optional(true));
        fields.add(new Field().name("created_at").type(FieldTypes.STRING).optional(true));
        fields.add(new Field().name("updated_at").type(FieldTypes.STRING).optional(true));

        CollectionSchema schema = new CollectionSchema();
        schema.name(collection).fields(fields);

        typeSenseClient.collections().create(schema);
        log.info("TypeSense collection '{}' created", collection);
      } catch (Exception createException) {
        log.warn("Failed to create TypeSense collection: {}", createException.getMessage());
      }
    }
  }

  /** Поиск документов в TypeSense */
  public List<SearchResultDto> search(String query) {
    List<SearchResultDto> results = new ArrayList<>();

    try {
      SearchParameters searchParameters =
          new SearchParameters()
              .q(query != null && !query.isEmpty() ? query : "*")
              .queryBy("title,content,author")
              .perPage(10)
              .highlightFullFields("title,content");

      SearchResult result =
          typeSenseClient.collections(collection).documents().search(searchParameters);

      if (result.getHits() != null) {
        for (org.typesense.model.SearchResultHit hit : result.getHits()) {
          Map<String, Object> doc = hit.getDocument();

          SearchResultDto searchResult =
              SearchResultDto.builder()
                  .id((String) doc.get("id"))
                  .title((String) doc.get("title"))
                  .content((String) doc.get("content"))
                  .author((String) doc.get("author"))
                  .score(hit.getTextMatch() != null ? hit.getTextMatch().floatValue() : 0.0f)
                  .engine("TypeSense")
                  .build();
          results.add(searchResult);
        }
      }

      log.debug("TypeSense search for '{}' returned {} results", query, results.size());
    } catch (Exception e) {
      log.error("Error searching in TypeSense: {}", e.getMessage(), e);
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
      Map<String, Object> doc = convertToTypeSenseDocument(document);

      typeSenseClient.collections(collection).documents().upsert(doc);

      log.debug("Document {} indexed in TypeSense", document.getId());
      return true;
    } catch (Exception e) {
      log.warn("TypeSense error indexing document {}: {}", document.getId(), e.getMessage());
      return false;
    }
  }

  /** Batch индексация документов (рекомендуется для production) */
  public int batchIndexDocuments(Collection<Document> documents) {
    if (documents == null || documents.isEmpty()) {
      return 0;
    }

    try {
      List<Map<String, Object>> docs =
          documents.stream().map(this::convertToTypeSenseDocument).collect(Collectors.toList());

      // TypeSense поддерживает batch import через importDocuments
      int indexed = 0;
      for (int i = 0; i < docs.size(); i += batchSize) {
        int end = Math.min(i + batchSize, docs.size());
        List<Map<String, Object>> batch = docs.subList(i, end);

        // TypeSense использует upsert для каждого документа в батче
        for (Map<String, Object> doc : batch) {
          typeSenseClient.collections(collection).documents().upsert(doc);
          indexed++;
        }

        log.debug("Indexed batch of {} documents in TypeSense", batch.size());
      }

      log.info("Successfully indexed {} documents in TypeSense", indexed);
      return indexed;
    } catch (Exception e) {
      log.error("Error during batch indexing in TypeSense: {}", e.getMessage(), e);
      return 0;
    }
  }

  /** Удаление документа по ID */
  public boolean deleteDocument(Long id) {
    if (id == null) {
      return false;
    }

    try {
      typeSenseClient.collections(collection).documents(String.valueOf(id)).delete();

      log.debug("Document {} deleted from TypeSense", id);
      return true;
    } catch (Exception e) {
      log.error("Error deleting document {} from TypeSense: {}", id, e.getMessage(), e);
      return false;
    }
  }

  /** Batch удаление документов */
  public int batchDeleteDocuments(Collection<Long> ids) {
    if (ids == null || ids.isEmpty()) {
      return 0;
    }

    int deleted = 0;
    for (Long id : ids) {
      if (deleteDocument(id)) {
        deleted++;
      }
    }

    log.info("Deleted {} documents from TypeSense", deleted);
    return deleted;
  }

  /** Проверка доступности TypeSense */
  public boolean isAvailable() {
    try {
      typeSenseClient.collections().retrieve();
      return true;
    } catch (Exception e) {
      log.debug("TypeSense health check failed: {}", e.getMessage());
      return false;
    }
  }

  /** Конвертация Document в TypeSense документ */
  private Map<String, Object> convertToTypeSenseDocument(Document document) {
    Map<String, Object> doc = new HashMap<>();

    if (document.getId() != null) {
      doc.put("id", String.valueOf(document.getId()));
    }
    if (document.getTitle() != null) {
      doc.put("title", document.getTitle());
    }
    if (document.getContent() != null) {
      doc.put("content", document.getContent());
    }
    if (document.getAuthor() != null) {
      doc.put("author", document.getAuthor());
    }
    if (document.getCategory() != null) {
      doc.put("category", document.getCategory());
    }
    if (document.getStatus() != null) {
      doc.put("status", document.getStatus());
    }
    if (document.getCreatedAt() != null) {
      doc.put("created_at", document.getCreatedAt().format(DATE_FORMATTER));
    }
    if (document.getUpdatedAt() != null) {
      doc.put("updated_at", document.getUpdatedAt().format(DATE_FORMATTER));
    }

    return doc;
  }
}
