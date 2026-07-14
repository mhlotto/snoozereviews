package com.mhlotto.snoozereviews.domain.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.mhlotto.snoozereviews.domain.sleep.SleepObservation;

import org.junit.Test;

import java.time.LocalDate;

public class SleepMetricsCalculatorTest {
    @Test
    public void calculatesMissingSameDayAndOvernightDurationMetrics() {
        SleepDerivedMetrics missing = SleepMetricsCalculator.calculate(observation(null, null, null, null));
        assertNull(missing.getDurationMinutes());
        assertFalse(missing.hasCompleteTimeRange());

        SleepDerivedMetrics sameDay = SleepMetricsCalculator.calculate(observation(60, 540, null, null));
        assertEquals(Integer.valueOf(480), sameDay.getDurationMinutes());
        assertFalse(sameDay.crossesMidnight());

        SleepDerivedMetrics overnight = SleepMetricsCalculator.calculate(observation(1350, 435, null, null));
        assertEquals(Integer.valueOf(525), overnight.getDurationMinutes());
        assertTrue(overnight.crossesMidnight());
    }

    @Test
    public void normalizesSleepRatingsWithoutTreatingZeroAsMissing() {
        assertNull(SleepMetricsCalculator.calculate(observation(null, null, null, null)).getNormalizedSleepRating());
        assertEquals(Double.valueOf(0.0), SleepMetricsCalculator.calculate(observation(null, null, 0, null)).getNormalizedSleepRating());
        assertEquals(Double.valueOf(0.2), SleepMetricsCalculator.calculate(observation(null, null, 1, null)).getNormalizedSleepRating());
        assertEquals(Double.valueOf(0.6), SleepMetricsCalculator.calculate(observation(null, null, 3, null)).getNormalizedSleepRating());
        assertEquals(Double.valueOf(1.0), SleepMetricsCalculator.calculate(observation(null, null, 5, null)).getNormalizedSleepRating());
    }

    @Test
    public void normalizesRestedRatingsWithoutTreatingZeroAsMissing() {
        assertNull(SleepMetricsCalculator.calculate(observation(null, null, null, null)).getNormalizedRestedRating());
        assertEquals(Double.valueOf(0.0), SleepMetricsCalculator.calculate(observation(null, null, null, 0)).getNormalizedRestedRating());
        assertEquals(Double.valueOf(1.0), SleepMetricsCalculator.calculate(observation(null, null, null, 5)).getNormalizedRestedRating());
    }

    @Test
    public void inputObservationIsNotMutatedAndNoOverallScoreExists() {
        SleepObservation observation = observation(60, 540, 4, 5);
        SleepMetricsCalculator.calculate(observation);

        assertEquals(Integer.valueOf(60), observation.getFellAsleepMinute());
        assertEquals(Integer.valueOf(4), observation.getSleepRating());
    }

    private SleepObservation observation(Integer sleepMinute, Integer wakeMinute, Integer sleepRating, Integer restedRating) {
        return SleepObservation.builder(LocalDate.of(2026, 7, 10))
                .fellAsleepMinute(sleepMinute)
                .wokeUpMinute(wakeMinute)
                .sleepRating(sleepRating)
                .restedRating(restedRating)
                .build();
    }
}
