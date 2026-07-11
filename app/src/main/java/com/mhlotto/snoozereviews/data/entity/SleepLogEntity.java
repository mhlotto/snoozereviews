package com.mhlotto.snoozereviews.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "sleep_logs",
        indices = {
                @Index(value = {"night_date"}, unique = true)
        }
)
public class SleepLogEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;

    @ColumnInfo(name = "night_date")
    @NonNull
    private String nightDate;

    @ColumnInfo(name = "sleep_location")
    private String sleepLocation;

    @ColumnInfo(name = "fell_asleep_minute")
    private Integer fellAsleepMinute;

    @ColumnInfo(name = "woke_up_minute")
    private Integer wokeUpMinute;

    @ColumnInfo(name = "slept_through_night")
    private Boolean sleptThroughNight;

    @ColumnInfo(name = "had_dreams")
    private Boolean hadDreams;

    @ColumnInfo(name = "sleep_rating")
    private Integer sleepRating;

    @ColumnInfo(name = "rested_rating")
    private Integer restedRating;

    @ColumnInfo(name = "awakening_count")
    private Integer awakeningCount;

    @ColumnInfo(name = "notes")
    private String notes;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "updated_at")
    private long updatedAt;

    public SleepLogEntity() {
    }

    @Ignore
    public SleepLogEntity(String nightDate) {
        this.nightDate = nightDate;
    }

    @Ignore
    public SleepLogEntity(SleepLogEntity other) {
        this.id = other.id;
        this.nightDate = other.nightDate;
        this.sleepLocation = other.sleepLocation;
        this.fellAsleepMinute = other.fellAsleepMinute;
        this.wokeUpMinute = other.wokeUpMinute;
        this.sleptThroughNight = other.sleptThroughNight;
        this.hadDreams = other.hadDreams;
        this.sleepRating = other.sleepRating;
        this.restedRating = other.restedRating;
        this.awakeningCount = other.awakeningCount;
        this.notes = other.notes;
        this.createdAt = other.createdAt;
        this.updatedAt = other.updatedAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public String getNightDate() {
        return nightDate;
    }

    public void setNightDate(@NonNull String nightDate) {
        this.nightDate = nightDate;
    }

    public String getSleepLocation() {
        return sleepLocation;
    }

    public void setSleepLocation(String sleepLocation) {
        this.sleepLocation = sleepLocation;
    }

    public Integer getFellAsleepMinute() {
        return fellAsleepMinute;
    }

    public void setFellAsleepMinute(Integer fellAsleepMinute) {
        this.fellAsleepMinute = fellAsleepMinute;
    }

    public Integer getWokeUpMinute() {
        return wokeUpMinute;
    }

    public void setWokeUpMinute(Integer wokeUpMinute) {
        this.wokeUpMinute = wokeUpMinute;
    }

    public Boolean getSleptThroughNight() {
        return sleptThroughNight;
    }

    public void setSleptThroughNight(Boolean sleptThroughNight) {
        this.sleptThroughNight = sleptThroughNight;
    }

    public Boolean getHadDreams() {
        return hadDreams;
    }

    public void setHadDreams(Boolean hadDreams) {
        this.hadDreams = hadDreams;
    }

    public Integer getSleepRating() {
        return sleepRating;
    }

    public void setSleepRating(Integer sleepRating) {
        this.sleepRating = sleepRating;
    }

    public Integer getRestedRating() {
        return restedRating;
    }

    public void setRestedRating(Integer restedRating) {
        this.restedRating = restedRating;
    }

    public Integer getAwakeningCount() {
        return awakeningCount;
    }

    public void setAwakeningCount(Integer awakeningCount) {
        this.awakeningCount = awakeningCount;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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
