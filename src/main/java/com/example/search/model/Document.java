package com.example.search.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "documents",
    indexes = {
      @Index(name = "idx_category", columnList = "category"),
      @Index(name = "idx_status", columnList = "status"),
      @Index(name = "idx_created_at", columnList = "created_at"),
      @Index(name = "idx_updated_at", columnList = "updated_at"),
      @Index(name = "idx_author", columnList = "author")
    })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String title;

  @Column(length = 5000)
  private String content;

  private String author;

  private String category; // например: "document", "attachment", "approval", "task"

  private String status; // например: "draft", "approved", "pending"

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
