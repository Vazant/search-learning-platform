#!/bin/bash

echo "🔍 Проверка статуса приложения..."
echo ""

# Цвета для вывода
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 1. Проверка порта 8080
echo "1️⃣  Проверка порта 8080..."
if lsof -ti:8080 > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Порт 8080 занят (приложение запущено)${NC}"
else
    echo -e "${RED}❌ Порт 8080 свободен (приложение не запущено)${NC}"
    exit 1
fi
echo ""

# 2. Проверка HTTP endpoint
echo "2️⃣  Проверка HTTP доступности..."
if curl -s -f http://localhost:8080/swagger-ui.html > /dev/null 2>&1; then
    echo -e "${GREEN}✅ HTTP сервер отвечает${NC}"
else
    echo -e "${YELLOW}⚠️  HTTP сервер еще не готов (может быть в процессе запуска)${NC}"
fi
echo ""

# 3. Проверка Swagger UI
echo "3️⃣  Проверка Swagger UI..."
if curl -s -f http://localhost:8080/swagger-ui.html | grep -q "swagger" 2>/dev/null; then
    echo -e "${GREEN}✅ Swagger UI доступен: http://localhost:8080/swagger-ui.html${NC}"
else
    echo -e "${YELLOW}⚠️  Swagger UI еще не готов${NC}"
fi
echo ""

# 4. Проверка GraphiQL
echo "4️⃣  Проверка GraphiQL..."
if curl -s -f http://localhost:8080/graphiql | grep -q "graphiql" 2>/dev/null; then
    echo -e "${GREEN}✅ GraphiQL доступен: http://localhost:8080/graphiql${NC}"
else
    echo -e "${YELLOW}⚠️  GraphiQL еще не готов${NC}"
fi
echo ""

# 5. Проверка REST API
echo "5️⃣  Проверка REST API..."
API_RESPONSE=$(curl -s -w "\n%{http_code}" http://localhost:8080/documents/search?page=0&size=1 2>/dev/null)
HTTP_CODE=$(echo "$API_RESPONSE" | tail -1)
if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✅ REST API работает (HTTP $HTTP_CODE)${NC}"
    TOTAL=$(echo "$API_RESPONSE" | head -1 | grep -o '"totalElements":[0-9]*' | cut -d: -f2)
    if [ -n "$TOTAL" ]; then
        echo -e "   📊 Найдено документов в базе: ${GREEN}$TOTAL${NC}"
    fi
else
    echo -e "${YELLOW}⚠️  REST API вернул код: $HTTP_CODE${NC}"
fi
echo ""

# 6. Проверка OpenAPI документации
echo "6️⃣  Проверка OpenAPI документации..."
if curl -s -f http://localhost:8080/v3/api-docs | grep -q "openapi" 2>/dev/null; then
    echo -e "${GREEN}✅ OpenAPI документация доступна: http://localhost:8080/v3/api-docs${NC}"
else
    echo -e "${YELLOW}⚠️  OpenAPI документация еще не готова${NC}"
fi
echo ""

# 7. Проверка H2 Console
echo "7️⃣  Проверка H2 Console..."
if curl -s -f http://localhost:8080/h2-console | grep -q "H2" 2>/dev/null; then
    echo -e "${GREEN}✅ H2 Console доступен: http://localhost:8080/h2-console${NC}"
else
    echo -e "${YELLOW}⚠️  H2 Console еще не готов${NC}"
fi
echo ""

# Итоговый статус
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📋 Итоговый статус:"
echo ""
echo "🌐 Основные интерфейсы:"
echo "   • Swagger UI:    http://localhost:8080/swagger-ui.html"
echo "   • GraphiQL:     http://localhost:8080/graphiql"
echo "   • H2 Console:   http://localhost:8080/h2-console"
echo "   • OpenAPI JSON: http://localhost:8080/v3/api-docs"
echo ""
echo "💡 Если все проверки пройдены - приложение готово к работе!"
echo ""


