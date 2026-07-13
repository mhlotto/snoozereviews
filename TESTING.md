# Snooze Reviews Testing

Feature behavior is summarized in [docs/FEATURES.md](docs/FEATURES.md). Backup behavior is documented in [docs/BACKUP_AND_RESTORE.md](docs/BACKUP_AND_RESTORE.md), and visual asset details are documented in [docs/VISUALS.md](docs/VISUALS.md).

## Automated Checks

Run local non-device checks:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
make check
./gradlew assembleDebugAndroidTest
```

Run instrumentation tests on a connected device or emulator when available:

```bash
./gradlew connectedDebugAndroidTest
```

The current CI-style workflow compiles instrumentation tests but may defer device execution.

## Manual Smoke Checklist

Use this checklist for later device testing. Do not mark items complete unless they are actually exercised on a device or emulator.

1. Fresh install with no app data.
2. Splash routes to last-night create.
3. Create a date-only sleep log.
4. Create a fully populated sleep log.
5. Restart the app and verify last-night detail routing.
6. Edit a log and clear optional fields.
7. Add a historical date through Add by Date.
8. Attempt a duplicate date through Add by Date and the form.
9. Browse history.
10. Edit from history and verify row refresh and reorder.
11. Navigate between History, Stats, Add by Date, Backup and Restore, and Settings.
12. Test dirty-form navigation with Keep editing and Discard.
13. Export a backup.
14. Import into empty app data.
15. Import over matching dates and verify replacement.
16. Verify local-only records remain after import.
17. Cancel import and export document pickers.
18. Add, duplicate, remove, and restore a custom sleep location in Settings.
19. Confirm removed custom locations disappear from new form choices.
20. Confirm old logs with removed custom locations still display the location.
21. Add, duplicate, remove, restore, and recategorize a custom descriptive tag in Settings.
22. Confirm removed custom tags disappear from new form choices.
23. Confirm old logs with removed custom tags still display the tag.
24. Confirm detail reports group selected tags under nonempty category headings.
25. Confirm orphaned and unknown selected tags appear under `Other`.
26. Toggle Had dreams Yes -> enter dream details -> No -> Yes and confirm text returns.
27. Save dream details, edit them, then save No and confirm they are cleared.
28. Rotate with visible and hidden dream details and confirm text is preserved.
29. Confirm detail reports show dream details only when Had dreams is Yes.
30. Confirm history rows still use compact tag previews and do not show dream text.
31. Export and import a version 4 backup with dream details, custom tags, and a zero rating.
32. Import version 1 and 2 backups and confirm dream details default to empty.
33. Import a version 1 backup containing encoded custom tag keys and confirm they appear in `Other`.
34. Rotate during loading, editing, Settings, and backup/import operations.
35. Test dark theme.
36. Test increased font size.
37. Test TalkBack labels where available.
38. Test Android 12+ splash behavior.
39. Test pre-Android 12 splash behavior if an older device is available.
27. Confirm the launcher icon remains recognizable under circle, rounded-square, squircle, and OEM masks.
28. Confirm the themed monochrome icon is legible where themed icons are enabled.
29. Confirm startup has no white flash before or after the native splash.

## Accessibility And Layout Checklist

- Toolbar titles and overflow actions are announced clearly.
- Buttons meet normal Material touch-target expectations.
- Error messages are visible and understandable without color alone.
- Progress states disable duplicate actions while preserving navigation where safe.
- Long notes and tag lists wrap or scroll without clipping.
- Screens remain usable on small phones and at larger font scales.

## Visual Asset Checklist

Use this checklist for a later visual device pass. The current automated workflow validates that resources compile and resolve, but it does not replace device inspection.

- Android 12+ native splash briefly shows the derived sleeping-face icon, then transitions to the full portrait launch screen.
- Pre-Android 12 AndroidX splash briefly shows the derived icon, then transitions to the full portrait launch screen.
- Splash remains visible for the existing routing delay and does not introduce a second full-screen animation.
- Launcher icon foreground is not clipped under circle, rounded-square, squircle, and irregular OEM masks.
- Legacy launcher icons have transparent edges where expected and no accidental white background.
- The themed monochrome icon is a simple one-color motif, not a grayscale copy of the illustration.
- Light theme surfaces, cards, chips, dialogs, and buttons maintain readable contrast.
- Dark theme avoids pure-black surfaces and keeps text, inputs, chips, and buttons readable.
- Warm yellow is used as an accent rather than body text.
- Common phone sizes do not clip toolbar titles, form controls, history rows, or backup text.
- Increased font size does not hide primary actions or truncate critical labels.
- Decorative imagery is not announced redundantly by TalkBack.

## Backup Notes

- Backup files contain private sleep details.
- Export uses Android's document creation flow; no storage permission is required.
- Import uses Android's document opening flow; no file path assumptions should be made.
- Logical JSON backups should not be confused with raw SQLite database files.
- Detailed backup validation, merge, and rollback behavior is documented in [docs/BACKUP_AND_RESTORE.md](docs/BACKUP_AND_RESTORE.md).
