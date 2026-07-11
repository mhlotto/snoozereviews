# Snooze Reviews

Snooze Reviews is an Android app for manually reviewing sleep quality. This repository currently contains the Android scaffold plus the first Room persistence layer for sleep logs.

## Current Status

Implemented:

- Android application scaffold
- AndroidX splash launch flow with last-night routing
- Placeholder Java destination activities using XML Views and View Binding
- Material Components theme with light and dark variants
- Room database version 1 for sleep logs and sleep-log tags
- Java repository with asynchronous persistence operations
- Room schema export and database instrumentation tests
- Basic scaffold tests
- Makefile developer commands
- GitHub Actions non-device checks

Not implemented yet:

- Sleep-entry UI
- Sleep-log detail UI
- Import/export
- Final splash artwork
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
- Database filename: `snooze-reviews.db`
- Database version: 1

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

The database, DAO, repository, and schema readiness tests are instrumentation tests. They can also be run directly:

```bash
./gradlew connectedDebugAndroidTest
```

Launch-related local unit tests are included in:

```bash
./gradlew testDebugUnitTest
```

Launch-related Activity tests are instrumentation tests and require a connected device or emulator:

```bash
./gradlew connectedDebugAndroidTest
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

`SplashActivity`, `SleepLogFormActivity`, and `SleepLogDetailActivity` live in `com.mhlotto.snoozereviews.ui`. Room entities live under `com.mhlotto.snoozereviews.data.entity`, DAOs under `com.mhlotto.snoozereviews.data.dao`, and `SnoozeReviewsDatabase` under `com.mhlotto.snoozereviews.data.db`.

## Launch Flow

`SplashActivity` is the only launcher Activity. It uses AndroidX SplashScreen with temporary launcher-icon artwork; final Snooze Reviews splash art will be added later.

On launch, the app calculates "last night" from the device's local calendar and time zone:

```text
last night date = LocalDate.now(Clock.systemDefaultZone()).minusDays(1)
```

The result is stored and queried as canonical ISO text, `yyyy-MM-dd`. The app does not subtract 24 hours from an epoch timestamp, so daylight-saving transitions are handled as calendar changes.

The splash screen remains visible until both conditions are true:

- At least 2.5 seconds have elapsed using monotonic elapsed time.
- The asynchronous `SleepLogRepository` lookup for last night's `night_date` has completed successfully.

Routing outcomes:

- No matching log: open `SleepLogFormActivity` for that night date.
- Matching log: open `SleepLogDetailActivity` with the sleep-log ID and night date.

After routing, `SplashActivity` finishes so Back exits from the destination instead of returning to splash.

If lookup fails, the splash is released after the 2.5-second minimum and `SplashActivity` shows a Material error state with Retry. Retry repeats the local-date calculation and repository lookup without imposing another mandatory splash delay. Errors are logged for diagnostics but not shown as stack traces to the user.

The destination screens are placeholders only. `SleepLogFormActivity` displays the night date and a note that the form will be implemented next. `SleepLogDetailActivity` displays the night date and a note that the report will be implemented later.

## Database

Room database version 1 is the initial production schema. It uses the file name `snooze-reviews.db`.

Tables:

- `sleep_logs`: one row per logged night, with an auto-generated `id`
- `sleep_log_tags`: tag membership rows keyed by `sleep_log_id` and `tag_key`

Important rules and conventions:

- `night_date` stores the local date the person went to sleep as ISO text, `yyyy-MM-dd`.
- There is a unique index on `sleep_logs.night_date`; the app does not upsert or merge duplicate nights.
- `fell_asleep_minute` and `woke_up_minute` store nullable minutes after midnight, `0` through `1439`.
- `slept_through_night` and `had_dreams` are nullable Boolean values: `true`, `false`, or `null`.
- `sleep_rating` and `rested_rating` are nullable integer ratings from `1` through `5`.
- `created_at` and `updated_at` are non-null UTC epoch milliseconds managed by the repository.
- Tags are stored as stable string keys in `sleep_log_tags`, not as comma-separated text.
- Unknown nonblank tag keys and sleep-location keys are preserved for forward compatibility.

`SleepLogRepository` is the intended application-facing persistence entry point. Public operations run on an injected background `Executor`; the default constructor delivers callbacks on the Android main thread and does not allow main-thread database queries.

Room schema export is configured to `app/schemas`. The generated version 1 schema is committed at:

```text
app/schemas/com.mhlotto.snoozereviews.data.db.SnoozeReviewsDatabase/1.json
```

There are no migrations yet because version 1 is the initial schema. Future schema changes must increment the database version, add explicit migrations, and add migration tests using the existing `MigrationTestHelper` test structure.
