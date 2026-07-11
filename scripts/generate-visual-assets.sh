#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SOURCE_IMAGE="${ROOT_DIR}/snooze-splash-base.png"
ARTWORK_DIR="${ROOT_DIR}/artwork/generated"
RES_DIR="${ROOT_DIR}/app/src/main/res"
WORK_DIR="$(mktemp -d)"

CROP_GEOMETRY="600x600+230+470"
NATIVE_CROP_SIZE=600
SPLASH_BG="#102846"

cleanup() {
    rm -rf "${WORK_DIR}"
}
trap cleanup EXIT

require_tool() {
    if ! command -v "$1" >/dev/null 2>&1; then
        printf "Required tool not found: %s\n" "$1" >&2
        exit 1
    fi
}

ensure_source() {
    if [ ! -f "${SOURCE_IMAGE}" ]; then
        printf "Missing source artwork: %s\n" "${SOURCE_IMAGE}" >&2
        exit 1
    fi
}

png_channels() {
    magick identify -format "%[channels]" "$1"
}

validate_png() {
    local file="$1"
    local expected="$2"
    local actual
    if [ ! -s "${file}" ]; then
        printf "Generated file is missing or empty: %s\n" "${file}" >&2
        exit 1
    fi
    actual="$(magick identify -format "%wx%h" "${file}")"
    if [ "${actual}" != "${expected}" ]; then
        printf "Unexpected dimensions for %s: expected %s, got %s\n" "${file}" "${expected}" "${actual}" >&2
        exit 1
    fi
}

make_native_crop_master() {
    local output="$1"

    magick "${SOURCE_IMAGE}" \
        -crop "${CROP_GEOMETRY}" +repage \
        -strip -colorspace sRGB \
        "${output}"
}

make_circle_art() {
    local crop_master="$1"
    local size="$2"
    local art_size="$3"
    local output="$4"
    local crop="${WORK_DIR}/crop-${size}-${art_size}.png"
    local circle="${WORK_DIR}/circle-${size}-${art_size}.png"
    local mask="${WORK_DIR}/mask-${size}-${art_size}.png"

    magick "${crop_master}" \
        -filter Lanczos -resize "${art_size}x${art_size}" \
        -strip -colorspace sRGB \
        "${crop}"

    magick -size "${art_size}x${art_size}" xc:none \
        -fill white \
        -draw "circle $((art_size / 2)),$((art_size / 2)) $((art_size / 2)),$((art_size / 18))" \
        "${mask}"

    magick "${crop}" "${mask}" -alpha off -compose CopyOpacity -composite "${circle}"

    magick -size "${size}x${size}" xc:none \
        "${circle}" -gravity center -compose over -composite \
        -strip -colorspace sRGB \
        "${output}"
}

make_full_launcher_icon() {
    local crop_master="$1"
    local size="$2"
    local art_size="$3"
    local output="$4"
    local circle="${WORK_DIR}/launcher-circle-${size}-${art_size}.png"

    make_circle_art "${crop_master}" "${size}" "${art_size}" "${circle}"
    magick -size "${size}x${size}" "xc:${SPLASH_BG}" \
        "${circle}" -gravity center -compose over -composite \
        -strip -colorspace sRGB \
        "${output}"
}

make_round_launcher_icon() {
    local input="$1"
    local size="$2"
    local output="$3"
    local mask="${WORK_DIR}/round-launcher-mask-${size}.png"

    magick -size "${size}x${size}" xc:none \
        -fill white \
        -draw "circle $((size / 2)),$((size / 2)) $((size / 2)),0" \
        "${mask}"
    magick "${input}" "${mask}" -alpha off -compose CopyOpacity -composite \
        -strip -colorspace sRGB \
        "${output}"
}

make_splash_icon() {
    local crop_master="$1"
    local size="$2"
    local art_size="$3"
    local output="$4"
    mkdir -p "$(dirname "${output}")"
    make_circle_art "${crop_master}" "${size}" "${art_size}" "${output}"
}

make_foreground_icon() {
    local crop_master="$1"
    local size="$2"
    local art_size="$3"
    local output="$4"
    mkdir -p "$(dirname "${output}")"
    make_circle_art "${crop_master}" "${size}" "${art_size}" "${output}"
}

make_legacy_icons() {
    local crop_master="$1"
    local size="$2"
    local art_size="$3"
    local icon_output="$4"
    local round_output="$5"
    local unmasked="${WORK_DIR}/launcher-${size}.png"

    mkdir -p "$(dirname "${icon_output}")"
    make_full_launcher_icon "${crop_master}" "${size}" "${art_size}" "${unmasked}"
    cp "${unmasked}" "${icon_output}"
    make_round_launcher_icon "${unmasked}" "${size}" "${round_output}"
}

