package com.example.search.dto.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

/** DTO для REST API ответа */
@Data
@Builder
public class DocumentDto {
  private Long id;
  private String title;
  private String content;
  private String author;
  private String category;
  private String status;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
