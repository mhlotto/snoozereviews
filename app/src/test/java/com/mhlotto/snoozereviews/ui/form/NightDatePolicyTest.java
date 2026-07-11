package com.mhlotto.snoozereviews.ui.form;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

public class NightDatePolicyTest {
    private final NightDatePolicy policy = new NightDatePolicy(
            Clock.fixed(Instant.parse("2026-07-11T12:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    public void rejectsFutureNightDates() {
        assertFalse(policy.isTodayOrPast("2026-07-12"));
    }

    @Test
    public void acceptsTodayAndPastDates() {
        assertTrue(policy.isTodayOrPast("2026-07-11"));
        assertTrue(policy.isTodayOrPast("2026-07-10"));
    }
}
