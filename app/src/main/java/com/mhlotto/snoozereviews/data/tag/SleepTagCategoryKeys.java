package com.mhlotto.snoozereviews.data.tag;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class SleepTagCategoryKeys {
    public static final String ENVIRONMENT = "ENVIRONMENT";
    public static final String PHYSICAL = "PHYSICAL";
    public static final String SLEEP_PATTERN = "SLEEP_PATTERN";
    public static final String MIND_AND_DREAMS = "MIND_AND_DREAMS";
    public static final String MORNING_RESULT = "MORNING_RESULT";
    public static final String OTHER = "OTHER";

    public static final List<String> ORDERED_KEYS = Collections.unmodifiableList(Arrays.asList(
            ENVIRONMENT,
            PHYSICAL,
            SLEEP_PATTERN,
            MIND_AND_DREAMS,
            MORNING_RESULT,
            OTHER
    ));

    private SleepTagCategoryKeys() {
    }

    public static boolean isValid(String key) {
        return ORDERED_KEYS.contains(key);
    }

    public static int orderOf(String key) {
        int index = ORDERED_KEYS.indexOf(key);
        return index < 0 ? ORDERED_KEYS.indexOf(OTHER) : index;
    }
}
