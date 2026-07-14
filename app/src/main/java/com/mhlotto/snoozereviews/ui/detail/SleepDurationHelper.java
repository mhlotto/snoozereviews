package com.mhlotto.snoozereviews.ui.detail;

import com.mhlotto.snoozereviews.domain.metrics.SleepDurationCalculator;

public final class SleepDurationHelper {
    public static final int MINUTES_PER_DAY = SleepDurationCalculator.MINUTES_PER_DAY;

    private SleepDurationHelper() {
    }

    public static Integer calculateDurationMinutes(Integer sleepMinute, Integer wakeMinute) {
        return SleepDurationCalculator.calculateDurationMinutes(sleepMinute, wakeMinute);
    }

    public static String formatDurationMinutes(Integer durationMinutes) {
        if (durationMinutes == null) {
            return "Not available";
        }
        int hours = durationMinutes / 60;
        int minutes = durationMinutes % 60;
        if (hours == 0) {
            return minutes + " " + (minutes == 1 ? "minute" : "minutes");
        }
        if (minutes == 0) {
            return hours + " " + (hours == 1 ? "hour" : "hours");
        }
        return hours + " " + (hours == 1 ? "hour" : "hours")
                + " " + minutes + " " + (minutes == 1 ? "minute" : "minutes");
    }

}
