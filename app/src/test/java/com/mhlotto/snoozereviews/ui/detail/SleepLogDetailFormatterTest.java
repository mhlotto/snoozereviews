package com.mhlotto.snoozereviews.ui.detail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.mhlotto.snoozereviews.R;
import com.mhlotto.snoozereviews.data.SleepLocationKeys;
import com.mhlotto.snoozereviews.data.SleepLogWithTags;
import com.mhlotto.snoozereviews.data.SleepTagKeys;
import com.mhlotto.snoozereviews.data.entity.SleepLogEntity;
import com.mhlotto.snoozereviews.data.entity.SleepLogTagEntity;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class SleepLogDetailFormatterTest {
    private final SleepLogDetailFormatter formatter = new SleepLogDetailFormatter(new FakeLabels(), Locale.US, true);

    @Test
    public void knownLocationMapsToExpectedLabel() {
        assertEquals("Bed", formatter.formatLocation(SleepLocationKeys.BED));
    }

    @Test
    public void unknownLocationUsesSafeFallback() {
        assertEquals("Unknown location (FUTURE)", formatter.formatLocation("FUTURE"));
    }

    @Test
    public void knownTagsFollowCatalogOrderAndUnknownTagsComeLast() {
        SleepLogWithTags value = valueWithTags(Arrays.asList(
                SleepTagKeys.GROGGY,
                "FUTURE_B",
                SleepTagKeys.TOO_HOT,
                "FUTURE_A",
                SleepTagKeys.CALM
        ));

        List<TagDisplayItem> tags = formatter.format(value).getTags();

        assertEquals("Too hot", tags.get(0).getLabel());
        assertEquals("Calm", tags.get(1).getLabel());
        assertEquals("Groggy", tags.get(2).getLabel());
        assertEquals("Unknown tag (FUTURE_A)", tags.get(3).getLabel());
        assertEquals("Unknown tag (FUTURE_B)", tags.get(4).getLabel());
    }

    @Test
    public void nullableBooleansFormatAsYesNoAndNotAnswered() {
        SleepLogEntity entity = baseEntity();
        entity.setSleptThroughNight(Boolean.TRUE);
        entity.setHadDreams(Boolean.FALSE);
        SleepLogDetailViewState yesNo = formatter.format(value(entity));

        entity.setSleptThroughNight(null);
        SleepLogDetailViewState unanswered = formatter.format(value(entity));

        assertEquals("Yes", yesNo.getSleptThroughNight());
        assertEquals("No", yesNo.getHadDreams());
        assertEquals("Not answered", unanswered.getSleptThroughNight());
    }

    @Test
    public void ratingsFormatCorrectlyAndNullRatingsAreNotRated() {
        SleepLogEntity entity = baseEntity();
        entity.setSleepRating(4);
        entity.setRestedRating(null);

        SleepLogDetailViewState state = formatter.format(value(entity));

        assertEquals("4 of 5", state.getSleepRating());
        assertEquals("Not rated", state.getRestedRating());
    }

    @Test
    public void awakeningCountZeroRemainsVisibleAndNullIsNotRecorded() {
        SleepLogEntity entity = baseEntity();
        entity.setAwakeningCount(0);
        assertEquals("0", formatter.format(value(entity)).getAwakeningCount());

        entity.setAwakeningCount(null);
        assertEquals("Not recorded", formatter.format(value(entity)).getAwakeningCount());
    }

    @Test
    public void blankNotesUseEmptyStateAndMultilineNotesRemainUnchanged() {
        SleepLogEntity entity = baseEntity();
        entity.setNotes("   ");
        assertEquals("No notes recorded.", formatter.format(value(entity)).getNotes());

        entity.setNotes("Line one\nLine two");
        assertEquals("Line one\nLine two", formatter.format(value(entity)).getNotes());
    }

    @Test
    public void isoNightDateFormatsWithFixedLocale() {
        assertEquals("Friday, July 10, 2026", formatter.formatNightDate("2026-07-10"));
    }

    @Test
    public void minimallyPopulatedDateOnlyLogProducesCompleteState() {
        SleepLogDetailViewState state = formatter.format(value(baseEntity()));

        assertEquals("Friday, July 10, 2026", state.getFormattedNightDate());
        assertEquals("Not recorded", state.getFellAsleepTime());
        assertEquals("Not recorded", state.getWokeUpTime());
        assertEquals("Not available", state.getDuration());
        assertEquals("Not recorded", state.getLocation());
        assertEquals("Not answered", state.getSleptThroughNight());
        assertEquals("Not rated", state.getSleepRating());
        assertTrue(state.getTags().isEmpty());
        assertEquals("No notes recorded.", state.getNotes());
    }

    private SleepLogWithTags valueWithTags(List<String> tagKeys) {
        SleepLogWithTags value = value(baseEntity());
        value.tags = new ArrayList<>();
        for (String tagKey : tagKeys) {
            value.tags.add(new SleepLogTagEntity(1L, tagKey));
        }
        return value;
    }

    private SleepLogWithTags value(SleepLogEntity entity) {
        SleepLogWithTags value = new SleepLogWithTags();
        value.sleepLog = entity;
        value.tags = new ArrayList<>();
        return value;
    }

    private SleepLogEntity baseEntity() {
        SleepLogEntity entity = new SleepLogEntity("2026-07-10");
        entity.setId(1L);
        entity.setCreatedAt(1L);
        entity.setUpdatedAt(1L);
        return entity;
    }

    private static class FakeLabels implements SleepLogDetailFormatter.LabelResolver {
        @Override
        public String getString(int resId) {
            if (resId == R.string.not_recorded) return "Not recorded";
            if (resId == R.string.not_answered) return "Not answered";
            if (resId == R.string.not_rated) return "Not rated";
            if (resId == R.string.no_notes_recorded) return "No notes recorded.";
            if (resId == R.string.answer_yes) return "Yes";
            if (resId == R.string.answer_no) return "No";
            if (resId == R.string.location_bed) return "Bed";
            if (resId == R.string.tag_too_hot) return "Too hot";
            if (resId == R.string.tag_calm) return "Calm";
            if (resId == R.string.tag_groggy) return "Groggy";
            throw new IllegalArgumentException("Unhandled label: " + resId);
        }

        @Override
        public String getString(int resId, Object... args) {
            if (resId == R.string.unknown_location_detail_format) return "Unknown location (" + args[0] + ")";
            if (resId == R.string.unknown_tag_detail_format) return "Unknown tag (" + args[0] + ")";
            if (resId == R.string.rating_of_five_format) return args[0] + " of 5";
            throw new IllegalArgumentException("Unhandled formatted label: " + resId);
        }
    }
}
