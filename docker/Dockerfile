FROM eclipse-temurin:8-jdk AS builder

WORKDIR /app
COPY . .
RUN ./gradlew shadowJar --no-daemon

FROM eclipse-temurin:8-jre

WORKDIR /workspaces
COPY --from=builder /app/build/libs/md-test-spec2excel-1.0.0.jar /app/md-test-spec2excel.jar

ENTRYPOINT ["java", "-jar", "/app/md-test-spec2excel.jar"]
