package com.example.search.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndexingResultDto {
    private Long documentId;
    private Boolean solrSuccess;
    private Boolean openSearchSuccess;
    private Boolean typeSenseSuccess;
    private String message;
}


