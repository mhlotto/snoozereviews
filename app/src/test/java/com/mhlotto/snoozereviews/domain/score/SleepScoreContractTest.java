package com.mhlotto.snoozereviews.domain.score;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SleepScoreContractTest {
    @Test
    public void validScoredResult() {
        SleepScoreComponent component = SleepScoreComponent.included("duration", 0.8, 0.5, 40.0);
        SleepScoreResult result = SleepScoreResult.scored(
                "experimental-v1",
                80,
                Collections.singletonList(component),
                Collections.singletonList("limited-data")
        );

        assertEquals("experimental-v1", result.getAlgorithmId());
        assertEquals(SleepScoreResult.Status.SCORED, result.getStatus());
        assertEquals(Integer.valueOf(80), result.getScore());
        assertEquals(component, result.getComponents().get(0));
        assertEquals("limited-data", result.getLimitationCodes().get(0));
    }

    @Test
    public void scoreRangeIsValidated() {
        assertThrows(IllegalArgumentException.class, () -> SleepScoreResult.scored("experimental-v1", -1, null, null));
        assertThrows(IllegalArgumentException.class, () -> SleepScoreResult.scored("experimental-v1", 101, null, null));
    }

    @Test
    public void insufficientDataResultHasNoScore() {
        SleepScoreResult result = SleepScoreResult.insufficientData("experimental-v1", null, null);

        assertEquals(SleepScoreResult.Status.INSUFFICIENT_DATA, result.getStatus());
        assertNull(result.getScore());
    }

    @Test
    public void algorithmIdIsRequired() {
        assertThrows(IllegalArgumentException.class, () -> SleepScoreResult.scored(null, 50, null, null));
        assertThrows(IllegalArgumentException.class, () -> SleepScoreResult.insufficientData(" ", null, null));
    }

    @Test
    public void componentAndLimitationCollectionsAreImmutable() {
        List<SleepScoreComponent> components = new ArrayList<>();
        components.add(SleepScoreComponent.omitted("duration", "missing-time"));
        List<String> limitations = new ArrayList<>();
        limitations.add("limited-data");

        SleepScoreResult result = SleepScoreResult.insufficientData("experimental-v1", components, limitations);
        components.clear();
        limitations.clear();

        assertEquals(1, result.getComponents().size());
        assertEquals(1, result.getLimitationCodes().size());
        assertThrows(UnsupportedOperationException.class, () -> result.getComponents().add(SleepScoreComponent.omitted("rating", "missing")));
        assertThrows(UnsupportedOperationException.class, () -> result.getLimitationCodes().add("other"));
    }

    @Test
    public void includedAndOmittedComponentsCanBeRepresented() {
        SleepScoreComponent included = SleepScoreComponent.included("duration", 0.8, 0.5, 40.0);
        SleepScoreComponent omitted = SleepScoreComponent.omitted("sleep-rating", "missing-rating");

        assertTrue(included.isIncluded());
        assertEquals(Double.valueOf(0.8), included.getNormalizedValue());
        assertFalse(omitted.isIncluded());
        assertNull(omitted.getContribution());
        assertEquals("missing-rating", omitted.getReasonCode());
    }

    @Test
    public void componentKeyIsRequired() {
        assertThrows(IllegalArgumentException.class, () -> SleepScoreComponent.omitted(null, "missing"));
        assertThrows(IllegalArgumentException.class, () -> SleepScoreComponent.included(" ", 1.0, 1.0, 1.0));
    }
}
