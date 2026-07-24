#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

mvn clean install -DskipTests

cd "$SCRIPT_DIR/samples/adaptable-teastore-image"
sh run.debug.sh

CONTAINER_ID=$(docker ps --format '{{.ID}} {{.Names}}' | grep -E '[-_]image[-_][0-9]+$' | awk '{print $1}' | head -n1)

if [ -z "$CONTAINER_ID" ]; then
    echo "Erreur : conteneur 'image' introuvable." >&2
    exit 1
fi

echo "Conteneur image : $CONTAINER_ID"
docker logs -f "$CONTAINER_ID"
