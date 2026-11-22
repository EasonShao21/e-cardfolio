# [AI assisted chat-017] Build Vue assets and package them into Spring Boot for a same-origin deployment.
FROM node:22-alpine AS frontend-build
WORKDIR /build/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM maven:3.9-eclipse-temurin-21-alpine AS backend-build
WORKDIR /build
COPY backend/pom.xml ./backend/pom.xml
COPY backend/src/ ./backend/src/
COPY --from=frontend-build /build/frontend/dist/ ./backend/src/main/resources/static/
RUN mvn -f backend/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
COPY --from=backend-build /build/backend/target/e-cardfolio-0.0.1-SNAPSHOT.jar ./app.jar
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar /app/app.jar"]
