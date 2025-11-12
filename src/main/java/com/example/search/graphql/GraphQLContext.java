package com.example.search.graphql;

import lombok.Builder;
import lombok.Data;

/**
 * Контекст GraphQL запроса
 * В реальном проекте здесь будет информация о текущем пользователе из JWT токена или сессии
 */
@Data
@Builder
public class GraphQLContext {
    private String userId;
    private String organizationId;
    private String[] roles;
    
    /**
     * Получить текущего пользователя из контекста
     * В production это будет извлекаться из SecurityContext или JWT токена
     */
    public static String getCurrentUserId() {
        // TODO: Интеграция с Spring Security
        // SecurityContext context = SecurityContextHolder.getContext();
        // Authentication auth = context.getAuthentication();
        // return auth != null ? auth.getName() : "anonymous";
        
        // Для демо возвращаем тестового пользователя
        return "user-1";
    }
    
    /**
     * Проверка, является ли пользователь администратором
     */
    public static boolean isAdmin() {
        // TODO: Проверка роли из SecurityContext
        return false;
    }
}



