package com.mhlotto.snoozereviews.ui.form;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

public final class TimeOfDayHelper {
    private TimeOfDayHelper() {
    }

    public static int toMinuteOfDay(int hourOfDay, int minute) {
        if (hourOfDay < 0 || hourOfDay > 23) {
            throw new IllegalArgumentException("hourOfDay must be between 0 and 23");
        }
        if (minute < 0 || minute > 59) {
            throw new IllegalArgumentException("minute must be between 0 and 59");
        }
        return hourOfDay * 60 + minute;
    }

    public static int hourOfDay(int minuteOfDay) {
        validateMinuteOfDay(minuteOfDay);
        return minuteOfDay / 60;
    }

    public static int minute(int minuteOfDay) {
        validateMinuteOfDay(minuteOfDay);
        return minuteOfDay % 60;
    }

    public static String formatMinuteOfDay(int minuteOfDay, Locale locale, boolean use24Hour) {
        validateMinuteOfDay(minuteOfDay);
        DateTimeFormatter formatter = use24Hour
                ? DateTimeFormatter.ofPattern("HH:mm", locale)
                : DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale);
        return LocalTime.of(hourOfDay(minuteOfDay), minute(minuteOfDay)).format(formatter);
    }

    private static void validateMinuteOfDay(int minuteOfDay) {
        if (minuteOfDay < 0 || minuteOfDay > 1439) {
            throw new IllegalArgumentException("minuteOfDay must be between 0 and 1439");
        }
    }
}
