package com.mhlotto.snoozereviews.ui.detail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SleepLogDetailViewState {
    private final long sleepLogId;
    private final String nightDate;
    private final String formattedNightDate;
    private final String fellAsleepTime;
    private final String wokeUpTime;
    private final String duration;
    private final String location;
    private final String sleptThroughNight;
    private final String hadDreams;
    private final String sleepRating;
    private final String restedRating;
    private final String awakeningCount;
    private final List<TagDisplayItem> tags;
    private final String notes;

    public SleepLogDetailViewState(
            long sleepLogId,
            String nightDate,
            String formattedNightDate,
            String fellAsleepTime,
            String wokeUpTime,
            String duration,
            String location,
            String sleptThroughNight,
            String hadDreams,
            String sleepRating,
            String restedRating,
            String awakeningCount,
            List<TagDisplayItem> tags,
            String notes
    ) {
        this.sleepLogId = sleepLogId;
        this.nightDate = nightDate;
        this.formattedNightDate = formattedNightDate;
        this.fellAsleepTime = fellAsleepTime;
        this.wokeUpTime = wokeUpTime;
        this.duration = duration;
        this.location = location;
        this.sleptThroughNight = sleptThroughNight;
        this.hadDreams = hadDreams;
        this.sleepRating = sleepRating;
        this.restedRating = restedRating;
        this.awakeningCount = awakeningCount;
        this.tags = Collections.unmodifiableList(new ArrayList<>(tags));
        this.notes = notes;
    }

    public long getSleepLogId() {
        return sleepLogId;
    }

    public String getNightDate() {
        return nightDate;
    }

    public String getFormattedNightDate() {
        return formattedNightDate;
    }

    public String getFellAsleepTime() {
        return fellAsleepTime;
    }

    public String getWokeUpTime() {
        return wokeUpTime;
    }

    public String getDuration() {
        return duration;
    }

    public String getLocation() {
        return location;
    }

    public String getSleptThroughNight() {
        return sleptThroughNight;
    }

    public String getHadDreams() {
        return hadDreams;
    }

    public String getSleepRating() {
        return sleepRating;
    }

    public String getRestedRating() {
        return restedRating;
    }

    public String getAwakeningCount() {
        return awakeningCount;
    }

    public List<TagDisplayItem> getTags() {
        return tags;
    }

    public String getNotes() {
        return notes;
    }
}
