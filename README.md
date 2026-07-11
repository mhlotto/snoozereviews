# Snooze Reviews

Snooze Reviews is an Android app for manually reviewing sleep quality. This repository currently contains the Android scaffold, launch flow, Room persistence layer, create/edit form, read-only sleep report, sleep history screen, shared toolbar navigation, a Stats placeholder, Add by Date, and logical JSON backup/restore.

## Current Status

Implemented:

- Android application scaffold
- AndroidX splash launch flow with last-night routing
- Java create/edit sleep-log form using XML Views and View Binding
- Read-only sleep-log detail report with an Edit action
- Sleep history screen with newest-first saved-log listing
- Shared toolbar navigation for History, Stats, Add by Date, and Backup and Restore
- Stats placeholder screen
- Add by Date flow for choosing a completed historical night
- Backup and Restore screen using versioned logical JSON documents
- Final visual identity derived from the approved source illustration
- Native Android splash icon, adaptive launcher icons, legacy launcher PNGs, and themed monochrome icon
- Material Components theme with light and dark variants
- Room database version 1 for sleep logs and sleep-log tags
- Java repository with asynchronous persistence operations
- Room schema export and database instrumentation tests
- Basic scaffold tests
- Makefile developer commands
- GitHub Actions non-device checks

Deferred:

- Final device visual QA across launcher mask implementations
- Real statistics and charts
- Deleting logs
- Search, filtering, and sorting
- Cloud sync, encryption, and automatic backups

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

## Visual Identity

The approved source illustration is kept at the repository root:

```text
snooze-splash-base.png
```

It is the immutable high-resolution source artwork and is not moved into Android resources or modified in place. The current source is a PNG image measuring `941 x 1672` pixels.

Derived Android assets are generated with:

```bash
make visual-assets
```

or directly:

```bash
./scripts/generate-visual-assets.sh
```

The script requires ImageMagick 7 (`magick`). `pngcheck` and `oxipng` are used when installed, but they are optional.

The native splash uses AndroidX SplashScreen with a solid deep-night background and a centered derived icon. The icon is cropped from the approved illustration to focus on the sleeping face, gray beard, blue-and-white nightcap, pillow, and readable `zzz`; the moon, alarm clock, distant scene details, and lower empty background are intentionally excluded at splash/icon sizes.

Launcher assets include adaptive foreground/background layers for API 26+, density-specific legacy and round PNGs, and a deliberately simplified one-color VectorDrawable monochrome icon for themed launchers. The launcher icon does not include app-name text or raw full-scene artwork.

The app palette is derived from the illustration:

- Deep night blue: `#102846`
- Medium moonlit blue: `#2F5F96`
- Soft pale blue: `#DCEBFA`
- Warm moon yellow: `#F5C95F`
- Off-white pillow: `#F8F2E4`
- Muted gray-blue: `#5D7188`
- Error red: `#BA1A1A`

Light and dark themes use semantic Material color roles so long forms and reports stay readable. Visual device checks for Android 12+ splash behavior, older Android splash behavior, launcher masks, themed icons, large font, and dark theme are documented in `TESTING.md`.

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

History-row formatting tests are included in:

```bash
./gradlew testDebugUnitTest
```

Navigation and Add-by-Date policy tests are included in:

```bash
./gradlew testDebugUnitTest
```

