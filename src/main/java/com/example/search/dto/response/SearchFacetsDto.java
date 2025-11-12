package com.example.search.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Facets для результатов поиска
 * Позволяет показать пользователю доступные фильтры с количеством результатов
 */
@Data
@Builder
public class SearchFacetsDto {
    private List<FacetDto> categories; // группировка по категориям
    private List<FacetDto> statuses; // группировка по статусам
    private List<FacetDto> authors; // группировка по авторам
}


