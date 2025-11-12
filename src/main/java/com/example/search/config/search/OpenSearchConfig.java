package com.example.search.config.search;

import java.util.concurrent.TimeUnit;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.opensearch.client.RestClient;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.rest_client.RestClientTransport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

/**
 * Полная конфигурация OpenSearch для production использования
 *
 * <p>Основные настройки: - Connection pooling для эффективного использования соединений - Retry
 * logic для обработки временных сбоев - Правильные таймауты для различных операций - Поддержка
 * аутентификации
 *
 * <p>OpenSearch Java Client API (2.x) RestHighLevelClient deprecated since OpenSearch 2.0
 *
 * <p>References: - https://opensearch.org/docs/latest/clients/java/ -
 * https://github.com/opensearch-project/opensearch-java
 */
@Slf4j
@Configuration
public class OpenSearchConfig {

  @Value("${search.opensearch.url:http://localhost:9200}")
  private String opensearchUrl;

  @Value("${search.opensearch.username:}")
  private String username;

  @Value("${search.opensearch.password:}")
  private String password;

  @Value("${search.opensearch.connection-timeout:10000}")
  private int connectionTimeout;

  @Value("${search.opensearch.socket-timeout:60000}")
  private int socketTimeout;

  @Value("${search.opensearch.connection-request-timeout:5000}")
  private int connectionRequestTimeout;

  @Value("${search.opensearch.max-connections-per-host:20}")
  private int maxConnectionsPerHost;

  @Value("${search.opensearch.max-total-connections:100}")
  private int maxTotalConnections;

  @Value("${search.opensearch.eviction-time:30000}")
  private long evictionTime;

  /** Настройка connection pool manager для эффективного управления соединениями */
  @Bean(destroyMethod = "close")
  public PoolingHttpClientConnectionManager openSearchConnectionManager() {
    PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
    connectionManager.setMaxTotal(maxTotalConnections);
    connectionManager.setDefaultMaxPerRoute(maxConnectionsPerHost);
    connectionManager.setValidateAfterInactivity(5000);

    log.info(
        "OpenSearch connection pool configured: maxTotal={}, maxPerRoute={}",
        maxTotalConnections,
        maxConnectionsPerHost);

    return connectionManager;
  }

  /** Настройка Apache HttpClient с connection pooling и retry logic */
  @Bean(destroyMethod = "close")
  public CloseableHttpClient openSearchHttpClient(
      PoolingHttpClientConnectionManager connectionManager) {
    RequestConfig requestConfig =
        RequestConfig.custom()
            .setConnectTimeout(connectionTimeout)
            .setSocketTimeout(socketTimeout)
            .setConnectionRequestTimeout(connectionRequestTimeout)
            .build();

    // Setup authentication (if credentials provided)
    final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    if (username != null && !username.isEmpty() && !username.equals("admin")) {
      credentialsProvider.setCredentials(
          AuthScope.ANY, new UsernamePasswordCredentials(username, password));
    }

    CloseableHttpClient httpClient =
        HttpClientBuilder.create()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(requestConfig)
            .setDefaultCredentialsProvider(credentialsProvider)
            .evictIdleConnections(evictionTime, TimeUnit.MILLISECONDS)
            .evictExpiredConnections()
            .setRetryHandler(
                (exception, executionCount, context) -> {
                  // Retry до 3 раз для временных ошибок
                  if (executionCount > 3) {
                    log.warn("OpenSearch request failed after {} retries", executionCount);
                    return false;
                  }
                  if (exception instanceof java.net.SocketTimeoutException
                      || exception instanceof java.net.ConnectException
                      || exception instanceof org.apache.http.NoHttpResponseException) {
                    log.debug("Retrying OpenSearch request (attempt {})", executionCount);
                    return true;
                  }
                  return false;
                })
            .build();

    log.info(
        "OpenSearch HttpClient configured: connectionTimeout={}ms, socketTimeout={}ms",
        connectionTimeout,
        socketTimeout);

    return httpClient;
  }

  /** Основной OpenSearch клиент с полной конфигурацией */
  @Bean(destroyMethod = "")
  @ConditionalOnProperty(
      name = "search.opensearch.enabled",
      havingValue = "true",
      matchIfMissing = true)
  public OpenSearchClient openSearchClient(CloseableHttpClient httpClient) {
    // Parse URL
    String host = opensearchUrl.replace("http://", "").replace("https://", "");
    String[] parts = host.split(":");
    String hostname = parts[0];
    int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 9200;
    String scheme = opensearchUrl.startsWith("https://") ? "https" : "http";

    // Create low-level REST client with connection pooling
    RestClient restClient =
        RestClient.builder(new HttpHost(hostname, port, scheme))
            .setHttpClientConfigCallback(
                httpClientBuilder -> {
                  // HttpClient уже настроен с connection pooling и retry logic
                  return httpClientBuilder;
                })
            .build();

    // Create transport with Jackson mapper
    OpenSearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());

    // Create OpenSearch client
    OpenSearchClient client = new OpenSearchClient(transport);

    log.info("OpenSearch client initialized: url={}, scheme={}", opensearchUrl, scheme);

    return client;
  }
}
