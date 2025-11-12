package com.example.search.dto.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SearchFilterDto {
  private String query; // поисковый запрос
  private String category; // фильтр по категории
  private String status; // фильтр по статусу
  private String author; // фильтр по автору
  private String sortBy; // поле для сортировки: "title", "createdAt", "author"
  private String sortOrder; // "ASC" или "DESC"
  private Integer page; // номер страницы (начиная с 0)
  private Integer size; // размер страницы

  // Поиск по датам
  private LocalDateTime createdAfter; // документы созданные после этой даты
  private LocalDateTime createdBefore; // документы созданные до этой даты
  private LocalDateTime updatedAfter; // документы обновленные после этой даты
  private LocalDateTime updatedBefore; // документы обновленные до этой даты

  // Включить facets в результат
  private Boolean includeFacets; // вернуть группировку результатов
}
