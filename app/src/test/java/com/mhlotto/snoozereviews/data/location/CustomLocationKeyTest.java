package com.mhlotto.snoozereviews.data.location;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CustomLocationKeyTest {
    @Test
    public void encodeDecodeRoundTrip() {
        String key = CustomLocationKey.encode("Hammock");

        assertTrue(CustomLocationKey.isCustomKey(key));
        assertEquals("CUSTOM_B64:SGFtbW9jaw", key);
        assertEquals("Hammock", CustomLocationKey.decode(key));
    }

    @Test
    public void unicodeNameRoundTrip() {
        String key = CustomLocationKey.encode("Café Loft");

        assertEquals("Café Loft", CustomLocationKey.decode(key));
    }

    @Test
    public void malformedCustomKeyReturnsNull() {
        assertNull(CustomLocationKey.decode("CUSTOM_B64:not valid base64!"));
        assertNull(CustomLocationKey.decode("CUSTOM_B64:"));
        assertFalse(CustomLocationKey.isCustomKey("BED"));
    }
}
