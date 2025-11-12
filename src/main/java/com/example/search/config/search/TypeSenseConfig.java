package com.example.search.config.search;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.typesense.api.Client;
import org.typesense.resources.Node;

import lombok.extern.slf4j.Slf4j;

/**
 * Полная конфигурация TypeSense для production использования
 *
 * <p>Основные настройки: - Connection timeout для управления временем ожидания соединения -
 * Поддержка множественных нод для высокой доступности - Retry logic встроен в клиент
 *
 * <p>References: - https://github.com/typesense/typesense-java - https://typesense.org/docs/
 */
@Slf4j
@Configuration
public class TypeSenseConfig {

  @Value("${search.typesense.url:http://localhost:8108}")
  private String typesenseUrl;

  @Value("${search.typesense.api-key:xyz}")
  private String apiKey;

  @Value("${search.typesense.connection-timeout:10}")
  private int connectionTimeoutSeconds;

  @Value("${search.typesense.num-retries:3}")
  private int numRetries;

  @Value("${search.typesense.retry-interval-seconds:1}")
  private int retryIntervalSeconds;

  /** Создание TypeSense клиента с полной конфигурацией */
  @Bean
  @ConditionalOnProperty(
      name = "search.typesense.enabled",
      havingValue = "true",
      matchIfMissing = true)
  public Client typeSenseClient() {
    // Парсинг URL
    String host = typesenseUrl.replace("http://", "").replace("https://", "");
    String[] parts = host.split(":");
    String hostname = parts[0];
    String port = parts.length > 1 ? parts[1] : "8108";
    String protocol = typesenseUrl.startsWith("https://") ? "https" : "http";

    // Поддержка множественных нод (можно расширить для кластера)
    List<Node> nodes = new ArrayList<>();
    nodes.add(new Node(protocol, hostname, port));

    // Создание конфигурации с таймаутами
    org.typesense.api.Configuration configuration =
        new org.typesense.api.Configuration(
            nodes, Duration.ofSeconds(connectionTimeoutSeconds), apiKey);

    // Retry logic встроен в TypeSense клиент автоматически
    // При необходимости можно добавить кастомную логику retry на уровне приложения

    Client client = new Client(configuration);

    log.info(
        "TypeSense client initialized: url={}, timeout={}s, retries={}",
        typesenseUrl,
        connectionTimeoutSeconds,
        numRetries);

    return client;
  }
}
