package com.mhlotto.snoozereviews.data.backup;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SleepBackupDocument {
    public static final String FORMAT = "snooze-reviews-backup";
    public static final int VERSION = 3;
    public static final int DATABASE_VERSION = 4;

    private final int databaseVersion;
    private final Instant exportedAt;
    private final List<SleepBackupCustomTag> customTags;
    private final List<SleepBackupRecord> records;

    public SleepBackupDocument(int databaseVersion, Instant exportedAt, List<SleepBackupRecord> records) {
        this(databaseVersion, exportedAt, Collections.emptyList(), records);
    }

    public SleepBackupDocument(int databaseVersion, Instant exportedAt, List<SleepBackupCustomTag> customTags, List<SleepBackupRecord> records) {
        this.databaseVersion = databaseVersion;
        this.exportedAt = exportedAt;
        this.customTags = Collections.unmodifiableList(new ArrayList<>(customTags));
        this.records = Collections.unmodifiableList(new ArrayList<>(records));
    }

    public int getDatabaseVersion() {
        return databaseVersion;
    }

    public Instant getExportedAt() {
        return exportedAt;
    }

    public List<SleepBackupCustomTag> getCustomTags() {
        return customTags;
    }

    public List<SleepBackupRecord> getRecords() {
        return records;
    }
}
