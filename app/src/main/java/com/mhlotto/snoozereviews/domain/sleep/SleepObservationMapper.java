package com.mhlotto.snoozereviews.domain.sleep;

import com.mhlotto.snoozereviews.data.SleepLogWithTags;
import com.mhlotto.snoozereviews.data.entity.SleepLogEntity;
import com.mhlotto.snoozereviews.data.entity.SleepLogTagEntity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class SleepObservationMapper {
    private SleepObservationMapper() {
    }

    public static SleepObservation fromSleepLogWithTags(SleepLogWithTags sleepLogWithTags) {
        if (sleepLogWithTags == null || sleepLogWithTags.getSleepLog() == null) {
            throw new IllegalArgumentException("sleepLogWithTags is required");
        }
        SleepLogEntity log = sleepLogWithTags.getSleepLog();
        return SleepObservation.builder(LocalDate.parse(log.getNightDate()))
                .fellAsleepMinute(log.getFellAsleepMinute())
                .wokeUpMinute(log.getWokeUpMinute())
                .awakeningCount(log.getAwakeningCount())
                .sleepRating(log.getSleepRating())
                .restedRating(log.getRestedRating())
                .sleptThroughNight(log.getSleptThroughNight())
                .hadDreams(log.getHadDreams())
                .sleepLocationKey(log.getSleepLocation())
                .tagKeys(tagKeySet(sleepLogWithTags))
                .build();
    }

    public static List<SleepObservation> fromSleepLogWithTagsList(List<SleepLogWithTags> logs) {
        List<SleepObservation> observations = new ArrayList<>(logs == null ? 0 : logs.size());
        if (logs == null) {
            return observations;
        }
        for (SleepLogWithTags log : logs) {
            observations.add(fromSleepLogWithTags(log));
        }
        return observations;
    }

    private static Set<String> tagKeySet(SleepLogWithTags sleepLogWithTags) {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        for (SleepLogTagEntity tag : sleepLogWithTags.getTags()) {
            keys.add(tag.getTagKey());
        }
        return keys;
    }
}
