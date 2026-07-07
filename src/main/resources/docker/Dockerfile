FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN mkdir -p /app/logs /app/reconcile /app/reports/working /app/reports/share

COPY target/MiddleTier-Money-*.jar app.jar

ENV LOGGING_FILE_NAME=/app/logs/money.log
ENV MONEY_RECONCILE_FILE_LOCATION=/app/reconcile
ENV MONEY_REPORT_WORKING=/app/reports/working
ENV MONEY_REPORT_SHARE=/app/reports/share

VOLUME ["/app/logs", "/app/reconcile", "/app/reports/working"]

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
