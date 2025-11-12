package com.example.search.service.document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.example.search.constants.DocumentConstants;
import com.example.search.constants.SortConstants;
import com.example.search.dto.request.AutocompleteRequest;
import com.example.search.dto.request.DocumentSearchRequest;
import com.example.search.dto.request.FacetRequest;
import com.example.search.dto.response.AutocompleteResultDto;
import com.example.search.dto.response.DocumentDto;
import com.example.search.dto.response.DocumentSearchResponse;
import com.example.search.dto.response.FacetDto;
import com.example.search.model.Document;
import com.example.search.repository.DocumentRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Сервис для поиска документов через REST API Использует Criteria API через кастомный репозиторий
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Validated
public class DocumentSearchService {

  private final DocumentRepository documentRepository;

  // Белый список полей для сортировки
  private static final List<String> ALLOWED_SORT_FIELDS =
      List.of(
          SortConstants.SORT_BY_TITLE,
          SortConstants.SORT_BY_CREATED_AT,
          SortConstants.SORT_BY_UPDATED_AT,
          SortConstants.SORT_BY_AUTHOR,
          SortConstants.SORT_BY_CATEGORY,
          SortConstants.SORT_BY_STATUS);

  /** Поиск документов с фильтрами, сортировкой и пагинацией GET /documents/search */
  public DocumentSearchResponse searchDocuments(@Valid DocumentSearchRequest request) {
    log.info(
        "Search request: query={}, category={}, status={}, author={}, page={}, size={}",
        request.getQuery(),
        request.getCategory(),
        request.getStatus(),
        request.getAuthor(),
        request.getPage(),
        request.getSize());
    log.info(
        "Date filters: createdAfter={}, createdBefore={}, updatedAfter={}, updatedBefore={}",
        request.getCreatedAfter(),
        request.getCreatedBefore(),
        request.getUpdatedAfter(),
        request.getUpdatedBefore());

    // Настройка пагинации
    int page = request.getPage() != null && request.getPage() >= 0 ? request.getPage() : 0;
    int size = request.getSize() != null && request.getSize() > 0 ? request.getSize() : 20;

    // Настройка сортировки
    Sort sort = buildSort(request.getSortBy(), request.getSortOrder());
    Pageable pageable = PageRequest.of(page, size, sort);

    // Выполнение поиска через кастомный репозиторий
    com.example.search.repository.DocumentSearchParams params =
        com.example.search.repository.DocumentSearchParams.builder()
            .query(request.getQuery())
            .category(request.getCategory())
            .status(request.getStatus())
            .author(request.getAuthor())
            .createdAfter(request.getCreatedAfter())
            .createdBefore(request.getCreatedBefore())
            .updatedAfter(request.getUpdatedAfter())
            .updatedBefore(request.getUpdatedBefore())
            .build();

    Page<Document> resultPage = documentRepository.searchWithFilters(params, pageable);

    // Конвертация в DTO
    List<DocumentDto> content =
        resultPage.getContent().stream().map(this::toDto).collect(Collectors.toList());

    return DocumentSearchResponse.builder()
        .content(content)
        .totalElements(resultPage.getTotalElements())
        .totalPages(resultPage.getTotalPages())
        .currentPage(resultPage.getNumber())
        .pageSize(resultPage.getSize())
        .hasNext(resultPage.hasNext())
        .hasPrevious(resultPage.hasPrevious())
        .build();
  }

