#!/bin/sh
set -e

if [ "${MONEY_REPORT_ENABLED:-false}" = "true" ]; then
    share_fstype=$(awk '$2 == "/app/reports/share" {print $3}' /proc/mounts)
    if [ "$share_fstype" != "cifs" ]; then
        echo "ERROR: /app/reports/share is not on a CIFS filesystem (found: ${share_fstype:-overlay/none})." >&2
        echo "ERROR: Ensure the CIFS share is mounted on the host before starting the container." >&2
        exit 1
    fi
fi

exec java -jar /app/app.jar
