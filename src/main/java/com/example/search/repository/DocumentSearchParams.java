package com.example.search.repository;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

/**
 * Параметры поиска для кастомного репозитория Используется для уменьшения количества параметров
 * метода
 */
@Data
@Builder
public class DocumentSearchParams {
  private String query;
  private String category;
  private String status;
  private String author;
  private LocalDateTime createdAfter;
  private LocalDateTime createdBefore;
  private LocalDateTime updatedAfter;
  private LocalDateTime updatedBefore;
}
