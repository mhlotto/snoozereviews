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
7. Confirm optional questions, ratings, and sleep location clear when the selected chip is tapped again.
8. Confirm no explicit `Not answered`, `Not rated`, or `Not specified` chips appear in the form.
9. Add a historical date through Add by Date.
10. Attempt a duplicate date through Add by Date and the form.
11. Browse history.
12. Edit from history and verify row refresh and reorder.
13. Navigate between History, Stats, Add by Date, Backup and Restore, and Settings.
14. Test dirty-form navigation with Keep editing and Discard.
15. Export a backup.
16. Import into empty app data.
17. Import over matching dates and verify replacement.
18. Verify local-only records remain after import.
19. Cancel import and export document pickers.
20. Add, duplicate, remove, and restore a custom sleep location in Settings.
21. Confirm removed custom locations disappear from new form choices.
22. Confirm old logs with removed custom locations still display the location.
23. Add, duplicate, remove, restore, and recategorize a custom descriptive tag in Settings.
24. Confirm removed custom tags disappear from new form choices.
25. Confirm old logs with removed custom tags still display the tag.
26. Confirm detail reports group selected tags under nonempty category headings.
27. Confirm orphaned and unknown selected tags appear under `Other`.
28. Toggle Had dreams Yes -> enter dream details -> clear selection -> Yes and confirm text returns.
29. Save dream details, edit them, then save No and confirm they are cleared.
30. Rotate with visible and hidden dream details and confirm text is preserved.
31. Confirm detail reports show dream details only when Had dreams is Yes.
32. Confirm history rows still use compact tag previews and do not show dream text.
33. Export and import a version 4 backup with dream details, custom tags, and a zero rating.
34. Import version 1 and 2 backups and confirm dream details default to empty.
35. Import a version 1 backup containing encoded custom tag keys and confirm they appear in `Other`.
36. Rotate during loading, editing, Settings, and backup/import operations.
37. Test dark theme.
38. Test increased font size.
39. Test TalkBack labels where available.
40. Test Android 12+ splash behavior.
41. Test pre-Android 12 splash behavior if an older device is available.
42. Confirm the launcher icon remains recognizable under circle, rounded-square, squircle, and OEM masks.
43. Confirm the themed monochrome icon is legible where themed icons are enabled.
44. Confirm startup has no white flash before or after the native splash.

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
