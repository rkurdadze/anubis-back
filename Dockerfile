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

# --- Установка зависимостей ---
RUN apt-get update && apt-get install -y \
    tesseract-ocr \
    tesseract-ocr-eng \
    tesseract-ocr-rus \
    tesseract-ocr-geo \
    imagemagick \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

# --- Настройки окружения ---
ENV APP_HOME=/opt/anubis
ENV TESSDATA_PREFIX=/usr/share/tesseract-ocr/5/tessdata
ENV ANUBIS_OCR_LANGUAGES="kat+eng+rus"

WORKDIR $APP_HOME

# --- Копируем артефакт ---
COPY --from=build /app/target/anubis-back*.jar ./anubis-back.jar

EXPOSE 4100

# --- Запуск приложения ---
ENTRYPOINT ["java", "-jar", "anubis-back.jar"]