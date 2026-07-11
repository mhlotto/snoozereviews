package com.mhlotto.snoozereviews.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.mhlotto.snoozereviews.data.dao.SleepLogDao;
import com.mhlotto.snoozereviews.data.db.SnoozeReviewsDatabase;
import com.mhlotto.snoozereviews.data.entity.SleepLogEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class SleepLogDaoTest {
    private SnoozeReviewsDatabase database;
    private SleepLogDao dao;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, SnoozeReviewsDatabase.class).build();
        dao = database.sleepLogDao();
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void insertMinimalLog() {
        long id = dao.createLogWithTags(minimalLog("2026-07-10"), Collections.emptyList());

        SleepLogWithTags result = dao.findById(id);

        assertNotNull(result);
        assertEquals("2026-07-10", result.getSleepLog().getNightDate());
        assertTrue(result.getTags().isEmpty());
    }

    @Test
    public void insertAndRetrieveFullyPopulatedLog() {
        SleepLogEntity log = fullLog("2026-07-10");

        long id = dao.createLogWithTags(log, Arrays.asList(SleepTagKeys.CALM, SleepTagKeys.DEEP_SLEEP));
        SleepLogWithTags result = dao.findById(id);

        assertNotNull(result);
        assertEquals(SleepLocationKeys.BED, result.getSleepLog().getSleepLocation());
        assertEquals(Integer.valueOf(1350), result.getSleepLog().getFellAsleepMinute());
        assertEquals(Integer.valueOf(435), result.getSleepLog().getWokeUpMinute());
        assertEquals(Boolean.TRUE, result.getSleepLog().getSleptThroughNight());
        assertEquals(Boolean.FALSE, result.getSleepLog().getHadDreams());
        assertEquals(Integer.valueOf(5), result.getSleepLog().getSleepRating());
        assertEquals(Integer.valueOf(4), result.getSleepLog().getRestedRating());
        assertEquals(Integer.valueOf(0), result.getSleepLog().getAwakeningCount());
        assertEquals("Good sleep.", result.getSleepLog().getNotes());
        assertEquals(2, result.getTags().size());
    }

    @Test
    public void duplicateTagsAreStoredOnce() {
        long id = dao.createLogWithTags(
                minimalLog("2026-07-10"),
                Arrays.asList(SleepTagKeys.CALM, " " + SleepTagKeys.CALM + " ", SleepTagKeys.DEEP_SLEEP)
        );

        assertEquals(2, dao.findById(id).getTags().size());
    }

    @Test
    public void listAllNewestNightDateFirst() {
        dao.createLogWithTags(minimalLog("2026-07-09"), Collections.emptyList());
        dao.createLogWithTags(minimalLog("2026-07-11"), Collections.emptyList());
        dao.createLogWithTags(minimalLog("2026-07-10"), Collections.emptyList());

        List<SleepLogWithTags> results = dao.listAllNewestFirst();

        assertEquals("2026-07-11", results.get(0).getSleepLog().getNightDate());
        assertEquals("2026-07-10", results.get(1).getSleepLog().getNightDate());
        assertEquals("2026-07-09", results.get(2).getSleepLog().getNightDate());
    }

    @Test
    public void lookupExistsAndCountWork() {
        long id = dao.createLogWithTags(minimalLog("2026-07-10"), Collections.emptyList());

        assertEquals(id, dao.findByNightDate("2026-07-10").getSleepLog().getId());
        assertTrue(dao.existsForNightDate("2026-07-10"));
        assertFalse(dao.existsForNightDate("2026-07-11"));
        assertEquals(1, dao.countSleepLogs());
    }

    @Test
    public void updateChangesFieldsAndReplacesTags() {
        long id = dao.createLogWithTags(minimalLog("2026-07-10"), Arrays.asList(SleepTagKeys.CALM));
        SleepLogEntity update = fullLog("2026-07-10");
        update.setId(id);
        update.setCreatedAt(100L);
        update.setUpdatedAt(300L);
        update.setSleepRating(3);

        dao.updateLogWithTags(update, Arrays.asList(SleepTagKeys.GROGGY, SleepTagKeys.HEADACHE));
        SleepLogWithTags result = dao.findById(id);

        assertEquals(Integer.valueOf(3), result.getSleepLog().getSleepRating());
        assertEquals(2, result.getTags().size());
        assertEquals(new HashSet<>(Arrays.asList(SleepTagKeys.GROGGY, SleepTagKeys.HEADACHE)), tagKeys(result));
    }

    @Test
    public void deleteCascadesToTagRows() {
        long id = dao.createLogWithTags(minimalLog("2026-07-10"), Arrays.asList(SleepTagKeys.CALM));

        assertTrue(dao.deleteLogById(id));

        assertNull(dao.findById(id));
        assertEquals(0, dao.countTagsForSleepLog(id));
    }

    @Test
    public void duplicateNightDateCreateFails() {
        dao.createLogWithTags(minimalLog("2026-07-10"), Arrays.asList(SleepTagKeys.CALM));

        assertThrows(RuntimeException.class, () ->
                dao.createLogWithTags(minimalLog("2026-07-10"), Arrays.asList(SleepTagKeys.GROGGY))
        );

        assertEquals(1, dao.countSleepLogs());
        assertEquals(1, dao.findByNightDate("2026-07-10").getTags().size());
    }

    @Test
    public void updatingToDuplicateNightDateFails() {
        long firstId = dao.createLogWithTags(minimalLog("2026-07-10"), Arrays.asList(SleepTagKeys.CALM));
        long secondId = dao.createLogWithTags(minimalLog("2026-07-11"), Arrays.asList(SleepTagKeys.GROGGY));
        SleepLogEntity update = minimalLog("2026-07-10");
        update.setId(secondId);

        assertThrows(RuntimeException.class, () ->
                dao.updateLogWithTags(update, Arrays.asList(SleepTagKeys.HEADACHE))
        );

        assertEquals(firstId, dao.findByNightDate("2026-07-10").getSleepLog().getId());
        assertEquals(secondId, dao.findByNightDate("2026-07-11").getSleepLog().getId());
        assertEquals(Collections.singleton(SleepTagKeys.GROGGY), tagKeys(dao.findById(secondId)));
    }

    @Test
    public void failedAggregateUpdateRollsBackParentAndTags() {
        long id = dao.createLogWithTags(minimalLog("2026-07-10"), Arrays.asList(SleepTagKeys.CALM));
        SleepLogEntity update = minimalLog("2026-07-12");
        update.setId(id);
        update.setSleepRating(5);

        assertThrows(IllegalArgumentException.class, () ->
                dao.updateLogWithTags(update, Arrays.asList(SleepTagKeys.GROGGY, null))
        );

        SleepLogWithTags result = dao.findById(id);
        assertEquals("2026-07-10", result.getSleepLog().getNightDate());
        assertNull(result.getSleepLog().getSleepRating());
        assertEquals(1, result.getTags().size());
        assertEquals(SleepTagKeys.CALM, result.getTags().get(0).getTagKey());
    }

    @Test
    public void nullableBooleansPreserveTrueFalseAndNull() {
        SleepLogEntity trueFalse = minimalLog("2026-07-10");
        trueFalse.setSleptThroughNight(Boolean.TRUE);
        trueFalse.setHadDreams(Boolean.FALSE);
        long firstId = dao.createLogWithTags(trueFalse, Collections.emptyList());

        SleepLogEntity nulls = minimalLog("2026-07-11");
        long secondId = dao.createLogWithTags(nulls, Collections.emptyList());

        assertEquals(Boolean.TRUE, dao.findById(firstId).getSleepLog().getSleptThroughNight());
        assertEquals(Boolean.FALSE, dao.findById(firstId).getSleepLog().getHadDreams());
        assertNull(dao.findById(secondId).getSleepLog().getSleptThroughNight());
        assertNull(dao.findById(secondId).getSleepLog().getHadDreams());
    }

    @Test
    public void optionalFieldsMayRemainNull() {
        long id = dao.createLogWithTags(minimalLog("2026-07-10"), Collections.emptyList());
        SleepLogEntity result = dao.findById(id).getSleepLog();

        assertNull(result.getSleepLocation());
        assertNull(result.getFellAsleepMinute());
        assertNull(result.getWokeUpMinute());
        assertNull(result.getSleepRating());
        assertNull(result.getRestedRating());
        assertNull(result.getAwakeningCount());
        assertNull(result.getNotes());
    }

    @Test
    public void versionOneDatabaseCanBeCreated() {
        database.query(new SimpleSQLiteQuery("SELECT 1")).close();
    }

    private SleepLogEntity minimalLog(String nightDate) {
        SleepLogEntity log = new SleepLogEntity(nightDate);
        log.setCreatedAt(100L);
        log.setUpdatedAt(200L);
        return log;
    }

    private SleepLogEntity fullLog(String nightDate) {
        SleepLogEntity log = minimalLog(nightDate);
        log.setSleepLocation(SleepLocationKeys.BED);
        log.setFellAsleepMinute(1350);
        log.setWokeUpMinute(435);
        log.setSleptThroughNight(Boolean.TRUE);
        log.setHadDreams(Boolean.FALSE);
        log.setSleepRating(5);
        log.setRestedRating(4);
        log.setAwakeningCount(0);
        log.setNotes("Good sleep.");
        return log;
    }

    private Set<String> tagKeys(SleepLogWithTags sleepLogWithTags) {
        Set<String> keys = new HashSet<>();
        for (int i = 0; i < sleepLogWithTags.getTags().size(); i++) {
            keys.add(sleepLogWithTags.getTags().get(i).getTagKey());
        }
        return keys;
    }
}
