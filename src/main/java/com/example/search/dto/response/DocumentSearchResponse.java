package com.example.search.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Response DTO для поиска документов
 */
@Data
@Builder
public class DocumentSearchResponse {
    private List<DocumentDto> content;
    private Long totalElements;
    private Integer totalPages;
    private Integer currentPage;
    private Integer pageSize;
    private Boolean hasNext;
    private Boolean hasPrevious;
}


