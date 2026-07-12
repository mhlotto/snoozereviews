package com.mhlotto.snoozereviews.data;

import com.mhlotto.snoozereviews.data.entity.SleepLogEntity;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

public final class SleepLogValidator {
    public static final int MAX_DREAM_DETAILS_CHARS = 10_000;

    private SleepLogValidator() {
    }

    public static SleepLogEntity validatedCopyForWrite(SleepLogEntity input) {
        if (input == null) {
            throw new IllegalArgumentException("sleep log must not be null");
        }

        SleepLogEntity copy = new SleepLogEntity(input);
        copy.setNightDate(validateNightDate(copy.getNightDate()));
        copy.setSleepLocation(normalizeOptionalNonblank(copy.getSleepLocation(), "sleepLocation"));
        copy.setFellAsleepMinute(validateMinute(copy.getFellAsleepMinute(), "fellAsleepMinute"));
        copy.setWokeUpMinute(validateMinute(copy.getWokeUpMinute(), "wokeUpMinute"));
        copy.setDreamDetails(normalizeDreamDetails(copy.getHadDreams(), copy.getDreamDetails()));
        copy.setSleepRating(validateRating(copy.getSleepRating(), "sleepRating"));
        copy.setRestedRating(validateRating(copy.getRestedRating(), "restedRating"));
        copy.setAwakeningCount(validateAwakeningCount(copy.getAwakeningCount()));
        copy.setNotes(normalizeOptionalText(copy.getNotes()));
        return copy;
    }

    public static List<String> normalizeTagKeys(Collection<String> tagKeys) {
        TreeSet<String> normalized = new TreeSet<>();
        if (tagKeys == null) {
            return new ArrayList<>();
        }

        for (String tagKey : tagKeys) {
            if (tagKey == null) {
                throw new IllegalArgumentException("tag key must not be null");
            }
            String trimmed = tagKey.trim();
            if (trimmed.isEmpty()) {
                throw new IllegalArgumentException("tag key must not be blank");
            }
            normalized.add(trimmed);
        }
        return new ArrayList<>(normalized);
    }

    private static String validateNightDate(String nightDate) {
        if (nightDate == null) {
            throw new IllegalArgumentException("nightDate must not be null");
        }
        try {
            LocalDate parsed = LocalDate.parse(nightDate);
            if (!parsed.toString().equals(nightDate)) {
                throw new IllegalArgumentException("nightDate must be a valid ISO date yyyy-MM-dd");
            }
            return nightDate;
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("nightDate must be a valid ISO date yyyy-MM-dd", exception);
        }
    }

    private static Integer validateMinute(Integer minute, String fieldName) {
        if (minute != null && (minute < 0 || minute > 1439)) {
            throw new IllegalArgumentException(fieldName + " must be between 0 and 1439");
        }
        return minute;
    }

    private static Integer validateRating(Integer rating, String fieldName) {
        if (rating != null && (rating < 1 || rating > 5)) {
            throw new IllegalArgumentException(fieldName + " must be between 1 and 5");
        }
        return rating;
    }

    private static Integer validateAwakeningCount(Integer awakeningCount) {
        if (awakeningCount != null && awakeningCount < 0) {
            throw new IllegalArgumentException("awakeningCount must be zero or greater");
        }
        return awakeningCount;
    }

    private static String normalizeOptionalNonblank(String value, String fieldName) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return trimmed;
    }

    private static String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String normalizeDreamDetails(Boolean hadDreams, String value) {
        if (!Boolean.TRUE.equals(hadDreams)) {
            return null;
        }
        String normalized = normalizeOptionalText(value);
        if (normalized != null && normalized.codePointCount(0, normalized.length()) > MAX_DREAM_DETAILS_CHARS) {
            throw new IllegalArgumentException("dreamDetails must be 10000 characters or fewer");
        }
        if (normalized != null) {
            validateDreamDetailsCharacters(normalized);
        }
        return normalized;
    }

    private static void validateDreamDetailsCharacters(String value) {
        for (int i = 0; i < value.length(); ) {
            int codePoint = value.codePointAt(i);
            if (Character.isISOControl(codePoint) && codePoint != '\n' && codePoint != '\r' && codePoint != '\t') {
                throw new IllegalArgumentException("dreamDetails must not contain control characters");
            }
            i += Character.charCount(codePoint);
        }
    }
}
