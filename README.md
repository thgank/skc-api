# SKC Purchase Requisition API

Добрый день!
Меня зовут Сериков Нурсултан, данный репозиторий является моим решением для тестового задания в рамках найма в SKC.
REST API сервис для управления позициями в заявке на закупку.

## Стек технологий

- **Java 21** + **Spring Boot 4.0.2** - вынужденное отклонение от условий ТЗ (проект на версии SpringBoot 2.7.x уже собрать невозможно)
- **Spring Data JPA** + **Hibernate** (ORM)
- **H2 Database** — in-memory, без внешних зависимостей
- **Spring Security** — Basic Auth
- **Bean Validation** + кастомные бизнес-проверки
- **springdoc-openapi** — Swagger UI
- **Lombok** — сокращение boilerplate
- **JUnit 5** + **Mockito** + **RestTestClient** — тесты

## Быстрый запуск

```bash
# Клонировать и перейти в директорию
cd skc-api

# Запуск приложения
./gradlew bootRun

# Запуск тестов
./gradlew clean test
```

После запуска:
- **API**: http://localhost:8080/api/v1/requisitions/
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **H2 Console**: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:skcdb`, user: `sa`, пароль пустой)

## Аутентификация

Basic Auth с двумя пользователями.

По умолчанию (локальная разработка):

| Логин   | Пароль  | Роль  |
|---------|---------|-------|
| `admin` | `admin` | ADMIN |
| `user`  | `user`  | USER  |

Переопределение через переменные окружения:

- `APP_SECURITY_ADMIN_USERNAME` (default: `admin`)
- `APP_SECURITY_ADMIN_PASSWORD` (default: `admin`)
- `APP_SECURITY_USER_USERNAME` (default: `user`)
- `APP_SECURITY_USER_PASSWORD` (default: `user`)

Для Railway задайте эти значения в `Service -> Variables`.

## API Эндпоинты

### Создание позиции
```bash
curl -X POST http://localhost:8080/api/v1/requisitions/1/items \
  -u admin:admin \
  -H "Content-Type: application/json" \
  -d '{
    "nomenclatureCode": "TRU-005",
    "nomenclatureName": "Скрепки канцелярские",
    "quantity": 200,
    "unitCode": "BOX",
    "priceWithoutVat": 150.00,
    "desiredDeliveryDate": "2026-04-01",
    "comment": "Срочная поставка"
  }'
```

### Обновление позиции (PATCH)
```bash
curl -X PATCH http://localhost:8080/api/v1/requisitions/1/items/1 \
  -u admin:admin \
  -H "Content-Type: application/json" \
  -d '{
    "quantity": 200,
    "desiredDeliveryDate": "2026-05-01",
    "comment": "Увеличили количество",
    "version": 0
  }'
```

### Удаление позиции
```bash
curl -X DELETE http://localhost:8080/api/v1/requisitions/1/items/2 \
  -u admin:admin
```

### Сводка по заявке
```bash
curl -X GET http://localhost:8080/api/v1/requisitions/1/summary \
  -u admin:admin
```

**Пример ответа:**
```json
{
  "totalAmountWithoutVat": 36275.00,
  "totalQuantity": 150,
  "minDesiredDeliveryDate": "2026-03-09",
  "maxDesiredDeliveryDate": "2026-03-09",
  "itemCount": 2,
  "currency": "KZT"
}
```

### Реактивация заявки (CANCELLED → DRAFT)
```bash
curl -X POST http://localhost:8080/api/v1/requisitions/4/reactivate \
  -u admin:admin
```
> Ответ: **204 No Content**. После реактивации заявка переходит в статус DRAFT и становится доступной для редактирования позиций.
> Доступно только для заявок в статусе CANCELLED. Для остальных статусов — 400 Bad Request.

### Пример ошибки
```bash
# Попытка создать позицию в APPROVED заявке
curl -X POST http://localhost:8080/api/v1/requisitions/2/items \
  -u admin:admin \
  -H "Content-Type: application/json" \
  -d '{
    "nomenclatureCode": "TRU-001",
    "nomenclatureName": "Бумага офисная A4",
    "quantity": 10,
    "unitCode": "PACK",
    "priceWithoutVat": 350.00,
    "desiredDeliveryDate": "2026-04-01"
  }'
