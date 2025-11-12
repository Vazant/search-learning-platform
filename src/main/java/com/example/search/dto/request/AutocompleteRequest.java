package com.example.search.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request DTO для автокомплита */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Параметры автокомплита")
public class AutocompleteRequest {
  @Schema(
      description =
          "Префикс для поиска (минимум 2 символа). Начни вводить текст и получи подсказки.",
      example = "Java",
      minLength = 2)
  @NotBlank(message = "Prefix is required")
  @Size(min = 2, message = "Prefix must be at least 2 characters")
  private String prefix;

  @Schema(
      description = "Поле для автокомплита",
      example = "title",
      allowableValues = {"title", "author", "category"})
  @NotBlank(message = "Field is required")
  private String field;

  @Schema(
      description = "Количество результатов (по умолчанию 10, максимум 50)",
      example = "10",
      minimum = "1",
      maximum = "50",
      defaultValue = "10")
  @Min(1)
  @Max(50)
  private Integer limit;
}
