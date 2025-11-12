package com.example.search.dto.request;

import lombok.Data;

@Data
public class DocumentInputDto {
  private String title;
  private String content;
  private String author;
  private String category;
  private String status;
}
