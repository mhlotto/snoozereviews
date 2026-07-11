# Backup and restore

Snooze Reviews supports manual logical JSON backup and restore through Android's Storage Access Framework.

## Logical backup

The app does not copy the Room SQLite database file. It does not export:

- `snooze-reviews.db`
- WAL or shared-memory files
- Room schema files
- Local Room IDs as persistent record identity

Logical JSON is used because raw SQLite files are tied to implementation details, can miss active WAL state, and are harder to validate or migrate independently.

## Storage Access Framework

Export uses a create-document flow with MIME type `application/json`. Import uses an open-document flow for JSON-oriented documents.

No storage permission is requested. The app uses `ContentResolver` input and output streams and does not treat document URIs as filesystem paths.

Canceling either picker is not an error.

## Privacy

Backup files may contain private sleep details such as notes, times, ratings, and tags. The app does not upload, share, or log backup contents.

Encryption, cloud sync, and scheduled backup are deferred.

## Format

Format identifier:

```text
snooze-reviews-backup
```

Current logical backup version:

```text
1
```

Top-level structure:

```json
{
  "format": "snooze-reviews-backup",
  "version": 1,
  "databaseVersion": 1,
  "exportedAt": "2026-07-11T14:30:00Z",
  "logs": []
}
```

`databaseVersion` is metadata. The logical backup version is the import compatibility gate.

Unsupported newer backup versions are rejected. Versions less than 1 are rejected.

## Exported log fields

Each log exports:

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

Room IDs are not exported. `nightDate` is the logical identity. Tags are exported as arrays on each log, not comma-separated strings.

Unknown valid location and tag keys are preserved.

## Deterministic export

Export output is deterministic apart from `exportedAt`:

- Logs are sorted by canonical `nightDate` ascending.
- Tag keys are sorted lexicographically within each record.
- JSON is pretty-printed with two-space indentation.
- The document ends with a newline.
- Null optional values are emitted explicitly.

Exporting zero logs is valid.

## Import validation

The whole document is parsed and validated before database modification. Validation covers:

- Format marker
- Backup version
- Positive `databaseVersion`
- Valid `exportedAt` instant
- Required `logs` array
- Record count limit
- Duplicate imported night dates
- Field types
- Canonical ISO night dates
- Minute, rating, awakening-count, and timestamp ranges
- `updatedAt >= createdAt`
- Tag array and nonblank tag keys

Unknown extra JSON fields are ignored for supported versions. Future night dates are not rejected solely because they are future dates.

## Merge import policy

Import is a merge, not a replace-all restore:

- Imported dates replace matching local dates.
- Imported dates not present locally are inserted.
- Local dates not present in the backup remain unchanged.

For a matching date, the existing local Room ID is preserved while user data fields, tags, `createdAt`, and `updatedAt` are replaced with imported values.

For a new date, Room generates a new local ID and stores the imported values.

Local-only records are not deleted or modified.

Before applying an import, the app calculates a confirmation summary showing imported records, new records, matching dates that will be replaced, and local-only records that will remain unchanged.

## Transactional rollback

The import transaction starts only after validation and user confirmation. The complete merge is applied in one Room transaction. If any insert, update, constraint, or tag write fails, the transaction rolls back and no partial changes remain.

## Limits

- Maximum backup size: 25 MiB
- Maximum log records: 100,000

These limits are defensive and should not affect realistic personal sleep histories.

## Deferred work

- Encryption or password protection
- Cloud sync
- Automatic scheduled backups
- Direct sharing
- Replace-all restore mode
- Future backup-format migrations
