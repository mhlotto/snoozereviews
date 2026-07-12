package com.mhlotto.snoozereviews.data.location;

import java.util.Locale;

public final class SleepLocationNameNormalizer {
    public static final int MAX_DISPLAY_NAME_CHARS = 80;

    private SleepLocationNameNormalizer() {
    }

    public static CleanedName clean(String rawName) {
        if (rawName == null) {
            throw new IllegalArgumentException("location name is required");
        }
        StringBuilder builder = new StringBuilder();
        boolean pendingSpace = false;
        for (int i = 0; i < rawName.length(); ) {
            int codePoint = rawName.codePointAt(i);
            if (Character.isISOControl(codePoint)) {
                throw new IllegalArgumentException("location name must not contain control characters");
            }
            if (Character.isWhitespace(codePoint)) {
                pendingSpace = builder.length() > 0;
            } else {
                if (pendingSpace) {
                    builder.append(' ');
                    pendingSpace = false;
                }
                builder.appendCodePoint(codePoint);
            }
            i += Character.charCount(codePoint);
        }

        String displayName = builder.toString();
        if (displayName.isEmpty()) {
            throw new IllegalArgumentException("location name is required");
        }
        if (displayName.codePointCount(0, displayName.length()) > MAX_DISPLAY_NAME_CHARS) {
            throw new IllegalArgumentException("location name must be 80 characters or fewer");
        }
        return new CleanedName(displayName, displayName.toLowerCase(Locale.ROOT));
    }

    public static final class CleanedName {
        private final String displayName;
        private final String normalizedName;

        private CleanedName(String displayName, String normalizedName) {
            this.displayName = displayName;
            this.normalizedName = normalizedName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getNormalizedName() {
            return normalizedName;
        }
    }
}
