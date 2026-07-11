package com.mhlotto.snoozereviews.data.backup;

import com.mhlotto.snoozereviews.data.entity.SleepLogEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SleepBackupRecord {
    private final SleepLogEntity sleepLog;
    private final List<String> tagKeys;

    public SleepBackupRecord(SleepLogEntity sleepLog, List<String> tagKeys) {
        this.sleepLog = new SleepLogEntity(sleepLog);
        this.sleepLog.setId(0L);
        this.tagKeys = Collections.unmodifiableList(new ArrayList<>(tagKeys));
    }

    public SleepLogEntity getSleepLog() {
        return new SleepLogEntity(sleepLog);
    }

    public List<String> getTagKeys() {
        return tagKeys;
    }

    public String getNightDate() {
        return sleepLog.getNightDate();
    }
}
