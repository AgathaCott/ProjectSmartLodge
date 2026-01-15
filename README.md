# SmartLodge — Система бронирований отелей

Многомодульное микросервисное приложение на Spring Boot 3.4.1 и Spring Cloud 2024.0.3:
- **API Gateway** — точка входа с маршрутизацией и JWT-валидацией (Spring Cloud Gateway)
- **Booking Service** — аутентификация пользователей, управление бронированиями (WebFlux + JPA)
- **Hotel Service** — управление отелями и номерами с резервированием (Spring MVC + JPA)
- **Eureka Server** — Service Registry для динамического обнаружения сервисов

Все сервисы используют встроенную БД H2. Взаимодействие между сервисами выполняется через REST API с поддержкой согласованности на уровне локальных транзакций и компенсирующих действий.

## Ключевые возможности

### Аутентификация и авторизация
- JWT-токены с HMAC подписью
- Ролевая модель (USER/ADMIN)
- OAuth2 Resource Server интеграция

### Бронирование
- Двухшаговый процесс с hold/confirm/release
- Идемпотентность через requestId
- Автоматическая компенсация при сбоях
- Корреляция запросов через X-Correlation-Id

### Управление данными
- DTO layer с валидацией (Jakarta Validation)
- Централизованная обработка исключений
- Lombok для сокращения boilerplate кода
- OpenAPI документация (Swagger UI)

### Надёжность
- Retry логика с экспоненциальным backoff
- Таймауты для удалённых вызовов
- Graceful degradation при недоступности сервисов

## Технологический стек

- **Java:** 17
- **Spring Boot:** 3.4.1
- **Spring Cloud:** 2024.0.3
- **База данных:** H2 (in-memory)
- **Security:** Spring Security 6.4.2 + JWT
- **Документация:** SpringDoc OpenAPI 2.5.0
- **Сборка:** Maven 3.9+

## Архитектура

### Порты сервисов
- `eureka-server`: 8761
- `api-gateway`: 8080
- `hotel-service`: 8081
- `booking-service`: 8082

### Взаимодействие
Gateway маршрутизирует все запросы к микросервисам через Eureka по serviceId. JWT токен проксируется в заголовке Authorization для валидации на уровне каждого сервиса.

## Быстрый старт

### 1. Сборка проекта
```bash
cd SmartLodge
mvn clean install -DskipTests -Dmaven.test.skip=true
```

### 2. Запуск сервисов

**Вариант A: Через Maven**
```bash
# Терминал 1: Eureka
mvn -pl eureka-server spring-boot:run

# Терминал 2: Hotel Service
cd hotel-service && mvn clean package spring-boot:repackage -Dmaven.test.skip=true
java -jar target/hotel-service-0.0.1-SNAPSHOT.jar

# Терминал 3: Booking Service
cd booking-service && mvn clean package spring-boot:repackage -Dmaven.test.skip=true
java -jar target/booking-service-0.0.1-SNAPSHOT.jar

# Терминал 4: API Gateway (опционально)
mvn -pl api-gateway spring-boot:run
```

**Вариант B: Через JAR файлы**
```bash
# После сборки запустить каждый сервис в отдельном терминале
java -jar eureka-server/target/eureka-server-0.0.1-SNAPSHOT.jar
java -jar hotel-service/target/hotel-service-0.0.1-SNAPSHOT.jar
java -jar booking-service/target/booking-service-0.0.1-SNAPSHOT.jar
```

### 3. Проверка статуса
- Eureka Dashboard: http://localhost:8761
- Swagger UI Hotel: http://localhost:8081/swagger-ui.html
- Swagger UI Booking: http://localhost:8082/swagger-ui.html


## Конфигурация

### JWT секрет
Для работы требуется согласованный секрет во всех сервисах. По умолчанию используется `dev-secret-please-change`.

Настройка в файлах `application.yml`:
- `api-gateway/src/main/resources/application.yml`
- `hotel-service/src/main/resources/application.yml`
- `booking-service/src/main/resources/application.yml`

**Важно:** Для production окружения замените на сильный секрет или используйте внешний OAuth2 провайдер (Keycloak, Auth0).

### База данных
Используется H2 в режиме in-memory. Схемы создаются автоматически при старте через Hibernate DDL.

H2 консоль доступна для hotel-service:
- URL: http://localhost:8081/h2-console
- JDBC URL: `jdbc:h2:mem:hotel`
- Username: `sa`
- Password: (пусто)

## Примеры использования API

### 1. Регистрация пользователя
```bash
curl -X POST http://localhost:8082/auth/register \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "user1",
    "password": "password123"
  }'
```

Для регистрации администратора добавьте поле `"admin": true`.

### 2. Получение JWT токена
```bash
curl -X POST http://localhost:8082/auth/login \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "user1",
    "password": "password123"
  }'
```

Ответ:
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiJ9...",
  "token_type": "Bearer"
}
```

Сохраните токен в переменную:
```bash
TOKEN="<полученный_токен>"
```

### 3. Создание отеля (требуется роль ADMIN)
```bash
curl -X POST http://localhost:8081/hotels \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "Grand Hotel",
    "city": "Москва",
    "address": "Тверская улица, 1"
  }'
