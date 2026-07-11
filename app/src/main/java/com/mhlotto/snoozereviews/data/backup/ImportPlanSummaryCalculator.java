package com.mhlotto.snoozereviews.data.backup;

import java.util.HashSet;
import java.util.Set;

public final class ImportPlanSummaryCalculator {
    private ImportPlanSummaryCalculator() {
    }

    public static ImportPlanSummary calculate(Set<String> importedNightDates, Set<String> localNightDates) {
        Set<String> imported = new HashSet<>(importedNightDates);
        Set<String> local = new HashSet<>(localNightDates);
        int replacements = 0;
        for (String nightDate : imported) {
            if (local.contains(nightDate)) {
                replacements++;
            }
        }
        int additions = imported.size() - replacements;
        int retained = 0;
        for (String nightDate : local) {
            if (!imported.contains(nightDate)) {
                retained++;
            }
        }
        return new ImportPlanSummary(imported.size(), additions, replacements, retained);
    }
}
