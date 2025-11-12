package com.example.search.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReindexResultDto {
    private Integer totalDocuments;
    private Integer successCount;
    private Integer failureCount;
    private String message;
}