  /** Получить facets по запрошенным измерениям GET /documents/facets */
  public Map<String, List<FacetDto>> getFacets(@Valid FacetRequest request) {
    log.info(
        "Facets request: query={}, dimensions={}", request.getQuery(), request.getDimensions());

    Map<String, List<FacetDto>> facets = new HashMap<>();

    if (request.getDimensions() == null || request.getDimensions().isEmpty()) {
      return facets;
    }

    // Получаем facets для каждого запрошенного измерения
    for (String dimension : request.getDimensions()) {
      com.example.search.repository.DocumentSearchParams params =
          com.example.search.repository.DocumentSearchParams.builder()
              .query(request.getQuery())
              .category(request.getCategory())
              .status(request.getStatus())
              .author(request.getAuthor())
              .createdAfter(request.getCreatedAfter())
              .createdBefore(request.getCreatedBefore())
              .updatedAfter(request.getUpdatedAfter())
              .updatedBefore(request.getUpdatedBefore())
              .build();

      List<Object[]> facetData =
          switch (dimension.toLowerCase()) {
            case DocumentConstants.FIELD_CATEGORY -> documentRepository.getCategoryFacets(params);
            case DocumentConstants.FIELD_STATUS -> documentRepository.getStatusFacets(params);
            case DocumentConstants.FIELD_AUTHOR -> documentRepository.getAuthorFacets(params);
            default -> new ArrayList<>();
          };

      List<FacetDto> facetDtos =
          facetData.stream()
              .map(
                  row ->
                      FacetDto.builder()
                          .value((String) row[0])
                          .count(((Number) row[1]).longValue())
                          .label((String) row[0])
                          .build())
              .collect(Collectors.toList());

      facets.put(dimension, facetDtos);
    }

    return facets;
  }

  /** Автокомплит по полю и префиксу GET /documents/autocomplete */
  public List<AutocompleteResultDto> autocomplete(@Valid AutocompleteRequest request) {
    // Валидация префикса (минимум 2 символа)
    if (request.getPrefix() == null || request.getPrefix().trim().length() < 2) {
      throw new IllegalArgumentException("Prefix must be at least 2 characters");
    }

    int limit =
        request.getLimit() != null && request.getLimit() > 0
            ? Math.min(request.getLimit(), 50) // Максимум 50
            : 10; // По умолчанию 10

    log.info(
        "Autocomplete request: field={}, prefix={}, limit={}",
        request.getField(),
        request.getPrefix(),
        limit);

    String normalizedPrefix = request.getPrefix().toLowerCase().trim();
    List<String> results =
        switch (request.getField().toLowerCase()) {
          case DocumentConstants.AUTOCOMPLETE_TYPE_TITLE ->
              documentRepository.autocompleteTitles(normalizedPrefix, limit);
          case DocumentConstants.AUTOCOMPLETE_TYPE_AUTHOR ->
              documentRepository.autocompleteAuthors(normalizedPrefix, limit);
          case DocumentConstants.AUTOCOMPLETE_TYPE_CATEGORY ->
              documentRepository.autocompleteCategories(normalizedPrefix, limit);
          default -> throw new IllegalArgumentException("Unknown field: " + request.getField());
        };

    return results.stream()
        .map(
            text ->
                AutocompleteResultDto.builder()
                    .text(text)
                    .type(request.getField().toLowerCase())
                    .build())
        .collect(Collectors.toList());
  }

  /** Построение Sort с валидацией полей */
  private Sort buildSort(String sortBy, String sortOrder) {
    if (sortBy == null || sortBy.trim().isEmpty()) {
      // По умолчанию сортируем по дате создания (новые сначала)
      return Sort.by(Sort.Direction.DESC, SortConstants.SORT_BY_CREATED_AT);
    }

    // Валидация поля сортировки (белый список)
    String normalizedSortBy = sortBy.trim();
    if (!ALLOWED_SORT_FIELDS.contains(normalizedSortBy)) {
      log.warn("Invalid sort field: {}, using default", normalizedSortBy);
      return Sort.by(Sort.Direction.DESC, SortConstants.SORT_BY_CREATED_AT);
    }

    Sort.Direction direction =
        "DESC".equalsIgnoreCase(sortOrder) ? Sort.Direction.DESC : Sort.Direction.ASC;

    return Sort.by(direction, normalizedSortBy);
  }

  /** Конвертация Document в DTO */
  private DocumentDto toDto(Document document) {
    return DocumentDto.builder()
        .id(document.getId())
        .title(document.getTitle())
        .content(document.getContent())
        .author(document.getAuthor())
        .category(document.getCategory())
        .status(document.getStatus())
        .createdAt(document.getCreatedAt())
        .updatedAt(document.getUpdatedAt())
        .build();
  }
}
