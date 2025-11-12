package com.example.search.repository;

import com.example.search.constants.DocumentConstants;
import com.example.search.model.Document;
import com.example.search.repository.specification.DocumentSpecification;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * Реализация кастомного репозитория с использованием Criteria API
 */
@Repository
@Slf4j
public class DocumentCustomRepositoryImpl implements DocumentCustomRepository {

    // Используем константы из DocumentConstants

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<Document> searchWithFilters(DocumentSearchParams params, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Document> cq = cb.createQuery(Document.class);
        Root<Document> root = cq.from(Document.class);

        // Логирование параметров для отладки
        log.info("🔍 Search params: query='{}', category='{}', status='{}', author='{}', createdAfter={}, createdBefore={}",
                params.getQuery(), params.getCategory(), params.getStatus(), params.getAuthor(),
                params.getCreatedAfter(), params.getCreatedBefore());

        // Создаем Specification и применяем его
        Specification<Document> spec = DocumentSpecification.fromFilter(params);

        // Применяем Specification к CriteriaQuery
        Predicate predicate = spec.toPredicate(root, cq, cb);
        if (predicate != null) {
            cq.where(predicate);
        }

        // Применяем сортировку из Pageable
        if (pageable.getSort().isSorted()) {
            List<Order> orders = new ArrayList<>();
            pageable.getSort().forEach(sortOrder -> {
                Path<?> path = root.get(sortOrder.getProperty());
                Order order = sortOrder.isAscending() 
                    ? cb.asc(path) 
                    : cb.desc(path);
                orders.add(order);
            });
            cq.orderBy(orders);
        }

        // Выполняем запрос с пагинацией
        TypedQuery<Document> typedQuery = entityManager.createQuery(cq);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        List<Document> results = typedQuery.getResultList();

        // Подсчитываем общее количество
        Long total = countWithFilters(params);

        return new PageImpl<>(results, pageable, total);
    }

    /**
     * Подсчет общего количества с учетом фильтров
     */
    private Long countWithFilters(DocumentSearchParams params) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Document> root = cq.from(Document.class);
        cq.select(cb.count(root));

        Specification<Document> spec = DocumentSpecification.fromFilter(params);

        Predicate predicate = spec.toPredicate(root, cq, cb);
        if (predicate != null) {
            cq.where(predicate);
        }

