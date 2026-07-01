#!/bin/bash
set -e

# Remove stale RUNNING_PID file if it exists
# This prevents Play Framework from refusing to start after container restarts
if [ -f /hascoapi/RUNNING_PID ]; then
    echo "Removing stale RUNNING_PID file..."
    rm -f /hascoapi/RUNNING_PID
fi

R_BIN="${R_SCRIPT_BIN:-Rscript}"
if ! command -v "${R_BIN}" >/dev/null 2>&1; then
    echo "[ERROR] R runtime not found. Expected executable: ${R_BIN}"
    exit 1
fi
echo "[startup] R runtime: $(command -v "${R_BIN}")"

FUSEKI_EFFECTIVE_URL="${FUSEKI_URL:-http://localhost:3030}"
echo "[startup] Effective FUSEKI_URL=${FUSEKI_EFFECTIVE_URL}"

# Wait for Fuseki to be fully ready (up to 60 seconds)
if command -v curl >/dev/null 2>&1; then
    echo "[startup] Waiting for Fuseki to be ready..."
    MAX_WAIT=60
    COUNTER=0
    until curl -fsS --max-time 2 "${FUSEKI_EFFECTIVE_URL}/$/ping" >/dev/null 2>&1 || [ $COUNTER -eq $MAX_WAIT ]; do
        echo "[startup] Waiting for Fuseki... ($COUNTER/$MAX_WAIT)"
        sleep 2
        COUNTER=$((COUNTER+2))
    done
    
    if [ $COUNTER -eq $MAX_WAIT ]; then
        echo "[WARN] Fuseki did not respond within ${MAX_WAIT} seconds (API will still start)"
    else
        echo "[startup] Fuseki connectivity check: OK (after ${COUNTER}s)"
    fi
fi

# Debug: Show environment and arguments
echo "[startup] ====== PRODUCTION LAUNCHER DEBUG ======"
echo "[startup] JAVA_OPTS: ${JAVA_OPTS}"
echo "[startup] Using config: hascoapi-docker.conf"
echo "[startup] About to execute: bin/hascoapi -v -Dconfig.resource=hascoapi-docker.conf $@"
echo "[startup] =========================================="

# Start the application with hascoapi-docker.conf
exec bin/hascoapi -v -Dconfig.resource=hascoapi-docker.conf "$@"
