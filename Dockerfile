FROM maven:3.9.8-eclipse-temurin-21 AS build

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests


# -------- Stage 2: Runtime --------
FROM eclipse-temurin:21-jre

LABEL maintainer="anubis-dev"

ENV APP_HOME=/opt/anubis
WORKDIR $APP_HOME

COPY --from=build /app/target/anubis-back*.jar ./anubis-back.jar

EXPOSE 4100

ENTRYPOINT ["java", "-jar", "anubis-back.jar"]
