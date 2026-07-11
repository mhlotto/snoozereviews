package com.mhlotto.snoozereviews.data.backup;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

public class ImportPlanSummaryCalculatorTest {
    @Test
    public void emptyBackupIntoEmptyDatabaseSummary() {
        ImportPlanSummary summary = ImportPlanSummaryCalculator.calculate(Collections.emptySet(), Collections.emptySet());

        assertEquals(0, summary.getTotalRecords());
        assertEquals(0, summary.getNewRecords());
        assertEquals(0, summary.getReplacementRecords());
        assertEquals(0, summary.getRetainedLocalRecords());
    }

    @Test
    public void countsAdditionsReplacementsAndRetainedLocalDates() {
        ImportPlanSummary summary = ImportPlanSummaryCalculator.calculate(
                set("2026-07-10", "2026-07-11", "2026-07-12"),
                set("2026-07-09", "2026-07-10", "2026-07-11")
        );

        assertEquals(3, summary.getTotalRecords());
        assertEquals(1, summary.getNewRecords());
        assertEquals(2, summary.getReplacementRecords());
        assertEquals(1, summary.getRetainedLocalRecords());
    }

    @Test
    public void matchingUsesNightDateOnly() {
        ImportPlanSummary summary = ImportPlanSummaryCalculator.calculate(
                set("2026-07-10"),
                set("2026-07-10")
        );

        assertEquals(0, summary.getNewRecords());
        assertEquals(1, summary.getReplacementRecords());
    }

    private HashSet<String> set(String... values) {
        return new HashSet<>(Arrays.asList(values));
    }
}