Backup codec, import-plan, and backup service tests are included in:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebugAndroidTest
```

See `TESTING.md` for the manual smoke-test, accessibility, large-font, dark-theme, and Storage Access Framework checklist for later device passes.

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

`SplashActivity`, `SleepLogFormActivity`, `SleepLogDetailActivity`, `SleepHistoryActivity`, `SleepStatsActivity`, `AddSleepByDateActivity`, and `BackupRestoreActivity` live in `com.mhlotto.snoozereviews.ui`. Room entities live under `com.mhlotto.snoozereviews.data.entity`, DAOs under `com.mhlotto.snoozereviews.data.dao`, and `SnoozeReviewsDatabase` under `com.mhlotto.snoozereviews.data.db`.

## Launch Flow

`SplashActivity` is the only launcher Activity. It uses AndroidX SplashScreen with the final derived splash icon and a solid deep-night background.

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

## Sleep History

`SleepHistoryActivity` lists saved logs through `SleepLogRepository.listAllSleepLogs()`. The repository and DAO ordering is authoritative: rows are shown by `night_date DESC`, newest first.

History states:

- Loading: indeterminate progress while the repository request is running.
- Empty: `No sleep logs yet.` with supporting text.
- Content: a RecyclerView of saved sleep logs.
- Error: `Sleep history could not be loaded.` with Retry.

Each row shows a localized night date, calculated duration or `Duration not available`, compact sleep and rested ratings, sleep location when recorded, and a tag preview. The tag preview shows at most three labels in the same catalog order used by the form and detail report. Additional tags are summarized as `+N`. Unknown nonblank tags are kept visible after known tags using safe fallback labels.

Selecting a row opens `SleepLogDetailActivity` with the stable sleep-log ID and canonical ISO night date. The history screen refreshes when returning from detail, so edits made through the detail/form flow update row summaries and can move a log to its new newest-first position.

A minimal History toolbar action is available from the detail report and the create/edit form. If the form has unsaved changes, choosing History uses the same discard confirmation as Back navigation before leaving the form.

Deferred history-related features include deleting logs, search, filtering, sorting controls, real statistics, and charts.

## Shared Navigation

Normal app destinations share a toolbar overflow menu with:

- History
- Stats
- Add by Date
- Backup and Restore

`SplashActivity` does not show the shared menu because it is launch-routing infrastructure. The current destination hides its own menu item, so History does not open another History screen, Stats does not open another Stats screen, Add by Date does not open another Add by Date screen, and Backup and Restore does not open another Backup and Restore screen.

Navigation uses normal Activity starts without clearing the task, unusual launch modes, or restarting splash. Back returns through the stack in the order screens were opened.

`SleepLogFormActivity` protects unsaved changes for every shared navigation action. Clean forms navigate immediately. Dirty forms show the existing discard dialog; `Keep editing` stays on the form, and `Discard` opens the originally selected destination.

## Stats

`SleepStatsActivity` is a navigable placeholder. It does not query the database, calculate fake values, or show charts. The screen explains that statistics will appear after enough sleep logs have been recorded and lists planned areas such as average duration, sleep quality, rested rating, nights slept through, and common tags.

## Add By Date

`AddSleepByDateActivity` lets the user choose the night date for a completed historical night. The selected date means the local calendar date when sleep began.

Date policy:

- Defaults to yesterday in the device's local calendar and time zone.
- Allows yesterday or earlier.
- Rejects today and future dates.
- Uses `LocalDate` calendar arithmetic instead of subtracting milliseconds.

Continue checks the selected canonical ISO date through `SleepLogRepository.findSleepLogByNightDate(...)`.

Routing outcomes:

- No existing log: opens `SleepLogFormActivity.newCreateIntent(...)` with the selected ISO date.
- Existing log: shows `A sleep log already exists for this date.` with actions to view the log or choose another date.
- Lookup failure: shows `The selected date could not be checked.` with Retry and does not assume the date is unused.

Changing the selected date clears old duplicate or error messages and invalidates stale lookup callbacks.

## Backup And Restore

`BackupRestoreActivity` exports and imports logical Snooze Reviews backup documents. It does not copy the Room SQLite file, WAL files, shared-memory files, Room schema files, or local database IDs. Logical JSON backups are used because raw SQLite files are tied to schema and SQLite implementation details and can miss active WAL state.

The screen uses Android's Storage Access Framework:

- Export uses a create-document flow with MIME type `application/json`.
- Import uses an open-document flow accepting JSON-oriented MIME types.
- No storage permission is requested.
- The app uses `ContentResolver` streams and does not treat document URIs as filesystem paths.

Backup files may contain private sleep details. The app does not upload, share, or log backup contents.

Current backup format:

```json
{
  "format": "snooze-reviews-backup",
  "version": 1,
  "databaseVersion": 1,
  "exportedAt": "2026-07-11T14:30:00Z",
  "logs": []
}
```

The logical backup version is independent from the Room database version. Version `1` is currently supported. Newer versions are rejected until an explicit parser is added.

Each exported log includes:

- `nightDate`
- `sleepLocation`
- `fellAsleepMinute`
- `wokeUpMinute`
- `sleptThroughNight`
- `hadDreams`
- `sleepRating`
- `restedRating`
- `awakeningCount`
- `notes`
- `createdAt`
- `updatedAt`
- `tags`

Room IDs are not exported. `nightDate` is the logical record identity. Tags are stored as arrays on each log, not comma-separated text. Unknown valid location and tag keys are preserved.

Export output is deterministic apart from `exportedAt`: logs are sorted by `nightDate` ascending, tag keys are sorted lexicographically, JSON is pretty-printed with two-space indentation, and the document ends with a newline. Exporting zero logs is valid.

Import is a merge, not a replace-all restore:

- Imported dates replace matching local dates.
- Imported dates not present locally are inserted.
- Local dates not present in the backup remain unchanged.

For a matching date, the local Room ID is preserved while user data fields, tags, `createdAt`, and `updatedAt` are replaced with imported values. New records receive new local Room IDs.

Import validates the whole document before database modification. Validation covers the format marker, backup version, exported timestamp, record count, duplicate imported night dates, field types, date format, minute/rating/count ranges, timestamp ordering, and tag keys. Unknown extra JSON fields are ignored for supported versions. Future `nightDate` values are not rejected during backup import solely because they are future dates.

Before applying an import, the app calculates a confirmation summary showing total imported records, new records, matching dates that will be replaced, and local-only records that will remain unchanged. The import transaction begins only after confirmation.

The complete import is applied in one Room transaction. If any insert, update, constraint, or tag operation fails, the transaction rolls back and no partial changes remain.

Safety limits:

- Maximum backup size: 25 MiB
- Maximum log records: 100,000

Deferred backup-related features include cloud sync, automatic scheduled backups, encryption/password protection, direct sharing, raw database copying, and future backup-format migrations.

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
