#!/bin/bash

# 1. Copier le JAR compilé depuis le dossier cible vers le dossier de build Docker
# On cible spécifiquement le JAR exécutable avec ses dépendances
JAR_SOURCE="../../target/io.github.brice10.siphonix-jar-with-dependencies.jar"

# (Note : selon comment Maven nomme le fichier, il peut s'appeler io.github.brice10.siphonix-1.0.0-jar-with-dependencies.jar. 
# Si la ligne au-dessus échoue, utilisez : JAR_SOURCE="../../target/*-jar-with-dependencies.jar" )

if ls ../../target/*-jar-with-dependencies.jar 1> /dev/null 2>&1; then
    echo "Copie du fichier Fat JAR vers le répertoire courant..."
    cp ../../target/*-jar-with-dependencies.jar ./siphonix-app.jar
else
    echo "Erreur : Le fichier *-jar-with-dependencies.jar n'existe pas."
    echo "Veuillez exécuter 'mvn clean package' à la racine du projet en premier."
    exit 1
fi

# 3. Build de l'image manuellement (docker compose ... up --build plante à l'étape interne
# "resolving provenance for metadata file" avec ce docker compose (bug connu), alors que le
# build lui-même reussit toujours. On builde donc avec `docker build` directement, qui n'a
# pas ce probleme, puis on laisse `up -d` (sans --build) demarrer avec l'image deja construite.
SIPHONIX_ARG="${SIPHONIX:-./siphonix-app.jar}"
if ! docker build -f Dockerfile.manager --build-arg SIPHONIX="$SIPHONIX_ARG" -t adaptable-teastore-image-image:latest .; then
    echo "Erreur : le build de l'image 'image' a echoue." >&2
    exit 1
fi

# 4. Lancement de docker-compose (sans --build, l'image vient d'etre construite ci-dessus)
docker-compose -f docker-compose.yml -p adaptable-teastore-image up -d