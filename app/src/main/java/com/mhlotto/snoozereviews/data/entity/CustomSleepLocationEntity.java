package com.mhlotto.snoozereviews.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "custom_sleep_locations",
        indices = {
                @Index(value = "normalized_name", unique = true)
        }
)
public class CustomSleepLocationEntity {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "location_key")
    private String locationKey;

    @NonNull
    @ColumnInfo(name = "display_name")
    private String displayName;

    @NonNull
    @ColumnInfo(name = "normalized_name")
    private String normalizedName;

    @ColumnInfo(name = "is_active")
    private boolean active;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "updated_at")
    private long updatedAt;

    public CustomSleepLocationEntity(
            @NonNull String locationKey,
            @NonNull String displayName,
            @NonNull String normalizedName,
            boolean active,
            long createdAt,
            long updatedAt
    ) {
        this.locationKey = locationKey;
        this.displayName = displayName;
        this.normalizedName = normalizedName;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @NonNull
    public String getLocationKey() {
        return locationKey;
    }

    public void setLocationKey(@NonNull String locationKey) {
        this.locationKey = locationKey;
    }

    @NonNull
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(@NonNull String displayName) {
        this.displayName = displayName;
    }

    @NonNull
    public String getNormalizedName() {
        return normalizedName;
    }

    public void setNormalizedName(@NonNull String normalizedName) {
        this.normalizedName = normalizedName;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
