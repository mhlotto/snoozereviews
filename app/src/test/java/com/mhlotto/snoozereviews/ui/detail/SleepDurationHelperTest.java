package com.mhlotto.snoozereviews.ui.detail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class SleepDurationHelperTest {
    @Test
    public void sameDayDurationCalculation() {
        assertEquals(Integer.valueOf(480), SleepDurationHelper.calculateDurationMinutes(60, 540));
    }

    @Test
    public void overnightDurationCalculation() {
        assertEquals(Integer.valueOf(525), SleepDurationHelper.calculateDurationMinutes(1350, 435));
    }

    @Test
    public void exactHourDurationFormatting() {
        assertEquals("8 hours", SleepDurationHelper.formatDurationMinutes(480));
    }

    @Test
    public void minutesOnlyDurationFormatting() {
        assertEquals("45 minutes", SleepDurationHelper.formatDurationMinutes(45));
    }

    @Test
    public void singularHourAndMinuteFormatting() {
        assertEquals("1 hour 1 minute", SleepDurationHelper.formatDurationMinutes(61));
    }

    @Test
    public void missingTimesReturnUnavailableDuration() {
        assertNull(SleepDurationHelper.calculateDurationMinutes(null, 435));
        assertNull(SleepDurationHelper.calculateDurationMinutes(1350, null));
        assertEquals("Not available", SleepDurationHelper.formatDurationMinutes(null));
    }

    @Test
    public void midnightValuesAreHandled() {
        assertEquals(Integer.valueOf(60), SleepDurationHelper.calculateDurationMinutes(0, 60));
        assertEquals(Integer.valueOf(60), SleepDurationHelper.calculateDurationMinutes(1380, 0));
    }

    @Test
    public void twentyThreeHourOvernightDurationIsHandled() {
        assertEquals(Integer.valueOf(1380), SleepDurationHelper.calculateDurationMinutes(480, 420));
        assertEquals("23 hours", SleepDurationHelper.formatDurationMinutes(1380));
    }
}
