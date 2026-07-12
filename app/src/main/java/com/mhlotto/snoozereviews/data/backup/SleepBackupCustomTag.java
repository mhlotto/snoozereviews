package com.mhlotto.snoozereviews.data.backup;

import com.mhlotto.snoozereviews.data.entity.CustomSleepTagEntity;

public class SleepBackupCustomTag {
    private final CustomSleepTagEntity entity;

    public SleepBackupCustomTag(CustomSleepTagEntity entity) {
        this.entity = new CustomSleepTagEntity(
                entity.getTagKey(),
                entity.getDisplayName(),
                entity.getNormalizedName(),
                entity.getCategoryKey(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public CustomSleepTagEntity getEntity() {
        return new CustomSleepTagEntity(
                entity.getTagKey(),
                entity.getDisplayName(),
                entity.getNormalizedName(),
                entity.getCategoryKey(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public String getTagKey() {
        return entity.getTagKey();
    }

    public String getNormalizedName() {
        return entity.getNormalizedName();
    }
}
