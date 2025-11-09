# Anubis OCR с Apache Tika и Tesseract в отдельном сервисе

## Архитектура

* Spring Boot-приложение **anubis-api** не содержит Tesseract внутри образа и обращается к внешнему HTTP-шлюзу.
* OCR-шлюз **anubis-ocr-gateway** собран на FastAPI, Apache Tika и Tesseract с моделями `tessdata_best` для грузинского (kat), русского (rus) и английского (eng) языков. Эндпоинты: `POST /ocr`, `GET /healthz`.
* При запуске из IDE бэкенд может автоматически поднять контейнер OCR через `docker compose` и дождаться его готовности.
* Для Docker-развёртывания предусмотрен общий `docker-compose.yml`, объединяющий API, OCR, PostgreSQL и Gotenberg.

## Переменные окружения бэкенда

| Переменная | Значение по умолчанию | Назначение |
|------------|----------------------|------------|
| `ANUBIS_DB_URL` | `jdbc:postgresql://db:5432/anubis` | JDBC-URL PostgreSQL |
| `ANUBIS_DB_USER` | `anubis` | Пользователь БД |
| `ANUBIS_DB_PASSWORD` | `anubis` | Пароль БД |
| `ANUBIS_OCR_ENABLED` | `true` | Включить/выключить интеграцию с OCR |
| `ANUBIS_OCR_SERVICE_URL` | `http://localhost:4101` | Базовый URL OCR-шлюза |
| `ANUBIS_OCR_LANGUAGES` | `kat+eng+rus` | Список языков для OCR-запроса |
| `ANUBIS_OCR_AUTOSTART` | `true` | Пытаться запустить контейнер OCR через `docker compose` при старте приложения |
| `ANUBIS_OCR_DOCKER_COMPOSE` | `docker/ocr/docker-compose.yml` | Путь к compose-файлу для автозапуска |
| `ANUBIS_OCR_DOCKER_SERVICE` | `ocr-gateway` | Имя сервиса в compose-файле |
| `ANUBIS_OCR_CONNECT_TIMEOUT` | `5s` | Таймаут установления соединения |
| `ANUBIS_OCR_READ_TIMEOUT` | `60s` | Таймаут чтения ответа |
| `ANUBIS_OCR_REQUEST_TIMEOUT` | `90s` | Общий таймаут HTTP-запроса |
| `ANUBIS_OCR_READINESS_TIMEOUT` | `120s` | Максимальное ожидание готовности OCR-шлюза |
| `ANUBIS_OCR_HEALTH_INTERVAL` | `2s` | Интервал повторных проверок `GET /healthz` |
| `ANUBIS_GOTENBERG_URL` | `http://gotenberg:3000` | URL сервиса превью |
| `ANUBIS_PREVIEW_ENABLED` | `true` | Управление генерацией превью |

## Переменные окружения OCR-шлюза

| Переменная | Значение по умолчанию | Описание |
|------------|-----------------------|----------|
| `OCR_LANGUAGES` | `kat+eng+rus`         | Языки Tesseract (используются модели `tessdata_best`) |
| `TIKA_MAX_FILE_SIZE` | `78 643 200` (75 МБ)  | Максимальный размер файла для обработки |

## Локальный запуск из IDE

1. Убедитесь, что установлен Docker (Docker Desktop / Docker Engine) и доступна команда `docker compose`.
2. Один раз соберите образ OCR-шлюза: 
   ```bash
     docker compose -f docker-compose.yml build ocr-gateway
   ```
3. To run use:
   ```bash
     docker run -d \
     --name anubis-ocr \
     -p 4101:4101 \
     anubis/ocr-gateway:latest
   ```
4. Проверка здоровья:
   ```bash
     curl -f http://localhost:4101/healthz
   ```
5. Запускайте `AnubisApplication` из IDE или командой:
   ```bash
   ./mvnw spring-boot:run
   ```
   При недоступности `http://localhost:4101/healthz` приложение выполнит `docker compose -f docker/ocr/docker-compose.yml up -d ocr-gateway` и будет ждать готовности OCR (по умолчанию до 120 секунд).
4. Для остановки контейнера вручную используйте `docker compose -f docker/ocr/docker-compose.yml down`.

> Если Docker недоступен или автозапуск не нужен, задайте `ANUBIS_OCR_AUTOSTART=false` и поднимите OCR-контейнер вручную.

## Локальный запуск всего стека в Docker

1. Соберите образы API и OCR:
   ```bash
   docker compose build
   ```
2. Поднимите стек:
   ```bash
   docker compose up -d
   ```
3. Основные сервисы:
   * API: `http://localhost:4100`
   * OCR-шлюз: `http://localhost:4101/healthz`
   * PostgreSQL: `localhost:5432`
   * Gotenberg: `http://localhost:3000`
4. Просмотр логов:
   ```bash
   docker compose logs -f api
   docker compose logs -f ocr-gateway
   ```
5. Остановка стека: `docker compose down`.

## Подготовка образов к деплою

1. Соберите образы и присвойте теги реестра:
   ```bash
   docker build -t registry.example.com/anubis/api:latest .
   docker build -t registry.example.com/anubis/ocr-gateway:latest ocr-service
   ```
2. (Опционально) Прогрейте OCR-образ локально, запустив `docker run --rm -p 4101:4101 registry.example.com/anubis/ocr-gateway:latest` и проверив `curl -f http://localhost:4101/healthz`.
3. Отправьте образы в реестр:
   ```bash
   docker push registry.example.com/anubis/api:latest
   docker push registry.example.com/anubis/ocr-gateway:latest
   ```
4. В production-compose отключите автозапуск (`ANUBIS_OCR_AUTOSTART=false`) и укажите опубликованные образы, чтобы управлять сервисами отдельно.

## Проверка работы OCR

1. Убедитесь, что контейнер запущен (`docker ps | grep ocr-gateway`).
2. Проверка здоровья:
   ```bash
   curl -f http://localhost:4101/healthz
   ```
3. Пример OCR-запроса:
   ```bash
   curl -X POST http://localhost:4101/ocr \
        -F "file=@sample.png" \
        -F "languages=kat+eng+rus"
   ```
4. В логах API появится запись о готовности OCR:
   `OCR-шлюз готов к работе: http://localhost:4101`.

## Превью документов (Gotenberg)

Для формирования превью используйте официальный образ:

```bash
docker run -d \
  --name gotenberg \
  -p 3000:3000 \
  -e LIBREOFFICE_ENABLE=true \
  -e CHROMIUM_DISABLE_GPU=true \
  -e PDF_ENGINES_ENABLE=true \
  gotenberg/gotenberg:8
```

Переменная `ANUBIS_GOTENBERG_URL` задаёт URL сервиса превью.
