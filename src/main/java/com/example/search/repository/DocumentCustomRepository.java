package com.example.search.repository;

import com.example.search.model.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Кастомный репозиторий для расширенной функциональности
 * Использует Criteria API через Specification
 */
public interface DocumentCustomRepository {

    /**
     * Поиск документов с фильтрами
     */
    Page<Document> searchWithFilters(DocumentSearchParams params, Pageable pageable);

    /**
     * Получить facets по категориям с учетом активных фильтров
     */
    List<Object[]> getCategoryFacets(DocumentSearchParams params);

    /**
     * Получить facets по статусам с учетом активных фильтров
     */
    List<Object[]> getStatusFacets(DocumentSearchParams params);

    /**
     * Получить facets по авторам с учетом активных фильтров
     */
    List<Object[]> getAuthorFacets(DocumentSearchParams params);

    /**
     * Автокомплит по title
     */
    List<String> autocompleteTitles(String prefix, int limit);

    /**
     * Автокомплит по author
     */
    List<String> autocompleteAuthors(String prefix, int limit);

    /**
     * Автокомплит по category
     */
    List<String> autocompleteCategories(String prefix, int limit);
}

