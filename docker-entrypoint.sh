#!/bin/bash
set -e

# Remove stale RUNNING_PID file if it exists
# This prevents Play Framework from refusing to start after container restarts
if [ -f /hascoapi/RUNNING_PID ]; then
    echo "Removing stale RUNNING_PID file..."
    rm -f /hascoapi/RUNNING_PID
fi

# Load pharma ontology into Fuseki
if [ -f /hascoapi/load-pharma-ontology.sh ]; then
    bash /hascoapi/load-pharma-ontology.sh &
fi

# Start the application
exec bin/hascoapi "$@"
