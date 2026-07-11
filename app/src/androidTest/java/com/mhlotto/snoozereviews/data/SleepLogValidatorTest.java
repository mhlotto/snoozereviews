package com.mhlotto.snoozereviews.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.mhlotto.snoozereviews.data.entity.SleepLogEntity;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class SleepLogValidatorTest {
    @Test
    public void invalidNightDatesFail() {
        assertThrows(IllegalArgumentException.class, () -> validateLogWithNightDate("2026-7-10"));
        assertThrows(IllegalArgumentException.class, () -> validateLogWithNightDate("2026-02-30"));
    }

    @Test
    public void nullNightDateFails() {
        assertThrows(IllegalArgumentException.class, () -> validateLogWithNightDate(null));
    }

    @Test
    public void invalidMinutesFail() {
        SleepLogEntity negative = new SleepLogEntity("2026-07-10");
        negative.setFellAsleepMinute(-1);
        assertThrows(IllegalArgumentException.class, () -> SleepLogValidator.validatedCopyForWrite(negative));

        SleepLogEntity tooLarge = new SleepLogEntity("2026-07-10");
        tooLarge.setWokeUpMinute(1440);
        assertThrows(IllegalArgumentException.class, () -> SleepLogValidator.validatedCopyForWrite(tooLarge));
    }

    @Test
    public void invalidRatingsFail() {
        SleepLogEntity low = new SleepLogEntity("2026-07-10");
        low.setSleepRating(0);
        assertThrows(IllegalArgumentException.class, () -> SleepLogValidator.validatedCopyForWrite(low));

        SleepLogEntity high = new SleepLogEntity("2026-07-10");
        high.setRestedRating(6);
        assertThrows(IllegalArgumentException.class, () -> SleepLogValidator.validatedCopyForWrite(high));
    }

    @Test
    public void invalidAwakeningCountAndLocationFail() {
        SleepLogEntity negativeAwakenings = new SleepLogEntity("2026-07-10");
        negativeAwakenings.setAwakeningCount(-1);
        assertThrows(IllegalArgumentException.class, () -> SleepLogValidator.validatedCopyForWrite(negativeAwakenings));

        SleepLogEntity blankLocation = new SleepLogEntity("2026-07-10");
        blankLocation.setSleepLocation("   ");
        assertThrows(IllegalArgumentException.class, () -> SleepLogValidator.validatedCopyForWrite(blankLocation));
    }

    @Test
    public void invalidTagsFail() {
        assertThrows(IllegalArgumentException.class, () -> SleepLogValidator.normalizeTagKeys(Arrays.asList("OK", null)));
        assertThrows(IllegalArgumentException.class, () -> SleepLogValidator.normalizeTagKeys(Arrays.asList("OK", "   ")));
    }

    @Test
    public void unknownValidTagsArePreservedAndTagsAreNormalized() {
        List<String> tags = SleepLogValidator.normalizeTagKeys(Arrays.asList(
                " " + SleepTagKeys.CALM + " ",
                "FUTURE_TAG",
                SleepTagKeys.CALM
        ));

        assertEquals(Arrays.asList(SleepTagKeys.CALM, "FUTURE_TAG"), tags);
    }

    @Test
    public void notesAreTrimmedAndBlankNotesBecomeNull() {
        SleepLogEntity note = new SleepLogEntity("2026-07-10");
        note.setNotes("  useful note  ");
        assertEquals("useful note", SleepLogValidator.validatedCopyForWrite(note).getNotes());

        SleepLogEntity blank = new SleepLogEntity("2026-07-10");
        blank.setNotes("   ");
        assertEquals(null, SleepLogValidator.validatedCopyForWrite(blank).getNotes());
    }

    private void validateLogWithNightDate(String nightDate) {
        SleepLogValidator.validatedCopyForWrite(new SleepLogEntity(nightDate));
    }
}
