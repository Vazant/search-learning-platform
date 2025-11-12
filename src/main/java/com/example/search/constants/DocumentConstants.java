package com.example.search.constants;

/**
 * Константы для работы с документами
 */
public final class DocumentConstants {

    private DocumentConstants() {
        // Utility class
    }

    // Статусы документов
    public static final String STATUS_APPROVED = "approved";
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_DRAFT = "draft";

    // Категории документов
    public static final String CATEGORY_DOCUMENT = "document";
    public static final String CATEGORY_TASK = "task";
    public static final String CATEGORY_ATTACHMENT = "attachment";
    public static final String CATEGORY_APPROVAL = "approval";

    // Поля для сортировки и поиска
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_AUTHOR = "author";
    public static final String FIELD_CATEGORY = "category";
    public static final String FIELD_STATUS = "status";
    public static final String FIELD_CREATED_AT = "createdAt";
    public static final String FIELD_UPDATED_AT = "updatedAt";
    public static final String FIELD_CONTENT = "content";

    // Типы автокомплита
    public static final String AUTOCOMPLETE_TYPE_TITLE = "title";
    public static final String AUTOCOMPLETE_TYPE_AUTHOR = "author";
    public static final String AUTOCOMPLETE_TYPE_CATEGORY = "category";

    // Названия индексов/коллекций для поисковых движков
    public static final String INDEX_NAME = "search_demo";
    public static final String COLLECTION_NAME = "search_demo";
    public static final String CORE_NAME = "search_demo";

    // Health status
    public static final String HEALTH_STATUS_UP = "UP";
    public static final String HEALTH_STATUS_DOWN = "DOWN";
    public static final String HEALTH_STATUS_DEGRADED = "DEGRADED";
}

