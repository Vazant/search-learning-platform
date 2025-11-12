package com.example.search.dto.response;

import java.util.List;

import com.example.search.model.Document;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SearchResultPageDto {
  private List<Document> content; // результаты поиска
  private Long totalElements; // общее количество результатов
  private Integer totalPages; // общее количество страниц
  private Integer currentPage; // текущая страница
  private Integer pageSize; // размер страницы
  private Boolean hasNext; // есть ли следующая страница
  private Boolean hasPrevious; // есть ли предыдущая страница
  private SearchFacetsDto facets; // группировка результатов для фильтрации
}
