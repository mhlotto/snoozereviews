package com.mhlotto.snoozereviews.domain.sleep;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public final class SleepObservation {
    private final LocalDate nightDate;
    private final Integer fellAsleepMinute;
    private final Integer wokeUpMinute;
    private final Integer awakeningCount;
    private final Integer sleepRating;
    private final Integer restedRating;
    private final Boolean sleptThroughNight;
    private final Boolean hadDreams;
    private final String sleepLocationKey;
    private final Set<String> tagKeys;

    private SleepObservation(Builder builder) {
        this.nightDate = Objects.requireNonNull(builder.nightDate, "nightDate is required");
        this.fellAsleepMinute = validateMinute(builder.fellAsleepMinute, "fellAsleepMinute");
        this.wokeUpMinute = validateMinute(builder.wokeUpMinute, "wokeUpMinute");
        this.awakeningCount = validateAwakeningCount(builder.awakeningCount);
        this.sleepRating = validateRating(builder.sleepRating, "sleepRating");
        this.restedRating = validateRating(builder.restedRating, "restedRating");
        this.sleptThroughNight = builder.sleptThroughNight;
        this.hadDreams = builder.hadDreams;
        this.sleepLocationKey = normalizeOptionalKey(builder.sleepLocationKey, "sleepLocationKey");
        this.tagKeys = Collections.unmodifiableSet(validateTagKeys(builder.tagKeys));
    }

    public static Builder builder(LocalDate nightDate) {
        return new Builder(nightDate);
    }

    public LocalDate getNightDate() {
        return nightDate;
    }

    public Integer getFellAsleepMinute() {
        return fellAsleepMinute;
    }

    public Integer getWokeUpMinute() {
        return wokeUpMinute;
    }

    public Integer getAwakeningCount() {
        return awakeningCount;
    }

    public Integer getSleepRating() {
        return sleepRating;
    }

    public Integer getRestedRating() {
        return restedRating;
    }

    public Boolean getSleptThroughNight() {
        return sleptThroughNight;
    }

    public Boolean getHadDreams() {
        return hadDreams;
    }

    public String getSleepLocationKey() {
        return sleepLocationKey;
    }

    public Set<String> getTagKeys() {
        return tagKeys;
    }

    private static Integer validateMinute(Integer minute, String fieldName) {
        if (minute != null && (minute < 0 || minute > 1439)) {
            throw new IllegalArgumentException(fieldName + " must be between 0 and 1439");
        }
        return minute;
    }

    private static Integer validateAwakeningCount(Integer awakeningCount) {
        if (awakeningCount != null && awakeningCount < 0) {
            throw new IllegalArgumentException("awakeningCount must be zero or greater");
        }
        return awakeningCount;
    }

    private static Integer validateRating(Integer rating, String fieldName) {
        if (rating != null && (rating < 0 || rating > 5)) {
            throw new IllegalArgumentException(fieldName + " must be between 0 and 5");
        }
        return rating;
    }

    private static String normalizeOptionalKey(String key, String fieldName) {
        if (key != null && key.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return key;
    }

    private static Set<String> validateTagKeys(Set<String> tagKeys) {
        LinkedHashSet<String> copy = new LinkedHashSet<>();
        for (String key : tagKeys) {
            if (key == null || key.trim().isEmpty()) {
                throw new IllegalArgumentException("tagKeys must not contain null or blank keys");
            }
            copy.add(key);
        }
        return copy;
    }

    public static final class Builder {
        private final LocalDate nightDate;
        private Integer fellAsleepMinute;
        private Integer wokeUpMinute;
        private Integer awakeningCount;
        private Integer sleepRating;
        private Integer restedRating;
        private Boolean sleptThroughNight;
        private Boolean hadDreams;
        private String sleepLocationKey;
        private Set<String> tagKeys = Collections.emptySet();

        private Builder(LocalDate nightDate) {
            this.nightDate = nightDate;
        }

        public Builder fellAsleepMinute(Integer fellAsleepMinute) {
            this.fellAsleepMinute = fellAsleepMinute;
            return this;
        }

        public Builder wokeUpMinute(Integer wokeUpMinute) {
            this.wokeUpMinute = wokeUpMinute;
            return this;
        }

        public Builder awakeningCount(Integer awakeningCount) {
            this.awakeningCount = awakeningCount;
            return this;
        }

        public Builder sleepRating(Integer sleepRating) {
            this.sleepRating = sleepRating;
            return this;
        }

        public Builder restedRating(Integer restedRating) {
            this.restedRating = restedRating;
            return this;
        }

        public Builder sleptThroughNight(Boolean sleptThroughNight) {
            this.sleptThroughNight = sleptThroughNight;
            return this;
        }

        public Builder hadDreams(Boolean hadDreams) {
            this.hadDreams = hadDreams;
            return this;
        }

        public Builder sleepLocationKey(String sleepLocationKey) {
            this.sleepLocationKey = sleepLocationKey;
            return this;
        }

        public Builder tagKeys(Set<String> tagKeys) {
            this.tagKeys = tagKeys == null ? Collections.emptySet() : new LinkedHashSet<>(tagKeys);
            return this;
        }

        public SleepObservation build() {
            return new SleepObservation(this);
        }
    }
}
