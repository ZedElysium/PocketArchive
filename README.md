# PocketArchive

PocketArchive is an offline-first Android document and link organizer. The project is developed incrementally as an Android engineering portfolio; AI, accounts, and cloud sync are intentionally out of scope.

## Current status

The app has a launchable Compose shell with light/dark themes, a minimal UDF state flow, and a system document picker for PDF and image selection. Selected documents are not copied or persisted yet.

## Modules

- `app`: Android application packaging and future composition root.
- `feature-library`: library screen, UDF state, actions, and one-time effects.
- `core-model`: platform-independent domain models shared by feature modules.

The dependency direction is `app -> feature-library -> core-model`.

## Build

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew :feature-library:assembleDebugAndroidTest
./gradlew lintDebug
```
