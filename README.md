# PocketArchive

PocketArchive is an offline-first Android document and link organizer. The project is developed incrementally as an Android engineering portfolio; AI, accounts, and cloud sync are intentionally out of scope.

## Current status

The app has a launchable Compose shell with light/dark themes and an honest empty-library state. Import remains disabled until the next implementation milestone.

## Modules

- `app`: Android application packaging and future composition root.
- `feature-library`: future library screen and user-interaction boundary.
- `core-model`: platform-independent domain models shared by feature modules.

The dependency direction is `app -> feature-library -> core-model`.

## Build

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew :feature-library:assembleDebugAndroidTest
./gradlew lintDebug
```

Detailed milestone notes are stored in [`docs/learning`](docs/learning/index.md).
