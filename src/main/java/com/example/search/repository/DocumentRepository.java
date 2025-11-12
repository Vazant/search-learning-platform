package com.example.search.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.example.search.model.Document;

/**
 * Основной репозиторий для документов Использует JpaSpecificationExecutor для Criteria API
 * Кастомная логика вынесена в DocumentCustomRepository
 */
@Repository
public interface DocumentRepository
    extends JpaRepository<Document, Long>,
        JpaSpecificationExecutor<Document>,
        DocumentCustomRepository {
  // Базовая функциональность через JpaRepository
  // Кастомная логика фильтрации вынесена в DocumentCustomRepository
  // Specification API используется через JpaSpecificationExecutor
}
