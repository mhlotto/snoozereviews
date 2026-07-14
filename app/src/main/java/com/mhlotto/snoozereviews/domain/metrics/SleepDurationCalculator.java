package com.mhlotto.snoozereviews.domain.metrics;

public final class SleepDurationCalculator {
    public static final int MINUTES_PER_DAY = 1440;

    private SleepDurationCalculator() {
    }

    public static Integer calculateDurationMinutes(Integer fellAsleepMinute, Integer wokeUpMinute) {
        if (fellAsleepMinute == null || wokeUpMinute == null) {
            return null;
        }
        validateMinute(fellAsleepMinute, "fellAsleepMinute");
        validateMinute(wokeUpMinute, "wokeUpMinute");
        if (wokeUpMinute >= fellAsleepMinute) {
            return wokeUpMinute - fellAsleepMinute;
        }
        return (MINUTES_PER_DAY - fellAsleepMinute) + wokeUpMinute;
    }

    public static boolean crossesMidnight(Integer fellAsleepMinute, Integer wokeUpMinute) {
        if (fellAsleepMinute == null || wokeUpMinute == null) {
            return false;
        }
        validateMinute(fellAsleepMinute, "fellAsleepMinute");
        validateMinute(wokeUpMinute, "wokeUpMinute");
        return wokeUpMinute < fellAsleepMinute;
    }

    private static void validateMinute(int minute, String fieldName) {
        if (minute < 0 || minute > 1439) {
            throw new IllegalArgumentException(fieldName + " must be between 0 and 1439");
        }
    }
}
