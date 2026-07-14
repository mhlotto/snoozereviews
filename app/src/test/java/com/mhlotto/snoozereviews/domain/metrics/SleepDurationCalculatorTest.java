package com.mhlotto.snoozereviews.domain.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SleepDurationCalculatorTest {
    @Test
    public void missingTimesReturnNull() {
        assertNull(SleepDurationCalculator.calculateDurationMinutes(null, null));
        assertNull(SleepDurationCalculator.calculateDurationMinutes(null, 435));
        assertNull(SleepDurationCalculator.calculateDurationMinutes(1350, null));
    }

    @Test
    public void sameDayAndCrossingMidnightDurations() {
        assertEquals(Integer.valueOf(480), SleepDurationCalculator.calculateDurationMinutes(60, 540));
        assertEquals(Integer.valueOf(525), SleepDurationCalculator.calculateDurationMinutes(1350, 435));
        assertFalse(SleepDurationCalculator.crossesMidnight(60, 540));
        assertTrue(SleepDurationCalculator.crossesMidnight(1350, 435));
    }

    @Test
    public void midnightAndEqualTimesAreHandled() {
        assertEquals(Integer.valueOf(60), SleepDurationCalculator.calculateDurationMinutes(0, 60));
        assertEquals(Integer.valueOf(60), SleepDurationCalculator.calculateDurationMinutes(1380, 0));
        assertEquals(Integer.valueOf(0), SleepDurationCalculator.calculateDurationMinutes(0, 0));
    }

    @Test
    public void minimumAndMaximumValidValuesAreHandled() {
        assertEquals(Integer.valueOf(1439), SleepDurationCalculator.calculateDurationMinutes(0, 1439));
        assertEquals(Integer.valueOf(1), SleepDurationCalculator.calculateDurationMinutes(1439, 0));
    }

    @Test
    public void invalidValuesThrow() {
        assertThrows(IllegalArgumentException.class, () -> SleepDurationCalculator.calculateDurationMinutes(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> SleepDurationCalculator.calculateDurationMinutes(0, -1));
        assertThrows(IllegalArgumentException.class, () -> SleepDurationCalculator.calculateDurationMinutes(1440, 0));
        assertThrows(IllegalArgumentException.class, () -> SleepDurationCalculator.calculateDurationMinutes(0, 1440));
    }
}
