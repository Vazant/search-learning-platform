package com.example.search.config.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Конфигурация для обработки дат в REST API
 * Поддерживает форматы:
 * - ISO 8601: 2024-01-01T00:00:00
 * - ISO 8601 с миллисекундами: 2025-11-10T18:11:16.046
 * - ISO 8601 с Z: 2025-11-10T18:11:16.046Z (UTC timezone, Z игнорируется для LocalDateTime)
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new StringToLocalDateTimeConverter());
    }

    /**
     * Конвертер строки в LocalDateTime
     * Поддерживает форматы с Z и без Z
     */
    private static class StringToLocalDateTimeConverter implements Converter<String, LocalDateTime> {
        @Override
        public LocalDateTime convert(String source) {
            if (source == null || source.trim().isEmpty()) {
                return null;
            }
            
            String cleaned = source.trim();
            
            // Удаляем Z в конце, если есть (для LocalDateTime timezone не нужен)
            if (cleaned.endsWith("Z")) {
                cleaned = cleaned.substring(0, cleaned.length() - 1);
            }
            
            // Пробуем разные форматы
            try {
                // ISO формат с миллисекундами: 2025-11-10T18:11:16.046
                return LocalDateTime.parse(cleaned, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception e) {
                // Пробуем формат без миллисекунд: 2025-11-10T18:11:16
                try {
                    return LocalDateTime.parse(cleaned, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
                } catch (Exception e2) {
                    throw new IllegalArgumentException("Cannot parse date: " + source + ". Expected format: yyyy-MM-dd'T'HH:mm:ss[.SSS][Z]", e2);
                }
            }
        }
    }
}

