# Snooze Reviews Testing

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
11. Navigate between History, Stats, Add by Date, and Backup and Restore.
12. Test dirty-form navigation with Keep editing and Discard.
13. Export a backup.
14. Import into empty app data.
15. Import over matching dates and verify replacement.
16. Verify local-only records remain after import.
17. Cancel import and export document pickers.
18. Rotate during loading, editing, and backup/import operations.
19. Test dark theme.
20. Test increased font size.
21. Test TalkBack labels where available.
22. Test Android 12+ splash behavior.
23. Test pre-Android 12 splash behavior if an older device is available.
24. Confirm the launcher icon remains recognizable under circle, rounded-square, squircle, and OEM masks.
25. Confirm the themed monochrome icon is legible where themed icons are enabled.
26. Confirm startup has no white flash before or after the native splash.

## Accessibility And Layout Checklist

- Toolbar titles and overflow actions are announced clearly.
- Buttons meet normal Material touch-target expectations.
- Error messages are visible and understandable without color alone.
- Progress states disable duplicate actions while preserving navigation where safe.
- Long notes and tag lists wrap or scroll without clipping.
- Screens remain usable on small phones and at larger font scales.

## Visual Asset Checklist

Use this checklist for a later visual device pass. The current automated workflow validates that resources compile and resolve, but it does not replace device inspection.

- Android 12+ native splash shows the derived sleeping-face icon on a solid deep-night background.
- Pre-Android 12 AndroidX splash behavior is visually consistent.
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
