# Visual system

Snooze Reviews uses a restrained sleep-oriented visual identity derived from the approved source illustration.

## Source artwork

The approved source artwork is kept at the repository root:

```text
snooze-splash-base.png
```

Source dimensions and format:

```text
941 x 1672 PNG
```

Do not overwrite, move, or modify this file in place.

Detailed provenance and generation notes live in [../artwork/README.md](../artwork/README.md).

## Native crop master

Generated Android artwork starts from a native crop master:

```text
artwork/generated/snooze-native-crop-master.png
600 x 600 PNG
```

Current crop geometry:

```text
600x600+230+470
```

The crop focuses on the sleeping face, gray beard, blue-and-white nightcap, pillow edge, and readable yellow `zzz`. Larger tested crops reintroduced moon and alarm-clock edges, so the 600-pixel crop remains the selected composition.

Android density outputs are generated directly from this native crop master with one Lanczos resize per output. Enlarged Android density files are resource outputs, not source masters.

## Splash artwork

The app uses AndroidX SplashScreen with:

- A solid deep-night background
- A centered circular derived icon
- No full-screen bitmap background
- No second custom splash animation

The existing 2.5-second launch-routing behavior is application logic and is not part of the artwork generation.

## Launcher icons

Launcher assets include:

- Adaptive icon background and foreground for API 26+
- Density-specific legacy launcher PNGs
- Round launcher PNGs
- A one-color VectorDrawable monochrome icon for themed launchers

The launcher icon uses the sleeping face/nightcap composition and does not include app-name text, the full portrait scene, the moon, or the alarm clock.

## Palette and themes

The palette is derived from the illustration:

- Deep night blue: `#102846`
- Medium moonlit blue: `#2F5F96`
- Soft pale blue: `#DCEBFA`
- Warm moon yellow: `#F5C95F`
- Off-white pillow: `#F8F2E4`
- Muted gray-blue: `#5D7188`
- Error red: `#BA1A1A`

Light and dark themes use semantic Material color roles. Yellow is used as an accent rather than large body text. Dark surfaces avoid pure black.

## Regenerating assets

Regenerate assets with:

```bash
make visual-assets
```

or:

```bash
./scripts/generate-visual-assets.sh
```

ImageMagick 7 is required through the `magick` command. `pngcheck` and `oxipng` are used when installed but are optional.

Generated source derivatives live under `artwork/generated/`. Android resource outputs live under `app/src/main/res/drawable-*` and `app/src/main/res/mipmap-*`.

## Manual visual checks

Device review is still required for:

- Android 12+ native splash rendering
- Pre-Android 12 AndroidX splash rendering
- Launcher masks from different OEM launchers
- Themed monochrome icon presentation
- Light and dark theme contrast
- Large-font layout behavior
- Startup with no white flash

The visual QA checklist is in [../TESTING.md](../TESTING.md).
