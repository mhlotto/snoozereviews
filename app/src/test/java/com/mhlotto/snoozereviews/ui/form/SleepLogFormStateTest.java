package com.mhlotto.snoozereviews.ui.form;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.mhlotto.snoozereviews.data.SleepLocationKeys;
import com.mhlotto.snoozereviews.data.SleepTagKeys;
import com.mhlotto.snoozereviews.data.entity.SleepLogEntity;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class SleepLogFormStateTest {
    @Test
    public void createStateMapsToSleepLogEntity() {
        SleepLogFormState state = SleepLogFormState.create("2026-07-10");
        state.setSleepLocationKey(SleepLocationKeys.BED);
        state.setFellAsleepMinute(1350);
        state.setWokeUpMinute(435);
        state.setSleepRating(4);

        SleepLogEntity entity = state.toEntityForSave();

        assertEquals(0L, entity.getId());
        assertEquals("2026-07-10", entity.getNightDate());
        assertEquals(SleepLocationKeys.BED, entity.getSleepLocation());
        assertEquals(Integer.valueOf(1350), entity.getFellAsleepMinute());
        assertEquals(Integer.valueOf(435), entity.getWokeUpMinute());
        assertEquals(Integer.valueOf(4), entity.getSleepRating());
    }

    @Test
    public void editStateMappingPreservesRecordId() {
        SleepLogFormState state = SleepLogFormState.create("2026-07-10");
        state.setId(42L);

        assertEquals(42L, state.toEntityForSave().getId());
    }

    @Test
    public void triStateAndRatingMappingPreservesNullableValues() {
        SleepLogFormState state = SleepLogFormState.create("2026-07-10");
        state.setSleptThroughNight(Boolean.TRUE);
        state.setHadDreams(Boolean.FALSE);
        state.setSleepRating(null);
        state.setRestedRating(0);

        SleepLogEntity entity = state.toEntityForSave();

        assertEquals(Boolean.TRUE, entity.getSleptThroughNight());
        assertEquals(Boolean.FALSE, entity.getHadDreams());
        assertNull(entity.getSleepRating());
        assertEquals(Integer.valueOf(0), entity.getRestedRating());
    }

    @Test
    public void nullableQuestionStatePreservesNullTrueAndFalse() {
        SleepLogFormState unanswered = SleepLogFormState.create("2026-07-10");
        assertNull(unanswered.toEntityForSave().getSleptThroughNight());
        assertNull(unanswered.toEntityForSave().getHadDreams());

        SleepLogFormState yes = new SleepLogFormState(unanswered);
        yes.setSleptThroughNight(Boolean.TRUE);
        yes.setHadDreams(Boolean.TRUE);
        assertEquals(Boolean.TRUE, yes.toEntityForSave().getSleptThroughNight());
        assertEquals(Boolean.TRUE, yes.toEntityForSave().getHadDreams());

        SleepLogFormState no = new SleepLogFormState(unanswered);
        no.setSleptThroughNight(Boolean.FALSE);
        no.setHadDreams(Boolean.FALSE);
        assertEquals(Boolean.FALSE, no.toEntityForSave().getSleptThroughNight());
        assertEquals(Boolean.FALSE, no.toEntityForSave().getHadDreams());

        assertFalse(unanswered.equals(yes));
        assertFalse(unanswered.equals(no));
        assertFalse(yes.equals(no));
    }

    @Test
    public void nullableLocationStatePreservesNullAndLocationKeys() {
        SleepLogFormState unspecified = SleepLogFormState.create("2026-07-10");
        assertNull(unspecified.toEntityForSave().getSleepLocation());

        SleepLogFormState bed = new SleepLogFormState(unspecified);
        bed.setSleepLocationKey(SleepLocationKeys.BED);

        SleepLogFormState couch = new SleepLogFormState(unspecified);
        couch.setSleepLocationKey(SleepLocationKeys.COUCH);

        assertEquals(SleepLocationKeys.BED, bed.toEntityForSave().getSleepLocation());
        assertEquals(SleepLocationKeys.COUCH, couch.toEntityForSave().getSleepLocation());
        assertTrue(bed.isDirtyComparedTo(unspecified));
        assertTrue(unspecified.isDirtyComparedTo(bed));
        assertTrue(couch.isDirtyComparedTo(bed));
    }

    @Test
    public void nullableTimeStatePreservesNullMidnightAndDirtyChanges() {
        SleepLogFormState noTimes = SleepLogFormState.create("2026-07-10");
        assertNull(noTimes.toEntityForSave().getFellAsleepMinute());
        assertNull(noTimes.toEntityForSave().getWokeUpMinute());

        SleepLogFormState midnight = new SleepLogFormState(noTimes);
        midnight.setFellAsleepMinute(0);
        midnight.setWokeUpMinute(1439);

        assertEquals(Integer.valueOf(0), midnight.toEntityForSave().getFellAsleepMinute());
        assertEquals(Integer.valueOf(1439), midnight.toEntityForSave().getWokeUpMinute());
        assertTrue(midnight.isDirtyComparedTo(noTimes));

        SleepLogFormState cleared = new SleepLogFormState(midnight);
        cleared.setFellAsleepMinute(null);
        cleared.setWokeUpMinute(null);
        assertTrue(cleared.isDirtyComparedTo(midnight));
        assertFalse(cleared.isDirtyComparedTo(noTimes));
    }

    @Test
    public void dreamDetailsPersistOnlyWhenHadDreamsIsYes() {
        SleepLogFormState yes = SleepLogFormState.create("2026-07-10");
        yes.setHadDreams(Boolean.TRUE);
        yes.setDreamDetails("  Forest\nPath  ");
        assertEquals("Forest\nPath", yes.toEntityForSave().getDreamDetails());

        SleepLogFormState blank = SleepLogFormState.create("2026-07-10");
        blank.setHadDreams(Boolean.TRUE);
        blank.setDreamDetails("   ");
        assertNull(blank.toEntityForSave().getDreamDetails());

        SleepLogFormState no = SleepLogFormState.create("2026-07-10");
        no.setHadDreams(Boolean.FALSE);
        no.setDreamDetails("Hidden text");
        assertNull(no.toEntityForSave().getDreamDetails());
        assertEquals("Hidden text", no.getDreamDetails());

        SleepLogFormState unanswered = SleepLogFormState.create("2026-07-10");
        unanswered.setHadDreams(null);
        unanswered.setDreamDetails("Hidden text");
        assertNull(unanswered.toEntityForSave().getDreamDetails());
        assertEquals("Hidden text", unanswered.getDreamDetails());
    }

    @Test
    public void dreamDetailsRejectExcessiveLengthAndDirtyComparisonIncludesTemporaryText() {
        SleepLogFormState original = SleepLogFormState.create("2026-07-10");
        SleepLogFormState changed = new SleepLogFormState(original);
        changed.setDreamDetails("Hidden text");

        assertTrue(changed.isDirtyComparedTo(original));

        SleepLogFormState tooLong = SleepLogFormState.create("2026-07-10");
        tooLong.setHadDreams(Boolean.TRUE);
        tooLong.setDreamDetails(repeat("a", com.mhlotto.snoozereviews.data.SleepLogValidator.MAX_DREAM_DETAILS_CHARS + 1));
        assertThrows(IllegalArgumentException.class, tooLong::toEntityForSave);
    }

    @Test
    public void awakeningInputMapsEmptyToNullAndRejectsInvalidValues() {
        assertNull(FormInputParser.parseAwakeningCount(""));
        assertNull(FormInputParser.parseAwakeningCount("   "));
        assertEquals(Integer.valueOf(0), FormInputParser.parseAwakeningCount("0"));
        assertThrows(IllegalArgumentException.class, () -> FormInputParser.parseAwakeningCount("-1"));
        assertThrows(IllegalArgumentException.class, () -> FormInputParser.parseAwakeningCount("abc"));
    }

    @Test
    public void emptyNotesNormalizeToNull() {
        SleepLogFormState state = SleepLogFormState.create("2026-07-10");
        state.setNotes("   ");

        assertNull(state.toEntityForSave().getNotes());
    }

    @Test
    public void tagSelectionIsDeduplicated() {
        SleepLogFormState state = SleepLogFormState.create("2026-07-10");
        state.setSelectedTagKeys(Arrays.asList(SleepTagKeys.CALM, SleepTagKeys.CALM, " " + SleepTagKeys.GROGGY + " "));

        assertEquals(Arrays.asList(SleepTagKeys.CALM, SleepTagKeys.GROGGY), state.getSelectedTagKeys());
    }

    @Test
    public void unknownValidKeysArePreserved() {
        SleepLogFormState state = SleepLogFormState.create("2026-07-10");
        state.setSleepLocationKey("FUTURE_LOCATION");
        state.setSelectedTagKeys(Collections.singletonList("FUTURE_TAG"));

        assertEquals("FUTURE_LOCATION", state.toEntityForSave().getSleepLocation());
        assertEquals(Collections.singletonList("FUTURE_TAG"), state.getSelectedTagKeys());
    }

    @Test
    public void dirtyComparisonDetectsRealChangesAndIgnoresTagOrdering() {
        SleepLogFormState original = SleepLogFormState.create("2026-07-10");
        original.setSelectedTagKeys(Arrays.asList(SleepTagKeys.CALM, SleepTagKeys.GROGGY));

        SleepLogFormState reordered = new SleepLogFormState(original);
        reordered.setSelectedTagKeys(Arrays.asList(SleepTagKeys.GROGGY, SleepTagKeys.CALM));

        SleepLogFormState changed = new SleepLogFormState(original);
        changed.setSleepRating(5);

        SleepLogFormState zeroRated = new SleepLogFormState(original);
        zeroRated.setSleepRating(0);

        SleepLogFormState clearedZero = new SleepLogFormState(zeroRated);
        clearedZero.setSleepRating(null);

        SleepLogFormState sleptThrough = new SleepLogFormState(original);
        sleptThrough.setSleptThroughNight(Boolean.TRUE);

        SleepLogFormState clearedSleptThrough = new SleepLogFormState(sleptThrough);
        clearedSleptThrough.setSleptThroughNight(null);

        SleepLogFormState didNotSleepThrough = new SleepLogFormState(original);
        didNotSleepThrough.setSleptThroughNight(Boolean.FALSE);

        SleepLogFormState clearedDidNotSleepThrough = new SleepLogFormState(didNotSleepThrough);
        clearedDidNotSleepThrough.setSleptThroughNight(null);

        SleepLogFormState located = new SleepLogFormState(original);
        located.setSleepLocationKey(SleepLocationKeys.BED);

        SleepLogFormState clearedLocation = new SleepLogFormState(located);
        clearedLocation.setSleepLocationKey(null);

        assertFalse(reordered.isDirtyComparedTo(original));
        assertTrue(changed.isDirtyComparedTo(original));
        assertTrue(zeroRated.isDirtyComparedTo(original));
        assertTrue(clearedZero.isDirtyComparedTo(zeroRated));
        assertTrue(sleptThrough.isDirtyComparedTo(original));
        assertTrue(clearedSleptThrough.isDirtyComparedTo(sleptThrough));
        assertTrue(didNotSleepThrough.isDirtyComparedTo(original));
        assertTrue(clearedDidNotSleepThrough.isDirtyComparedTo(didNotSleepThrough));
        assertTrue(located.isDirtyComparedTo(original));
        assertTrue(clearedLocation.isDirtyComparedTo(located));
        assertFalse(original.equals(zeroRated));
    }

    @Test
    public void restoredStateRetainsAllValues() {
        SleepLogFormState state = SleepLogFormState.create("2026-07-10");
        state.setId(9L);
        state.setSleepLocationKey(SleepLocationKeys.COUCH);
        state.setFellAsleepMinute(10);
        state.setWokeUpMinute(20);
        state.setSleptThroughNight(null);
        state.setHadDreams(Boolean.TRUE);
        state.setDreamDetails("dream");
        state.setSleepRating(0);
        state.setRestedRating(null);
        state.setAwakeningCount(3);
        state.setNotes("note");
        state.setSelectedTagKeys(Arrays.asList(SleepTagKeys.CALM, "FUTURE_TAG"));

        SleepLogFormState restored = new SleepLogFormState(state);

        assertEquals(state, restored);
    }

    private String repeat(String value, int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            builder.append(value);
        }
        return builder.toString();
    }
}
