package com.mhlotto.snoozereviews.ui.form;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Locale;

public class TimeOfDayHelperTest {
    @Test
    public void convertsHourMinuteToMinutesAfterMidnight() {
        assertEquals(1350, TimeOfDayHelper.toMinuteOfDay(22, 30));
        assertEquals(435, TimeOfDayHelper.toMinuteOfDay(7, 15));
    }

    @Test
    public void formatsTimeInTwentyFourHourMode() {
        assertEquals("22:30", TimeOfDayHelper.formatMinuteOfDay(1350, Locale.US, true));
    }

    @Test
    public void formatsTimeInTwelveHourMode() {
        String formatted = TimeOfDayHelper.formatMinuteOfDay(1350, Locale.US, false)
                .replace('\u202f', ' ');
        assertEquals("10:30 PM", formatted);
    }
}
