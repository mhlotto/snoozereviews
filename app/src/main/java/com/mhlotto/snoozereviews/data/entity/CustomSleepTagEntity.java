package com.mhlotto.snoozereviews.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "custom_sleep_tags",
        indices = {
                @Index(value = "normalized_name", unique = true)
        }
)
public class CustomSleepTagEntity {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "tag_key")
    private String tagKey;

    @NonNull
    @ColumnInfo(name = "display_name")
    private String displayName;

    @NonNull
    @ColumnInfo(name = "normalized_name")
    private String normalizedName;

    @NonNull
    @ColumnInfo(name = "category_key")
    private String categoryKey;

    @ColumnInfo(name = "is_active")
    private boolean active;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "updated_at")
    private long updatedAt;

    public CustomSleepTagEntity(@NonNull String tagKey, @NonNull String displayName, @NonNull String normalizedName,
                                @NonNull String categoryKey, boolean active, long createdAt, long updatedAt) {
        this.tagKey = tagKey;
        this.displayName = displayName;
        this.normalizedName = normalizedName;
        this.categoryKey = categoryKey;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @NonNull public String getTagKey() { return tagKey; }
    public void setTagKey(@NonNull String tagKey) { this.tagKey = tagKey; }
    @NonNull public String getDisplayName() { return displayName; }
    public void setDisplayName(@NonNull String displayName) { this.displayName = displayName; }
    @NonNull public String getNormalizedName() { return normalizedName; }
    public void setNormalizedName(@NonNull String normalizedName) { this.normalizedName = normalizedName; }
    @NonNull public String getCategoryKey() { return categoryKey; }
    public void setCategoryKey(@NonNull String categoryKey) { this.categoryKey = categoryKey; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
