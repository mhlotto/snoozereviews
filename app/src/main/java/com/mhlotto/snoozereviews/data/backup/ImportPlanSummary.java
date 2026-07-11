package com.mhlotto.snoozereviews.data.backup;

public class ImportPlanSummary {
    private final int totalRecords;
    private final int newRecords;
    private final int replacementRecords;
    private final int retainedLocalRecords;

    public ImportPlanSummary(int totalRecords, int newRecords, int replacementRecords, int retainedLocalRecords) {
        this.totalRecords = totalRecords;
        this.newRecords = newRecords;
        this.replacementRecords = replacementRecords;
        this.retainedLocalRecords = retainedLocalRecords;
    }

    public int getTotalRecords() {
        return totalRecords;
    }

    public int getNewRecords() {
        return newRecords;
    }

    public int getReplacementRecords() {
        return replacementRecords;
    }

    public int getRetainedLocalRecords() {
        return retainedLocalRecords;
    }
}