```

### 4. Создание номера (требуется роль ADMIN)
```bash
curl -X POST http://localhost:8081/rooms \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "hotelId": 1,
    "number": "101",
    "capacity": 2,
    "pricePerNight": 5000.00,
    "available": true
  }'
```

### 5. Получение списка отелей
```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8081/hotels
```

### 6. Создание бронирования
```bash
curl -X POST http://localhost:8082/bookings \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "roomId": 1,
    "startDate": "2026-02-15",
    "endDate": "2026-02-20",
    "requestId": "unique-request-id-123"
  }'
```

**Примечание:** `requestId` обеспечивает идемпотентность — повторные запросы с тем же ID не создадут дубликатов.

### 7. Просмотр своих бронирований
```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8082/bookings
```

### 8. Получение статистики популярных номеров
```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8081/stats/rooms/popular
```

## API Endpoints

### Authentication Service (Booking Service)
| Метод | Endpoint | Описание | Доступ |
|-------|----------|----------|--------|
| POST | `/auth/register` | Регистрация пользователя | Public |
| POST | `/auth/login` | Вход и получение JWT | Public |

### Booking Service
| Метод | Endpoint | Описание | Доступ |
|-------|----------|----------|--------|
| GET | `/bookings` | Список бронирований пользователя | User |
| POST | `/bookings` | Создание бронирования | User |
| GET | `/bookings/all` | Все бронирования (admin) | Admin |

### Hotel Service
| Метод | Endpoint | Описание | Доступ |
|-------|----------|----------|--------|
| GET | `/hotels` | Список всех отелей | User |
| GET | `/hotels/{id}` | Детали отеля | User |
| POST | `/hotels` | Создание отеля | Admin |
| PUT | `/hotels/{id}` | Обновление отеля | Admin |
| DELETE | `/hotels/{id}` | Удаление отеля | Admin |
| GET | `/rooms/{id}` | Детали номера | User |
| POST | `/rooms` | Создание номера | Admin |
| PUT | `/rooms/{id}` | Обновление номера | Admin |
| DELETE | `/rooms/{id}` | Удаление номера | Admin |
| POST | `/rooms/{id}/hold` | Резервирование номера | System |
| POST | `/rooms/{id}/confirm` | Подтверждение резерва | System |
| POST | `/rooms/{id}/release` | Освобождение резерва | System |

### Statistics (Hotel Service)
| Метод | Endpoint | Описание | Доступ |
|-------|----------|----------|--------|
| GET | `/stats/rooms/popular` | Популярные номера | User |

## Архитектурные решения

### Согласованность данных
Система использует паттерн Saga с компенсирующими транзакциями:

1. **PENDING** — создание бронирования в booking-service
2. **HOLD** — резервирование номера в hotel-service
3. **CONFIRM** — подтверждение успешного бронирования
4. **RELEASE** (при ошибке) — освобождение номера и отмена бронирования

### Идемпотентность
Все критичные операции поддерживают идемпотентность через `requestId`:
- Повторные запросы с тем же requestId возвращают результат первой операции
- Предотвращает дублирование бронирований при сетевых сбоях

### Отказоустойчивость
- Retry механизм с экспоненциальным backoff (3 попытки)
- Таймауты на удалённые вызовы (5 секунд)
- Circuit breaker паттерн (рекомендуется добавить Resilience4j)

### Трассировка
Каждый запрос сопровождается заголовком `X-Correlation-Id` для сквозного отслеживания в логах.

## Тестирование

### Запуск unit тестов
```bash
mvn test
```

### Запуск integration тестов
```bash
mvn verify
```

### Покрытие тестами
- **BookingService:** Идемпотентность, компенсация, валидация
- **HotelService:** CRUD операции, hold/confirm/release логика
- **Integration:** End-to-end сценарии с WireMock

## Структура проекта

```
SmartLodge/
├── eureka-server/          # Service Discovery
├── api-gateway/            # API Gateway
├── hotel-service/          # Управление отелями
│   ├── dto/               # Data Transfer Objects
│   ├── model/             # JPA Entities
│   ├── repo/              # Spring Data Repositories
│   ├── service/           # Бизнес-логика
│   ├── web/               # REST Controllers
│   ├── exception/         # Exception Handlers
│   └── security/          # JWT Configuration
├── booking-service/        # Бронирования и Auth
│   ├── dto/               # Data Transfer Objects
│   ├── model/             # JPA Entities
│   ├── repo/              # Spring Data Repositories
│   ├── service/           # Бизнес-логика
│   ├── web/               # REST Controllers
│   ├── exception/         # Exception Handlers
│   ├── security/          # JWT Configuration
│   └── constants/         # Константы
└── pom.xml                # Parent POM
```

## Дальнейшее развитие

### Рекомендуемые улучшения
1. Добавить Resilience4j для Circuit Breaker
2. Интегрировать Spring Cloud Sleuth для distributed tracing
3. Настроить централизованную конфигурацию (Spring Cloud Config)
4. Добавить rate limiting на Gateway
5. Реализовать caching (Redis/Caffeine)
6. Добавить metrics (Prometheus + Grafana)
7. Настроить production-ready БД (PostgreSQL)
8. Добавить Docker Compose для простого развёртывания

---  
**Дата:** Январь 2026
