package com.mhlotto.snoozereviews.data.tag;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class CustomSleepTagKey {
    public static final String PREFIX = "CUSTOM_TAG_B64:";

    private CustomSleepTagKey() {
    }

    public static String encode(String displayName) {
        SleepTagNameNormalizer.CleanedName cleaned = SleepTagNameNormalizer.clean(displayName);
        String encoded = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(cleaned.getDisplayName().getBytes(StandardCharsets.UTF_8));
        return PREFIX + encoded;
    }

    public static boolean isCustomKey(String key) {
        return key != null && key.startsWith(PREFIX);
    }

    public static String decode(String key) {
        if (!isCustomKey(key)) {
            return null;
        }
        String encoded = key.substring(PREFIX.length());
        if (encoded.isEmpty()) {
            return null;
        }
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(encoded);
            return SleepTagNameNormalizer.clean(new String(decoded, StandardCharsets.UTF_8)).getDisplayName();
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
