# Snooze Reviews

Snooze Reviews is an Android app for manually reviewing sleep quality. This repository currently contains the Android scaffold, launch flow, Room persistence layer, create/edit form, and read-only sleep report.

## Current Status

Implemented:

- Android application scaffold
- AndroidX splash launch flow with last-night routing
- Java create/edit sleep-log form using XML Views and View Binding
- Read-only sleep-log detail report with an Edit action
- Material Components theme with light and dark variants
- Room database version 1 for sleep logs and sleep-log tags
- Java repository with asynchronous persistence operations
- Room schema export and database instrumentation tests
- Basic scaffold tests
- Makefile developer commands
- GitHub Actions non-device checks

Not implemented yet:

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

Form-related local unit tests are also included in:

```bash
./gradlew testDebugUnitTest
```

Detail-report formatting and duration tests are included in:

```bash
./gradlew testDebugUnitTest
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

`SleepLogFormActivity` is the real create/edit form. `SleepLogDetailActivity` loads the saved record by ID and renders a read-only report.

## Sleep Log Form

`SleepLogFormActivity` supports two explicit modes:

- Create mode: launched with `newCreateIntent(context, nightDate)`, pre-fills the supplied ISO night date, and opens the detail placeholder after a successful save.
- Edit mode: launched with `newEditIntent(context, sleepLogId)`, loads the existing log through `SleepLogRepository`, and finishes with `RESULT_OK` after a successful update.

Implemented fields:

- Night date
- Optional sleep location
- Optional fell-asleep and woke-up times
- Optional tri-state questions for slept-through-night and had-dreams
- Optional 1-through-5 sleep and rested ratings
- Optional awakening count
- Optional descriptive tags
- Optional multiline notes

Night dates are selected with a local-calendar date picker. The selected value is stored internally as ISO `yyyy-MM-dd`, displayed in a localized date format, and cannot be later than the device's current local date. Today and past dates are allowed.

Times are selected with a time picker that respects the device 12-hour or 24-hour preference. Stored values are nullable minutes after midnight from `0` through `1439`. Clearing a time stores `null`.

Tri-state questions preserve all database states:

- Yes: `true`
- No: `false`
- Not answered: `null`

Ratings use selectable values `1` through `5` plus a `Not rated` state that stores `null`. Awakening count is optional; empty input stores `null`, and invalid or negative input shows a field error.

Sleep locations and tags store stable keys, not display labels. Unknown nonblank keys loaded from existing records are displayed as fallback choices and preserved unless the user clears or replaces them. Tags are stored as rows in `sleep_log_tags`, not comma-separated text.

Saving uses `SleepLogRepository` asynchronously. Save is disabled while a request is in progress. Create returns the generated ID, sets `RESULT_OK`, opens the detail report, and finishes. Edit preserves the existing ID, lets the repository preserve `created_at` and refresh `updated_at`, sets `RESULT_OK`, and finishes.

Duplicate night dates are rejected by the existing unique `night_date` constraint. The form shows a specific duplicate-date message and preserves the user's entered values.

Unsaved changes are tracked against the initial create state or the loaded edit state. Toolbar Back and system Back show a Material confirmation dialog with `Keep editing` and `Discard` when changes are present.

## Sleep Report

`SleepLogDetailActivity` accepts a sleep-log ID and night-date placeholder, then loads the authoritative record through `SleepLogRepository.findSleepLogById(...)`. The ID is authoritative; the loaded database date is what the report ultimately displays.

Report sections:

- Summary: localized night date, fell-asleep time, wake time, calculated duration, and sleep location
- Ratings: sleep quality and rested-after-waking ratings
- Sleep details: slept-through-night, dreams, and awakening count
- Descriptive tags: noninteractive chips for selected tags
- Notes: multiline notes text

Missing-value policy:

- Missing times, location, and awakening count show `Not recorded`.
- Missing duration inputs show `Not available`.
- Nullable Boolean answers show `Yes`, `No`, or `Not answered`.
- Missing ratings show `Not rated`.
- Empty tags show `No descriptive tags recorded.`
- Empty notes show `No notes recorded.`

Duration is calculated in memory only and is never stored in Room. If wake time is greater than or equal to sleep time, duration is `wake - sleep`. If wake time is earlier, the report treats it as crossing midnight: `(1440 - sleep) + wake`.

Dates are parsed from ISO `yyyy-MM-dd` and displayed with the device locale. Times are stored as minutes after midnight and displayed with the device 12-hour or 24-hour preference.

Known sleep-location and tag keys use localized labels from the existing UI catalog. Unknown nonblank keys are preserved and displayed with safe fallback labels such as `Unknown location (KEY)` or `Unknown tag (KEY)`. Known tags follow the form catalog order; unknown tags appear after known tags in sorted order.

The detail report has loading, not-found, and retryable load-error states. Edit launches `SleepLogFormActivity.newEditIntent(...)`; after a successful edit, the report reloads the full record from Room instead of trusting only returned extras.

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
