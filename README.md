# Snooze Reviews

<p align="center">
  <img src="app/src/main/res/drawable-nodpi/snooze_splash_full.png" alt="Snooze Reviews splash artwork" width="240">
</p>

Snooze Reviews is an Android app for manually logging and reviewing sleep. It is built as a small Java/XML Android application with local Room persistence and no network access.

The app currently focuses on private, on-device sleep-log entry, review, history, navigation, and logical JSON backup/restore.

## Current features

- Manual sleep-log creation and editing
- Optional inline dream details
- One sleep log per night date
- Last-night launch routing from the splash screen
- Read-only sleep report with calculated duration
- Newest-first sleep history
- Add by Date flow for completed historical nights
- Stats placeholder screen
- Versioned JSON backup and restore
- Settings for custom sleep locations and descriptive tags
- Light and dark Material themes
- Custom native splash and launcher artwork

See [docs/FEATURES.md](docs/FEATURES.md) for detailed user-facing behavior.

## Technical overview

| Area | Choice |
| --- | --- |
| Language | Java |
| UI | XML Views with Android Views and View Binding |
| Minimum SDK | 29 |
| Namespace | `com.mhlotto.snoozereviews` |
| Storage | Room |
| Database | `snooze-reviews.db`, version 4 |
| Networking | None |
| Requested permissions | None |

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for package structure, persistence conventions, and navigation details.

## Prerequisites

- Android Studio or Android SDK command-line tools
- JDK 17
- Android SDK compatible with the configured `compileSdk`
- Optional: ImageMagick 7 for regenerating visual assets

## Build

Use the checked-in Gradle wrapper through the Makefile:

```bash
make debug
make test
make lint
make check
```

Equivalent primary Gradle build command:

```bash
./gradlew assembleDebug
```

Release tooling is documented in [docs/RELEASING.md](docs/RELEASING.md). Normal local releases use `make release-patch`, `make release-minor`, or `make release-major`.

## Install and run

Install the debug build on a connected device or emulator:

```bash
make install
```

You can also open this directory in Android Studio, sync the Gradle project, and run the `app` configuration.

## Tests

Run connected instrumentation tests on a device or emulator:

```bash
make connected-test
```

Instrumentation tests require a connected device or emulator. See [TESTING.md](TESTING.md) for automated test commands, manual smoke checks, Storage Access Framework checks, accessibility checks, and visual QA notes.

## Visual assets

The approved titled splash artwork is `snooze-splash-base.png` at the repository root. Launcher icons intentionally use a separate untitled icon source. Regenerate Android visual assets with:

```bash
make visual-assets
```

ImageMagick 7 is required. See [docs/VISUALS.md](docs/VISUALS.md) and [artwork/README.md](artwork/README.md) for details.

## Documentation

- [Features](docs/FEATURES.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Backup and restore](docs/BACKUP_AND_RESTORE.md)
- [Scoring design](docs/SCORING.md)
- [Releasing](docs/RELEASING.md)
- [Visual system](docs/VISUALS.md)
- [Testing and manual validation](TESTING.md)

## Deferred work

- Real statistics and charts
- Sleep-log deletion
- Search, filtering, and sorting
- Cloud sync or automatic backup
- Remaining device, launcher-mask, and accessibility QA
