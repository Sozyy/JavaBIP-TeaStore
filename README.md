# Terminal A - lancer le TeaStore (depuis experimentation-platform)
cd experimentation-platform/Sources/examples/docker
docker-compose -f docker-compose_default.yaml up -d

# Vérifier que l'image service est bien accessible :
curl http://localhost:8083/tools.descartes.teastore.image/rest/metrics/state
# -> doit retourner RUNNING

# Terminal B - lancer SiphoniX (depuis siphonix-integrated)
cd siphonix-integrated
./install-javabip-jars.sh    # une seule fois
mvn clean package -DskipTests
java -cp target/io.github.brice10.siphonix-jar-with-dependencies.jar adapteastore.Main