package com.mhlotto.snoozereviews.ui.history;

import com.mhlotto.snoozereviews.R;
import com.mhlotto.snoozereviews.data.SleepLogWithTags;
import com.mhlotto.snoozereviews.data.entity.SleepLogEntity;
import com.mhlotto.snoozereviews.ui.detail.SleepDurationHelper;
import com.mhlotto.snoozereviews.ui.detail.SleepLogDetailFormatter;
import com.mhlotto.snoozereviews.ui.detail.TagDisplayItem;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SleepHistoryItemFormatter {
    private static final int TAG_PREVIEW_LIMIT = 3;

    private final SleepLogDetailFormatter.LabelResolver labels;
    private final SleepLogDetailFormatter detailFormatter;
    private final Locale locale;

    public SleepHistoryItemFormatter(
            SleepLogDetailFormatter.LabelResolver labels,
            Locale locale,
            boolean use24HourTime
    ) {
        this.labels = labels;
        this.locale = locale;
        this.detailFormatter = new SleepLogDetailFormatter(labels, locale, use24HourTime);
    }

    public List<SleepHistoryItem> formatList(List<SleepLogWithTags> logs) {
        List<SleepHistoryItem> items = new ArrayList<>(logs.size());
        for (SleepLogWithTags log : logs) {
            items.add(format(log));
        }
        return items;
    }

    public SleepHistoryItem format(SleepLogWithTags logWithTags) {
        SleepLogEntity log = logWithTags.getSleepLog();
        String displayDate = formatDate(log.getNightDate());
        String duration = formatDuration(log);
        String sleepRating = formatCompactRating(log.getSleepRating());
        String restedRating = formatCompactRating(log.getRestedRating());
        String ratingSummary = labels.getString(R.string.history_rating_summary_format, sleepRating, restedRating);
        String location = detailFormatter.formatLocation(log.getSleepLocation());
        List<TagDisplayItem> allTags = detailFormatter.formatTags(logWithTags.getTags());
        List<String> previewTags = new ArrayList<>();
        for (int i = 0; i < Math.min(TAG_PREVIEW_LIMIT, allTags.size()); i++) {
            previewTags.add(allTags.get(i).getLabel());
        }
        int remaining = Math.max(0, allTags.size() - TAG_PREVIEW_LIMIT);
        String accessibility = labels.getString(
                R.string.history_accessibility_summary_format,
                displayDate,
                duration,
                ratingSummary,
                location
        );
        return new SleepHistoryItem(
                log.getId(),
                log.getNightDate(),
                displayDate,
                duration,
                sleepRating,
                restedRating,
                ratingSummary,
                location,
                previewTags,
                remaining,
                accessibility
        );
    }

    private String formatDate(String nightDate) {
        try {
            return LocalDate.parse(nightDate)
                    .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale));
        } catch (DateTimeParseException exception) {
            return nightDate == null ? labels.getString(R.string.not_recorded) : nightDate;
        }
    }

    private String formatDuration(SleepLogEntity log) {
        Integer duration = SleepDurationHelper.calculateDurationMinutes(
                log.getFellAsleepMinute(),
                log.getWokeUpMinute()
        );
        if (duration == null) {
            return labels.getString(R.string.duration_not_available);
        }
        return SleepDurationHelper.formatDurationMinutes(duration);
    }

    private String formatCompactRating(Integer rating) {
        if (rating == null) {
            return labels.getString(R.string.not_rated);
        }
        return labels.getString(R.string.history_rating_compact_format, rating);
    }
}
