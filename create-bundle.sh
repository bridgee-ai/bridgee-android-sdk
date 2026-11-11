#!/bin/bash
set -euo pipefail
MODE=${1:-}
case "$MODE" in
  zip)
    echo "Release ZIP/signing is disabled: the legacy implicit GPG signer and hand-written artifacts were unsafe." >&2
    echo "See docs/RELEASE_PUBLICATION.md. No key is selected and no artifact is signed or published." >&2
    exit 2
    ;;
  build|local) ;;
  *) echo "Usage: ./create-bundle.sh build|local (zip/signing disabled)" >&2; exit 1 ;;
esac
cd "$(dirname "$0")"
# Optional path to an existing, approved Gradle executable; never evaluated as shell code.
GRADLE=${GRADLE_COMMAND:-./gradlew}
"$GRADLE" :bridgeesdk:testDebugUnitTest :bridgeesdk:assembleRelease :bridgeesdk:sourcesJar :bridgeesdk:javadocJar :bridgeesdk:generatePomFileForMavenPublication
if [ "$MODE" = local ]; then
  "$GRADLE" :bridgeesdk:publishToMavenLocal
  echo "Published to Maven LOCAL only, not Maven Central."
else
  echo "Unsigned Gradle artifacts prepared in bridgeesdk/build; not published."
fi