main() {
    require_tool magick
    ensure_source

    local dimensions
    dimensions="$(magick identify -format "%wx%h %m %[colorspace]" "${SOURCE_IMAGE}")"
    printf "Source: %s\n" "${SOURCE_IMAGE}"
    printf "Source details: %s\n" "${dimensions}"

    local source_width source_height
    source_width="$(magick identify -format "%w" "${SOURCE_IMAGE}")"
    source_height="$(magick identify -format "%h" "${SOURCE_IMAGE}")"
    if [ "${source_width}" -lt 900 ] || [ "${source_height}" -lt 1200 ]; then
        printf "Source artwork is smaller than expected: %sx%s\n" "${source_width}" "${source_height}" >&2
        exit 1
    fi

    mkdir -p "${ARTWORK_DIR}"

    local native_crop_master="${ARTWORK_DIR}/snooze-native-crop-master.png"

    find "${ARTWORK_DIR}" -type f -name '*.png' -delete
    make_native_crop_master "${native_crop_master}"

    validate_png "${native_crop_master}" "${NATIVE_CROP_SIZE}x${NATIVE_CROP_SIZE}"

    make_splash_icon "${native_crop_master}" 288 225 "${RES_DIR}/drawable-mdpi/snooze_splash_icon.png"
    make_splash_icon "${native_crop_master}" 432 338 "${RES_DIR}/drawable-hdpi/snooze_splash_icon.png"
    make_splash_icon "${native_crop_master}" 576 450 "${RES_DIR}/drawable-xhdpi/snooze_splash_icon.png"
    make_splash_icon "${native_crop_master}" 864 675 "${RES_DIR}/drawable-xxhdpi/snooze_splash_icon.png"
    make_splash_icon "${native_crop_master}" 1152 900 "${RES_DIR}/drawable-xxxhdpi/snooze_splash_icon.png"

    make_foreground_icon "${native_crop_master}" 108 85 "${RES_DIR}/mipmap-mdpi/ic_launcher_foreground.png"
    make_foreground_icon "${native_crop_master}" 162 128 "${RES_DIR}/mipmap-hdpi/ic_launcher_foreground.png"
    make_foreground_icon "${native_crop_master}" 216 170 "${RES_DIR}/mipmap-xhdpi/ic_launcher_foreground.png"
    make_foreground_icon "${native_crop_master}" 324 255 "${RES_DIR}/mipmap-xxhdpi/ic_launcher_foreground.png"
    make_foreground_icon "${native_crop_master}" 432 340 "${RES_DIR}/mipmap-xxxhdpi/ic_launcher_foreground.png"

    make_legacy_icons "${native_crop_master}" 48 40 "${RES_DIR}/mipmap-mdpi/ic_launcher.png" "${RES_DIR}/mipmap-mdpi/ic_launcher_round.png"
    make_legacy_icons "${native_crop_master}" 72 60 "${RES_DIR}/mipmap-hdpi/ic_launcher.png" "${RES_DIR}/mipmap-hdpi/ic_launcher_round.png"
    make_legacy_icons "${native_crop_master}" 96 80 "${RES_DIR}/mipmap-xhdpi/ic_launcher.png" "${RES_DIR}/mipmap-xhdpi/ic_launcher_round.png"
    make_legacy_icons "${native_crop_master}" 144 120 "${RES_DIR}/mipmap-xxhdpi/ic_launcher.png" "${RES_DIR}/mipmap-xxhdpi/ic_launcher_round.png"
    make_legacy_icons "${native_crop_master}" 192 159 "${RES_DIR}/mipmap-xxxhdpi/ic_launcher.png" "${RES_DIR}/mipmap-xxxhdpi/ic_launcher_round.png"

    validate_png "${RES_DIR}/drawable-mdpi/snooze_splash_icon.png" "288x288"
    validate_png "${RES_DIR}/drawable-hdpi/snooze_splash_icon.png" "432x432"
    validate_png "${RES_DIR}/drawable-xhdpi/snooze_splash_icon.png" "576x576"
    validate_png "${RES_DIR}/drawable-xxhdpi/snooze_splash_icon.png" "864x864"
    validate_png "${RES_DIR}/drawable-xxxhdpi/snooze_splash_icon.png" "1152x1152"
    validate_png "${RES_DIR}/mipmap-mdpi/ic_launcher.png" "48x48"
    validate_png "${RES_DIR}/mipmap-hdpi/ic_launcher.png" "72x72"
    validate_png "${RES_DIR}/mipmap-xhdpi/ic_launcher.png" "96x96"
    validate_png "${RES_DIR}/mipmap-xxhdpi/ic_launcher.png" "144x144"
    validate_png "${RES_DIR}/mipmap-xxxhdpi/ic_launcher.png" "192x192"

    case "$(png_channels "${RES_DIR}/drawable-xxxhdpi/snooze_splash_icon.png")" in
        *a*|*A*) ;;
        *) printf "Splash icon outputs are expected to contain alpha.\n" >&2; exit 1 ;;
    esac

    if command -v pngcheck >/dev/null 2>&1; then
        pngcheck -q "${ARTWORK_DIR}"/*.png "${RES_DIR}"/drawable-*/*.png "${RES_DIR}"/mipmap-*/*.png
        printf "pngcheck: ok\n"
    else
        printf "pngcheck: not installed; skipped\n"
    fi

    if command -v oxipng >/dev/null 2>&1; then
        oxipng -q -o 2 "${ARTWORK_DIR}"/*.png "${RES_DIR}"/drawable-*/*.png "${RES_DIR}"/mipmap-*/*.png
        printf "oxipng: optimized final PNGs\n"
    else
        printf "oxipng: not installed; skipped\n"
    fi

    printf "\nGenerated visual assets from native crop master (%s):\n" "${CROP_GEOMETRY}"
    find "${ARTWORK_DIR}" "${RES_DIR}" \
        \( -name 'snooze-native-crop-master.png' -o -name 'snooze_splash_icon.png' -o -name 'ic_launcher*.png' \) \
        -type f | sort | sed "s#${ROOT_DIR}/#  #"
}

main "$@"
