package com.mhlotto.snoozereviews.data.tag;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CustomSleepTagKeyTest {
    @Test
    public void encodeDecode_roundTripsCleanedName() {
        String key = CustomSleepTagKey.encode("  Weighted   Blanket  ");

        assertTrue(key.startsWith(CustomSleepTagKey.PREFIX));
        assertEquals("Weighted Blanket", CustomSleepTagKey.decode(key));
    }

    @Test
    public void encodeDecode_preservesUnicode() {
        String key = CustomSleepTagKey.encode("Café Futon");

        assertEquals("Café Futon", CustomSleepTagKey.decode(key));
    }

    @Test
    public void malformedCustomKeysReturnNull() {
        assertFalse(CustomSleepTagKey.isCustomKey("TOO_HOT"));
        assertNull(CustomSleepTagKey.decode("TOO_HOT"));
        assertNull(CustomSleepTagKey.decode(CustomSleepTagKey.PREFIX));
        assertNull(CustomSleepTagKey.decode(CustomSleepTagKey.PREFIX + "%%%"));
    }
}
