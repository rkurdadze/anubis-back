# -------- Stage 1: Build (Maven) --------
FROM maven:3.9.8-eclipse-temurin-21 AS build

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests


# -------- Stage 2: Runtime --------
FROM eclipse-temurin:21-jdk

LABEL maintainer="anubis-dev"

# --- Установка зависимостей и Tesseract ---
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        tesseract-ocr \
        tesseract-ocr-eng \
        tesseract-ocr-rus \
        tesseract-ocr-geo \
        libtesseract-dev \
        fonts-dejavu-core \
        curl imagemagick && \
    rm -rf /var/lib/apt/lists/*


# --- Установка лучших моделей Tesseract ---
RUN mkdir -p /usr/share/tesseract-ocr/5/tessdata && \
    for lang in eng rus kat; do \
        echo "⬇️ Downloading best model for ${lang}..." && \
        curl -fsSL -o /usr/share/tesseract-ocr/5/tessdata/${lang}.traineddata \
        https://github.com/tesseract-ocr/tessdata_best/raw/main/${lang}.traineddata || \
        (echo "⚠️ Warning: failed to download ${lang}.traineddata, falling back to default package"); \
    done


# --- Настройки окружения ---
ENV APP_HOME=/opt/anubis
ENV TESSDATA_PREFIX=/usr/share/tesseract-ocr/5/tessdata
ENV ANUBIS_OCR_LANGUAGES="eng+rus+kat"
ENV LD_LIBRARY_PATH="/usr/lib/x86_64-linux-gnu"
ENV PATH="$PATH:/usr/bin"

WORKDIR $APP_HOME

# --- Копируем артефакт ---
COPY --from=build /app/target/anubis-back*.jar ./anubis-back.jar

EXPOSE 4100

# --- Проверка OCR перед запуском ---
RUN echo "Testing Tesseract installation..." && \
    tesseract --version && \
    echo "Available OCR languages:" && \
    tesseract --list-langs || true

# --- Запуск приложения ---
ENTRYPOINT ["java", "-jar", "anubis-back.jar"]
