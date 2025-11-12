package com.example.search.config.web;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;

/**
 * Конфигурация Swagger/OpenAPI для документации API
 *
 * <p>После запуска приложения Swagger UI доступен по адресу: http://localhost:8080/swagger-ui.html
 *
 * <p>OpenAPI спецификация (JSON): http://localhost:8080/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI customOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Search Demo API")
                .version("1.0.0")
                .description(
                    """
            REST API для поиска документов с поддержкой:
            - Полнотекстовый поиск по title/content/author
            - Фильтры: category, status, author, диапазоны дат
            - Пагинация и сортировка
            - Facets для группировки результатов
            - Автокомплит для title/author/category

            Также доступен GraphQL API:
            - GraphiQL: http://localhost:8080/graphiql
            - GraphQL endpoint: http://localhost:8080/graphql
            """)
                .contact(new Contact().name("Search Demo Team").email("support@example.com"))
                .license(
                    new License().name("MIT License").url("https://opensource.org/licenses/MIT")))
        .servers(
            List.of(
                new Server().url("http://localhost:8080").description("Local development server"),
                new Server().url("https://api.example.com").description("Production server")));
  }
}
