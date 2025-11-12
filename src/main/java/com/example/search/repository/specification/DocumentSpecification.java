package com.example.search.repository.specification;

import com.example.search.constants.DocumentConstants;
import com.example.search.model.Document;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Specification для фильтрации документов с использованием Criteria API
 * Оптимизированная реализация с использованием лучших практик Spring Data JPA
 * 
 * Улучшения:
 * - Использование констант для имен полей (избегаем опечаток)
 * - Оптимизированное комбинирование спецификаций (Stream API)
 * - Эффективная обработка null значений
 * - Минимизация создания лишних объектов
 */
public class DocumentSpecification {

    // Используем константы из DocumentConstants

    /**
     * Создать Specification для полнотекстового поиска (title, content, author)
     * 
     * ⚠️ ПРОИЗВОДИТЕЛЬНОСТЬ:
     * - `LOWER(field) LIKE '%query%'` НЕ использует индексы (full table scan)
     * - На больших таблицах (>10K записей) это будет МЕДЛЕННО
     * - Рекомендуется использовать внешние поисковые движки (Solr, OpenSearch, TypeSense)
     *   для полнотекстового поиска в production
     * 
     * Оптимизации:
     * - Порядок полей: title (короче) → author → content (длиннее)
     * - Использует LOWER() для case-insensitive поиска (ANSI-совместимо)
     * - Паттерн '%query%' для поиска подстроки
     * 
     * Для production:
     * - Используйте SolrSearchService, OpenSearchService или TypeSenseService
     * - Они поддерживают полнотекстовый поиск с индексацией
     * - Гораздо быстрее для больших объемов данных
     * 
     * @param query поисковый запрос (null или пустая строка = пропуск фильтра)
     * @return Specification или null если query пустой
     */
    public static Specification<Document> hasTextSearch(String query) {
        if (query == null) {
            return null;
        }
        String trimmedQuery = query.trim();
        if (trimmedQuery.isEmpty()) {
            return null; // Пропускаем пустые фильтры
        }
        
        final String searchPattern = "%" + trimmedQuery.toLowerCase() + "%";
        
        return (root, criteriaQuery, criteriaBuilder) -> {
            // Порядок важен: title первым (обычно короче, быстрее проверяется)
            Predicate titleMatch = criteriaBuilder.like(
                criteriaBuilder.lower(root.get(DocumentConstants.FIELD_TITLE)), searchPattern
            );
            Predicate authorMatch = criteriaBuilder.like(
                criteriaBuilder.lower(root.get(DocumentConstants.FIELD_AUTHOR)), searchPattern
            );
            Predicate contentMatch = criteriaBuilder.like(
                criteriaBuilder.lower(root.get(DocumentConstants.FIELD_CONTENT)), searchPattern
            );

            // Объединяем через OR: найдет в любом из полей
            // Порядок: title → author → content (от коротких к длинным полям)
            return criteriaBuilder.or(titleMatch, authorMatch, contentMatch);
        };
    }

    /**
     * Фильтр по категории (точное совпадение)
     * 
     * @param category категория документа (null = пропуск фильтра)
     * @return Specification или null
     */
    public static Specification<Document> hasCategory(String category) {
        if (category == null) {
            return null;
        }
        String trimmedCategory = category.trim();
        if (trimmedCategory.isEmpty()) {
            return null;
        }
        final String finalCategory = trimmedCategory;
        return (root, criteriaQuery, criteriaBuilder) -> 
            criteriaBuilder.equal(root.get(DocumentConstants.FIELD_CATEGORY), finalCategory);
    }
    
    /**
     * Фильтр по статусу (точное совпадение)
     * 
     * @param status статус документа (null = пропуск фильтра)
     * @return Specification или null
     */
    public static Specification<Document> hasStatus(String status) {
        if (status == null) {
            return null;
        }
        String trimmedStatus = status.trim();
        if (trimmedStatus.isEmpty()) {
            return null;
        }
        final String finalStatus = trimmedStatus;
        return (root, criteriaQuery, criteriaBuilder) -> 
            criteriaBuilder.equal(root.get(DocumentConstants.FIELD_STATUS), finalStatus);
    }

    /**
     * Фильтр по автору (LIKE поиск, частичное совпадение)
     * 
     * @param author часть имени автора (null = пропуск фильтра)
     * @return Specification или null
     */
    public static Specification<Document> hasAuthor(String author) {
        if (author == null) {
            return null;
        }
        String trimmedAuthor = author.trim();
        if (trimmedAuthor.isEmpty()) {
            return null;
        }
        final String searchPattern = "%" + trimmedAuthor.toLowerCase() + "%";
        return (root, criteriaQuery, criteriaBuilder) -> 
            criteriaBuilder.like(
                criteriaBuilder.lower(root.get(DocumentConstants.FIELD_AUTHOR)), searchPattern
            );
    }

