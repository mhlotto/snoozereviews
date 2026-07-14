package com.mhlotto.snoozereviews.domain.sleep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.mhlotto.snoozereviews.data.SleepLocationKeys;
import com.mhlotto.snoozereviews.data.SleepLogWithTags;
import com.mhlotto.snoozereviews.data.SleepTagKeys;
import com.mhlotto.snoozereviews.data.entity.SleepLogEntity;
import com.mhlotto.snoozereviews.data.entity.SleepLogTagEntity;
import com.mhlotto.snoozereviews.data.location.CustomLocationKey;
import com.mhlotto.snoozereviews.data.tag.CustomSleepTagKey;

import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;

public class SleepObservationMapperTest {
    @Test
    public void mapsAllBasicFields() {
        SleepLogEntity entity = entity();
        entity.setSleepLocation(SleepLocationKeys.BED);
        entity.setFellAsleepMinute(0);
        entity.setWokeUpMinute(435);
        entity.setAwakeningCount(2);
        entity.setSleepRating(0);
        entity.setRestedRating(5);
        entity.setSleptThroughNight(Boolean.FALSE);
        entity.setHadDreams(Boolean.TRUE);
        SleepObservation observation = SleepObservationMapper.fromSleepLogWithTags(value(entity, SleepTagKeys.CALM));

        assertEquals(LocalDate.of(2026, 7, 10), observation.getNightDate());
        assertEquals(SleepLocationKeys.BED, observation.getSleepLocationKey());
        assertEquals(Integer.valueOf(0), observation.getFellAsleepMinute());
        assertEquals(Integer.valueOf(435), observation.getWokeUpMinute());
        assertEquals(Integer.valueOf(2), observation.getAwakeningCount());
        assertEquals(Integer.valueOf(0), observation.getSleepRating());
        assertEquals(Integer.valueOf(5), observation.getRestedRating());
        assertEquals(Boolean.FALSE, observation.getSleptThroughNight());
        assertEquals(Boolean.TRUE, observation.getHadDreams());
        assertTrue(observation.getTagKeys().contains(SleepTagKeys.CALM));
    }

    @Test
    public void mapsNullFields() {
        SleepObservation observation = SleepObservationMapper.fromSleepLogWithTags(value(entity()));

        assertNull(observation.getSleepLocationKey());
        assertNull(observation.getFellAsleepMinute());
        assertNull(observation.getWokeUpMinute());
        assertNull(observation.getSleepRating());
        assertNull(observation.getRestedRating());
    }

    @Test
    public void mapsCustomAndUnknownKeysWithoutMutation() {
        SleepLogEntity entity = entity();
        String customLocation = CustomLocationKey.encode("Hammock");
        String customTag = CustomSleepTagKey.encode("Weighted blanket");
        entity.setSleepLocation(customLocation);
        SleepLogWithTags value = value(entity, customTag, "FUTURE_TAG");

        SleepObservation observation = SleepObservationMapper.fromSleepLogWithTags(value);
        value.tags.add(new SleepLogTagEntity(1L, "LATER_TAG"));

        assertEquals(customLocation, observation.getSleepLocationKey());
        assertTrue(observation.getTagKeys().contains(customTag));
        assertTrue(observation.getTagKeys().contains("FUTURE_TAG"));
        assertEquals(2, observation.getTagKeys().size());
    }

    @Test
    public void listMappingPreservesOrderAndCopiesItems() {
        SleepLogWithTags first = value(entity("2026-07-11"), SleepTagKeys.GROGGY);
        SleepLogWithTags second = value(entity("2026-07-10"), SleepTagKeys.CALM);

        assertEquals(Arrays.asList(
                LocalDate.of(2026, 7, 11),
                LocalDate.of(2026, 7, 10)
        ), Arrays.asList(
                SleepObservationMapper.fromSleepLogWithTagsList(Arrays.asList(first, second)).get(0).getNightDate(),
                SleepObservationMapper.fromSleepLogWithTagsList(Arrays.asList(first, second)).get(1).getNightDate()
        ));
    }

    private SleepLogEntity entity() {
        return entity("2026-07-10");
    }

    private SleepLogEntity entity(String nightDate) {
        SleepLogEntity entity = new SleepLogEntity(nightDate);
        entity.setId(1L);
        return entity;
    }

    private SleepLogWithTags value(SleepLogEntity entity, String... tags) {
        SleepLogWithTags value = new SleepLogWithTags();
        value.sleepLog = entity;
        for (String tag : tags) {
            value.tags.add(new SleepLogTagEntity(entity.getId(), tag));
        }
        return value;
    }
}
