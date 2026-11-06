#!/usr/bin/env bash
# Compila e roda o experimento dentro de um container Maven + JDK 21 (sem instalar Java na máquina).
# O Oracle precisa estar de pé: docker compose up -d
# Para só refazer os gráficos a partir do resultados/resumo.csv: ./rodar.sh --somente-graficos
set -euo pipefail
cd "$(dirname "$0")"
# Cache das dependências do Maven num volume, com dono = usuário atual
docker run --rm -v lab04-maven-repo:/m2 maven:3.9-eclipse-temurin-21 chown "$(id -u):$(id -g)" /m2
docker run --rm --network host \
  -u "$(id -u):$(id -g)" -e MAVEN_CONFIG=/var/maven/.m2 \
  -v lab04-maven-repo:/var/maven/.m2 \
  -v "$PWD":/app -w /app \
  maven:3.9-eclipse-temurin-21 \
  sh -c 'mvn -q -Duser.home=/var/maven -DskipTests package && java -Xmx256m -jar target/lab04-parse-1.0.0.jar "$@"' -- "$@"
