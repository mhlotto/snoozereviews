package com.mhlotto.snoozereviews.ui.form;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class NightDatePolicy {
    private final Clock clock;

    public NightDatePolicy() {
        this(Clock.systemDefaultZone());
    }

    public NightDatePolicy(Clock clock) {
        this.clock = clock;
    }

    public boolean isValidIsoDate(String nightDate) {
        if (nightDate == null) {
            return false;
        }
        try {
            return LocalDate.parse(nightDate).toString().equals(nightDate);
        } catch (DateTimeParseException exception) {
            return false;
        }
    }

    public boolean isTodayOrPast(String nightDate) {
        if (!isValidIsoDate(nightDate)) {
            return false;
        }
        LocalDate date = LocalDate.parse(nightDate);
        return !date.isAfter(LocalDate.now(clock));
    }
}
