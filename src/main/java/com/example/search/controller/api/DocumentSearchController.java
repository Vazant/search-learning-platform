package com.example.search.controller.api;

import com.example.search.dto.request.DocumentSearchRequest;
import com.example.search.dto.request.FacetRequest;
import com.example.search.dto.request.AutocompleteRequest;
import com.example.search.dto.response.DocumentSearchResponse;
import com.example.search.dto.response.FacetDto;
import com.example.search.dto.response.AutocompleteResultDto;
import com.example.search.service.document.DocumentSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API контроллер для поиска документов
 * 
 * Endpoints:
 * - POST /documents/search - поиск с фильтрами (JSON body)
 * - POST /documents/facets - получение facets (JSON body)
 * - POST /documents/autocomplete - автокомплит (JSON body)
 */
@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Document Search", description = "API для поиска документов с фильтрами, сортировкой, пагинацией и facets")
public class DocumentSearchController {

    private final DocumentSearchService documentSearchService;

    @Operation(
            summary = "Поиск документов",
            description = """
                    **POST запрос с JSON body** - поиск документов с фильтрами, сортировкой и пагинацией
                    
                    **Формат запроса:**
                    - Метод: **POST**
                    - Content-Type: **application/json**
                    - Тело запроса: JSON объект с параметрами поиска
                    
                    **Основной параметр поиска:**
                    - `query` - полнотекстовый поиск по title/content/author (LIKE, без учета регистра)
                      Ищет подстроку в любом из трех полей одновременно.
                      Пример: `{"query": "Java"}` найдет документы где "Java" есть в заголовке, содержимом или авторе.
                    
                    **Фильтры:**
                    - `category` - точное совпадение категории (например: "document", "task", "approval")
                    - `status` - точное совпадение статуса (например: "approved", "pending", "draft")
                    - `author` - LIKE поиск по автору (без учета регистра)
                    
                    **Диапазоны дат:**
                    - `createdAfter`, `createdBefore` - фильтр по дате создания
                    - `updatedAfter`, `updatedBefore` - фильтр по дате обновления
                    - Формат: ISO 8601 (например: "2024-01-01T00:00:00" или "2024-01-01T00:00:00Z")
                    
                    **Пагинация:**
                    - `page` - номер страницы (0-based, по умолчанию 0)
                    - `size` - размер страницы (1-100, по умолчанию 20)
                    
                    **Сортировка:**
                    - `sortBy` - поле для сортировки: title, createdAt, updatedAt, author, category, status
                    - `sortOrder` - направление: ASC (по возрастанию) или DESC (по убыванию, по умолчанию)
                    
                    **Важно:**
                    - Все параметры опциональны
                    - Если параметры не указаны, возвращаются все документы с пагинацией
                    - Можно комбинировать любые параметры
                    - Поиск работает по принципу AND (все условия должны выполняться)
                    - Используй JSON body, а не query параметры
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Успешный поиск",
                    content = @Content(schema = @Schema(implementation = DocumentSearchResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Неверные параметры запроса (валидация не пройдена)"
            )
    })
    @PostMapping("/search")
    public ResponseEntity<DocumentSearchResponse> searchDocuments(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "JSON body с параметрами поиска. Все параметры опциональны. Можно комбинировать любые фильтры.",
                required = false,
                content = @Content(
                    schema = @Schema(implementation = DocumentSearchRequest.class),
                    examples = {
                        @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "Поиск по Java",
                            value = "{\"query\":\"Java\",\"page\":0,\"size\":20,\"sortBy\":\"createdAt\",\"sortOrder\":\"DESC\"}",
                            description = "Найдет: 'Java 17 LTS: Новые возможности', 'Spring Boot 3: Миграция и новые возможности'"
                        ),
                        @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "Одобренные документы",
                            value = "{\"category\":\"document\",\"status\":\"approved\",\"page\":0,\"size\":20,\"sortBy\":\"createdAt\",\"sortOrder\":\"DESC\"}",
                            description = "14 одобренных документов категории 'document'"
                        ),
                        @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "Поиск по автору",
                            value = "{\"author\":\"Алексей\",\"page\":0,\"size\":20,\"sortBy\":\"createdAt\",\"sortOrder\":\"DESC\"}",
                            description = "Найдет 2 документа: 'Apache Solr' и 'Spring Boot 3' (автор: Алексей Иванов)"
                        ),
                        @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "Поиск по поисковым движкам",
                            value = "{\"query\":\"поиск\",\"category\":\"document\",\"status\":\"approved\",\"page\":0,\"size\":20,\"sortBy\":\"createdAt\",\"sortOrder\":\"DESC\"}",
                            description = "Найдет: 'Apache Solr', 'OpenSearch vs Elasticsearch', 'TypeSense', 'E-commerce поиск', 'Log Analysis с OpenSearch'"
                        ),
                        @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "Все документы",
                            value = "{\"page\":0,\"size\":20,\"sortBy\":\"createdAt\",\"sortOrder\":\"DESC\"}",
                            description = "Все 24 документа, отсортированные по дате создания (новые первыми)"
                        )
                    }
                )
            )
            @RequestBody @Valid DocumentSearchRequest request) {
        log.info("REST API: Search documents request received");
        log.info("Request params: query='{}', category='{}', status='{}', author='{}', page={}, size={}",
                request.getQuery(), request.getCategory(), request.getStatus(), 
                request.getAuthor(), request.getPage(), request.getSize());
        log.info("Date params: createdAfter={}, createdBefore={}, updatedAfter={}, updatedBefore={}",
                request.getCreatedAfter(), request.getCreatedBefore(),
                request.getUpdatedAfter(), request.getUpdatedBefore());
        DocumentSearchResponse response = documentSearchService.searchDocuments(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Получить facets",
            description = """
                    **POST запрос с JSON body** - получить группировку результатов по измерениям (category, status, author).
                    
                    **Формат запроса:**
                    - Метод: **POST**
                    - Content-Type: **application/json**
                    - Тело запроса: JSON объект с параметрами
                    
                    Facets учитывают активные фильтры (исключая само измерение, для которого запрашивается facet).
                    
                    Например, если запросить facets для category с фильтром status=approved,
                    вернутся все категории документов со статусом approved с количеством документов в каждой.
                    
                    **Параметры (JSON body):**
                    - `dimensions` - массив строк с измерениями: ["category", "status", "author"]
                    - `query`, `category`, `status`, `author`, `dates` - фильтры (исключая измерение, для которого запрашиваем facet)
                    
                    **Пример запроса:**
                    ```json
                    {
                      "dimensions": ["category", "status"],
                      "status": "approved"
                    }
                    ```
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Успешно получены facets",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Неверные параметры запроса"
            )
    })
    @PostMapping("/facets")
    public ResponseEntity<Map<String, List<FacetDto>>> getFacets(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "JSON body с параметрами для получения facets",
                required = false,
                content = @Content(
                    schema = @Schema(implementation = FacetRequest.class),
                    examples = {
                        @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "Все facets",
                            value = "{\"dimensions\":[\"category\",\"status\",\"author\"]}",
                            description = "Получить facets для всех измерений (category, status, author)"
                        ),
                        @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "Facets для одобренных документов",
                            value = "{\"status\":\"approved\",\"dimensions\":[\"category\",\"author\"]}",
                            description = "Получить facets по категориям и авторам для одобренных документов"
                        ),
                        @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "Facets для документов про Java",
                            value = "{\"query\":\"Java\",\"dimensions\":[\"category\",\"status\"]}",
                            description = "Получить facets по категориям и статусам для документов про Java"
                        ),
                        @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "Только категории",
                            value = "{\"dimensions\":[\"category\"]}",
                            description = "Получить только facets по категориям: document (20), task (3), approval (1), attachment (1)"
                        )
                    }
                )
            )
            @RequestBody @Valid FacetRequest request) {
        log.info("REST API: Get facets request");
        Map<String, List<FacetDto>> facets = documentSearchService.getFacets(request);
        return ResponseEntity.ok(facets);
    }

    @Operation(
            summary = "Автокомплит",
            description = """
                    **POST запрос с JSON body** - автокомплит по префиксу для указанного поля.
                    
                    **Формат запроса:**
                    - Метод: **POST**
                    - Content-Type: **application/json**
                    - Тело запроса: JSON объект с параметрами
                    
                    Результаты отсортированы по релевантности (точные совпадения в начале строки имеют приоритет).
                    
                    **Параметры (JSON body):**
                    - `prefix` - префикс для поиска (обязательно, минимум 2 символа)
                    - `field` - поле для автокомплита (обязательно: "title", "author", "category")
                    - `limit` - количество результатов (1-50, по умолчанию 10)
                    
                    **Примеры запросов:**
                    ```json
                    {"prefix": "Java", "field": "title", "limit": 10}
                    {"prefix": "Алекс", "field": "author", "limit": 5}
                    {"prefix": "doc", "field": "category"}
                    ```
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Успешно получены результаты автокомплита",
                    content = @Content(schema = @Schema(implementation = AutocompleteResultDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Неверные параметры (prefix < 2 символов, неверное поле, limit вне диапазона)"
            )
    })
    @PostMapping("/autocomplete")
    public ResponseEntity<List<AutocompleteResultDto>> autocomplete(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "JSON body с параметрами автокомплита",
                required = true,
                content = @Content(
                    schema = @Schema(implementation = AutocompleteRequest.class),
                    examples = {
                        @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "Автокомплит заголовков на 'Java'",
                            value = "{\"prefix\":\"Java\",\"field\":\"title\",\"limit\":10}",
                            description = "Вернет: 'Java 17 LTS: Новые возможности'"
                        ),
                        @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "Автокомплит заголовков на 'поиск'",
                            value = "{\"prefix\":\"поиск\",\"field\":\"title\",\"limit\":10}",
                            description = "Вернет: 'Apache Solr: Введение в полнотекстовый поиск', 'E-commerce поиск: Требования и решения'"
                        ),
                        @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "Автокомплит авторов на 'Алекс'",
                            value = "{\"prefix\":\"Алекс\",\"field\":\"author\",\"limit\":10}",
                            description = "Вернет: 'Алексей Иванов'"
                        ),
                        @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "Автокомплит категорий на 'doc'",
                            value = "{\"prefix\":\"doc\",\"field\":\"category\",\"limit\":10}",
                            description = "Вернет: 'document'"
                        ),
                        @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "Автокомплит заголовков на 'Spring'",
                            value = "{\"prefix\":\"Spring\",\"field\":\"title\",\"limit\":5}",
                            description = "Вернет: 'Spring Boot 3: Миграция и новые возможности', 'Security в Spring Boot приложениях'"
                        )
                    }
                )
            )
            @RequestBody @Valid AutocompleteRequest request) {
        log.info("REST API: Autocomplete request");
        List<AutocompleteResultDto> results = documentSearchService.autocomplete(request);
        return ResponseEntity.ok(results);
    }
}

