package com.example.search.service.monitoring;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Анализатор частоты GraphQL запросов
 *
 * <p>Автоматически определяет: - Hot queries (часто выполняемые запросы) - кандидаты на кэширование
 * - Медленные запросы, которые выполняются часто - приоритет для оптимизации
 *
 * <p>Как работает: 1. Считает частоту каждого уникального запроса 2. Каждую минуту анализирует
 * статистику 3. Автоматически предлагает кэшировать hot queries
 */
@Service
@Slf4j
public class QueryFrequencyAnalyzer {

  // Хранит частоту запросов (хэш запроса -> количество выполнений)
  private final Map<String, QueryStats> queryStats = new ConcurrentHashMap<>();

  // Пороги для принятия решений
  private static final int HOT_QUERY_THRESHOLD = 10; // Запросов в минуту
  private static final long SLOW_QUERY_THRESHOLD_MS = 500; // Медленный запрос
  private static final int CACHE_CANDIDATE_THRESHOLD = 20; // Запросов в минуту для кэширования

  /** Записать выполнение запроса */
  public void recordQuery(String query) {
    if (query == null || query.trim().isEmpty()) {
      return;
    }

    String queryHash = hashQuery(query);
    QueryStats stats = queryStats.computeIfAbsent(queryHash, k -> new QueryStats(query));
    stats.incrementCount();
  }

  /** Проверить, нужно ли кэшировать запрос */
  public void checkIfShouldCache(String query, long duration) {
    if (query == null || query.trim().isEmpty()) {
      return;
    }

    String queryHash = hashQuery(query);
    QueryStats stats = queryStats.get(queryHash);

    if (stats == null) {
      return;
    }

    // Обновляем среднее время выполнения
    stats.updateAverageDuration(duration);

    // Если запрос выполняется часто И медленный - предлагаем кэшировать
    if (stats.getCountPerMinute() >= CACHE_CANDIDATE_THRESHOLD
        && stats.getAverageDuration() > SLOW_QUERY_THRESHOLD_MS) {
      log.info(
          "🔥 CACHE CANDIDATE: Query executed {} times/min, avg duration {}ms. Consider caching!\nQuery: {}",
          stats.getCountPerMinute(),
          stats.getAverageDuration(),
          query.length() > 200 ? query.substring(0, 200) + "..." : query);
    }
  }

  /** Периодический анализ статистики (каждую минуту) */
  @Scheduled(fixedRate = 60000) // Каждую минуту
  public void analyzeQueryFrequency() {
    if (queryStats.isEmpty()) {
      return;
    }

    log.info("📊 GraphQL Query Frequency Analysis:");
    log.info("   Total unique queries: {}", queryStats.size());

    // Находим hot queries
    queryStats.entrySet().stream()
        .filter(entry -> entry.getValue().getCountPerMinute() >= HOT_QUERY_THRESHOLD)
        .sorted(
            (a, b) ->
                Long.compare(b.getValue().getCountPerMinute(), a.getValue().getCountPerMinute()))
        .limit(10) // Топ 10
        .forEach(
            entry -> {
              QueryStats stats = entry.getValue();
              log.info(
                  "   🔥 Hot query: {} times/min, avg {}ms, total {} times",
                  stats.getCountPerMinute(),
                  stats.getAverageDuration(),
                  stats.getTotalCount());
            });

    // Находим медленные запросы, которые выполняются часто
    queryStats.entrySet().stream()
        .filter(
            entry -> {
              QueryStats stats = entry.getValue();
              return stats.getAverageDuration() > SLOW_QUERY_THRESHOLD_MS
                  && stats.getCountPerMinute() >= 5;
            })
        .sorted(
            (a, b) ->
                Long.compare(b.getValue().getAverageDuration(), a.getValue().getAverageDuration()))
        .limit(5) // Топ 5
        .forEach(
            entry -> {
              QueryStats stats = entry.getValue();
              log.warn(
                  "   ⚠️ Slow frequent query: {}ms avg, {} times/min. Consider optimization!",
                  stats.getAverageDuration(),
                  stats.getCountPerMinute());
            });

    // Сбрасываем счетчики для следующей минуты
    queryStats.values().forEach(QueryStats::resetMinuteCounter);
  }

  /** Получить статистику по запросу */
  public QueryStats getStats(String query) {
    String queryHash = hashQuery(query);
    return queryStats.get(queryHash);
  }

  /** Хэширование запроса для идентификации */
  private String hashQuery(String query) {
    // Нормализуем запрос (убираем пробелы, переносы строк)
    String normalized = query.replaceAll("\\s+", " ").trim();
    return String.valueOf(normalized.hashCode());
  }

  /** Статистика по запросу */
  public static class QueryStats {
    private final String query;
    private final AtomicInteger totalCount = new AtomicInteger(0);
    private final AtomicInteger countThisMinute = new AtomicInteger(0);
    private final AtomicLong totalDuration = new AtomicLong(0);

    public QueryStats(String query) {
      this.query = query;
    }

    public void incrementCount() {
      totalCount.incrementAndGet();
      countThisMinute.incrementAndGet();
    }

    public void updateAverageDuration(long duration) {
      totalDuration.addAndGet(duration);
    }

    public void resetMinuteCounter() {
      countThisMinute.set(0);
    }

    public int getCountPerMinute() {
      return countThisMinute.get();
    }

    public long getAverageDuration() {
      int count = totalCount.get();
      if (count == 0) {
        return 0;
      }
      return totalDuration.get() / count;
    }

    public int getTotalCount() {
      return totalCount.get();
    }

    public String getQuery() {
      return query;
    }
  }
}
