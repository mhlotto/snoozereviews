package com.mhlotto.snoozereviews.data.location;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class CustomLocationKey {
    public static final String PREFIX = "CUSTOM_B64:";

    private CustomLocationKey() {
    }

    public static String encode(String displayName) {
        SleepLocationNameNormalizer.CleanedName cleaned = SleepLocationNameNormalizer.clean(displayName);
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
            String displayName = new String(decoded, StandardCharsets.UTF_8);
            return SleepLocationNameNormalizer.clean(displayName).getDisplayName();
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