    /**
     * Фильтр по дате создания (после указанной даты включительно)
     * 
     * @param date минимальная дата создания (null = пропуск фильтра)
     * @return Specification или null
     */
    public static Specification<Document> createdAfter(LocalDateTime date) {
        if (date == null) {
            return null;
        }
        return (root, criteriaQuery, criteriaBuilder) -> 
            criteriaBuilder.greaterThanOrEqualTo(root.get(DocumentConstants.FIELD_CREATED_AT), date);
    }

    /**
     * Фильтр по дате создания (до указанной даты включительно)
     * 
     * @param date максимальная дата создания (null = пропуск фильтра)
     * @return Specification или null
     */
    public static Specification<Document> createdBefore(LocalDateTime date) {
        if (date == null) {
            return null;
        }
        return (root, criteriaQuery, criteriaBuilder) -> 
            criteriaBuilder.lessThanOrEqualTo(root.get(DocumentConstants.FIELD_CREATED_AT), date);
    }

    /**
     * Фильтр по дате обновления (после указанной даты включительно)
     * 
     * @param date минимальная дата обновления (null = пропуск фильтра)
     * @return Specification или null
     */
    public static Specification<Document> updatedAfter(LocalDateTime date) {
        if (date == null) {
            return null;
        }
        return (root, criteriaQuery, criteriaBuilder) -> 
            criteriaBuilder.greaterThanOrEqualTo(root.get(DocumentConstants.FIELD_UPDATED_AT), date);
    }

    /**
     * Фильтр по дате обновления (до указанной даты включительно)
     * 
     * @param date максимальная дата обновления (null = пропуск фильтра)
     * @return Specification или null
     */
    public static Specification<Document> updatedBefore(LocalDateTime date) {
        if (date == null) {
            return null;
        }
        return (root, criteriaQuery, criteriaBuilder) -> 
            criteriaBuilder.lessThanOrEqualTo(root.get(DocumentConstants.FIELD_UPDATED_AT), date);
    }

    /**
     * Комбинировать несколько Specification через AND
     * 
     * ⚡ ОПТИМИЗАЦИЯ ПРОИЗВОДИТЕЛЬНОСТИ:
     * Порядок условий в SQL влияет на производительность!
     * Более селективные условия (category, status) должны быть первыми,
     * чтобы БД могла использовать индексы и уменьшить количество проверяемых строк
     * перед применением медленного LIKE поиска.
     * 
     * Рекомендуемый порядок в fromFilter():
     * 1. category, status (точные совпадения, используют индексы) ← БЫСТРО
     * 2. author (LIKE, но более селективно чем content) ← СРЕДНЕ
     * 3. query (полнотекстовый поиск, самый медленный) ← МЕДЛЕННО
     * 4. dates (диапазоны, используют индексы) ← БЫСТРО
     * 
     * Оптимизация:
     * - Использует Stream API для эффективной фильтрации null значений
     * - Пропускает пустые спецификации (null)
     * - Минимизирует количество операций AND
     * 
     * @param specs массив спецификаций для комбинирования
     * @return комбинированная Specification или пустая (если все null)
     */
    @SafeVarargs
    public static Specification<Document> combine(Specification<Document>... specs) {
        return Stream.of(specs)
                .filter(Objects::nonNull)
                .reduce(Specification::and)
                .orElse(Specification.where(null));
    }

    /**
     * Создать Specification из фильтра
     * 
     * ⚡ ОПТИМИЗИРОВАННЫЙ ПОРЯДОК УСЛОВИЙ:
     * Порядок важен для производительности! Более селективные условия первыми.
     * 
     * Порядок:
     * 1. category, status - точные совпадения (используют индексы) ← БЫСТРО
     * 2. dates - диапазоны (используют индексы) ← БЫСТРО
     * 3. author - LIKE поиск (более селективно) ← СРЕДНЕ
     * 4. query - полнотекстовый поиск (самый медленный) ← МЕДЛЕННО
     * 
     * Это позволяет БД:
     * - Сначала отфильтровать по индексированным полям
     * - Уменьшить количество строк для проверки LIKE
     * - Использовать индексы максимально эффективно
     */
    public static Specification<Document> fromFilter(com.example.search.repository.DocumentSearchParams params) {
        if (params == null) {
            return Specification.where(null);
        }
        // Оптимизированный порядок: быстрые условия первыми
        return combine(
            // 1. Точные совпадения (используют индексы) - БЫСТРО
            hasCategory(params.getCategory()),
            hasStatus(params.getStatus()),
            // 2. Диапазоны дат (используют индексы) - БЫСТРО
            createdAfter(params.getCreatedAfter()),
            createdBefore(params.getCreatedBefore()),
            updatedAfter(params.getUpdatedAfter()),
            updatedBefore(params.getUpdatedBefore()),
            // 3. LIKE поиск по автору (более селективно) - СРЕДНЕ
            hasAuthor(params.getAuthor()),
            // 4. Полнотекстовый поиск (самый медленный) - МЕДЛЕННО
            hasTextSearch(params.getQuery())
        );
    }

    // Приватный конструктор для утилитного класса
    private DocumentSpecification() {
        throw new UnsupportedOperationException("Utility class");
    }
}

