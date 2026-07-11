package com.mhlotto.snoozereviews.ui.history;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class SleepHistoryItem {
    private final long sleepLogId;
    private final String nightDate;
    private final String displayDate;
    private final String duration;
    private final String sleepRatingLabel;
    private final String restedRatingLabel;
    private final String ratingSummary;
    private final String locationLabel;
    private final List<String> tagLabels;
    private final int remainingTagCount;
    private final String accessibilitySummary;

    public SleepHistoryItem(
            long sleepLogId,
            String nightDate,
            String displayDate,
            String duration,
            String sleepRatingLabel,
            String restedRatingLabel,
            String ratingSummary,
            String locationLabel,
            List<String> tagLabels,
            int remainingTagCount,
            String accessibilitySummary
    ) {
        this.sleepLogId = sleepLogId;
        this.nightDate = nightDate;
        this.displayDate = displayDate;
        this.duration = duration;
        this.sleepRatingLabel = sleepRatingLabel;
        this.restedRatingLabel = restedRatingLabel;
        this.ratingSummary = ratingSummary;
        this.locationLabel = locationLabel;
        this.tagLabels = Collections.unmodifiableList(new ArrayList<>(tagLabels));
        this.remainingTagCount = remainingTagCount;
        this.accessibilitySummary = accessibilitySummary;
    }

    public long getSleepLogId() {
        return sleepLogId;
    }

    public String getNightDate() {
        return nightDate;
    }

    public String getDisplayDate() {
        return displayDate;
    }

    public String getDuration() {
        return duration;
    }

    public String getSleepRatingLabel() {
        return sleepRatingLabel;
    }

    public String getRestedRatingLabel() {
        return restedRatingLabel;
    }

    public String getRatingSummary() {
        return ratingSummary;
    }

    public String getLocationLabel() {
        return locationLabel;
    }

    public List<String> getTagLabels() {
        return tagLabels;
    }

    public int getRemainingTagCount() {
        return remainingTagCount;
    }

    public String getAccessibilitySummary() {
        return accessibilitySummary;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SleepHistoryItem)) {
            return false;
        }
        SleepHistoryItem that = (SleepHistoryItem) other;
        return sleepLogId == that.sleepLogId
                && remainingTagCount == that.remainingTagCount
                && Objects.equals(nightDate, that.nightDate)
                && Objects.equals(displayDate, that.displayDate)
                && Objects.equals(duration, that.duration)
                && Objects.equals(sleepRatingLabel, that.sleepRatingLabel)
                && Objects.equals(restedRatingLabel, that.restedRatingLabel)
                && Objects.equals(ratingSummary, that.ratingSummary)
                && Objects.equals(locationLabel, that.locationLabel)
                && Objects.equals(tagLabels, that.tagLabels)
                && Objects.equals(accessibilitySummary, that.accessibilitySummary);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                sleepLogId,
                nightDate,
                displayDate,
                duration,
                sleepRatingLabel,
                restedRatingLabel,
                ratingSummary,
                locationLabel,
                tagLabels,
                remainingTagCount,
                accessibilitySummary
        );
    }
}
