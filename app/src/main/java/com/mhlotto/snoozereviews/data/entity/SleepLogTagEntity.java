package com.mhlotto.snoozereviews.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;

@Entity(
        tableName = "sleep_log_tags",
        primaryKeys = {"sleep_log_id", "tag_key"},
        foreignKeys = {
                @ForeignKey(
                        entity = SleepLogEntity.class,
                        parentColumns = "id",
                        childColumns = "sleep_log_id",
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {
                @Index(value = {"sleep_log_id"})
        }
)
public class SleepLogTagEntity {
    @ColumnInfo(name = "sleep_log_id")
    private long sleepLogId;

    @ColumnInfo(name = "tag_key")
    @NonNull
    private String tagKey;

    public SleepLogTagEntity() {
    }

    @Ignore
    public SleepLogTagEntity(long sleepLogId, String tagKey) {
        this.sleepLogId = sleepLogId;
        this.tagKey = tagKey;
    }

    public long getSleepLogId() {
        return sleepLogId;
    }

    public void setSleepLogId(long sleepLogId) {
        this.sleepLogId = sleepLogId;
    }

    @NonNull
    public String getTagKey() {
        return tagKey;
    }

    public void setTagKey(@NonNull String tagKey) {
        this.tagKey = tagKey;
    }
}
