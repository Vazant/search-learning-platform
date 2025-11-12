package com.example.search.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Facet (группировка) для фильтрации результатов поиска
 * Используется для отображения количества документов по категориям, статусам и т.д.
 */
@Data
@Builder
public class FacetDto {
    private String value; // значение (например, "document", "approved")
    private Long count; // количество документов с этим значением
    private String label; // человекочитаемая метка (опционально)
}


