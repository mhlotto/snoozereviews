package com.mhlotto.snoozereviews.data.tag;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

public class SleepTagNameNormalizerTest {
    @Test
    public void clean_trimsCollapsesWhitespaceAndNormalizesCase() {
        SleepTagNameNormalizer.CleanedName cleaned = SleepTagNameNormalizer.clean("  WEIGHTED   Blanket  ");

        assertEquals("WEIGHTED Blanket", cleaned.getDisplayName());
        assertEquals("weighted blanket", cleaned.getNormalizedName());
    }

    @Test
    public void clean_rejectsBlankControlAndLongNames() {
        assertInvalid(null);
        assertInvalid("   ");
        assertInvalid("Bad\nName");
        assertInvalid(repeat("a", SleepTagNameNormalizer.MAX_DISPLAY_NAME_CHARS + 1));
    }

    private void assertInvalid(String name) {
        try {
            SleepTagNameNormalizer.clean(name);
            fail("Expected invalid custom tag name");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

    private String repeat(String value, int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            builder.append(value);
        }
        return builder.toString();
    }
}
