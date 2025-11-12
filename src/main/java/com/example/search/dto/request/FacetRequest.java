package com.example.search.dto.request;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request DTO для получения facets */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Параметры для получения facets")
public class FacetRequest {
  @Schema(
      description = "Полнотекстовый поиск (опционально, для фильтрации перед получением facets)",
      example = "Java")
  private String query;

  @Schema(
      description = "Фильтр по категории (исключая измерение, для которого запрашиваем facet)",
      example = "document",
      allowableValues = {"document", "task", "attachment", "approval"})
  private String category;

  @Schema(
      description = "Фильтр по статусу (исключая измерение, для которого запрашиваем facet)",
      example = "approved",
      allowableValues = {"approved", "pending", "draft"})
  private String status;

  @Schema(
      description = "Фильтр по автору (исключая измерение, для которого запрашиваем facet)",
      example = "Алексей")
  private String author;

  @Schema(description = "Документы созданные после этой даты", example = "2024-01-01T00:00:00")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime createdAfter;

  @Schema(description = "Документы созданные до этой даты", example = "2024-12-31T23:59:59")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime createdBefore;

  @Schema(description = "Документы обновленные после этой даты", example = "2024-01-01T00:00:00")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime updatedAfter;

  @Schema(description = "Документы обновленные до этой даты", example = "2024-12-31T23:59:59")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime updatedBefore;

  @Schema(
      description = "Список измерений для получения facets (через запятую)",
      example = "category,status,author",
      allowableValues = {"category", "status", "author"})
  private List<String> dimensions;
}
