package com.mhlotto.snoozereviews.ui.history;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.mhlotto.snoozereviews.R;
import com.mhlotto.snoozereviews.data.SleepLocationKeys;
import com.mhlotto.snoozereviews.data.SleepLogWithTags;
import com.mhlotto.snoozereviews.data.SleepTagKeys;
import com.mhlotto.snoozereviews.data.entity.SleepLogEntity;
import com.mhlotto.snoozereviews.data.entity.SleepLogTagEntity;
import com.mhlotto.snoozereviews.ui.detail.SleepLogDetailFormatter;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class SleepHistoryItemFormatterTest {
    private final SleepHistoryItemFormatter formatter = new SleepHistoryItemFormatter(
            new FakeLabels(),
            Locale.US,
            true
    );

    @Test
    public void repositoryNewestFirstResultsRemainInOrder() {
        List<SleepLogWithTags> logs = Arrays.asList(
                value(entity(3, "2026-07-12")),
                value(entity(2, "2026-07-11")),
                value(entity(1, "2026-07-10"))
        );

        List<SleepHistoryItem> items = formatter.formatList(logs);

        assertEquals("2026-07-12", items.get(0).getNightDate());
        assertEquals("2026-07-11", items.get(1).getNightDate());
        assertEquals("2026-07-10", items.get(2).getNightDate());
    }

    @Test
    public void formatsDateDurationRatingsAndLocation() {
        SleepLogEntity entity = entity(1, "2026-07-10");
        entity.setFellAsleepMinute(1350);
        entity.setWokeUpMinute(435);
        entity.setSleepRating(4);
        entity.setRestedRating(3);
        entity.setSleepLocation(SleepLocationKeys.BED);

        SleepHistoryItem item = formatter.format(value(entity));

        assertEquals("Friday, July 10, 2026", item.getDisplayDate());
        assertEquals("8 hours 45 minutes", item.getDuration());
        assertEquals("4/5", item.getSleepRatingLabel());
        assertEquals("3/5", item.getRestedRatingLabel());
        assertEquals("Sleep: 4/5  |  Rested: 3/5", item.getRatingSummary());
        assertEquals("Bed", item.getLocationLabel());
    }

    @Test
    public void sameDayAndMissingDurationFormatting() {
        SleepLogEntity sameDay = entity(1, "2026-07-10");
        sameDay.setFellAsleepMinute(60);
        sameDay.setWokeUpMinute(180);
        assertEquals("2 hours", formatter.format(value(sameDay)).getDuration());

        SleepLogEntity missing = entity(2, "2026-07-11");
        assertEquals("Duration not available", formatter.format(value(missing)).getDuration());
    }

    @Test
    public void nullRatingsAndUnknownLocationUsePolicy() {
        SleepLogEntity entity = entity(1, "2026-07-10");
        entity.setSleepLocation("FUTURE_LOCATION");

        SleepHistoryItem item = formatter.format(value(entity));

        assertEquals("Not rated", item.getSleepRatingLabel());
        assertEquals("Not rated", item.getRestedRatingLabel());
        assertEquals("Unknown location (FUTURE_LOCATION)", item.getLocationLabel());
    }

    @Test
    public void tagsFollowCatalogOrderUnknownsComeLastAndPreviewIsLimited() {
        SleepLogWithTags value = value(entity(1, "2026-07-10"));
        value.tags.add(new SleepLogTagEntity(1, "FUTURE_B"));
        value.tags.add(new SleepLogTagEntity(1, SleepTagKeys.GROGGY));
        value.tags.add(new SleepLogTagEntity(1, SleepTagKeys.TOO_HOT));
        value.tags.add(new SleepLogTagEntity(1, "FUTURE_A"));
        value.tags.add(new SleepLogTagEntity(1, SleepTagKeys.CALM));

        SleepHistoryItem item = formatter.format(value);

        assertEquals(Arrays.asList("Too hot", "Calm", "Groggy"), item.getTagLabels());
        assertEquals(2, item.getRemainingTagCount());
    }

    @Test
    public void zeroTagsProduceEmptyPreview() {
        SleepHistoryItem item = formatter.format(value(entity(1, "2026-07-10")));

        assertTrue(item.getTagLabels().isEmpty());
        assertEquals(0, item.getRemainingTagCount());
    }

    @Test
    public void rowIdentityAndCanonicalDateArePreserved() {
        SleepHistoryItem item = formatter.format(value(entity(42, "2026-07-10")));

        assertEquals(42L, item.getSleepLogId());
        assertEquals("2026-07-10", item.getNightDate());
    }

    @Test
    public void diffUtilDetectsChangedDisplayedContent() {
        SleepLogEntity oldEntity = entity(1, "2026-07-10");
        oldEntity.setSleepRating(4);
        SleepLogEntity newEntity = entity(1, "2026-07-10");
        newEntity.setSleepRating(5);

        SleepHistoryItem oldItem = formatter.format(value(oldEntity));
        SleepHistoryItem newItem = formatter.format(value(newEntity));

        assertTrue(SleepHistoryAdapter.DIFF_CALLBACK.areItemsTheSame(oldItem, newItem));
        assertFalse(SleepHistoryAdapter.DIFF_CALLBACK.areContentsTheSame(oldItem, newItem));
    }

    @Test
    public void minimallyPopulatedAndMalformedDatesProduceValidRows() {
        SleepHistoryItem minimal = formatter.format(value(entity(1, "2026-07-10")));
        assertEquals("Duration not available", minimal.getDuration());
        assertEquals("Not recorded", minimal.getLocationLabel());

        SleepHistoryItem malformed = formatter.format(value(entity(2, "bad-date")));
        assertEquals("bad-date", malformed.getDisplayDate());
    }

    @Test
    public void accessibilitySummaryContainsUsefulInformation() {
        SleepLogEntity entity = entity(1, "2026-07-10");
        entity.setFellAsleepMinute(60);
        entity.setWokeUpMinute(120);

        SleepHistoryItem item = formatter.format(value(entity));

        assertTrue(item.getAccessibilitySummary().contains("Friday, July 10, 2026"));
        assertTrue(item.getAccessibilitySummary().contains("1 hour"));
    }

    private SleepLogWithTags value(SleepLogEntity entity) {
        SleepLogWithTags value = new SleepLogWithTags();
        value.sleepLog = entity;
        value.tags = new ArrayList<>();
        return value;
    }

    private SleepLogEntity entity(long id, String nightDate) {
        SleepLogEntity entity = new SleepLogEntity(nightDate);
        entity.setId(id);
        entity.setCreatedAt(1);
        entity.setUpdatedAt(1);
        return entity;
    }

    private static class FakeLabels implements SleepLogDetailFormatter.LabelResolver {
        @Override
        public String getString(int resId) {
            if (resId == R.string.not_recorded) return "Not recorded";
            if (resId == R.string.not_rated) return "Not rated";
            if (resId == R.string.duration_not_available) return "Duration not available";
            if (resId == R.string.location_bed) return "Bed";
            if (resId == R.string.tag_too_hot) return "Too hot";
            if (resId == R.string.tag_calm) return "Calm";
            if (resId == R.string.tag_groggy) return "Groggy";
            throw new IllegalArgumentException("Unhandled label: " + resId);
        }

        @Override
        public String getString(int resId, Object... args) {
            if (resId == R.string.history_rating_compact_format) return args[0] + "/5";
            if (resId == R.string.history_rating_summary_format) return "Sleep: " + args[0] + "  |  Rested: " + args[1];
            if (resId == R.string.history_accessibility_summary_format) {
                return args[0] + ". " + args[1] + ". " + args[2] + ". " + args[3] + ".";
            }
            if (resId == R.string.unknown_location_detail_format) return "Unknown location (" + args[0] + ")";
            if (resId == R.string.unknown_tag_detail_format) return "Unknown tag (" + args[0] + ")";
            throw new IllegalArgumentException("Unhandled formatted label: " + resId);
        }
    }
}
