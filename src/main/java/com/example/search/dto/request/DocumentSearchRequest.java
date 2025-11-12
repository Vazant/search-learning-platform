package com.example.search.dto.request;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request DTO для поиска документов через REST API */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Параметры поиска документов")
public class DocumentSearchRequest {
  @Schema(
      description =
          """
      **Полнотекстовый поиск** - основной параметр для поиска документов по тексту.

      **Как работает:**
      - Ищет подстроку в трех полях одновременно: `title`, `content`, `author`
      - Поиск без учета регистра (находит "Java", "java", "JAVA")
      - Использует SQL LIKE с паттерном `%query%` (ищет подстроку в любом месте)

      **SQL эквивалент:**
      ```sql
      WHERE (
          LOWER(title) LIKE '%query%' OR
          LOWER(content) LIKE '%query%' OR
          LOWER(author) LIKE '%query%'
      )
      ```

      **Важно:**
      - Параметр опциональный - если не указан, возвращаются все документы
      - Можно комбинировать с другими фильтрами (category, status, author, dates)
      """,
      example = "Java")
  private String query;

  @Schema(
      description = "Фильтр по категории (точное совпадение)",
      example = "document",
      allowableValues = {"document", "task", "attachment", "approval"})
  private String category;

  @Schema(
      description = "Фильтр по статусу (точное совпадение)",
      example = "approved",
      allowableValues = {"approved", "pending", "draft"})
  private String status;

  @Schema(description = "Фильтр по автору (LIKE поиск, без учета регистра)", example = "Алексей")
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
      description = "Номер страницы (0-based)",
      example = "0",
      minimum = "0",
      defaultValue = "0")
  @Min(0)
  private Integer page;

  @Schema(
      description = "Размер страницы",
      example = "20",
      minimum = "1",
      maximum = "100",
      defaultValue = "20")
  @Min(1)
  @Max(100)
  private Integer size;

  @Schema(
      description = "Поле для сортировки",
      example = "createdAt",
      allowableValues = {"title", "createdAt", "updatedAt", "author", "category", "status"})
  private String sortBy;

  @Schema(
      description = "Направление сортировки",
      example = "DESC",
      allowableValues = {"ASC", "DESC"},
      defaultValue = "DESC")
  private String sortOrder;
}
