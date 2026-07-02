#!/bin/bash

# Nom du réseau à vérifier
NETWORK_NAME="teastore-network"

# Vérifie si le réseau existe déjà
if ! docker network ls | grep -q "$NETWORK_NAME"; then
    echo "Le réseau '$NETWORK_NAME' n'existe pas. Création du réseau..."
    docker network create "$NETWORK_NAME"
else
    echo "Le réseau '$NETWORK_NAME' existe déjà."
fi

# Lancement de docker-compose
docker-compose -f docker/docker-compose_default.yaml -p adaptable_teastore up -d