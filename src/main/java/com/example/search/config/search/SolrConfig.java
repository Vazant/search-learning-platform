package com.example.search.config.search;

import java.util.concurrent.TimeUnit;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

/**
 * Полная конфигурация Solr для production использования
 *
 * <p>Основные настройки: - Connection pooling для эффективного использования соединений - Retry
 * logic для обработки временных сбоев - Правильные таймауты для различных операций - Health check
 * поддержка
 *
 * <p>HttpSolrClient использует Apache HttpClient под капотом Compatible with Spring Boot 3.2.x и
 * Solr 9.4.x
 *
 * <p>References: - https://solr.apache.org/guide/solr/9_4/deployment-guide/solrj.html -
 * https://solr.apache.org/docs/9_4_1/solrj/org/apache/solr/client/solrj/impl/HttpSolrClient.Builder.html
 * - Http2SolrClient требует Jetty 10 (несовместим с Spring Boot 3.2's Jetty 12)
 */
@Slf4j
@Configuration
public class SolrConfig {

  @Value("${search.solr.url:http://localhost:8983/solr}")
  private String solrUrl;

  @Value("${search.solr.connection-timeout:10000}")
  private int connectionTimeout;

  @Value("${search.solr.socket-timeout:60000}")
  private int socketTimeout;

  @Value("${search.solr.max-connections-per-host:20}")
  private int maxConnectionsPerHost;

  @Value("${search.solr.max-total-connections:100}")
  private int maxTotalConnections;

  @Value("${search.solr.connection-request-timeout:5000}")
  private int connectionRequestTimeout;

  @Value("${search.solr.eviction-time:30000}")
  private long evictionTime;

  /** Настройка connection pool manager для эффективного управления соединениями */
  @Bean(destroyMethod = "close")
  public PoolingHttpClientConnectionManager connectionManager() {
    PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
    connectionManager.setMaxTotal(maxTotalConnections);
    connectionManager.setDefaultMaxPerRoute(maxConnectionsPerHost);

    // Evict idle connections
    connectionManager.setValidateAfterInactivity(5000);

    log.info(
        "Solr connection pool configured: maxTotal={}, maxPerRoute={}",
        maxTotalConnections,
        maxConnectionsPerHost);

    return connectionManager;
  }

  /** Настройка Apache HttpClient с connection pooling и retry logic */
  @Bean(destroyMethod = "close")
  public CloseableHttpClient httpClient(PoolingHttpClientConnectionManager connectionManager) {
    RequestConfig requestConfig =
        RequestConfig.custom()
            .setConnectTimeout(connectionTimeout)
            .setSocketTimeout(socketTimeout)
            .setConnectionRequestTimeout(connectionRequestTimeout)
            .build();

    CloseableHttpClient httpClient =
        HttpClientBuilder.create()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(requestConfig)
            .evictIdleConnections(evictionTime, TimeUnit.MILLISECONDS)
            .evictExpiredConnections()
            .setRetryHandler(
                (exception, executionCount, context) -> {
                  // Retry до 3 раз для временных ошибок
                  if (executionCount > 3) {
                    log.warn("Solr request failed after {} retries", executionCount);
                    return false;
                  }
                  if (exception instanceof java.net.SocketTimeoutException
                      || exception instanceof java.net.ConnectException
                      || exception instanceof org.apache.http.NoHttpResponseException) {
                    log.debug("Retrying Solr request (attempt {})", executionCount);
                    return true;
                  }
                  return false;
                })
            .build();

    log.info(
        "Solr HttpClient configured: connectionTimeout={}ms, socketTimeout={}ms",
        connectionTimeout,
        socketTimeout);

    return httpClient;
  }

  /**
   * Основной Solr клиент с полной конфигурацией
   *
   * <p>Примечание: HttpSolrClient помечен как deprecated в Solr 9.0+, но это единственный вариант
   * для Spring Boot 3.2, так как Http2SolrClient требует Jetty 10, а Spring Boot 3.2 использует
   * Jetty 12 (несовместимо).
   */
  @Bean(destroyMethod = "close")
  @ConditionalOnProperty(name = "search.solr.enabled", havingValue = "true", matchIfMissing = true)
  @SuppressWarnings("deprecation")
  public SolrClient solrClient(CloseableHttpClient httpClient) {
    // Убеждаемся, что URL заканчивается на /solr (без core/collection)
    String baseUrl;
    if (solrUrl.endsWith("/solr")) {
      baseUrl = solrUrl;
    } else if (solrUrl.endsWith("/")) {
      baseUrl = solrUrl + "solr";
    } else {
      baseUrl = solrUrl + "/solr";
    }

    HttpSolrClient client =
        new HttpSolrClient.Builder(baseUrl)
            .withConnectionTimeout(connectionTimeout, TimeUnit.MILLISECONDS)
            .withSocketTimeout(socketTimeout, TimeUnit.MILLISECONDS)
            .withHttpClient(httpClient)
            .build();

    log.info("Solr client initialized with base URL: {}", baseUrl);

    return client;
  }
}
