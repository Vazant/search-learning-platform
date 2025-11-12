package com.example.search.graphql;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.example.search.constants.SortConstants;
import com.example.search.dto.request.DateRangeInputDto;
import com.example.search.dto.request.DocumentInputDto;
import com.example.search.dto.request.SearchFilterInputDto;
import com.example.search.dto.response.AutocompleteResultDto;
import com.example.search.dto.response.IndexingResultDto;
import com.example.search.dto.response.ReindexResultDto;
import com.example.search.dto.response.SearchComparisonDto;
import com.example.search.dto.response.SearchFilterDto;
import com.example.search.dto.response.SearchResultDto;
import com.example.search.dto.response.SearchResultPageDto;
import com.example.search.model.Document;
import com.example.search.service.document.DocumentService;
import com.example.search.service.monitoring.SearchMetricsService;
import com.example.search.service.permission.PermissionService;
import com.example.search.service.search.SearchService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class DocumentResolver {

  private final DocumentService documentService;
  private final SearchService searchService;
  private final com.example.search.service.search.UnifiedSearchService unifiedSearchService;
  private final PermissionService permissionService;
  private final SearchMetricsService metricsService;

  /** Получить все документы (с проверкой прав доступа) */
  @QueryMapping
  public List<Document> allDocuments() {
    String userId = GraphQLContext.getCurrentUserId();
    log.info("Fetching all documents for user: {}", userId);

    List<Document> documents = documentService.getAllDocuments();

    // Фильтруем по правам доступа
    List<Long> documentIds = documents.stream().map(Document::getId).collect(Collectors.toList());

    List<Long> allowedIds = permissionService.filterByPermissions(documentIds, userId);

    return documents.stream()
        .filter(doc -> allowedIds.contains(doc.getId()))
        .collect(Collectors.toList());
  }

  /** Получить документ по ID (с проверкой прав доступа) */
  @QueryMapping
  public Document document(@Argument Long id) {
    String userId = GraphQLContext.getCurrentUserId();
    log.info("Fetching document with id: {} for user: {}", id, userId);

    Document document = documentService.getDocumentById(id);

    // Проверка прав доступа
    if (!permissionService.canViewDocument(id, userId)) {
      log.warn("User {} attempted to access document {} without permission", userId, id);
      throw new RuntimeException("Access denied to document: " + id);
    }

    return document;
  }

  /** Поиск документов с фильтрами, сортировкой и пагинацией (permission-aware) */
  @QueryMapping
  public SearchResultPageDto searchDocuments(@Argument SearchFilterInputDto filter) {
    String userId = GraphQLContext.getCurrentUserId();
    long startTime = System.currentTimeMillis();

    log.info(
        "Search request from user {}: query={}, category={}, status={}",
        userId,
        filter != null ? filter.getQuery() : null,
        filter != null ? filter.getCategory() : null,
        filter != null ? filter.getStatus() : null);

    try {
      // Конвертируем новый input в старый DTO (для обратной совместимости)
      SearchFilterDto searchFilter = convertToSearchFilterDto(filter);

      // Используем UnifiedSearchService - объединяет все поисковые движки
      // Стратегия: внешние движки для полнотекстового поиска, JPA для фильтров
      SearchResultPageDto result = unifiedSearchService.searchDocuments(searchFilter, userId);

      long duration = System.currentTimeMillis() - startTime;
      log.info(
          "Search completed in {}ms, found {} documents for user {}",
          duration,
          result.getTotalElements(),
          userId);

      // Записываем метрики
      metricsService.recordQuery("searchDocuments", duration);

      // Логируем медленные запросы (>500ms)
      if (duration > 500) {
        log.warn("Slow search query detected: {}ms for user {}", duration, userId);
      }

      return result;
    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;
      log.error("Search failed after {}ms for user {}: {}", duration, userId, e.getMessage(), e);
      throw e;
    }
  }

  /** Конвертация нового input типа в старый DTO */
  private SearchFilterDto convertToSearchFilterDto(SearchFilterInputDto input) {
    if (input == null) {
      return SearchFilterDto.builder().build();
    }

    SearchFilterDto.SearchFilterDtoBuilder builder =
        SearchFilterDto.builder()
            .query(input.getQuery())
            .category(input.getCategory())
            .status(input.getStatus())
            .author(input.getAuthor())
            .includeFacets(input.getIncludeFacets());

    // Конвертация сортировки
    if (input.getSortBy() != null) {
      // Конвертируем enum в строку (например, "CREATED_AT" -> "createdAt")
      String sortBy = convertSortField(input.getSortBy());
      builder.sortBy(sortBy);
    }

    if (input.getSortOrder() != null) {
      builder.sortOrder(input.getSortOrder());
    }

    // Конвертация пагинации
    if (input.getPagination() != null) {
      builder.page(input.getPagination().getPage());
      builder.size(input.getPagination().getSize());
    }

    // Конвертация диапазона дат
    if (input.getDateRange() != null) {
      DateRangeInputDto dateRange = input.getDateRange();
      builder.createdAfter(dateRange.getCreatedAfterAsDateTime());
      builder.createdBefore(dateRange.getCreatedBeforeAsDateTime());
      builder.updatedAfter(dateRange.getUpdatedAfterAsDateTime());
      builder.updatedBefore(dateRange.getUpdatedBeforeAsDateTime());
    }

    return builder.build();
  }

  /** Конвертация enum SortField в строку для БД */
  private String convertSortField(String sortField) {
    if (sortField == null) return null;

    switch (sortField.toUpperCase()) {
      case "CREATED_AT":
        return SortConstants.SORT_BY_CREATED_AT;
      case "UPDATED_AT":
        return SortConstants.SORT_BY_UPDATED_AT;
      case "TITLE":
        return SortConstants.SORT_BY_TITLE;
      case "AUTHOR":
        return SortConstants.SORT_BY_AUTHOR;
      case "CATEGORY":
        return SortConstants.SORT_BY_CATEGORY;
      case "STATUS":
        return SortConstants.SORT_BY_STATUS;
      case "RELEVANCE":
        return SortConstants.SORT_BY_CREATED_AT; // по умолчанию
      default:
        return SortConstants.SORT_BY_CREATED_AT;
    }
  }

  /** Автокомплит для поиска (оптимизированный, с релевантностью) */
  @QueryMapping
  public List<AutocompleteResultDto> autocomplete(
      @Argument String prefix, @Argument String type, @Argument Integer limit) {
    String userId = GraphQLContext.getCurrentUserId();
    long startTime = System.currentTimeMillis();

    log.info(
        "Autocomplete request from user {}: prefix={}, type={}, limit={}",
        userId,
        prefix,
        type,
        limit);

    try {
      // Используем UnifiedSearchService для автокомплита с relevance tuning
      // Оптимизации: кэширование, relevance scoring, permission-aware
      List<AutocompleteResultDto> results =
          unifiedSearchService.autocomplete(prefix, type, limit, userId);

      long duration = System.currentTimeMillis() - startTime;
      log.info("Autocomplete completed in {}ms, returned {} results", duration, results.size());

      // Записываем метрики
      metricsService.recordQuery("autocomplete", duration);

      return results;
    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;
      log.error("Autocomplete failed after {}ms: {}", duration, e.getMessage(), e);
      throw e;
    }
  }

  @QueryMapping
  public List<SearchResultDto> searchWithSolr(@Argument String query) {
    log.info("Searching with Solr: {}", query);
    return searchService.searchWithSolr(query);
  }

  @QueryMapping
  public List<SearchResultDto> searchWithOpenSearch(@Argument String query) {
    log.info("Searching with OpenSearch: {}", query);
    return searchService.searchWithOpenSearch(query);
  }

  @QueryMapping
  public List<SearchResultDto> searchWithTypeSense(@Argument String query) {
    log.info("Searching with TypeSense: {}", query);
    return searchService.searchWithTypeSense(query);
  }

  @QueryMapping
  public SearchComparisonDto compareSearchEngines(@Argument String query) {
    log.info("Comparing search engines for query: {}", query);
    return searchService.compareSearchEngines(query);
  }

  @MutationMapping
  public Document createDocument(@Argument DocumentInputDto input) {
    String userId = GraphQLContext.getCurrentUserId();
    log.info("User {} creating document: {}", userId, input.getTitle());

    // Проверка прав на создание
    if (!permissionService.canCreateDocument(userId, input.getCategory())) {
      throw new RuntimeException(
          "Access denied: cannot create document in category " + input.getCategory());
    }

    return documentService.createDocument(input);
  }

  @MutationMapping
  public Document updateDocument(@Argument Long id, @Argument DocumentInputDto input) {
    String userId = GraphQLContext.getCurrentUserId();
    log.info("User {} updating document with id: {}", userId, id);

    // Проверка прав на редактирование
    if (!permissionService.canEditDocument(id, userId)) {
      throw new RuntimeException("Access denied: cannot edit document " + id);
    }

    return documentService.updateDocument(id, input);
  }

  @MutationMapping
  public Boolean deleteDocument(@Argument Long id) {
    String userId = GraphQLContext.getCurrentUserId();
    log.info("User {} deleting document with id: {}", userId, id);

    // Проверка прав на удаление
    if (!permissionService.canDeleteDocument(id, userId)) {
      throw new RuntimeException("Access denied: cannot delete document " + id);
    }

    return documentService.deleteDocument(id);
  }

  @MutationMapping
  public IndexingResultDto indexDocument(@Argument Long id) {
    log.info("Indexing document with id: {}", id);
    return searchService.indexDocument(id);
  }

  @MutationMapping
  public ReindexResultDto reindexAll() {
    log.info("Reindexing all documents");
    return searchService.reindexAll();
  }
}
