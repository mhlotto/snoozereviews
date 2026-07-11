package com.mhlotto.snoozereviews.ui.addbydate;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class AddByDatePolicy {
    private final Clock clock;

    public AddByDatePolicy() {
        this(Clock.systemDefaultZone());
    }

    public AddByDatePolicy(Clock clock) {
        this.clock = clock;
    }

    public String defaultNightDate() {
        return yesterday().toString();
    }

    public boolean isAllowedNightDate(String nightDate) {
        if (!isValidIsoDate(nightDate)) {
            return false;
        }
        return !LocalDate.parse(nightDate).isAfter(yesterday());
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

    public LocalDate yesterday() {
        return LocalDate.now(clock).minusDays(1);
    }
}
