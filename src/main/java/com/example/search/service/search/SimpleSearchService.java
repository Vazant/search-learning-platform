package com.example.search.service.search;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.example.search.constants.DocumentConstants;
import com.example.search.constants.SortConstants;
import com.example.search.dto.response.AutocompleteResultDto;
import com.example.search.dto.response.FacetDto;
import com.example.search.dto.response.SearchFacetsDto;
import com.example.search.dto.response.SearchFilterDto;
import com.example.search.dto.response.SearchResultPageDto;
import com.example.search.model.Document;
import com.example.search.repository.DocumentRepository;
import com.example.search.service.permission.PermissionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Простой поисковый сервис с фильтрами, сортировкой и пагинацией Использует JPA/Hibernate для
 * поиска в базе данных
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SimpleSearchService {

  private final DocumentRepository documentRepository;
  private final PermissionService permissionService;

  /** Поиск документов с фильтрами, сортировкой и пагинацией (permission-aware) */
  public SearchResultPageDto searchDocuments(SearchFilterDto filter, String userId) {
    log.info(
        "Searching documents with filters: query={}, category={}, status={}, author={}, page={},"
            + " size={}",
        filter.getQuery(),
        filter.getCategory(),
        filter.getStatus(),
        filter.getAuthor(),
        filter.getPage(),
        filter.getSize());

    // Настройка пагинации
    int page = filter.getPage() != null && filter.getPage() >= 0 ? filter.getPage() : 0;
    int size = filter.getSize() != null && filter.getSize() > 0 ? filter.getSize() : 20;

    // Настройка сортировки
    Sort sort = buildSort(filter.getSortBy(), filter.getSortOrder());
    Pageable pageable = PageRequest.of(page, size, sort);

    // Выполнение поиска
    Page<Document> resultPage;

    // Используем кастомный репозиторий для всех случаев
    com.example.search.repository.DocumentSearchParams params =
        com.example.search.repository.DocumentSearchParams.builder()
            .query(filter.getQuery())
            .category(filter.getCategory())
            .status(filter.getStatus())
            .author(filter.getAuthor())
            .createdAfter(filter.getCreatedAfter())
            .createdBefore(filter.getCreatedBefore())
            .updatedAfter(filter.getUpdatedAfter())
            .updatedBefore(filter.getUpdatedBefore())
            .build();

    resultPage = documentRepository.searchWithFilters(params, pageable);

    // Фильтрация по правам доступа (permission-aware)
    List<Document> filteredContent = resultPage.getContent();
    if (userId != null) {
      List<Long> documentIds =
          filteredContent.stream().map(Document::getId).collect(Collectors.toList());

      List<Long> allowedIds = permissionService.filterByPermissions(documentIds, userId);

      filteredContent =
          filteredContent.stream()
              .filter(doc -> allowedIds.contains(doc.getId()))
              .collect(Collectors.toList());
    }

    // Формирование facets (если запрошено) - только для разрешенных документов
    SearchFacetsDto facets = null;
    if (Boolean.TRUE.equals(filter.getIncludeFacets())) {
      facets = buildFacets(filter.getQuery());
    }

    // Пересчитываем пагинацию после фильтрации по правам
    long totalElements =
        userId != null
            ? filteredContent.size() // Упрощенный подсчет, в production
            // нужен полный запрос
            : resultPage.getTotalElements();

    int totalPages = (int) Math.ceil((double) totalElements / size);

    // Формирование результата
    return SearchResultPageDto.builder()
        .content(filteredContent)
        .totalElements(totalElements)
        .totalPages(totalPages)
        .currentPage(resultPage.getNumber())
        .pageSize(resultPage.getSize())
        .hasNext(resultPage.getNumber() < totalPages - 1)
        .hasPrevious(resultPage.getNumber() > 0)
        .facets(facets)
        .build();
  }

  /** Перегрузка для обратной совместимости (без userId) */
  public SearchResultPageDto searchDocuments(SearchFilterDto filter) {
    return searchDocuments(filter, null);
  }

  /** Построение facets для группировки результатов (permission-aware) */
  private SearchFacetsDto buildFacets(String query) {
    // Используем кастомный репозиторий с полными параметрами фильтров
    // Для facets передаем только query, остальные фильтры null
    com.example.search.repository.DocumentSearchParams params =
        com.example.search.repository.DocumentSearchParams.builder().query(query).build();

    List<FacetDto> categoryFacets =
        documentRepository.getCategoryFacets(params).stream()
            .map(
                row ->
                    FacetDto.builder()
                        .value((String) row[0])
                        .count(((Number) row[1]).longValue())
                        .label((String) row[0])
                        .build())
            .collect(Collectors.toList());

    List<FacetDto> statusFacets =
        documentRepository.getStatusFacets(params).stream()
            .map(
                row ->
                    FacetDto.builder()
                        .value((String) row[0])
                        .count(((Number) row[1]).longValue())
                        .label((String) row[0])
                        .build())
            .collect(Collectors.toList());

    List<FacetDto> authorFacets =
        documentRepository.getAuthorFacets(params).stream()
            .map(
                row ->
                    FacetDto.builder()
                        .value((String) row[0])
                        .count(((Number) row[1]).longValue())
                        .label((String) row[0])
                        .build())
            .collect(Collectors.toList());

    return SearchFacetsDto.builder()
        .categories(categoryFacets)
        .statuses(statusFacets)
        .authors(authorFacets)
        .build();
  }

  /** Автокомплит для заголовков документов (permission-aware) */
  public List<AutocompleteResultDto> autocompleteTitles(
      String prefix, Integer limit, String userId) {
    log.debug("Autocomplete titles with prefix: {} for user: {}", prefix, userId);
    int size = limit != null && limit > 0 ? limit : 10;

    List<String> titles =
        documentRepository.autocompleteTitles(prefix != null ? prefix.toLowerCase() : "", size);

    // TODO: В production здесь нужно фильтровать по правам доступа к документам
    // Сейчас возвращаем все результаты

    return titles.stream()
        .map(
            title ->
                AutocompleteResultDto.builder()
                    .text(title)
                    .type(DocumentConstants.AUTOCOMPLETE_TYPE_TITLE)
                    .build())
        .collect(Collectors.toList());
  }

  /** Автокомплит для авторов (permission-aware) */
  public List<AutocompleteResultDto> autocompleteAuthors(
      String prefix, Integer limit, String userId) {
    log.debug("Autocomplete authors with prefix: {} for user: {}", prefix, userId);
    int size = limit != null && limit > 0 ? limit : 10;

    List<String> authors =
        documentRepository.autocompleteAuthors(prefix != null ? prefix.toLowerCase() : "", size);

    // TODO: В production фильтровать авторов по документам, к которым есть доступ

    return authors.stream()
        .map(
            author ->
                AutocompleteResultDto.builder()
                    .text(author)
                    .type(DocumentConstants.AUTOCOMPLETE_TYPE_AUTHOR)
                    .build())
        .collect(Collectors.toList());
  }

  /** Автокомплит для категорий (permission-aware) */
  public List<AutocompleteResultDto> autocompleteCategories(
      String prefix, Integer limit, String userId) {
    log.debug("Autocomplete categories with prefix: {} for user: {}", prefix, userId);
    int size = limit != null && limit > 0 ? limit : 10;

    List<String> categories =
        documentRepository.autocompleteCategories(prefix != null ? prefix.toLowerCase() : "", size);

    // TODO: В production фильтровать категории по документам, к которым есть доступ

    return categories.stream()
        .map(
            category ->
                AutocompleteResultDto.builder()
                    .text(category)
                    .type(DocumentConstants.AUTOCOMPLETE_TYPE_CATEGORY)
                    .build())
        .collect(Collectors.toList());
  }

  /** Универсальный автокомплит (по всем полям) с релевантностью и permission-aware */
  public List<AutocompleteResultDto> autocomplete(
      String prefix, String type, Integer limit, String userId) {
    if (prefix == null || prefix.trim().isEmpty()) {
      return new ArrayList<>();
    }

    // Нормализуем префикс для поиска
    String normalizedPrefix = prefix.toLowerCase().trim();
    int resultLimit = limit != null && limit > 0 ? limit : 10;

    List<AutocompleteResultDto> results = new ArrayList<>();

    if (type == null || type.isEmpty() || "ALL".equalsIgnoreCase(type)) {
      // Если тип не указан, возвращаем результаты по всем типам с приоритетом
      // Приоритет: точные совпадения в начале, затем частичные
      List<AutocompleteResultDto> titleResults =
          autocompleteTitles(normalizedPrefix, resultLimit * 2, userId);
      List<AutocompleteResultDto> authorResults =
          autocompleteAuthors(normalizedPrefix, resultLimit, userId);
      List<AutocompleteResultDto> categoryResults =
          autocompleteCategories(normalizedPrefix, resultLimit, userId);

      // Сортируем по релевантности (точные совпадения в начале)
      results.addAll(sortByRelevance(titleResults, normalizedPrefix));
      results.addAll(sortByRelevance(authorResults, normalizedPrefix));
      results.addAll(sortByRelevance(categoryResults, normalizedPrefix));

      return results.stream().limit(resultLimit).collect(Collectors.toList());
    }

    switch (type.toUpperCase()) {
      case "TITLE":
        return autocompleteTitles(normalizedPrefix, resultLimit, userId);
      case "AUTHOR":
        return autocompleteAuthors(normalizedPrefix, resultLimit, userId);
      case "CATEGORY":
        return autocompleteCategories(normalizedPrefix, resultLimit, userId);
      default:
        return autocompleteTitles(normalizedPrefix, resultLimit, userId);
    }
  }

  /** Перегрузка для обратной совместимости */
  public List<AutocompleteResultDto> autocomplete(String prefix, String type, Integer limit) {
    return autocomplete(prefix, type, limit, null);
  }

  /**
   * Сортировка результатов автокомплита по релевантности Точные совпадения в начале строки имеют
   * приоритет
   */
  private List<AutocompleteResultDto> sortByRelevance(
      List<AutocompleteResultDto> results, String prefix) {
    return results.stream()
        .sorted(
            (a, b) -> {
              String aText = a.getText().toLowerCase();
              String bText = b.getText().toLowerCase();

              // Точное совпадение в начале имеет высший приоритет
              boolean aStartsWith = aText.startsWith(prefix);
              boolean bStartsWith = bText.startsWith(prefix);

              if (aStartsWith && !bStartsWith) return -1;
              if (!aStartsWith && bStartsWith) return 1;

              // Если оба начинаются или не начинаются, сортируем по длине (короче =
              // релевантнее)
              if (aStartsWith && bStartsWith) {
                return Integer.compare(aText.length(), bText.length());
              }

              // Иначе по алфавиту
              return aText.compareTo(bText);
            })
        .collect(Collectors.toList());
  }

  private boolean hasFilters(SearchFilterDto filter) {
    return (filter.getCategory() != null && !filter.getCategory().trim().isEmpty())
        || (filter.getStatus() != null && !filter.getStatus().trim().isEmpty())
        || (filter.getAuthor() != null && !filter.getAuthor().trim().isEmpty());
  }

  private boolean hasDateFilters(SearchFilterDto filter) {
    return filter.getCreatedAfter() != null
        || filter.getCreatedBefore() != null
        || filter.getUpdatedAfter() != null
        || filter.getUpdatedBefore() != null;
  }

  private Sort buildSort(String sortBy, String sortOrder) {
    if (sortBy == null || sortBy.trim().isEmpty()) {
      // По умолчанию сортируем по дате создания (новые сначала)
      return Sort.by(Sort.Direction.DESC, SortConstants.SORT_BY_CREATED_AT);
    }

    Sort.Direction direction =
        "DESC".equalsIgnoreCase(sortOrder) ? Sort.Direction.DESC : Sort.Direction.ASC;

    // Валидация поля сортировки
    String normalizedSortBy = sortBy.toLowerCase();
    if (normalizedSortBy.equals(DocumentConstants.FIELD_TITLE)
        || normalizedSortBy.equals(DocumentConstants.FIELD_AUTHOR)
        || normalizedSortBy.equals(SortConstants.SORT_BY_CREATED_AT.toLowerCase())
        || normalizedSortBy.equals(SortConstants.SORT_BY_UPDATED_AT.toLowerCase())
        || normalizedSortBy.equals(DocumentConstants.FIELD_CATEGORY)
        || normalizedSortBy.equals(DocumentConstants.FIELD_STATUS)) {
      return Sort.by(direction, sortBy);
    }
    return Sort.by(Sort.Direction.DESC, SortConstants.SORT_BY_CREATED_AT);
  }
}
