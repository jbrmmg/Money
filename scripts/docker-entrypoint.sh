#!/bin/sh
set -e

# Start the Python transactions web app in the background
gunicorn \
  --bind 0.0.0.0:8081 \
  --workers 2 \
  --chdir /app/scripts \
  transactions_web:app &

# Start the Java application in the foreground (PID 1)
exec java -jar /app/app.jar
