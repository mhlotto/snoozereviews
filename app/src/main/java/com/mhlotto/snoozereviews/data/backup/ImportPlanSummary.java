package com.mhlotto.snoozereviews.data.backup;

public class ImportPlanSummary {
    private final int totalRecords;
    private final int newRecords;
    private final int replacementRecords;
    private final int retainedLocalRecords;
    private final int customTagsToAdd;
    private final int customTagsToUpdate;

    public ImportPlanSummary(int totalRecords, int newRecords, int replacementRecords, int retainedLocalRecords) {
        this(totalRecords, newRecords, replacementRecords, retainedLocalRecords, 0, 0);
    }

    public ImportPlanSummary(int totalRecords, int newRecords, int replacementRecords, int retainedLocalRecords,
                             int customTagsToAdd, int customTagsToUpdate) {
        this.totalRecords = totalRecords;
        this.newRecords = newRecords;
        this.replacementRecords = replacementRecords;
        this.retainedLocalRecords = retainedLocalRecords;
        this.customTagsToAdd = customTagsToAdd;
        this.customTagsToUpdate = customTagsToUpdate;
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

    public int getCustomTagsToAdd() {
        return customTagsToAdd;
    }

    public int getCustomTagsToUpdate() {
        return customTagsToUpdate;
    }
}
