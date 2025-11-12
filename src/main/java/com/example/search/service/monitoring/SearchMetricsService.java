package com.example.search.service.monitoring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Сервис для сбора метрик производительности поиска
 * В production можно интегрировать с Prometheus, Micrometer и т.д.
 */
@Service
@Slf4j
public class SearchMetricsService {

    private final ConcurrentHashMap<String, AtomicLong> requestCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> totalDuration = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> slowQueries = new ConcurrentHashMap<>();
    
    private static final long SLOW_QUERY_THRESHOLD_MS = 500;

    /**
     * Записать метрику выполнения запроса
     */
    public void recordQuery(String operation, long durationMs) {
        requestCounts.computeIfAbsent(operation, k -> new AtomicLong(0)).incrementAndGet();
        totalDuration.computeIfAbsent(operation, k -> new AtomicLong(0)).addAndGet(durationMs);
        
        if (durationMs > SLOW_QUERY_THRESHOLD_MS) {
            slowQueries.computeIfAbsent(operation, k -> new AtomicLong(0)).incrementAndGet();
            log.warn("Slow {} query detected: {}ms", operation, durationMs);
        }
    }

    /**
     * Получить среднее время выполнения операции
     */
    public double getAverageDuration(String operation) {
        AtomicLong count = requestCounts.get(operation);
        AtomicLong total = totalDuration.get(operation);
        
        if (count == null || total == null || count.get() == 0) {
            return 0.0;
        }
        
        return (double) total.get() / count.get();
    }

    /**
     * Получить количество запросов
     */
    public long getRequestCount(String operation) {
        AtomicLong count = requestCounts.get(operation);
        return count != null ? count.get() : 0;
    }

    /**
     * Получить количество медленных запросов
     */
    public long getSlowQueryCount(String operation) {
        AtomicLong count = slowQueries.get(operation);
        return count != null ? count.get() : 0;
    }

    /**
     * Сброс метрик (для тестов)
     */
    public void reset() {
        requestCounts.clear();
        totalDuration.clear();
        slowQueries.clear();
    }
}