        return entityManager.createQuery(cq).getSingleResult();
    }

    @Override
    public List<Object[]> getCategoryFacets(DocumentSearchParams params) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);
        Root<Document> root = cq.from(Document.class);

        // SELECT category, COUNT(*) FROM Document WHERE ... GROUP BY category
        cq.multiselect(root.get(DocumentConstants.FIELD_CATEGORY), cb.count(root));
        cq.groupBy(root.get(DocumentConstants.FIELD_CATEGORY));

        // Применяем фильтры (без category, чтобы получить все категории)
        Specification<Document> spec = DocumentSpecification.combine(
            DocumentSpecification.hasTextSearch(params.getQuery()),
            DocumentSpecification.hasStatus(params.getStatus()),
            DocumentSpecification.hasAuthor(params.getAuthor()),
            DocumentSpecification.createdAfter(params.getCreatedAfter()),
            DocumentSpecification.createdBefore(params.getCreatedBefore()),
            DocumentSpecification.updatedAfter(params.getUpdatedAfter()),
            DocumentSpecification.updatedBefore(params.getUpdatedBefore())
        );

        Predicate predicate = spec.toPredicate(root, cq, cb);
        if (predicate != null) {
            cq.where(predicate);
        }

        // Исключаем NULL категории
        Predicate notNullCategory = cb.isNotNull(root.get(DocumentConstants.FIELD_CATEGORY));
        if (predicate != null) {
            cq.where(cb.and(predicate, notNullCategory));
        } else {
            cq.where(notNullCategory);
        }

        return entityManager.createQuery(cq).getResultList();
    }

    @Override
    public List<Object[]> getStatusFacets(DocumentSearchParams params) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);
        Root<Document> root = cq.from(Document.class);

        cq.multiselect(root.get(DocumentConstants.FIELD_STATUS), cb.count(root));
        cq.groupBy(root.get(DocumentConstants.FIELD_STATUS));

        Specification<Document> spec = DocumentSpecification.combine(
            DocumentSpecification.hasTextSearch(params.getQuery()),
            DocumentSpecification.hasCategory(params.getCategory()),
            DocumentSpecification.hasAuthor(params.getAuthor()),
            DocumentSpecification.createdAfter(params.getCreatedAfter()),
            DocumentSpecification.createdBefore(params.getCreatedBefore()),
            DocumentSpecification.updatedAfter(params.getUpdatedAfter()),
            DocumentSpecification.updatedBefore(params.getUpdatedBefore())
        );

        Predicate predicate = spec.toPredicate(root, cq, cb);
        Predicate notNullStatus = cb.isNotNull(root.get(DocumentConstants.FIELD_STATUS));
        if (predicate != null) {
            cq.where(cb.and(predicate, notNullStatus));
        } else {
            cq.where(notNullStatus);
        }

        return entityManager.createQuery(cq).getResultList();
    }

    @Override
    public List<Object[]> getAuthorFacets(DocumentSearchParams params) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);
        Root<Document> root = cq.from(Document.class);

        cq.multiselect(root.get(DocumentConstants.FIELD_AUTHOR), cb.count(root));
        cq.groupBy(root.get(DocumentConstants.FIELD_AUTHOR));

        Specification<Document> spec = DocumentSpecification.combine(
            DocumentSpecification.hasTextSearch(params.getQuery()),
            DocumentSpecification.hasCategory(params.getCategory()),
            DocumentSpecification.hasStatus(params.getStatus()),
            DocumentSpecification.createdAfter(params.getCreatedAfter()),
            DocumentSpecification.createdBefore(params.getCreatedBefore()),
            DocumentSpecification.updatedAfter(params.getUpdatedAfter()),
            DocumentSpecification.updatedBefore(params.getUpdatedBefore())
        );

        Predicate predicate = spec.toPredicate(root, cq, cb);
        Predicate notNullAuthor = cb.isNotNull(root.get(DocumentConstants.FIELD_AUTHOR));
        if (predicate != null) {
            cq.where(cb.and(predicate, notNullAuthor));
        } else {
            cq.where(notNullAuthor);
        }

        return entityManager.createQuery(cq).getResultList();
    }

    @Override
    public List<String> autocompleteTitles(String prefix, int limit) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<String> cq = cb.createQuery(String.class);
        Root<Document> root = cq.from(Document.class);

        cq.select(root.get(DocumentConstants.FIELD_TITLE)).distinct(true);
        
        // LOWER(title) LIKE LOWER(prefix%)
        String searchPattern = prefix.toLowerCase() + "%";
        Predicate likePredicate = cb.like(
            cb.lower(root.get(DocumentConstants.FIELD_TITLE)), searchPattern
        );
        cq.where(likePredicate);
        cq.orderBy(cb.asc(root.get(DocumentConstants.FIELD_TITLE)));

        TypedQuery<String> query = entityManager.createQuery(cq);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    @Override
    public List<String> autocompleteAuthors(String prefix, int limit) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<String> cq = cb.createQuery(String.class);
        Root<Document> root = cq.from(Document.class);

        cq.select(root.get(DocumentConstants.FIELD_AUTHOR)).distinct(true);
        
        String searchPattern = prefix.toLowerCase() + "%";
        Predicate likePredicate = cb.like(
            cb.lower(root.get(DocumentConstants.FIELD_AUTHOR)), searchPattern
        );
        cq.where(likePredicate);
        cq.orderBy(cb.asc(root.get(DocumentConstants.FIELD_AUTHOR)));

        TypedQuery<String> query = entityManager.createQuery(cq);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    @Override
    public List<String> autocompleteCategories(String prefix, int limit) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<String> cq = cb.createQuery(String.class);
        Root<Document> root = cq.from(Document.class);

        cq.select(root.get(DocumentConstants.FIELD_CATEGORY)).distinct(true);
        
        String searchPattern = prefix.toLowerCase() + "%";
        Predicate likePredicate = cb.like(
            cb.lower(root.get(DocumentConstants.FIELD_CATEGORY)), searchPattern
        );
        Predicate notNull = cb.isNotNull(root.get(DocumentConstants.FIELD_CATEGORY));
        cq.where(cb.and(likePredicate, notNull));
        cq.orderBy(cb.asc(root.get(DocumentConstants.FIELD_CATEGORY)));

        TypedQuery<String> query = entityManager.createQuery(cq);
        query.setMaxResults(limit);
        return query.getResultList();
    }
}