```

**Ответ (400):**
```json
{
  "errorCode": "REQUISITION_NOT_IN_DRAFT",
  "message": "Операция запрещена: заявка в статусе APPROVED",
  "field": null,
  "rejectedValue": null
}
```

## Бизнес-правила

### Создание позиции (POST)
1. Заявка должна быть в статусе **DRAFT**
2. `quantity` > 0
3. `priceWithoutVat` ≥ 0
4. `desiredDeliveryDate` не ранее **сегодня + 3 дня**
5. `unitCode` должен быть разрешён для данной номенклатуры (справочник)
6. Запрещены дубликаты `nomenclatureCode` внутри одной заявки
7. `nomenclatureCode` должен существовать в справочнике
8. `nomenclatureName` должен совпадать со справочным
9. `rowNumber` назначается автоматически: `max(rowNumber) + 1`
10. После создания пересчитывается `totalLotSumNoNds` заявки

### Обновление позиции (PATCH)
1. Заявка должна быть в **DRAFT**
2. Разрешено менять только: `quantity`, `desiredDeliveryDate`, `comment`
3. `quantity` не может быть меньше 1
4. `desiredDeliveryDate` не ранее **сегодня + 3 дня**
5. Обязательна передача `version` для **оптимистичной блокировки**
6. При конфликте версий — **409 Conflict**

### Удаление позиции (DELETE)
1. Заявка должна быть в **DRAFT**
2. Запрещено удалять **последнюю** позицию в заявке
3. После удаления пересчитывается сумма заявки
4. **Перенумерация rowNumber не выполняется** — остаются «дырки». Это осознанный компромисс: перенумерация может сломать внешние ссылки на rowNumber; при необходимости можно добавить отдельный endpoint `/reorder`

## Статусы заявок

В системе используется расширенный набор статусов:

| Статус          | Описание                            | Редактирование позиций |
|-----------------|-------------------------------------|------------------------|
| `DRAFT`         | Черновик                            | ✅ Разрешено           |
| `SUBMITTED`     | Подана на рассмотрение              | ❌ Только чтение       |
| `APPROVED`      | Согласована                         | ❌ Только чтение       |
| `IN_PROCUREMENT`| В процессе закупки                  | ❌ Только чтение       |
| `CLOSED`        | Закрыта                             | ❌ Только чтение       |
| `REJECTED`      | Отклонена                           | ❌ Только чтение       |
| `CANCELLED`     | Отменена                            | ❗ Только чтение (но можно реактивировать в DRAFT) |

> **Примечание:** `SUBMITTED`, `IN_PROCUREMENT`, `REJECTED` — расширение базового набора (DRAFT/APPROVED/CANCELLED/CLOSED). Они присутствуют в enum для полноты модели, но поведение одинаковое: любой не-DRAFT статус запрещает мутации позиций.

## Маппинг полей Entity ↔ API

| Entity (БД)     | API (DTO)             | Описание                    |
|------------------|-----------------------|-----------------------------|
| `truCode`        | `nomenclatureCode`    | Код номенклатуры            |
| `truName`        | `nomenclatureName`    | Наименование номенклатуры   |
| `count`          | `quantity`            | Количество                  |
| `mkei`           | `unitCode`            | Код единицы измерения       |
| `price`          | `priceWithoutVat`     | Цена без НДС                |
| `durationMonth`  | `desiredDeliveryDate` | Желаемая дата поставки      |

## Формат ошибок

Все ошибки возвращаются в едином формате:

```json
{
  "errorCode": "ERROR_CODE_ENUM",
  "message": "Человекочитаемое описание",
  "field": "имя_поля_или_null",
  "rejectedValue": "отвергнутое_значение_или_null"
}
```

### Коды ошибок

| Код                                  | HTTP | Описание                                       |
|--------------------------------------|------|-------------------------------------------------|
| `REQUISITION_NOT_FOUND`              | 404  | Заявка не найдена                               |
| `ITEM_NOT_FOUND`                     | 404  | Позиция не найдена                              |
| `REQUISITION_NOT_IN_DRAFT`           | 400  | Заявка не в статусе DRAFT                       |
| `NOMENCLATURE_NOT_FOUND`             | 400  | Номенклатура не найдена в справочнике           |
| `NOMENCLATURE_NAME_MISMATCH`         | 400  | Наименование не совпадает со справочником       |
| `UNIT_NOT_ALLOWED_FOR_NOMENCLATURE`  | 400  | Единица измерения не разрешена для номенклатуры |
| `DUPLICATE_NOMENCLATURE_IN_REQUISITION` | 400 | Дубликат номенклатуры в заявке                 |
| `INVALID_DELIVERY_DATE`              | 400  | Дата поставки раньше допустимой                 |
| `INVALID_QUANTITY`                   | 400  | Недопустимое количество                         |
| `LAST_ITEM_DELETE_FORBIDDEN`         | 400  | Нельзя удалить последнюю позицию                |
| `OPTIMISTIC_LOCK_CONFLICT`           | 409  | Конфликт версий (оптимистичная блокировка)      |
| `INVALID_STATUS_TRANSITION`          | 400  | Недопустимый переход статуса заявки              |

## Оптимистичная блокировка

Реализована через `@Version` (JPA/Hibernate):

1. Клиент получает позицию с текущей `version`
2. При PATCH передаёт `version` в теле запроса
3. Сервис сверяет `version` из запроса с `version` из БД
4. При несовпадении — **409 Conflict** (app-level check)
5. При параллельном обновлении Hibernate также проверяет версию на уровне SQL (`WHERE version = ?`)
6. `ObjectOptimisticLockingFailureException` перехватывается `@ControllerAdvice` → 409

## Тестовые данные

При старте создаются 4 заявки:

| ID | Номер           | Статус    | Позиций | Сумма без НДС |
|----|-----------------|-----------|---------|---------------|
| 1  | ЗК-2025-00001   | DRAFT     | 2       | 36 275.00     |
| 2  | ЗК-2025-00002   | APPROVED  | 1       | 22 500.00     |
| 3  | ЗК-2025-00003   | CLOSED    | 2       | 11 600.00     |
| 4  | ЗК-2025-00004   | CANCELLED | 1       | 5 000.00      |

### Справочник номенклатур (12 шт.)

| Код     | Наименование             | Допустимые единицы     |
|---------|--------------------------|------------------------|
| TRU-001 | Бумага офисная A4        | PIECE, PACK, BOX       |
| TRU-002 | Картридж для принтера    | PIECE                  |
| TRU-003 | Ручка шариковая          | PIECE, PACK, BOX       |
| TRU-004 | Папка-регистратор        | PIECE                  |
| TRU-005 | Скрепки канцелярские     | PACK, BOX              |
| TRU-006 | Степлер                  | PIECE                  |
| TRU-007 | Клей-карандаш            | PIECE, PACK            |
| TRU-008 | Маркер текстовый         | PIECE, PACK, SET       |
| TRU-009 | Ножницы офисные          | PIECE                  |
| TRU-010 | Калькулятор              | PIECE                  |
| TRU-011 | Блокнот А5               | PIECE, PACK            |
| TRU-012 | Файл-вкладыш            | PACK, BOX              |

### Единицы измерения (7 шт.)

| Код    | Наименование |
|--------|-------------|
| PIECE  | Штука       |
| PACK   | Упаковка    |
| BOX    | Коробка     |
| KG     | Килограмм   |
| LITER  | Литр        |
| METER  | Метр        |
| SET    | Комплект    |

## Тесты

```bash
./gradlew clean test
```

- Всего: **38** тестов
- **23 unit-теста** (`service`-слой, JUnit 5 + Mockito)
- **15 интеграционных тестов** (`integration`-слой, Spring Boot + RestTestClient)
- В интеграционных включён тест оптимистичной блокировки (`OptimisticLockingTest`) с проверкой **409 Conflict**

## Архитектура

```
com.tslnkk.skcapi/
├── aspect/           # LoggingAspect
├── config/           # SecurityConfig, OpenApiConfig
├── controller/       # REST контроллеры
├── domain/           # JPA сущности (PurchaseRequisition, RequisitionItem, RequisitionStatus)
├── dto/              # Request/Response DTO (records)
├── exception/        # ErrorCode, BusinessException, GlobalExceptionHandler
├── init/             # DataInitializer (тестовые данные)
├── reference/        # ReferenceDataService (справочники в памяти)
├── repository/       # Spring Data JPA репозитории
└── service/          # Бизнес-логика (RequisitionItemService)
```
