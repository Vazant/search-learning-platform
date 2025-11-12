package com.example.search.dto.request;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DateRangeInputDto {
  private String createdAfter;
  private String createdBefore;
  private String updatedAfter;
  private String updatedBefore;

  // Helper методы для конвертации
  public LocalDateTime getCreatedAfterAsDateTime() {
    return parseDateTime(createdAfter);
  }

  public LocalDateTime getCreatedBeforeAsDateTime() {
    return parseDateTime(createdBefore);
  }

  public LocalDateTime getUpdatedAfterAsDateTime() {
    return parseDateTime(updatedAfter);
  }

  public LocalDateTime getUpdatedBeforeAsDateTime() {
    return parseDateTime(updatedBefore);
  }

  private LocalDateTime parseDateTime(String dateTimeStr) {
    if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
      return null;
    }
    try {
      return LocalDateTime.parse(dateTimeStr);
    } catch (Exception e) {
      return null;
    }
  }
}
