package com.mhlotto.snoozereviews.domain.metrics;

public final class SleepDerivedMetrics {
    private final Integer durationMinutes;
    private final Double normalizedSleepRating;
    private final Double normalizedRestedRating;
    private final boolean hasCompleteTimeRange;
    private final boolean crossesMidnight;
    private final boolean hasSleepRating;
    private final boolean hasRestedRating;

    public SleepDerivedMetrics(
            Integer durationMinutes,
            Double normalizedSleepRating,
            Double normalizedRestedRating,
            boolean hasCompleteTimeRange,
            boolean crossesMidnight,
            boolean hasSleepRating,
            boolean hasRestedRating
    ) {
        this.durationMinutes = durationMinutes;
        this.normalizedSleepRating = normalizedSleepRating;
        this.normalizedRestedRating = normalizedRestedRating;
        this.hasCompleteTimeRange = hasCompleteTimeRange;
        this.crossesMidnight = crossesMidnight;
        this.hasSleepRating = hasSleepRating;
        this.hasRestedRating = hasRestedRating;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public Double getNormalizedSleepRating() {
        return normalizedSleepRating;
    }

    public Double getNormalizedRestedRating() {
        return normalizedRestedRating;
    }

    public boolean hasCompleteTimeRange() {
        return hasCompleteTimeRange;
    }

    public boolean crossesMidnight() {
        return crossesMidnight;
    }

    public boolean hasSleepRating() {
        return hasSleepRating;
    }

    public boolean hasRestedRating() {
        return hasRestedRating;
    }
}
