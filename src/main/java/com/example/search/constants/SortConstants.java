package com.example.search.constants;

/** Константы для сортировки */
public final class SortConstants {

  private SortConstants() {
    // Utility class
  }

  // Направления сортировки
  public static final String SORT_ASC = "ASC";
  public static final String SORT_DESC = "DESC";

  // Поля для сортировки (значения enum)
  public static final String SORT_FIELD_CREATED_AT = "CREATED_AT";
  public static final String SORT_FIELD_UPDATED_AT = "UPDATED_AT";
  public static final String SORT_FIELD_TITLE = "TITLE";
  public static final String SORT_FIELD_AUTHOR = "AUTHOR";
  public static final String SORT_FIELD_CATEGORY = "CATEGORY";
  public static final String SORT_FIELD_STATUS = "STATUS";
  public static final String SORT_FIELD_RELEVANCE = "RELEVANCE";

  // Поля для сортировки (имена полей в БД)
  public static final String SORT_BY_TITLE = "title";
  public static final String SORT_BY_CREATED_AT = "createdAt";
  public static final String SORT_BY_UPDATED_AT = "updatedAt";
  public static final String SORT_BY_AUTHOR = "author";
  public static final String SORT_BY_CATEGORY = "category";
  public static final String SORT_BY_STATUS = "status";
}
