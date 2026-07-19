FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Install Python 3 and dependencies for the transactions web app
RUN apk add --no-cache python3 py3-pip

COPY scripts/requirements.txt /app/scripts/requirements.txt
RUN pip3 install --no-cache-dir --break-system-packages -r /app/scripts/requirements.txt

RUN mkdir -p /app/logs /app/reconcile /app/reports/working /app/reports/share

COPY target/MiddleTier-Money-*.jar app.jar
COPY scripts/ /app/scripts/

RUN chmod +x /app/scripts/docker-entrypoint.sh

ENV LOGGING_FILE_NAME=/app/logs/money.log
ENV MONEY_RECONCILE_FILE_LOCATION=/app/reconcile
ENV MONEY_REPORT_WORKING=/app/reports/working
ENV MONEY_REPORT_SHARE=/app/reports/share
ENV MONEY_API_HOST=http://localhost:8080/api/v1

VOLUME ["/app/logs", "/app/reconcile", "/app/reports/working"]

EXPOSE 8080 8081

ENTRYPOINT ["/app/scripts/docker-entrypoint.sh"]
