package com.mhlotto.snoozereviews.data;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.mhlotto.snoozereviews.data.entity.SleepLogEntity;
import com.mhlotto.snoozereviews.data.entity.SleepLogTagEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SleepLogWithTags {
    @Embedded
    public SleepLogEntity sleepLog;

    @Relation(parentColumn = "id", entityColumn = "sleep_log_id")
    public List<SleepLogTagEntity> tags = new ArrayList<>();

    public SleepLogEntity getSleepLog() {
        return sleepLog;
    }

    public List<SleepLogTagEntity> getTags() {
        if (tags == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(tags);
    }
}
