package com.example.search.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AutocompleteResultDto {
  private String text; // текст для автокомплита
  private String type; // тип: "title", "author", "category"
  private Long documentId; // ID документа (опционально)
}
