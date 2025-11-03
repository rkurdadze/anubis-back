## Переменные окружения для Docker

- `ANUBIS_DB_URL=jdbc:postgresql://db:5432/anubis`
- `ANUBIS_DB_USER=anubis`
- `ANUBIS_DB_PASSWORD=anubis`
- `ANUBIS_OCR_ENABLED=true`
- `ANUBIS_OCR_LANGUAGES=kat+eng+rus`
- `ANUBIS_OCR_DATAPATH=/usr/share/tesseract-ocr/5/tessdata`
- `ANUBIS_GOTENBERG_URL=http://gotenberg:3000`
- `ANUBIS_PREVIEW_ENABLED=true`

### Gotenberg + LibreOffice + Chromium

Для генерации PDF-превью необходимо запустить сервис Gotenberg рядом с API. Пример запуска в Docker:

```bash
docker run -d \
  --name gotenberg \
  -p 3000:3000 \
  -e LIBREOFFICE_ENABLE=true \
  -e CHROMIUM_DISABLE_GPU=true \
  -e PDF_ENGINES_ENABLE=true \
  gotenberg/gotenberg:8
```

API обращается к сервису по адресу `http://gotenberg:3000` (можно изменить переменной `ANUBIS_GOTENBERG_URL`).
