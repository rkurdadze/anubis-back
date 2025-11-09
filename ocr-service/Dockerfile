FROM python:3.11-slim AS base

ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1

# ------------------------------------------------------
# 🧩 Системные зависимости
# ------------------------------------------------------
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        tesseract-ocr \
        tesseract-ocr-eng \
        tesseract-ocr-rus \
        tesseract-ocr-kat \
        libtesseract-dev \
        openjdk-21-jre-headless \
        curl \
        ca-certificates \
        unzip \
        wget && \
    rm -rf /var/lib/apt/lists/*

# ------------------------------------------------------
# 📦 Установка tessdata_best и tessdata_fast
# ------------------------------------------------------
RUN mkdir -p /usr/share/tesseract-ocr/5/tessdata && \
    cd /usr/share/tesseract-ocr/5/tessdata && \
    for lang in eng rus kat; do \
        echo "⬇️ Installing tessdata_best model for ${lang}" && \
        curl -fsSL -o ${lang}.traineddata \
            https://github.com/tesseract-ocr/tessdata_best/raw/main/${lang}.traineddata && \
        echo "⬇️ Installing tessdata_fast model for ${lang}" && \
        curl -fsSL -o ${lang}_fast.traineddata \
            https://github.com/tesseract-ocr/tessdata_fast/raw/main/${lang}.traineddata; \
    done

# ------------------------------------------------------
# 📚 Установка Apache Tika (Server JAR 3.2.3)
# ------------------------------------------------------
ENV TIKA_VERSION=3.2.3
RUN mkdir -p /opt/tika && \
    echo "⬇️ Downloading Apache Tika ${TIKA_VERSION}" && \
    curl -fsSL -o /opt/tika/tika-server-standard-${TIKA_VERSION}.jar \
        https://dlcdn.apache.org/tika/${TIKA_VERSION}/tika-server-standard-${TIKA_VERSION}.jar && \
    ln -sf /opt/tika/tika-server-standard-${TIKA_VERSION}.jar /opt/tika/tika-server.jar

# ------------------------------------------------------
# 🐍 Установка Python-зависимостей
# ------------------------------------------------------
WORKDIR /opt/ocr

COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

COPY main.py ./main.py

# ------------------------------------------------------
# ⚙️ Прогрев Apache Tika
# ------------------------------------------------------
RUN echo "🔥 Warming up Apache Tika server..." && \
    java -version && \
    java -jar /opt/tika/tika-server.jar --help || true && \
    python3 - <<'PY'
from tika import parser
parser.from_buffer(b"WARMUP", xmlContent=False)
PY

# ------------------------------------------------------
# 🌍 Переменные окружения
# ------------------------------------------------------
ENV TIKA_SERVER_JAR=/opt/tika/tika-server.jar \
    TIKA_SERVER_ENDPOINT=http://127.0.0.1:9998 \
    OCR_LANGUAGES=kat+eng+rus \
    TESSDATA_PREFIX=/usr/share/tesseract-ocr/5/tessdata \
    LANG=ka_GE.UTF-8 \
    LC_ALL=ka_GE.UTF-8

EXPOSE 4101

# ------------------------------------------------------
# 🧠 Запуск Tika + FastAPI
# ------------------------------------------------------
CMD java -jar /opt/tika/tika-server.jar > /tmp/tika.log 2>&1 & \
    echo "Waiting for Apache Tika to start..." && sleep 3 && \
    uvicorn main:app --host 0.0.0.0 --port 4101

# ------------------------------------------------------
# ❤️ Healthcheck — проверяет готовность OCR API
# ------------------------------------------------------
#HEALTHCHECK --interval=20s --timeout=5s --retries=3 \
#    CMD curl -fs http://localhost:4101/healthz || exit 1
