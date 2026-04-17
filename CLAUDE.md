# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Android library (AAR) published to Maven Central as `ai.bridgee:bridgee-android-sdk`. Provides marketing-attribution: resolves the Google Play Install Referrer, calls Bridgee's `/match` API to get UTM source/medium/campaign, then forwards them to a host-app-supplied `AnalyticsProvider` (typically Firebase Analytics).

The README is in Portuguese; many code comments and the `create-bundle.sh` script are too. Follow that style when editing existing prose, but keep new code identifiers in English to match the existing source.

## Common commands

Build, test, and publish are driven through `create-bundle.sh`, which wraps Gradle:

```bash
./create-bundle.sh build   # clean + assemble; verifies AAR is produced
./create-bundle.sh local   # build + publishToMavenLocal (~/.m2/repository)
./create-bundle.sh zip     # build + GPG-sign + bundle ZIP for Maven Central Portal
```

Direct Gradle equivalents (use these for tighter loops):

```bash
./gradlew :bridgeesdk:assembleRelease     # build the AAR only
./gradlew :bridgeesdk:test                # JVM unit tests
./gradlew :bridgeesdk:connectedAndroidTest  # instrumented tests (device/emulator required)
./gradlew :bridgeesdk:publishToMavenLocal
```

`zip` mode requires a GPG secret key on the host (`gpg --list-secret-keys`) — it signs every artifact and produces `bridgee-android-sdk-<VERSION>-bundle.zip` for upload at https://central.sonatype.com/publishing.

## Releasing a new version

Version lives in **two** places that must be kept in sync:
- `gradle.properties` → `VERSION_NAME` / `VERSION_CODE`
- `create-bundle.sh` → the hardcoded `VERSION="..."` constant near the top

The README's install snippets (`implementation 'ai.bridgee:bridgee-android-sdk:X.Y.Z'`) also need bumping. Recent commit messages follow `feat: vX.Y.Z - <summary>` / `fix: <summary>`.

## Architecture

Public API surface (everything in `ai.bridgee.android.sdk`, anything under `internal/` is not part of the contract):

- `BridgeeSDK` — singleton (`getInstance(context, provider, tenantId, tenantKey, dryRun)`). Single public entry point: `firstOpen(MatchBundle)` and `firstOpen(MatchBundle, ResponseCallback<MatchResponse>)`.
- `AnalyticsProvider` — interface the host app implements; the SDK never depends on Firebase directly.
- `MatchBundle` — fluent builder of match hints (`withEmail`, `withPhone`, `withName`, `withGclid`, `withCustomParam`). Wraps a `Bundle`.
- `MatchResponse` — UTM triple returned by `/match`; `toBundle()` is what gets logged as event params.
- `ResponseCallback<T>` — `ok(T)` / `error(Exception)`. Used everywhere internally too.

`firstOpen` flow (see `BridgeeSDK.resolveAttribution`):
1. `InstallReferrerResolver` opens an `InstallReferrerClient`, reads the referrer URL, prepends `success:` or `error:<reason>` and returns it as a string. Errors are surfaced as `ok("error:...")` rather than `error(e)` — the pipeline always proceeds.
2. The referrer string is added to the `MatchBundle` as `install_referrer`, plus `event_name=first_open`.
3. `MatchApiClient.match` POSTs the bundle (converted to a `metadata: [{key,value}]` list via `MatchRequest.fromBundle`) to `https://api.bridgee.ai/match`. Authentication is an `x-tenant-token` header containing `Base64(tenantId + ";" + tenantKey)` produced by `TenantTokenEncoder`. Timeouts are aggressive: 500 ms connect / 1500 ms read.
4. On success, the SDK sets three user properties (`install_source/medium/campaign`) and logs four events: `first_open`, `campaign_details`, and `<tenantId>_first_open`, `<tenantId>_campaign_details`. Hyphens in event/property names are replaced with underscores before forwarding.

`dryRun=true` short-circuits `setUserProperty` and `logEvent` (the network call and callbacks still happen, only the `AnalyticsProvider` is skipped). Use this when adding tracing — don't add logs that fire only outside dry-run.

`MatchApiClient` is constructed fresh per call inside `BridgeeSDK.resolveMatch` and `shutdown()` is invoked in both success and error paths — its `ExecutorService` is single-use. Don't move it back to a singleton without revisiting that lifecycle.

## Constraints

- Java 8 source/target; `minSdk 21`, `compileSdk 34`. Don't introduce APIs above 21 without `Build.VERSION` guards (see `MatchApiClient.isNetworkAvailable` for the existing pattern).
- Dependencies: keep it minimal. Currently only `gson` (implementation) and `installreferrer` (api). Adding a transitive dep means updating the hand-written `<dependencies>` block in `bridgeesdk/build.gradle`'s `withXml` and the `<dependencies>` block in `create-bundle.sh`'s POM template.
- The SDK manifests `usesCleartextTraffic=true` via `manifestPlaceholders`; production traffic is HTTPS, so don't rely on that flag for new functionality.
