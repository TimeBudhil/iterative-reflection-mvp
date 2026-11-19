#!/usr/bin/env bash
# Builds the backend image with Spring Boot buildpacks (needs a running Docker daemon). Does not modify mvp/
# (it only writes the git-ignored mvp/target/). The prod profile is chosen at run time by compose, not here.
set -euo pipefail
cd "$(dirname "$0")/../mvp"
IMAGE="${BACKEND_IMAGE:-iterative-reflection/mvp:rehearsal}"
./mvnw -DskipTests spring-boot:build-image \
  -Dspring-boot.build-image.imageName="$IMAGE" \
  -Dspring-boot.build-image.env.BP_JVM_VERSION=25
