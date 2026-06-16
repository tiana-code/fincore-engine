FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace
COPY . .
RUN chmod +x gradlew \
 && ./gradlew :services:ledger:bootJar --no-daemon -x test \
 && find services/ledger/build/libs -name '*.jar' ! -name '*-plain.jar' -exec cp {} /workspace/app.jar \;

FROM gcr.io/distroless/java21-debian12:nonroot AS runtime
WORKDIR /app
COPY --from=builder /workspace/app.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
