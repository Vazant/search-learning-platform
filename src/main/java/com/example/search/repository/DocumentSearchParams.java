package com.example.search.repository;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Параметры поиска для кастомного репозитория
 * Используется для уменьшения количества параметров метода
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

