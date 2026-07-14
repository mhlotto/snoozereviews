package com.mhlotto.snoozereviews.domain.metrics;

import com.mhlotto.snoozereviews.domain.sleep.SleepObservation;

public final class SleepMetricsCalculator {
    private SleepMetricsCalculator() {
    }

    public static SleepDerivedMetrics calculate(SleepObservation observation) {
        if (observation == null) {
            throw new IllegalArgumentException("observation is required");
        }
        Integer duration = SleepDurationCalculator.calculateDurationMinutes(
                observation.getFellAsleepMinute(),
                observation.getWokeUpMinute()
        );
        return new SleepDerivedMetrics(
                duration,
                normalizeRating(observation.getSleepRating()),
                normalizeRating(observation.getRestedRating()),
                duration != null,
                SleepDurationCalculator.crossesMidnight(observation.getFellAsleepMinute(), observation.getWokeUpMinute()),
                observation.getSleepRating() != null,
                observation.getRestedRating() != null
        );
    }

    public static Double normalizeRating(Integer rating) {
        if (rating == null) {
            return null;
        }
        if (rating < 0 || rating > 5) {
            throw new IllegalArgumentException("rating must be between 0 and 5");
        }
        return rating / 5.0d;
    }
}
