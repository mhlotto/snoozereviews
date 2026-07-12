# Features

Snooze Reviews is an on-device sleep-log app for manually recording and reviewing nights of sleep. This document describes user-facing behavior; persistence details live in [ARCHITECTURE.md](ARCHITECTURE.md), and backup details live in [BACKUP_AND_RESTORE.md](BACKUP_AND_RESTORE.md).

## Launch and last-night routing

`SplashActivity` is the only launcher Activity. On launch, it calculates "last night" from the device's local calendar and time zone:

```text
last night date = LocalDate.now(Clock.systemDefaultZone()).minusDays(1)
```

The splash screen remains visible until at least 2.5 seconds have elapsed and the asynchronous repository lookup for that night date has completed.

Routing outcomes:

- No matching log: open the create form for last night.
- Matching log: open the sleep report for that log.
- Lookup failure: show a retryable splash error state.

After routing, the splash Activity finishes so Back does not return to splash.

## Create and edit form

`SleepLogFormActivity` supports create and edit modes.

- Create mode receives an initial ISO night date and does not write to the database until Save is pressed.
- Edit mode receives a sleep-log ID, loads the saved record through the repository, and populates every field.

Supported fields:

- Night date
- Optional sleep location
- Optional fell-asleep and woke-up times
- Optional tri-state questions for slept-through-night and had-dreams
- Optional sleep quality and rested ratings
- Optional awakening count
- Optional descriptive tags
- Optional multiline notes

The form allows a minimally populated log containing only a night date.

## Dates and times

Night dates are stored as canonical ISO text, `yyyy-MM-dd`, and represent the local calendar date when the person went to sleep. The form displays dates in a localized format and rejects dates later than the current local date.

Add by Date is stricter: it is for completed historical nights and allows only yesterday or earlier.

Times are optional and stored as minutes after midnight from `0` through `1439`. The app does not store full sleep/wake timestamps or calculated duration. Wake times earlier than sleep times are allowed because they commonly mean the sleep crossed midnight.

## Nullable answers, ratings, tags, and notes

Tri-state answers preserve all three values:

- Yes: `true`
- No: `false`
- Not answered: `null`

Ratings are optional values from `1` through `5`; `Not rated` stores `null`.

Sleep locations and tags use stable keys rather than display labels. Unknown nonblank keys from existing data or backups are preserved and shown with safe fallback labels. Tags are stored as rows, not comma-separated text.

### Built-in and custom sleep locations

The built-in sleep locations are fixed and cannot be edited or removed:

- Bed
- Couch
- Bed and couch
- Recliner or chair
- Hotel or guest bed
- Floor
- Vehicle
- Other

Settings lets users add custom sleep locations. Custom names are cleaned by trimming outside whitespace and collapsing internal whitespace. Duplicate comparison is case-insensitive and whitespace-normalized.

Removing a custom location is a soft removal: it disappears from future form choices, but existing sleep logs keep and display that location. If a removed custom location is entered again, Settings asks whether to restore it.

### Built-in and custom descriptive tags

Built-in descriptive tags are fixed, read-only, and grouped into stable categories:

- Temperature and environment
- Physical condition
- Sleep pattern
- Mind and dreams
- Morning result

Settings includes a `Descriptive tags` management screen. Users can add custom tags, choose one category for each tag, change a custom tag's category later, soft-remove tags from future form choices, and restore removed tags. Duplicate detection is global across categories and uses the same case-insensitive, whitespace-normalized name rule as custom locations.

Custom tags are stored in sleep logs as encoded keys beginning with `CUSTOM_TAG_B64:`. Removing a custom tag does not change historical sleep logs. When editing a log that already uses a removed or orphaned custom tag, the form keeps it selected and readable so the user can retain or deselect it.

Notes are optional multiline text. Empty or whitespace-only notes may be normalized to `null`.

## Duplicate night dates and unsaved changes

There may be no more than one sleep log for a given night date. Duplicate create or update attempts fail without overwriting, merging, or silently changing another record.

The form tracks unsaved changes. Toolbar Back, system Back, and shared navigation actions show a Material confirmation dialog with `Keep editing` and `Discard` when changes are present.

## Sleep report

`SleepLogDetailActivity` loads the authoritative record by sleep-log ID and renders a read-only report.

Report sections include:

- Summary: night date, sleep time, wake time, duration, and location
- Ratings
- Sleep details: slept through, dreams, and awakening count
- Descriptive tags
- Notes

Missing optional values are shown explicitly with labels such as `Not recorded`, `Not answered`, `Not rated`, or `Not available`.

Selected descriptive tags are grouped under category headings in the report. Only categories with selected tags are shown. Built-in tags use their fixed categories, custom tags use their current Settings category, removed custom tags keep their stored category, and orphaned or unknown tags appear under `Other`.

Duration is calculated only when both sleep and wake times are present:

```text
if wake >= sleep:
    duration = wake - sleep
if wake < sleep:
    duration = (1440 - sleep) + wake
```

Duration is never written to the database.

The report includes an Edit action. After a successful edit, it reloads the full record from the repository.

## History

`SleepHistoryActivity` lists all saved sleep logs newest first by `night_date DESC`. Rows show a localized date, duration summary, compact ratings, location, and a preview of up to three tags with an overflow count.

History rows keep descriptive tags compact rather than showing category sections. Selecting a row opens the sleep report for that record. The history screen refreshes after returning from detail so edits and changed ordering are reflected.

## Shared navigation

Normal app screens share toolbar navigation for:

- History
- Stats
- Add by Date
- Backup and Restore
- Settings

The active destination hides its own menu item. Navigation uses normal Activity starts and does not restart splash or clear the task.

## Settings

`SettingsActivity` shows built-in sleep locations as read-only and active custom locations as removable rows. It also links to the descriptive tag management screen. Adding custom locations or tags rejects blanks, control characters, names longer than 80 characters, built-in duplicates, and active custom duplicates.

Custom locations and custom tags are stored in sleep logs as encoded keys, so historical logs and backups can preserve labels even if a Settings row is removed.

## Stats placeholder

`SleepStatsActivity` is a navigable placeholder. It does not query the database, calculate fake values, or show charts. Real statistics are deferred.

## Add by Date

`AddSleepByDateActivity` lets the user choose a completed historical night date. It defaults to yesterday and rejects today and future dates.

After Continue:

- If no log exists for the selected date, it opens the create form for that date.
- If a log already exists, it offers to view that log or choose another date.
- If lookup fails, it shows a retryable error and does not assume the date is unused.

## Current limitations

- Stats are placeholder-only.
- There is no delete flow.
- There is no search, filtering, or sorting UI.
- Backup is manual, local, and unencrypted.
- Remaining device, accessibility, and launcher-mask QA is documented in [../TESTING.md](../TESTING.md).
