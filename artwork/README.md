# Snooze Reviews Artwork

`../snooze-splash-base.png` is the approved titled full-screen splash illustration and should remain at the repository root. Do not overwrite or edit it in place except when replacing it with a newly approved splash source.

`snooze-icon-source.png` is the preserved untitled illustration used for launcher and compact splash icons. Do not replace it with the titled splash art; title text is intentionally excluded from small icon resources.

Generated assets are produced with:

```bash
./scripts/generate-visual-assets.sh
```

The script requires ImageMagick 7 through the `magick` command. It uses `pngcheck` and `oxipng` when those tools are installed, but they are optional.

## Source, Crop, And Outputs

The approved full-screen splash source artwork is:

```text
snooze-splash-base.png
941 x 1672 PNG
```

It contains the `Snooze Reviews` title and is used only for the custom full-screen splash shown by `SplashActivity`.

The approved icon source artwork is:

```text
artwork/snooze-icon-source.png
941 x 1672 PNG
```

It preserves the untitled illustration for native splash and launcher icon generation.

The preserved generated derivative is the native crop master:

```text
artwork/generated/snooze-native-crop-master.png
600 x 600 PNG
```

Native crop geometry:

```text
600x600+230+470
```

The crop is generated from `artwork/snooze-icon-source.png` and is centered on the sleeping man's face, gray beard, blue-and-white nightcap, pillow edge, and readable yellow `zzz`. Larger test crops from roughly 650 to 900 pixels pulled the moon and alarm-clock edges back into the icon composition, so the original 600-pixel crop remains the largest useful crop for the approved simplified native splash and launcher artwork.

The native crop master is stored at its actual crop dimensions. The script does not preserve artificial 1152-pixel or 1024-pixel source masters. Android density outputs are composed directly from `snooze-native-crop-master.png` with one Lanczos resize per output.

The splash and launcher foreground outputs use a circular image treatment on transparent canvases to avoid an obvious rectangular crop. Legacy launcher icons composite the circular image over the final deep-night background.

The custom launch screen also packages a full-portrait derivative directly from the titled root source:

```text
app/src/main/res/drawable-nodpi/snooze_splash_full.png
941 x 1672 PNG
```

This file is used as Activity content after the brief native splash phase. It preserves the full source composition, including the title, and is not generated from the crop master.

The title must not appear in launcher icons, round icons, adaptive foregrounds, the native splash icon, or the monochrome themed icon. Launcher labels already provide the app name, and embedded title text would be illegible at small icon sizes.

## Generated Android Outputs

Splash icons:

```text
drawable-mdpi/snooze_splash_icon.png       288 x 288, artwork 225 x 225
drawable-hdpi/snooze_splash_icon.png       432 x 432, artwork 338 x 338
drawable-xhdpi/snooze_splash_icon.png      576 x 576, artwork 450 x 450
drawable-xxhdpi/snooze_splash_icon.png     864 x 864, artwork 675 x 675
drawable-xxxhdpi/snooze_splash_icon.png   1152 x 1152, artwork 900 x 900
```

The `xxhdpi` and `xxxhdpi` splash outputs require enlargement from the 600-pixel native crop because Android's splash icon density targets are larger than the selected crop. These files are Android resource outputs, not source masters, and they do not contain native 864-pixel or 1152-pixel artwork detail.

Adaptive launcher foregrounds:

```text
mipmap-mdpi/ic_launcher_foreground.png       108 x 108, artwork 85 x 85
mipmap-hdpi/ic_launcher_foreground.png       162 x 162, artwork 128 x 128
mipmap-xhdpi/ic_launcher_foreground.png      216 x 216, artwork 170 x 170
mipmap-xxhdpi/ic_launcher_foreground.png     324 x 324, artwork 255 x 255
mipmap-xxxhdpi/ic_launcher_foreground.png    432 x 432, artwork 340 x 340
```

Legacy launcher icons:

```text
mipmap-mdpi/ic_launcher.png                  48 x 48, artwork 40 x 40
mipmap-hdpi/ic_launcher.png                  72 x 72, artwork 60 x 60
mipmap-xhdpi/ic_launcher.png                 96 x 96, artwork 80 x 80
mipmap-xxhdpi/ic_launcher.png               144 x 144, artwork 120 x 120
mipmap-xxxhdpi/ic_launcher.png              192 x 192, artwork 159 x 159
```

Round launcher icons use the same legacy dimensions and artwork sizes, with an alpha mask applied at the final output size.
