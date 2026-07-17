#!/usr/bin/env bash
# install-javabip-jars.sh
# À exécuter UNE SEULE FOIS depuis la racine du projet, avant "mvn compile"
# Installe les JARs locaux (dossier libs/) dans le dépôt Maven local (~/.m2)

set -e
LIBS_DIR="$(dirname "$0")/libs"

echo "==> Installation des JARs JavaBIP dans ~/.m2 ..."

mvn install:install-file -Dfile="$LIBS_DIR/org.javabip.api-0.1.0-SNAPSHOT.jar" \
    -DgroupId=org.javabip -DartifactId=org.javabip.api \
    -Dversion=0.1.0-SNAPSHOT -Dpackaging=jar

mvn install:install-file -Dfile="$LIBS_DIR/org.javabip.engine.api-0.1.0-SNAPSHOT.jar" \
    -DgroupId=org.javabip -DartifactId=org.javabip.engine.api \
    -Dversion=0.1.0-SNAPSHOT -Dpackaging=jar

mvn install:install-file -Dfile="$LIBS_DIR/org.javabip.engine.core-0.1.0-SNAPSHOT.jar" \
    -DgroupId=org.javabip -DartifactId=org.javabip.engine.core \
    -Dversion=0.1.0-SNAPSHOT -Dpackaging=jar

mvn install:install-file -Dfile="$LIBS_DIR/org.javabip.engine.bdd-0.1.0-SNAPSHOT.jar" \
    -DgroupId=org.javabip -DartifactId=org.javabip.engine.bdd \
    -Dversion=0.1.0-SNAPSHOT -Dpackaging=jar

mvn install:install-file -Dfile="$LIBS_DIR/org.javabip.engine.coordinator-0.1.0-SNAPSHOT.jar" \
    -DgroupId=org.javabip -DartifactId=org.javabip.engine.coordinator \
    -Dversion=0.1.0-SNAPSHOT -Dpackaging=jar

mvn install:install-file -Dfile="$LIBS_DIR/org.javabip.engine.factory-0.1.0-SNAPSHOT.jar" \
    -DgroupId=org.javabip -DartifactId=org.javabip.engine.factory \
    -Dversion=0.1.0-SNAPSHOT -Dpackaging=jar

mvn install:install-file -Dfile="$LIBS_DIR/org.javabip.glue-0.1.0-SNAPSHOT.jar" \
    -DgroupId=org.javabip -DartifactId=org.javabip.glue \
    -Dversion=0.1.0-SNAPSHOT -Dpackaging=jar

mvn install:install-file -Dfile="$LIBS_DIR/org.javabip.executor-0.1.0-SNAPSHOT.jar" \
    -DgroupId=org.javabip -DartifactId=org.javabip.executor \
    -Dversion=0.1.0-SNAPSHOT -Dpackaging=jar

mvn install:install-file -Dfile="$LIBS_DIR/javabdd-1.0b2.jar" \
    -DgroupId=net.sf.javabdd -DartifactId=javabdd \
    -Dversion=1.0b2 -Dpackaging=jar

echo ""
echo "==> Tous les JARs sont installés. Tu peux maintenant lancer : mvn compile"