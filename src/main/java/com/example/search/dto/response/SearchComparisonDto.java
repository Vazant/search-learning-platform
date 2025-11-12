package com.example.search.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchComparisonDto {
    private String query;
    private List<SearchResultDto> solrResults;
    private List<SearchResultDto> openSearchResults;
    private List<SearchResultDto> typeSenseResults;
    private Integer solrTime;
    private Integer openSearchTime;
    private Integer typeSenseTime;
}


