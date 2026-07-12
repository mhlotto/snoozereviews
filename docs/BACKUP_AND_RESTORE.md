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

The Room database is excluded from Android platform cloud backup and device-transfer extraction rules. User-controlled JSON export/import is the intended backup mechanism.

Encryption, cloud sync, and scheduled backup are deferred.

## Format

Format identifier:

```text
snooze-reviews-backup
```

Current logical backup version:

```text
2
```

Top-level structure:

```json
{
  "format": "snooze-reviews-backup",
  "version": 2,
  "databaseVersion": 3,
  "exportedAt": "2026-07-11T14:30:00Z",
  "customTags": [],
  "logs": []
}
```

`databaseVersion` is metadata. The logical backup version is the import compatibility gate.

Unsupported newer backup versions are rejected. Versions less than 1 are rejected. Version 1 backups remain importable, but they do not include custom tag metadata.

## Custom tag definitions

Backup version 2 exports active and inactive custom descriptive tags in `customTags`. Built-in tags are not exported.

Each custom tag definition contains:

- `tagKey`
- `displayName`
- `categoryKey`
- `isActive`
- `createdAt`
- `updatedAt`

Custom tag keys use:

```text
CUSTOM_TAG_B64:<base64url-encoded UTF-8 display name>
```

Definitions are sorted by normalized display name. Import validates duplicate keys, duplicate normalized names, key/display-name mismatches, category keys, timestamps, and collisions with built-in tag names before changing the database.

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

Custom sleep locations are exported through the existing `sleepLocation` field using the encoded key format:

```text
CUSTOM_B64:<base64url-encoded UTF-8 display name>
```

No separate custom-location Settings table is exported. Custom locations continue to travel only through sleep-log `sleepLocation` keys.

Custom tag definitions are exported separately starting in backup format version 2. Active/inactive custom tag state and category assignment are preserved by version 2 backups.

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
- Custom tag definition keys, names, categories, active states, and timestamps in version 2 backups

Unknown extra JSON fields are ignored for supported versions. Future night dates are not rejected solely because they are future dates.

## Merge import policy

Import is a merge, not a replace-all restore:

- Imported dates replace matching local dates.
- Imported dates not present locally are inserted.
- Local dates not present in the backup remain unchanged.

For a matching date, the existing local Room ID is preserved while user data fields, tags, `createdAt`, and `updatedAt` are replaced with imported values.

For a new date, Room generates a new local ID and stores the imported values.

Local-only records are not deleted or modified.

If an imported sleep log contains a valid `CUSTOM_B64:` location key, import decodes the display name and ensures a matching custom-location row exists. New reconstructed custom locations become active. Existing local active or inactive rows are not duplicated, and inactive rows remain inactive.

If a custom key cannot be decoded, the key is preserved as an unknown forward-compatible location value.

For version 2 backups, imported matching custom tag keys replace the local custom tag category and active state. New custom tag keys are inserted. Local-only custom tags remain unchanged.

For version 1 backups, there is no `customTags` array. If a log contains a valid `CUSTOM_TAG_B64:` key, import decodes the display name and creates a missing custom-tag row as active in the `Other` category. Existing local custom tag rows keep their current category and active state. Version 1 cannot restore original custom-tag category or active/inactive state because it did not contain that metadata.

Before applying an import, the app calculates a confirmation summary showing imported records, new records, matching dates that will be replaced, local-only records that will remain unchanged, and custom tag definitions that will be added or updated.

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
- Exporting active/inactive custom-location Settings state separately
