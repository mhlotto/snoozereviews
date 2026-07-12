package com.mhlotto.snoozereviews.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.mhlotto.snoozereviews.data.db.SnoozeReviewsDatabase;
import com.mhlotto.snoozereviews.data.entity.SleepLogEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class SleepLogRepositoryTest {
    private SnoozeReviewsDatabase database;
    private MutableClock clock;
    private SleepLogRepository repository;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, SnoozeReviewsDatabase.class).build();
        clock = new MutableClock(1_000L);
        Executor directExecutor = Runnable::run;
        repository = new SleepLogRepository(database, directExecutor, directExecutor, clock);
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void createAssignsTimestampsAndCallbackReceivesSuccess() {
        Result<Long> created = new Result<>();

        repository.createSleepLog(new SleepLogEntity("2026-07-10"), Arrays.asList(SleepTagKeys.CALM), created);

        assertNull(created.error);
        assertNotNull(created.value);
        SleepLogWithTags result = database.sleepLogDao().findById(created.value);
        assertEquals(1_000L, result.getSleepLog().getCreatedAt());
        assertEquals(1_000L, result.getSleepLog().getUpdatedAt());
        assertEquals(1, result.getTags().size());
    }

    @Test
    public void updatePreservesCreatedAtAndRefreshesUpdatedAt() {
        Result<Long> created = new Result<>();
        repository.createSleepLog(new SleepLogEntity("2026-07-10"), Collections.emptyList(), created);

        clock.setMillis(2_500L);
        SleepLogEntity update = new SleepLogEntity("2026-07-10");
        update.setId(created.value);
        update.setSleepRating(4);
        Result<Void> updated = new Result<>();

        repository.updateSleepLog(update, Arrays.asList(SleepTagKeys.GROGGY), updated);

        assertNull(updated.error);
        SleepLogEntity result = database.sleepLogDao().findById(created.value).getSleepLog();
        assertEquals(1_000L, result.getCreatedAt());
        assertEquals(2_500L, result.getUpdatedAt());
        assertEquals(Integer.valueOf(4), result.getSleepRating());
    }

    @Test
    public void createUpdateAndClearDreamDetails() {
        SleepLogEntity create = new SleepLogEntity("2026-07-10");
        create.setHadDreams(Boolean.TRUE);
        create.setDreamDetails("  Forest\nPath  ");
        Result<Long> created = new Result<>();

        repository.createSleepLog(create, Collections.emptyList(), created);

        assertNull(created.error);
        assertEquals("Forest\nPath", database.sleepLogDao().findById(created.value).getSleepLog().getDreamDetails());

        SleepLogEntity update = new SleepLogEntity("2026-07-10");
        update.setId(created.value);
        update.setHadDreams(Boolean.FALSE);
        update.setDreamDetails("Hidden");
        Result<Void> updated = new Result<>();
        repository.updateSleepLog(update, Collections.emptyList(), updated);

        assertNull(updated.error);
        SleepLogEntity cleared = database.sleepLogDao().findById(created.value).getSleepLog();
        assertEquals(Boolean.FALSE, cleared.getHadDreams());
        assertNull(cleared.getDreamDetails());
    }

    @Test
    public void callbacksReceiveValidationAndDatabaseFailures() {
        Result<Long> invalid = new Result<>();
        repository.createSleepLog(new SleepLogEntity("bad-date"), Collections.emptyList(), invalid);
        assertTrue(invalid.error instanceof IllegalArgumentException);

        Result<Long> first = new Result<>();
        repository.createSleepLog(new SleepLogEntity("2026-07-10"), Collections.emptyList(), first);

        Result<Long> duplicate = new Result<>();
        repository.createSleepLog(new SleepLogEntity("2026-07-10"), Collections.emptyList(), duplicate);
        assertNotNull(duplicate.error);
        assertEquals(1, database.sleepLogDao().countSleepLogs());
    }

    @Test
    public void readOperationsReturnExpectedResults() {
        Result<Long> created = new Result<>();
        repository.createSleepLog(new SleepLogEntity("2026-07-10"), Collections.emptyList(), created);

        Result<SleepLogWithTags> byId = new Result<>();
        repository.findSleepLogById(created.value, byId);
        assertEquals("2026-07-10", byId.value.getSleepLog().getNightDate());

        Result<SleepLogWithTags> byNightDate = new Result<>();
        repository.findSleepLogByNightDate("2026-07-10", byNightDate);
        assertEquals(created.value.longValue(), byNightDate.value.getSleepLog().getId());

        Result<Boolean> exists = new Result<>();
        repository.existsForNightDate("2026-07-10", exists);
        assertTrue(exists.value);

        Result<Integer> count = new Result<>();
        repository.countSleepLogs(count);
        assertEquals(Integer.valueOf(1), count.value);

        Result<Boolean> deleted = new Result<>();
        repository.deleteSleepLog(created.value, deleted);
        assertTrue(deleted.value);

        Result<Boolean> deletedAgain = new Result<>();
        repository.deleteSleepLog(created.value, deletedAgain);
        assertFalse(deletedAgain.value);
    }

    @Test
    public void operationsDoNotRequireAllowMainThreadQueries() {
        AtomicReference<Throwable> error = new AtomicReference<>();
        Thread thread = new Thread(() -> {
            Result<Integer> result = new Result<>();
            repository.countSleepLogs(result);
            error.set(result.error);
        });
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
        assertNull(error.get());
    }

    private static class Result<T> implements SleepLogRepository.Callback<T> {
        T value;
        Throwable error;

        @Override
        public void onSuccess(T result) {
            this.value = result;
        }

        @Override
        public void onError(Throwable error) {
            this.error = error;
        }
    }

    private static class MutableClock extends Clock {
        private long millis;

        MutableClock(long millis) {
            this.millis = millis;
        }

        void setMillis(long millis) {
            this.millis = millis;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(millis);
        }

        @Override
        public long millis() {
            return millis;
        }
    }
}
