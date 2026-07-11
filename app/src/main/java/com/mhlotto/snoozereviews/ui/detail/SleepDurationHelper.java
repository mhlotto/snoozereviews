package com.mhlotto.snoozereviews.ui.detail;

public final class SleepDurationHelper {
    public static final int MINUTES_PER_DAY = 1440;

    private SleepDurationHelper() {
    }

    public static Integer calculateDurationMinutes(Integer sleepMinute, Integer wakeMinute) {
        if (sleepMinute == null || wakeMinute == null) {
            return null;
        }
        validateMinute(sleepMinute);
        validateMinute(wakeMinute);
        if (wakeMinute >= sleepMinute) {
            return wakeMinute - sleepMinute;
        }
        return (MINUTES_PER_DAY - sleepMinute) + wakeMinute;
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

    private static void validateMinute(int minute) {
        if (minute < 0 || minute > 1439) {
            throw new IllegalArgumentException("minute must be between 0 and 1439");
        }
    }
}
