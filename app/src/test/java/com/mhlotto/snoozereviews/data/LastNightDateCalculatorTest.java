package com.mhlotto.snoozereviews.data;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class LastNightDateCalculatorTest {
    @Test
    public void calculatesNormalPreviousDay() {
        assertEquals("2026-07-10", calculateForDate(LocalDate.of(2026, 7, 11)));
    }

    @Test
    public void calculatesAcrossMonthBoundary() {
        assertEquals("2026-06-30", calculateForDate(LocalDate.of(2026, 7, 1)));
    }

    @Test
    public void calculatesAcrossYearBoundary() {
        assertEquals("2025-12-31", calculateForDate(LocalDate.of(2026, 1, 1)));
    }

    @Test
    public void calculatesLeapYearFebruaryTransition() {
        assertEquals("2024-02-29", calculateForDate(LocalDate.of(2024, 3, 1)));
    }

    @Test
    public void sameInstantCanProduceDifferentLocalDates() {
        Instant instant = Instant.parse("2026-07-11T02:00:00Z");

        String newYork = new LastNightDateCalculator(
                Clock.fixed(instant, ZoneId.of("America/New_York"))
        ).calculateLastNightDate();
        String tokyo = new LastNightDateCalculator(
                Clock.fixed(instant, ZoneId.of("Asia/Tokyo"))
        ).calculateLastNightDate();

        assertEquals("2026-07-09", newYork);
        assertEquals("2026-07-10", tokyo);
    }

    @Test
    public void resultUsesIsoFormatting() {
        assertEquals("2026-01-09", calculateForDate(LocalDate.of(2026, 1, 10)));
    }

    private String calculateForDate(LocalDate localDate) {
        Clock clock = Clock.fixed(localDate.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        return new LastNightDateCalculator(clock).calculateLastNightDate();
    }
}
