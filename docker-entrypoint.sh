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

# Probe connectivity as startup diagnostics (best effort, do not block app startup).
if command -v curl >/dev/null 2>&1; then
    if curl -fsS --max-time 3 "${FUSEKI_EFFECTIVE_URL}/$/ping" >/dev/null 2>&1 || \
       curl -fsS --max-time 3 "${FUSEKI_EFFECTIVE_URL}" >/dev/null 2>&1; then
        echo "[startup] Fuseki connectivity check: OK"
    else
        echo "[WARN] Fuseki connectivity check failed for ${FUSEKI_EFFECTIVE_URL} (API will still start)"
    fi
fi

# Start the application
exec bin/hascoapi "$@"
