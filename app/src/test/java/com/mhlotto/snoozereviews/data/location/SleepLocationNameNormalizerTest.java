package com.mhlotto.snoozereviews.data.location;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SleepLocationNameNormalizerTest {
    @Test
    public void trimsCollapsesWhitespaceAndNormalizesCase() {
        SleepLocationNameNormalizer.CleanedName cleaned = SleepLocationNameNormalizer.clean("  Guest   Room  ");

        assertEquals("Guest Room", cleaned.getDisplayName());
        assertEquals("guest room", cleaned.getNormalizedName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBlankName() {
        SleepLocationNameNormalizer.clean("   ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsControlCharacters() {
        SleepLocationNameNormalizer.clean("Ham\nmock");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNamesLongerThanEightyCharacters() {
        SleepLocationNameNormalizer.clean("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
    }
}
