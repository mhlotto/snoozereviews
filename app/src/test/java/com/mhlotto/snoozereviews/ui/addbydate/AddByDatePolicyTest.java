package com.mhlotto.snoozereviews.ui.addbydate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

public class AddByDatePolicyTest {
    @Test
    public void yesterdayAndOlderDatesAreAccepted() {
        AddByDatePolicy policy = policy("2026-07-11T12:00:00Z", "America/New_York");

        assertTrue(policy.isAllowedNightDate("2026-07-10"));
        assertTrue(policy.isAllowedNightDate("2026-01-01"));
    }

    @Test
    public void todayAndFutureDatesAreRejected() {
        AddByDatePolicy policy = policy("2026-07-11T12:00:00Z", "America/New_York");

        assertFalse(policy.isAllowedNightDate("2026-07-11"));
        assertFalse(policy.isAllowedNightDate("2026-07-12"));
    }

    @Test
    public void januaryFirstProducesDecemberThirtyFirstAsYesterday() {
        AddByDatePolicy policy = policy("2026-01-01T12:00:00Z", "America/New_York");

        assertEquals("2025-12-31", policy.defaultNightDate());
    }

    @Test
    public void leapYearMarchFirstProducesFebruaryTwentyNinthAsYesterday() {
        AddByDatePolicy policy = policy("2024-03-01T12:00:00Z", "America/New_York");

        assertEquals("2024-02-29", policy.defaultNightDate());
    }

    @Test
    public void sameInstantCanProduceDifferentLocalYesterdays() {
        String instant = "2026-07-11T01:00:00Z";

        assertEquals("2026-07-09", policy(instant, "America/New_York").defaultNightDate());
        assertEquals("2026-07-10", policy(instant, "Asia/Tokyo").defaultNightDate());
    }

    @Test
    public void canonicalIsoFormattingIsPreservedAndMalformedDatesAreRejected() {
        AddByDatePolicy policy = policy("2026-07-11T12:00:00Z", "America/New_York");

        assertEquals("2026-07-10", policy.defaultNightDate());
        assertFalse(policy.isAllowedNightDate("2026-7-10"));
        assertFalse(policy.isAllowedNightDate("not-a-date"));
        assertFalse(policy.isAllowedNightDate(null));
    }

    @Test
    public void restoredSelectedDateCanBeValidated() {
        AddByDatePolicy policy = policy("2026-07-11T12:00:00Z", "America/New_York");

        assertTrue(policy.isAllowedNightDate("2026-07-04"));
    }

    private AddByDatePolicy policy(String instant, String zoneId) {
        return new AddByDatePolicy(Clock.fixed(Instant.parse(instant), ZoneId.of(zoneId)));
    }
}
