#!/usr/bin/env bash
mvn clean package -Dmaven.test.skip=true
mkdir -p backend/target/dependency && (cd backend/target/dependency; jar -xf ../*.jar)
docker rmi mic_metersphere:2.0.1
rm mic_metersphere_2.0.1.tar
docker build --build-arg MS_VERSION=2.0.1 -t mic_metersphere:2.0.1 .
docker save -o mic_metersphere_2.0.1.tar mic_metersphere:2.0.1
python3 -m http.server