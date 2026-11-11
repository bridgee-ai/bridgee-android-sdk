# Android 3.0.0 release candidate

Prepared 11 November 2026. Registry publication and release tags have NOT occurred.

Existing configuration, firstOpen, MatchBundle and provider API signatures remain available, but analytics delivery is deliberately incompatible: Bridgee no longer emits first_open or the configured tenant_first_open. Firebase owns first_open; campaign_details and install_* remain. See [the migration guide](docs/FIRST_OPEN_MIGRATION.md). Preserved signatures do not mean identical analytics behavior.

Use Android3.0.0 and iOS2.0.0 for this change. CocoaPods ~>1.1 includes1.2.0 but excludes2.0.0, avoiding an implicit policy change in old RN consumers. RN0.2.0 is the pre-1.0 breaking-update boundary.

Native packages must be published and verified first. Then update RN pins to Android3.0.0 and exact iOS2.0.0, rebuild and retest the exact tarball and native consumers. RN pins remain Android2.3.0 and iOS~>1.1 until then. Do not publish this intermediate RN package as a completed native upgrade.

Deferred resolution is navigation context, not install proof or attribution. Hosts own allowed routes, login-pending state, durable acknowledgement after displaying content and idempotent navigation. iOS requires explicit continuation after installation; no automatic App Store context transport is claimed. Physical-device/store/Firebase/GA4 and actual customer lockfile/build acceptance remain separate.

No previous version is replaced. Registry publisher access/signing, exact final artifacts and release approval remain required before distribution.
