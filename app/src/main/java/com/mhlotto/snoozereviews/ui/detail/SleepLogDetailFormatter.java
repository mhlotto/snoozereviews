package com.mhlotto.snoozereviews.ui.detail;

import com.mhlotto.snoozereviews.data.SleepLogWithTags;
import com.mhlotto.snoozereviews.data.entity.SleepLogEntity;
import com.mhlotto.snoozereviews.data.entity.SleepLogTagEntity;
import com.mhlotto.snoozereviews.ui.form.FormOption;
import com.mhlotto.snoozereviews.ui.form.SleepLogFormCatalog;
import com.mhlotto.snoozereviews.ui.form.TagCategory;
import com.mhlotto.snoozereviews.ui.form.TimeOfDayHelper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class SleepLogDetailFormatter {
    public interface LabelResolver {
        String getString(int resId);

        String getString(int resId, Object... args);
    }

    private final LabelResolver labels;
    private final Locale locale;
    private final boolean use24HourTime;

    public SleepLogDetailFormatter(LabelResolver labels, Locale locale, boolean use24HourTime) {
        this.labels = labels;
        this.locale = locale;
        this.use24HourTime = use24HourTime;
    }

    public SleepLogDetailViewState format(SleepLogWithTags sleepLogWithTags) {
        SleepLogEntity log = sleepLogWithTags.getSleepLog();
        return new SleepLogDetailViewState(
                log.getId(),
                log.getNightDate(),
                formatNightDate(log.getNightDate()),
                formatTime(log.getFellAsleepMinute()),
                formatTime(log.getWokeUpMinute()),
                SleepDurationHelper.formatDurationMinutes(
                        SleepDurationHelper.calculateDurationMinutes(log.getFellAsleepMinute(), log.getWokeUpMinute())),
                formatLocation(log.getSleepLocation()),
                formatBoolean(log.getSleptThroughNight()),
                formatBoolean(log.getHadDreams()),
                formatRating(log.getSleepRating()),
                formatRating(log.getRestedRating()),
                formatAwakeningCount(log.getAwakeningCount()),
                formatTags(sleepLogWithTags.getTags()),
                formatNotes(log.getNotes())
        );
    }

    public String formatNightDate(String nightDate) {
        try {
            return LocalDate.parse(nightDate)
                    .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale));
        } catch (DateTimeParseException exception) {
            return nightDate == null ? labels.getString(com.mhlotto.snoozereviews.R.string.not_recorded) : nightDate;
        }
    }

    public String formatLocation(String key) {
        if (key == null || key.trim().isEmpty()) {
            return labels.getString(com.mhlotto.snoozereviews.R.string.not_recorded);
        }
        for (FormOption option : SleepLogFormCatalog.LOCATION_OPTIONS) {
            if (key.equals(option.getKey())) {
                return labels.getString(option.getLabelResId());
            }
        }
        return labels.getString(com.mhlotto.snoozereviews.R.string.unknown_location_detail_format, key);
    }

    public List<TagDisplayItem> formatTags(List<SleepLogTagEntity> tagRows) {
        Map<String, Integer> knownOrder = new HashMap<>();
        Map<String, Integer> labelIds = new HashMap<>();
        int order = 0;
        for (TagCategory category : SleepLogFormCatalog.TAG_CATEGORIES) {
            for (FormOption option : category.getOptions()) {
                knownOrder.put(option.getKey(), order++);
                labelIds.put(option.getKey(), option.getLabelResId());
            }
        }

        Set<String> keys = new HashSet<>();
        for (SleepLogTagEntity row : tagRows) {
            keys.add(row.getTagKey());
        }

        List<String> sortedKeys = new ArrayList<>(keys);
        sortedKeys.sort(Comparator
                .comparingInt((String key) -> knownOrder.getOrDefault(key, Integer.MAX_VALUE))
                .thenComparing(key -> key));

        List<TagDisplayItem> items = new ArrayList<>();
        for (String key : sortedKeys) {
            Integer labelId = labelIds.get(key);
            String label = labelId == null
                    ? labels.getString(com.mhlotto.snoozereviews.R.string.unknown_tag_detail_format, key)
                    : labels.getString(labelId);
            items.add(new TagDisplayItem(key, label));
        }
        return Collections.unmodifiableList(items);
    }

    private String formatTime(Integer minute) {
        if (minute == null) {
            return labels.getString(com.mhlotto.snoozereviews.R.string.not_recorded);
        }
        return TimeOfDayHelper.formatMinuteOfDay(minute, locale, use24HourTime);
    }

    private String formatBoolean(Boolean value) {
        if (value == null) {
            return labels.getString(com.mhlotto.snoozereviews.R.string.not_answered);
        }
        return labels.getString(value
                ? com.mhlotto.snoozereviews.R.string.answer_yes
                : com.mhlotto.snoozereviews.R.string.answer_no);
    }

    private String formatRating(Integer rating) {
        if (rating == null) {
            return labels.getString(com.mhlotto.snoozereviews.R.string.not_rated);
        }
        return labels.getString(com.mhlotto.snoozereviews.R.string.rating_of_five_format, rating);
    }

    private String formatAwakeningCount(Integer count) {
        if (count == null) {
            return labels.getString(com.mhlotto.snoozereviews.R.string.not_recorded);
        }
        return String.valueOf(count);
    }

    private String formatNotes(String notes) {
        if (notes == null || notes.trim().isEmpty()) {
            return labels.getString(com.mhlotto.snoozereviews.R.string.no_notes_recorded);
        }
        return notes;
    }
}
