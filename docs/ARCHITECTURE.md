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
- Room database version: 4

## Package structure

```text
com.mhlotto.snoozereviews
com.mhlotto.snoozereviews.data
com.mhlotto.snoozereviews.data.backup
com.mhlotto.snoozereviews.data.dao
com.mhlotto.snoozereviews.data.db
com.mhlotto.snoozereviews.data.entity
com.mhlotto.snoozereviews.data.location
com.mhlotto.snoozereviews.data.tag
com.mhlotto.snoozereviews.ui
```

Room entities live under `data.entity`, DAOs under `data.dao`, and `SnoozeReviewsDatabase` under `data.db`. Custom location helpers live under `data.location`; custom tag helpers live under `data.tag`. UI Activities and formatting helpers live under `ui` and feature-specific UI subpackages.

## Activities

- `SplashActivity`: launcher and last-night routing.
- `SleepLogFormActivity`: create and edit form.
- `SleepLogDetailActivity`: read-only sleep report.
- `SleepHistoryActivity`: newest-first history list.
- `SleepStatsActivity`: statistics placeholder.
- `AddSleepByDateActivity`: date selection and create/detail routing.
- `BackupRestoreActivity`: manual JSON export and import.
- `SettingsActivity`: settings hub and custom sleep-location management.
- `SleepTagSettingsActivity`: custom descriptive tag management.

`SplashActivity` is the only launcher. Other Activities are normal non-exported app destinations.

## Repository pattern

Activities use repositories rather than DAOs directly. `SleepLogRepository` is the app-facing persistence entry point for sleep-log operations.

Public repository operations run on an injected background `Executor`; the default production setup returns UI-facing callbacks on the Android main thread. The database is not configured with `allowMainThreadQueries()`.

The backup layer uses a separate backup service/repository path for JSON import/export while still applying database changes through data-layer transactions.

## Room database

`SnoozeReviewsDatabase` is version `4` with `exportSchema = true`. Schema files are committed under:

```text
app/schemas/com.mhlotto.snoozereviews.data.db.SnoozeReviewsDatabase/
```

Version 1 is the initial production sleep-log schema. Version 2 adds custom sleep locations through explicit `MIGRATION_1_2`. Version 3 adds custom descriptive tags through explicit `MIGRATION_2_3`. Version 4 adds nullable dream details through explicit `MIGRATION_3_4`. The database builder supports `1 -> 2 -> 3 -> 4`, `2 -> 3 -> 4`, and `3 -> 4` upgrades.

## Tables

### `sleep_logs`

One row represents one logged night. Important columns include:

- `id`: auto-generated local primary key
- `night_date`: canonical ISO local date string
- optional location, times, answers, dream details, ratings, awakening count, and notes
- `created_at` and `updated_at`: UTC epoch milliseconds

There is a unique index on `night_date`; the app does not upsert or merge duplicate nights.

### `sleep_log_tags`

Rows represent tag membership for a sleep log.

- Composite primary key: `sleep_log_id`, `tag_key`
- Foreign key to `sleep_logs.id`
- `ON DELETE CASCADE`

Tags are unordered set membership. Built-in tags are fixed constants. Custom tag definitions live separately in `custom_sleep_tags`; `sleep_log_tags` intentionally has no foreign key to that table so historical selected tags remain readable after soft removal.

### `custom_sleep_locations`

User-managed sleep locations live in `custom_sleep_locations`.

- `location_key`: primary key, encoded as `CUSTOM_B64:<base64url UTF-8 display name>`
- `display_name`: cleaned user-facing name
- `normalized_name`: duplicate-comparison name
- `is_active`: whether the location appears in future form choices
- `created_at` and `updated_at`: UTC epoch milliseconds

There is a unique index on `normalized_name`. Rows are soft-deactivated rather than hard-deleted so existing logs can keep historical location labels. Built-in locations are not stored in this table.

### `custom_sleep_tags`

User-managed descriptive tag definitions live in `custom_sleep_tags`.

- `tag_key`: primary key, encoded as `CUSTOM_TAG_B64:<base64url UTF-8 display name>`
- `display_name`: cleaned user-facing name
- `normalized_name`: duplicate-comparison name
- `category_key`: one of `ENVIRONMENT`, `PHYSICAL`, `SLEEP_PATTERN`, `MIND_AND_DREAMS`, `MORNING_RESULT`, or `OTHER`
- `is_active`: whether the tag appears in future form choices
- `created_at` and `updated_at`: UTC epoch milliseconds

There is a unique index on `normalized_name`. Rows are soft-deactivated rather than hard-deleted so existing logs can keep historical tag labels. Built-in tags are not stored in this table.

## Data conventions

- Night dates are ISO text, `yyyy-MM-dd`, representing the local date when sleep began.
- Sleep and wake times are nullable minutes after midnight from `0` through `1439`.
- Nullable Boolean answers use `true`, `false`, or `null`.
- Dream details are nullable text and are persisted only when `had_dreams` is `true`.
- Sleep quality and rested ratings are nullable integers from `0` through `5`; `null` means not rated and `0` is a real rating.
- `created_at` and `updated_at` are non-null UTC epoch milliseconds.
- Unknown nonblank location and tag keys are preserved for forward compatibility.
- Custom location keys use URL-safe Base64 without padding and start with `CUSTOM_B64:`.
- Custom tag keys use URL-safe Base64 without padding and start with `CUSTOM_TAG_B64:`.

## Transactions and constraints

Create and update operations that include tags are transactional. Duplicate night-date failures leave existing rows and tags unchanged. Delete operations rely on the foreign-key cascade for tags.

Backup import is also transactional; see [BACKUP_AND_RESTORE.md](BACKUP_AND_RESTORE.md).

## Navigation and back stack

The app uses normal Activity navigation. Shared toolbar destinations open on top of the current screen without task-clearing flags or unusual launch modes. Dirty form navigation is intercepted so the user can keep editing or discard changes before leaving.

Splash finishes after routing so Back from splash-routed destinations exits normally instead of returning to the splash screen.

## Security and privacy posture

The app requests no Android permissions and performs no networking. Backup and restore use Android's Storage Access Framework instead of direct filesystem paths or storage permissions.
