package com.example.search.controller.health;

import com.example.search.constants.DocumentConstants;
import com.example.search.service.monitoring.QueryFrequencyAnalyzer;
import com.example.search.service.monitoring.SearchMetricsService;
import com.example.search.service.search.SolrSearchService;
import com.example.search.service.search.OpenSearchService;
import com.example.search.service.search.TypeSenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Health checks и мониторинг для поисковых движков
 * 
 * Используется для:
 * - Проверки доступности поисковых движков
 * - Мониторинга производительности
 * - Отслеживания медленных запросов
 * - CI/CD health checks
 */
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Search Health", description = "Health checks и мониторинг поисковых движков")
public class SearchHealthController {

    private final SolrSearchService solrSearchService;
    private final OpenSearchService openSearchService;
    private final TypeSenseService typeSenseService;
    private final SearchMetricsService metricsService;
    private final QueryFrequencyAnalyzer queryFrequencyAnalyzer;

    @Operation(summary = "Проверка здоровья всех поисковых движков")
    @GetMapping("/search-engines")
    public ResponseEntity<Map<String, Object>> checkSearchEngines() {
        Map<String, Object> health = new HashMap<>();
        
        // Проверка Solr
        boolean solrHealthy = checkSolr();
        health.put("solr", Map.of(
            "status", solrHealthy ? DocumentConstants.HEALTH_STATUS_UP : DocumentConstants.HEALTH_STATUS_DOWN,
            "url", "http://localhost:8983/solr"
        ));
        
        // Проверка OpenSearch
        boolean openSearchHealthy = checkOpenSearch();
        health.put("opensearch", Map.of(
            "status", openSearchHealthy ? DocumentConstants.HEALTH_STATUS_UP : DocumentConstants.HEALTH_STATUS_DOWN,
            "url", "http://localhost:9200"
        ));
        
        // Проверка TypeSense
        boolean typeSenseHealthy = checkTypeSense();
        health.put("typesense", Map.of(
            "status", typeSenseHealthy ? DocumentConstants.HEALTH_STATUS_UP : DocumentConstants.HEALTH_STATUS_DOWN,
            "url", "http://localhost:8108"
        ));
        
        // Общий статус
        boolean allHealthy = solrHealthy && openSearchHealthy && typeSenseHealthy;
        health.put("overall", Map.of(
            "status", allHealthy ? DocumentConstants.HEALTH_STATUS_UP : DocumentConstants.HEALTH_STATUS_DEGRADED,
            "message", allHealthy 
                ? "All search engines are healthy" 
                : "Some search engines are unavailable"
        ));
        
        return ResponseEntity.ok(health);
    }

    @Operation(summary = "Метрики производительности поиска")
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // Метрики поиска
        metrics.put("search", Map.of(
            "averageDuration", metricsService.getAverageDuration("search"),
            "requestCount", metricsService.getRequestCount("search"),
            "slowQueryCount", metricsService.getSlowQueryCount("search")
        ));
        
        // Метрики autocomplete
        metrics.put("autocomplete", Map.of(
            "averageDuration", metricsService.getAverageDuration("autocomplete"),
            "requestCount", metricsService.getRequestCount("autocomplete"),
            "slowQueryCount", metricsService.getSlowQueryCount("autocomplete")
        ));
        
        // Метрики facets
        metrics.put("facets", Map.of(
            "averageDuration", metricsService.getAverageDuration("facets"),
            "requestCount", metricsService.getRequestCount("facets"),
            "slowQueryCount", metricsService.getSlowQueryCount("facets")
        ));
        
        return ResponseEntity.ok(metrics);
    }

    @Operation(summary = "Статистика GraphQL запросов и рекомендации по кэшированию")
    @GetMapping("/graphql-stats")
    public ResponseEntity<Map<String, Object>> getGraphQLStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // Метрики по операциям
        Map<String, Object> operations = new HashMap<>();
        operations.put("searchDocuments", Map.of(
            "averageDuration", metricsService.getAverageDuration("searchDocuments"),
            "requestCount", metricsService.getRequestCount("searchDocuments"),
            "slowQueryCount", metricsService.getSlowQueryCount("searchDocuments")
        ));
        operations.put("autocomplete", Map.of(
            "averageDuration", metricsService.getAverageDuration("autocomplete"),
            "requestCount", metricsService.getRequestCount("autocomplete"),
            "slowQueryCount", metricsService.getSlowQueryCount("autocomplete")
        ));
        
        stats.put("operations", operations);
        
        // Рекомендации по кэшированию (hot queries)
        // В реальном проекте можно добавить более детальную статистику
        stats.put("recommendations", Map.of(
            "message", "Check logs for 'CACHE CANDIDATE' messages. Hot queries are logged automatically every minute.",
            "howToUse", "Look for log messages with '🔥 CACHE CANDIDATE' prefix. These queries should be cached."
        ));
        
        return ResponseEntity.ok(stats);
    }

    private boolean checkSolr() {
        try {
            // В production можно использовать ping endpoint
            // Сейчас проверяем через попытку поиска
            solrSearchService.search("health_check");
            return true;
        } catch (Exception e) {
            log.warn("Solr health check failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean checkOpenSearch() {
        try {
            // Проверка через попытку поиска
            openSearchService.search("health_check");
            return true;
        } catch (Exception e) {
            log.warn("OpenSearch health check failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean checkTypeSense() {
        try {
            // Проверка через попытку поиска
            typeSenseService.search("health_check");
            return true;
        } catch (Exception e) {
            log.warn("TypeSense health check failed: {}", e.getMessage());
            return false;
        }
    }
}

