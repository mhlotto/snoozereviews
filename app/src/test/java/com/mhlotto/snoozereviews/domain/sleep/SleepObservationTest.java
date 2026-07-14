package com.mhlotto.snoozereviews.domain.sleep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

public class SleepObservationTest {
    @Test
    public void preservesNullableValues() {
        SleepObservation observation = SleepObservation.builder(LocalDate.of(2026, 7, 10)).build();

        assertEquals(LocalDate.of(2026, 7, 10), observation.getNightDate());
        assertNull(observation.getFellAsleepMinute());
        assertNull(observation.getWokeUpMinute());
        assertNull(observation.getAwakeningCount());
        assertNull(observation.getSleepRating());
        assertNull(observation.getRestedRating());
        assertNull(observation.getSleptThroughNight());
        assertNull(observation.getHadDreams());
        assertNull(observation.getSleepLocationKey());
        assertTrue(observation.getTagKeys().isEmpty());
    }

    @Test
    public void preservesRatingZeroAndMidnightMinuteZero() {
        SleepObservation observation = SleepObservation.builder(LocalDate.of(2026, 7, 10))
                .fellAsleepMinute(0)
                .wokeUpMinute(0)
                .sleepRating(0)
                .restedRating(0)
                .build();

        assertEquals(Integer.valueOf(0), observation.getFellAsleepMinute());
        assertEquals(Integer.valueOf(0), observation.getWokeUpMinute());
        assertEquals(Integer.valueOf(0), observation.getSleepRating());
        assertEquals(Integer.valueOf(0), observation.getRestedRating());
    }

    @Test
    public void defensivelyCopiesTagsAndReturnedSetIsImmutable() {
        Set<String> tags = new LinkedHashSet<>();
        tags.add("CUSTOM_TAG");
        SleepObservation observation = SleepObservation.builder(LocalDate.of(2026, 7, 10))
                .tagKeys(tags)
                .build();

        tags.add("LATER_TAG");

        assertEquals(1, observation.getTagKeys().size());
        assertTrue(observation.getTagKeys().contains("CUSTOM_TAG"));
        assertThrows(UnsupportedOperationException.class, () -> observation.getTagKeys().add("NEW_TAG"));
    }

    @Test
    public void unknownKeysArePreserved() {
        SleepObservation observation = SleepObservation.builder(LocalDate.of(2026, 7, 10))
                .sleepLocationKey("FUTURE_LOCATION")
                .tagKeys(setOf("FUTURE_TAG"))
                .build();

        assertEquals("FUTURE_LOCATION", observation.getSleepLocationKey());
        assertTrue(observation.getTagKeys().contains("FUTURE_TAG"));
    }

    @Test
    public void constructionRejectsInvalidStructuralInputs() {
        assertThrows(NullPointerException.class, () -> SleepObservation.builder(null).build());
        assertThrows(IllegalArgumentException.class, () -> SleepObservation.builder(LocalDate.of(2026, 7, 10)).fellAsleepMinute(-1).build());
        assertThrows(IllegalArgumentException.class, () -> SleepObservation.builder(LocalDate.of(2026, 7, 10)).wokeUpMinute(1440).build());
        assertThrows(IllegalArgumentException.class, () -> SleepObservation.builder(LocalDate.of(2026, 7, 10)).sleepRating(6).build());
        assertThrows(IllegalArgumentException.class, () -> SleepObservation.builder(LocalDate.of(2026, 7, 10)).restedRating(-1).build());
        assertThrows(IllegalArgumentException.class, () -> SleepObservation.builder(LocalDate.of(2026, 7, 10)).awakeningCount(-1).build());
        assertThrows(IllegalArgumentException.class, () -> SleepObservation.builder(LocalDate.of(2026, 7, 10)).sleepLocationKey("  ").build());
        assertThrows(IllegalArgumentException.class, () -> SleepObservation.builder(LocalDate.of(2026, 7, 10)).tagKeys(setOf("  ")).build());
    }

    private Set<String> setOf(String value) {
        Set<String> values = new LinkedHashSet<>();
        values.add(value);
        return values;
    }
}
