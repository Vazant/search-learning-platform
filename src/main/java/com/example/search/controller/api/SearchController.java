package com.example.search.controller.api;

import com.example.search.dto.request.DocumentInputDto;
import com.example.search.dto.response.DocumentDto;
import com.example.search.dto.response.SearchResultDto;
import com.example.search.dto.response.SearchComparisonDto;
import com.example.search.dto.response.IndexingResultDto;
import com.example.search.dto.response.ReindexResultDto;
import com.example.search.model.Document;
import com.example.search.service.document.DocumentService;
import com.example.search.service.search.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API Controller - альтернатива GraphQL API
 * 
 * ЗАЧЕМ REST API ВМЕСТЕ С GRAPHQL:
 * - GraphQL: для фронтенда с гибкими запросами, один endpoint
 * - REST: для legacy систем, простых CRUD операций, кэширования
 * 
 * ПРИМЕРЫ ИСПОЛЬЗОВАНИЯ:
 * 
 * 1. Создать документ:
 *    POST /api/documents
 *    {
 *      "title": "My Document",
 *      "content": "Content here",
 *      "author": "John Doe"
 *    }
 * 
 * 2. Поиск с Solr:
 *    GET /api/search/solr?q=Apache
 * 
 * 3. Сравнение движков:
 *    GET /api/search/compare?q=search
 * 
 * 4. Индексация:
 *    POST /api/documents/1/index
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Documents & Search Engines", description = "CRUD операции с документами и поиск через внешние движки (Solr, OpenSearch, TypeSense)")
public class SearchController {

    private final DocumentService documentService;
    private final SearchService searchService;

    // === CRUD Операции ===

    @Operation(summary = "Получить все документы", description = "Возвращает список всех документов в системе")
    @ApiResponse(responseCode = "200", description = "Список документов")
    @GetMapping("/documents")
    public ResponseEntity<List<Document>> getAllDocuments() {
        log.info("REST API: Fetching all documents");
        return ResponseEntity.ok(documentService.getAllDocuments());
    }

