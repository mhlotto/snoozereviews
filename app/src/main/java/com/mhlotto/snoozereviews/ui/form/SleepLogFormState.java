package com.mhlotto.snoozereviews.ui.form;

import com.mhlotto.snoozereviews.data.SleepLogValidator;
import com.mhlotto.snoozereviews.data.SleepLogWithTags;
import com.mhlotto.snoozereviews.data.entity.SleepLogEntity;
import com.mhlotto.snoozereviews.data.entity.SleepLogTagEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.TreeSet;

public class SleepLogFormState {
    private long id;
    private String nightDate;
    private String sleepLocationKey;
    private Integer fellAsleepMinute;
    private Integer wokeUpMinute;
    private Boolean sleptThroughNight;
    private Boolean hadDreams;
    private Integer sleepRating;
    private Integer restedRating;
    private Integer awakeningCount;
    private String notes;
    private TreeSet<String> selectedTagKeys = new TreeSet<>();

    public SleepLogFormState() {
    }

    public SleepLogFormState(SleepLogFormState other) {
        this.id = other.id;
        this.nightDate = other.nightDate;
        this.sleepLocationKey = other.sleepLocationKey;
        this.fellAsleepMinute = other.fellAsleepMinute;
        this.wokeUpMinute = other.wokeUpMinute;
        this.sleptThroughNight = other.sleptThroughNight;
        this.hadDreams = other.hadDreams;
        this.sleepRating = other.sleepRating;
        this.restedRating = other.restedRating;
        this.awakeningCount = other.awakeningCount;
        this.notes = other.notes;
        this.selectedTagKeys = new TreeSet<>(other.selectedTagKeys);
    }

    public static SleepLogFormState create(String nightDate) {
        SleepLogFormState state = new SleepLogFormState();
        state.setNightDate(nightDate);
        return state;
    }

    public static SleepLogFormState fromSleepLogWithTags(SleepLogWithTags sleepLogWithTags) {
        SleepLogEntity entity = sleepLogWithTags.getSleepLog();
        SleepLogFormState state = new SleepLogFormState();
        state.id = entity.getId();
        state.nightDate = entity.getNightDate();
        state.sleepLocationKey = entity.getSleepLocation();
        state.fellAsleepMinute = entity.getFellAsleepMinute();
        state.wokeUpMinute = entity.getWokeUpMinute();
        state.sleptThroughNight = entity.getSleptThroughNight();
        state.hadDreams = entity.getHadDreams();
        state.sleepRating = entity.getSleepRating();
        state.restedRating = entity.getRestedRating();
        state.awakeningCount = entity.getAwakeningCount();
        state.notes = entity.getNotes();
        for (SleepLogTagEntity tag : sleepLogWithTags.getTags()) {
            state.selectedTagKeys.add(tag.getTagKey());
        }
        return state;
    }

    public SleepLogEntity toEntityForSave() {
        SleepLogEntity entity = new SleepLogEntity(nightDate);
        entity.setId(id);
        entity.setSleepLocation(sleepLocationKey);
        entity.setFellAsleepMinute(fellAsleepMinute);
        entity.setWokeUpMinute(wokeUpMinute);
        entity.setSleptThroughNight(sleptThroughNight);
        entity.setHadDreams(hadDreams);
        entity.setSleepRating(sleepRating);
        entity.setRestedRating(restedRating);
        entity.setAwakeningCount(awakeningCount);
        entity.setNotes(notes);
        return SleepLogValidator.validatedCopyForWrite(entity);
    }

    public boolean isDirtyComparedTo(SleepLogFormState other) {
        return !equals(other);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getNightDate() {
        return nightDate;
    }

    public void setNightDate(String nightDate) {
        this.nightDate = nightDate;
    }

    public String getSleepLocationKey() {
        return sleepLocationKey;
    }

    public void setSleepLocationKey(String sleepLocationKey) {
        this.sleepLocationKey = normalizeBlankToNull(sleepLocationKey);
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
        this.notes = normalizeNotes(notes);
    }

    public ArrayList<String> getSelectedTagKeys() {
        return new ArrayList<>(selectedTagKeys);
    }

    public void setSelectedTagKeys(Collection<String> selectedTagKeys) {
        this.selectedTagKeys = new TreeSet<>(SleepLogValidator.normalizeTagKeys(selectedTagKeys));
    }

    public void setSelectedTagKeysFromRestored(Collection<String> selectedTagKeys) {
        this.selectedTagKeys = new TreeSet<>(selectedTagKeys == null ? Collections.emptyList() : selectedTagKeys);
    }

    private static String normalizeBlankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String normalizeNotes(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof SleepLogFormState)) {
            return false;
        }
        SleepLogFormState that = (SleepLogFormState) object;
        return id == that.id
                && Objects.equals(nightDate, that.nightDate)
                && Objects.equals(sleepLocationKey, that.sleepLocationKey)
                && Objects.equals(fellAsleepMinute, that.fellAsleepMinute)
                && Objects.equals(wokeUpMinute, that.wokeUpMinute)
                && Objects.equals(sleptThroughNight, that.sleptThroughNight)
                && Objects.equals(hadDreams, that.hadDreams)
                && Objects.equals(sleepRating, that.sleepRating)
                && Objects.equals(restedRating, that.restedRating)
                && Objects.equals(awakeningCount, that.awakeningCount)
                && Objects.equals(notes, that.notes)
                && Objects.equals(selectedTagKeys, that.selectedTagKeys);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, nightDate, sleepLocationKey, fellAsleepMinute, wokeUpMinute,
                sleptThroughNight, hadDreams, sleepRating, restedRating, awakeningCount, notes, selectedTagKeys);
    }
}
