package com.mhlotto.snoozereviews.data;

import java.time.Clock;
import java.time.LocalDate;

public class LastNightDateCalculator {
    private final Clock clock;

    public LastNightDateCalculator() {
        this(Clock.systemDefaultZone());
    }

    public LastNightDateCalculator(Clock clock) {
        this.clock = clock;
    }

    public String calculateLastNightDate() {
        return LocalDate.now(clock).minusDays(1).toString();
    }
}
