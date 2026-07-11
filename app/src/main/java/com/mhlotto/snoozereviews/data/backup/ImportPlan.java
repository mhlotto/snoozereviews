package com.mhlotto.snoozereviews.data.backup;

public class ImportPlan {
    private final SleepBackupDocument document;
    private final ImportPlanSummary summary;

    public ImportPlan(SleepBackupDocument document, ImportPlanSummary summary) {
        this.document = document;
        this.summary = summary;
    }

    public SleepBackupDocument getDocument() {
        return document;
    }

    public ImportPlanSummary getSummary() {
        return summary;
    }
}
