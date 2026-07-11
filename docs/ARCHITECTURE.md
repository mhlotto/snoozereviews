# Architecture

Snooze Reviews is a Java Android app using XML layouts, Android Views, View Binding, AndroidX, Material Components, and Room.

## Technical choices

- Language: Java
- UI: XML Views
- Namespace/application ID: `com.mhlotto.snoozereviews`
- Minimum SDK: 29
- Local storage: Room
- Networking: none
- Requested Android permissions: none
- Database filename: `snooze-reviews.db`
- Room database version: 1

## Package structure

```text
com.mhlotto.snoozereviews
com.mhlotto.snoozereviews.data
com.mhlotto.snoozereviews.data.backup
com.mhlotto.snoozereviews.data.dao
com.mhlotto.snoozereviews.data.db
com.mhlotto.snoozereviews.data.entity
com.mhlotto.snoozereviews.ui
```

Room entities live under `data.entity`, DAOs under `data.dao`, and `SnoozeReviewsDatabase` under `data.db`. UI Activities and formatting helpers live under `ui` and feature-specific UI subpackages.

## Activities

- `SplashActivity`: launcher and last-night routing.
- `SleepLogFormActivity`: create and edit form.
- `SleepLogDetailActivity`: read-only sleep report.
- `SleepHistoryActivity`: newest-first history list.
- `SleepStatsActivity`: statistics placeholder.
- `AddSleepByDateActivity`: date selection and create/detail routing.
- `BackupRestoreActivity`: manual JSON export and import.

`SplashActivity` is the only launcher. Other Activities are normal non-exported app destinations.

## Repository pattern

Activities use repositories rather than DAOs directly. `SleepLogRepository` is the app-facing persistence entry point for sleep-log operations.

Public repository operations run on an injected background `Executor`; the default production setup returns UI-facing callbacks on the Android main thread. The database is not configured with `allowMainThreadQueries()`.

The backup layer uses a separate backup service/repository path for JSON import/export while still applying database changes through data-layer transactions.

## Room database

`SnoozeReviewsDatabase` is version `1` with `exportSchema = true`. Schema files are committed under:

```text
app/schemas/com.mhlotto.snoozereviews.data.db.SnoozeReviewsDatabase/1.json
```

There are no migrations yet because version 1 is the initial production schema. Future schema changes must increment the database version, add explicit migrations, and add migration tests.

## Tables

### `sleep_logs`

One row represents one logged night. Important columns include:

- `id`: auto-generated local primary key
- `night_date`: canonical ISO local date string
- optional location, times, answers, ratings, awakening count, and notes
- `created_at` and `updated_at`: UTC epoch milliseconds

There is a unique index on `night_date`; the app does not upsert or merge duplicate nights.

### `sleep_log_tags`

Rows represent tag membership for a sleep log.

- Composite primary key: `sleep_log_id`, `tag_key`
- Foreign key to `sleep_logs.id`
- `ON DELETE CASCADE`

Tags are unordered set membership. There is no separate tag-definition table.

## Data conventions

- Night dates are ISO text, `yyyy-MM-dd`, representing the local date when sleep began.
- Sleep and wake times are nullable minutes after midnight from `0` through `1439`.
- Nullable Boolean answers use `true`, `false`, or `null`.
- Sleep quality and rested ratings are nullable integers from `1` through `5`.
- `created_at` and `updated_at` are non-null UTC epoch milliseconds.
- Unknown nonblank location and tag keys are preserved for forward compatibility.

## Transactions and constraints

Create and update operations that include tags are transactional. Duplicate night-date failures leave existing rows and tags unchanged. Delete operations rely on the foreign-key cascade for tags.

Backup import is also transactional; see [BACKUP_AND_RESTORE.md](BACKUP_AND_RESTORE.md).

## Navigation and back stack

The app uses normal Activity navigation. Shared toolbar destinations open on top of the current screen without task-clearing flags or unusual launch modes. Dirty form navigation is intercepted so the user can keep editing or discard changes before leaving.

Splash finishes after routing so Back from splash-routed destinations exits normally instead of returning to the splash screen.

## Security and privacy posture

The app requests no Android permissions and performs no networking. Backup and restore use Android's Storage Access Framework instead of direct filesystem paths or storage permissions.
