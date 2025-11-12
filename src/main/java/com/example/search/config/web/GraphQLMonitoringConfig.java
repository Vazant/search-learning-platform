package com.example.search.config.web;

import com.example.search.service.monitoring.QueryFrequencyAnalyzer;
import com.example.search.service.monitoring.SearchMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlResponse;

/**
 * Автоматический мониторинг GraphQL запросов
 * 
 * Что делает:
 * - Логирует все GraphQL запросы с временем выполнения
 * - Автоматически обнаруживает медленные запросы (>500ms)
 * - Анализирует частоту запросов для определения hot queries
 * - Собирает метрики для анализа производительности
 * 
 * Как использовать:
 * - Все запросы автоматически логируются
 * - Медленные запросы (>500ms) логируются с предупреждением
 * - Каждую минуту выводится статистика hot queries
 * - Запросы-кандидаты на кэширование логируются с префиксом "🔥 CACHE CANDIDATE"
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class GraphQLMonitoringConfig {

    private final SearchMetricsService metricsService;
    private final QueryFrequencyAnalyzer queryFrequencyAnalyzer;

    /**
     * WebGraphQlInterceptor для автоматического мониторинга всех GraphQL запросов
     * 
     * Этот interceptor перехватывает все GraphQL запросы и:
     * 1. Логирует время выполнения
     * 2. Обнаруживает медленные запросы
     * 3. Анализирует частоту для определения hot queries
     */
    @Bean
    public WebGraphQlInterceptor graphQLPerformanceInterceptor() {
        return (request, chain) -> {
            long startTime = System.currentTimeMillis();
            String query = request.getDocument();
            
            recordQueryStart(query);
            
            return chain.next(request)
                .doOnNext(response -> handleSuccessfulResponse(response, query, startTime))
                .doOnError(error -> handleError(error, startTime));
        };
    }

    private void recordQueryStart(String query) {
        queryFrequencyAnalyzer.recordQuery(query);
        String logQuery = truncateQuery(query, 100);
        log.debug("GraphQL query started: {}", logQuery);
    }

    private void handleSuccessfulResponse(WebGraphQlResponse response, String query, long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        int errorCount = getErrorCount(response);
        
        logQueryExecution(duration, errorCount);
        logSlowQueries(duration, query, response);
        recordMetrics(query, duration);
        queryFrequencyAnalyzer.checkIfShouldCache(query, duration);
    }

    private void logQueryExecution(long duration, int errorCount) {
        log.info("GraphQL query executed: duration={}ms, errors={}", duration, errorCount);
    }

    private void logSlowQueries(long duration, String query, WebGraphQlResponse response) {
        if (duration > 1000) {
            logCriticalSlowQuery(duration, query);
        } else if (duration > 500) {
            logSlowQuery(duration, query, response);
        }
    }

    private void logSlowQuery(long duration, String query, WebGraphQlResponse response) {
        String truncatedQuery = truncateQuery(query, 500);
        Object errors = response.getErrors().isEmpty() ? "none" : response.getErrors();
        log.warn("⚠️ SLOW GraphQL query detected: {}ms\nQuery: {}\nErrors: {}", 
            duration, truncatedQuery, errors);
    }

    private void logCriticalSlowQuery(long duration, String query) {
        log.error("🚨 CRITICAL: Very slow GraphQL query: {}ms\nQuery: {}", duration, query);
    }

    private void recordMetrics(String query, long duration) {
        if (query != null) {
            String operationName = extractOperationName(query);
            metricsService.recordQuery(operationName, duration);
        }
    }

    private void handleError(Throwable error, long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        log.error("GraphQL query failed after {}ms: {}", duration, error.getMessage());
    }

    private int getErrorCount(WebGraphQlResponse response) {
        return response.getErrors().size();
    }

    private String truncateQuery(String query, int maxLength) {
        if (query == null) {
            return null;
        }
        return query.length() > maxLength ? query.substring(0, maxLength) + "..." : query;
    }

    /**
     * Извлекает имя операции из GraphQL запроса
     */
    private String extractOperationName(String query) {
        if (query == null) {
            return "unknown";
        }
        
        // Пытаемся найти имя операции
        String[] lines = query.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("query ") || line.startsWith("mutation ")) {
                String[] parts = line.split("\\s+");
                if (parts.length > 1) {
                    return parts[1].replace("{", "").trim();
                }
            }
        }
        
        // Если не нашли, используем хэш запроса
        return "query_" + Math.abs(query.hashCode());
    }
}

