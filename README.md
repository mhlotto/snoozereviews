# Snooze Reviews

Snooze Reviews is an Android app for manually reviewing sleep quality. This repository currently contains only the initial application scaffold and development tooling.

## Current Status

Implemented:

- Android application scaffold
- Single Java launcher activity using XML Views and View Binding
- Material Components theme with light and dark variants
- Room dependency setup and committed schema directory
- Basic unit and instrumentation test placeholders
- Makefile developer commands
- GitHub Actions non-device checks

Not implemented yet:

- Sleep logging
- Production database schema, entities, and DAOs
- Import/export
- Splash artwork
- History
- Statistics

## Technical Choices

- Language: Java
- UI: XML layouts with Android Views
- AndroidX: enabled
- Material Components: enabled
- Local storage: Room
- Networking: none
- Requested Android permissions: none
- Namespace/application ID: `com.mhlotto.snoozereviews`
- Minimum SDK: 29

## Prerequisites

- Android Studio with the Android SDK installed
- JDK 17 for local and CI builds
- Android SDK platform 36 or newer compatible with `compileSdk 36`

## Build And Test

Use the checked-in Gradle wrapper through the Makefile:

```bash
make debug
make test
make lint
make check
```

Equivalent direct Gradle commands:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
```

Install the debug build on a connected device or emulator:

```bash
make install
```

Run instrumentation tests on a connected device or emulator:

```bash
make connected-test
```

## Android Studio

Open this directory in Android Studio. Let Android Studio sync the Gradle project, then run the `app` configuration on a device or emulator.

## Package Structure

```text
com.mhlotto.snoozereviews
com.mhlotto.snoozereviews.data
com.mhlotto.snoozereviews.data.db
com.mhlotto.snoozereviews.data.dao
com.mhlotto.snoozereviews.data.entity
com.mhlotto.snoozereviews.ui
```

`MainActivity` lives in `com.mhlotto.snoozereviews.ui`. Future Room database classes will live under `com.mhlotto.snoozereviews.data.db`.

Room schema export is configured to `app/schemas`, which is committed so future database schema changes can be reviewed. Room 2.8.4 requires at least one entity for a `@Database` class, so this scaffold intentionally omits `SnoozeReviewsDatabase` until the first real schema is introduced.
