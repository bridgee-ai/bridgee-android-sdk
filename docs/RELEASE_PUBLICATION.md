# Release publication safety

`create-bundle.sh zip` is intentionally disabled before any build, key lookup, signing or upload. The former path could select one key but sign with another default key, manually rebuilt sources without the approved license, and wrote its own POM. It must not be used for release.

`create-bundle.sh build` prepares unsigned artifacts using the Gradle definitions. If the wrapper JAR is unavailable, set `GRADLE_COMMAND` to an already approved compatible Gradle executable. It does not download a toolchain. `local` explicitly writes only to the local Maven repository, not Central.

Use the verified Gradle outputs: `bridgeesdk/build/outputs/aar/bridgeesdk-release.aar`, `bridgeesdk/build/libs/bridgeesdk-sources.jar`, and `bridgeesdk/build/publications/maven/pom-default.xml`. Inspect license inclusion and exact version, dependencies and hashes. These files alone are not a signed Central submission; complete release metadata/documentation requirements must be checked by the authorized publisher.

A future signing path must require an explicitly approved full signer fingerprint, verify that exact identity, and pass it to every signature operation. Never pick the first/default key, generate a key automatically, or infer signing authority from an available key. No signing or credential generation is implemented here.

Maven publication remains gated on a verified authorized publisher, signing identity, final artifact review and confirmed upload outcome. No existing version/tag is replaced. Android 3.0.0 changes analytics behavior; read FIRST_OPEN_MIGRATION.md before rollout.
