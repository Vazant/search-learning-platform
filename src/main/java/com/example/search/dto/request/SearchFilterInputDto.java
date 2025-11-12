package com.example.search.dto.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SearchFilterInputDto {
    private String query;
    private String category;
    private String status;
    private String author;
    private String sortBy; // будет маппиться из enum SortField
    private String sortOrder; // будет маппиться из enum SortOrder
    private PaginationInputDto pagination;
    private DateRangeInputDto dateRange;
    private Boolean includeFacets;
}