    @Operation(summary = "Получить документ по ID", description = "Возвращает документ с указанным ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Документ найден"),
            @ApiResponse(responseCode = "404", description = "Документ не найден")
    })
    @GetMapping("/documents/{id}")
    public ResponseEntity<Document> getDocument(
            @Parameter(description = "ID документа", required = true) @PathVariable Long id) {
        log.info("REST API: Fetching document {}", id);
        Document doc = documentService.getDocumentById(id);
        return ResponseEntity.ok(doc);
    }

    @Operation(summary = "Создать документ", description = "Создает новый документ в системе")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Документ успешно создан"),
            @ApiResponse(responseCode = "400", description = "Неверные данные документа")
    })
    @PostMapping("/documents")
    public ResponseEntity<Document> createDocument(
            @Parameter(description = "Данные документа", required = true)
            @RequestBody DocumentInputDto input) {
        log.info("REST API: Creating document '{}'", input.getTitle());
        Document created = documentService.createDocument(input);
        return ResponseEntity.ok(created);
    }

    @Operation(summary = "Обновить документ", description = "Обновляет существующий документ по ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Документ успешно обновлен"),
            @ApiResponse(responseCode = "404", description = "Документ не найден"),
            @ApiResponse(responseCode = "400", description = "Неверные данные")
    })
    @PutMapping("/documents/{id}")
    public ResponseEntity<Document> updateDocument(
            @Parameter(description = "ID документа", required = true) @PathVariable Long id,
            @Parameter(description = "Обновленные данные документа", required = true)
            @RequestBody DocumentInputDto input) {
        log.info("REST API: Updating document {}", id);
        Document updated = documentService.updateDocument(id, input);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Удалить документ", description = "Удаляет документ по ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Документ успешно удален"),
            @ApiResponse(responseCode = "404", description = "Документ не найден")
    })
    @DeleteMapping("/documents/{id}")
    public ResponseEntity<Boolean> deleteDocument(
            @Parameter(description = "ID документа", required = true) @PathVariable Long id) {
        log.info("REST API: Deleting document {}", id);
        Boolean result = documentService.deleteDocument(id);
        return ResponseEntity.ok(result);
    }

    // === Поисковые операции ===

    @Operation(
            summary = "Поиск через Apache Solr",
            description = """
                    Поиск документов с использованием Apache Solr.
                    
                    Возможности Solr:
                    - BM25 ranking algorithm
                    - Faceted search (группировка по категориям)
                    - Hit highlighting (подсветка найденных слов)
                    - Spell checking
                    - More Like This (похожие документы)
                    - Geo spatial search
                    """
    )
    @ApiResponse(responseCode = "200", description = "Результаты поиска")
    @GetMapping("/search/solr")
    public ResponseEntity<List<SearchResultDto>> searchWithSolr(
            @Parameter(description = "Поисковый запрос", required = true, example = "Apache Solr")
            @RequestParam String q) {
        log.info("REST API: Solr search for '{}'", q);
        List<SearchResultDto> results = searchService.searchWithSolr(q);
        return ResponseEntity.ok(results);
    }

    @Operation(
            summary = "Поиск через OpenSearch",
            description = """
                    Поиск документов с использованием OpenSearch.
                    
                    Возможности OpenSearch:
                    - Distributed search across shards
                    - Powerful aggregations (terms, stats, date histogram)
                    - SQL support для запросов
                    - Machine learning features
                    - Anomaly detection
                    - Index lifecycle management
                    """
    )
    @ApiResponse(responseCode = "200", description = "Результаты поиска")
    @GetMapping("/search/opensearch")
    public ResponseEntity<List<SearchResultDto>> searchWithOpenSearch(
            @Parameter(description = "Поисковый запрос", required = true, example = "OpenSearch")
            @RequestParam String q) {
        log.info("REST API: OpenSearch search for '{}'", q);
        List<SearchResultDto> results = searchService.searchWithOpenSearch(q);
        return ResponseEntity.ok(results);
    }

    @Operation(
            summary = "Поиск через TypeSense",
            description = """
                    Поиск документов с использованием TypeSense.
                    
                    Возможности TypeSense:
                    - Typo tolerance (автокоррекция до 2 символов)
                    - Fast search (< 50ms)
                    - Faceting и filtering
                    - Geo search
                    - Prefix search для автокомплита
                    - Простой API
                    """
    )
    @ApiResponse(responseCode = "200", description = "Результаты поиска")
    @GetMapping("/search/typesense")
    public ResponseEntity<List<SearchResultDto>> searchWithTypeSense(
            @Parameter(description = "Поисковый запрос", required = true, example = "TypeSense")
            @RequestParam String q) {
        log.info("REST API: TypeSense search for '{}'", q);
        List<SearchResultDto> results = searchService.searchWithTypeSense(q);
        return ResponseEntity.ok(results);
    }

    @Operation(
            summary = "Сравнение всех поисковых движков",
            description = """
                    Выполняет поиск во всех движках параллельно и сравнивает результаты.
                    
                    Возвращает:
                    - Результаты поиска для каждого движка
                    - Время выполнения каждого запроса
                    - Релевантность (scores)
                    
                    Типичные результаты:
                    - TypeSense: 5-20ms (самый быстрый, in-memory)
                    - Solr: 20-100ms (стабильный, большие индексы)
                    - OpenSearch: 30-150ms (распределенный поиск)
                    """
    )
    @ApiResponse(responseCode = "200", description = "Результаты сравнения")
    @GetMapping("/search/compare")
    public ResponseEntity<SearchComparisonDto> compareSearchEngines(
            @Parameter(description = "Поисковый запрос для сравнения", required = true, example = "search")
            @RequestParam String q) {
        log.info("REST API: Comparing all search engines for '{}'", q);
        SearchComparisonDto comparison = searchService.compareSearchEngines(q);
        
        // Выводим результаты в лог для анализа
        log.info("⏱ Performance results:");
        log.info("   Solr:       {}ms ({} results)", 
                comparison.getSolrTime(), comparison.getSolrResults().size());
        log.info("   OpenSearch: {}ms ({} results)", 
                comparison.getOpenSearchTime(), comparison.getOpenSearchResults().size());
        log.info("   TypeSense:  {}ms ({} results)", 
                comparison.getTypeSenseTime(), comparison.getTypeSenseResults().size());
        
        return ResponseEntity.ok(comparison);
    }

    // === Индексация ===

    @Operation(
            summary = "Индексировать документ",
            description = "Индексирует документ во все поисковые движки (Solr, OpenSearch, TypeSense)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Результат индексации"),
            @ApiResponse(responseCode = "404", description = "Документ не найден")
    })
    @PostMapping("/documents/{id}/index")
    public ResponseEntity<IndexingResultDto> indexDocument(
            @Parameter(description = "ID документа для индексации", required = true) @PathVariable Long id) {
        log.info("REST API: Indexing document {}", id);
        IndexingResultDto result = searchService.indexDocument(id);
        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "Реиндексировать все документы",
            description = "Реиндексирует все документы во все поисковые движки. Может занять время при большом количестве документов."
    )
    @ApiResponse(responseCode = "200", description = "Результат реиндексации")
    @PostMapping("/reindex")
    public ResponseEntity<ReindexResultDto> reindexAll() {
        log.info("REST API: Reindexing all documents");
        ReindexResultDto result = searchService.reindexAll();
        return ResponseEntity.ok(result);
    }

    // === Health & Info endpoints ===

    @Operation(
            summary = "Проверка доступности поисковых движков",
            description = """
                    Проверяет доступность всех поисковых движков (Solr, OpenSearch, TypeSense).
                    Используется для мониторинга и health checks.
                    
                    Возвращает статус каждого движка: UP или DOWN с описанием ошибки.
                    """
    )
    @ApiResponse(responseCode = "200", description = "Статус всех движков")
    @GetMapping("/health/search-engines")
    public ResponseEntity<java.util.Map<String, String>> healthCheck() {
        java.util.Map<String, String> health = new java.util.HashMap<>();
        
        try {
            searchService.searchWithSolr("test");
            health.put("solr", "UP");
        } catch (Exception e) {
            health.put("solr", "DOWN: " + e.getMessage());
        }
        
        try {
            searchService.searchWithOpenSearch("test");
            health.put("opensearch", "UP");
        } catch (Exception e) {
            health.put("opensearch", "DOWN: " + e.getMessage());
        }
        
        try {
            searchService.searchWithTypeSense("test");
            health.put("typesense", "UP");
        } catch (Exception e) {
            health.put("typesense", "DOWN: " + e.getMessage());
        }
        
        return ResponseEntity.ok(health);
    }
}

