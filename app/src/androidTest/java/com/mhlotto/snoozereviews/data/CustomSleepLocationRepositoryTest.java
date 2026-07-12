package com.mhlotto.snoozereviews.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.mhlotto.snoozereviews.data.db.SnoozeReviewsDatabase;
import com.mhlotto.snoozereviews.data.entity.CustomSleepLocationEntity;
import com.mhlotto.snoozereviews.data.location.CustomSleepLocationRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;

@RunWith(AndroidJUnit4.class)
public class CustomSleepLocationRepositoryTest {
    private SnoozeReviewsDatabase database;
    private MutableClock clock;
    private CustomSleepLocationRepository repository;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, SnoozeReviewsDatabase.class).build();
        clock = new MutableClock(1_000L);
        repository = new CustomSleepLocationRepository(
                database.customSleepLocationDao(),
                Runnable::run,
                Runnable::run,
                clock,
                Arrays.asList("BED", "Bed", "Couch")
        );
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void addAssignsTimestampsAndRejectsActiveDuplicate() {
        Result<CustomSleepLocationRepository.AddResult> first = new Result<>();
        repository.add("  Hammock  ", first);

        assertNull(first.error);
        assertEquals(CustomSleepLocationRepository.AddStatus.ADDED, first.value.getStatus());
        CustomSleepLocationEntity row = database.customSleepLocationDao().findByNormalizedName("hammock");
        assertEquals("Hammock", row.getDisplayName());
        assertEquals(1_000L, row.getCreatedAt());
        assertEquals(1_000L, row.getUpdatedAt());

        Result<CustomSleepLocationRepository.AddResult> duplicate = new Result<>();
        repository.add("HAMMOCK", duplicate);

        assertEquals(CustomSleepLocationRepository.AddStatus.DUPLICATE_ACTIVE, duplicate.value.getStatus());
        assertEquals(1, database.customSleepLocationDao().countActive());
    }

    @Test
    public void fixedLocationDuplicateIsRejected() {
        Result<CustomSleepLocationRepository.AddResult> result = new Result<>();

        repository.add("bed", result);

        assertNull(result.error);
        assertEquals(CustomSleepLocationRepository.AddStatus.DUPLICATE_ACTIVE, result.value.getStatus());
        assertEquals(0, database.customSleepLocationDao().countActive());
    }

    @Test
    public void removeSoftDeletesAndRestoreReusesExistingKey() {
        Result<CustomSleepLocationRepository.AddResult> added = new Result<>();
        repository.add("Hammock", added);
        String key = added.value.getLocation().getLocationKey();

        Result<Void> removed = new Result<>();
        clock.setMillis(2_000L);
        repository.remove(key, removed);

        assertNull(removed.error);
        assertEquals(0, database.customSleepLocationDao().countActive());
        assertEquals(2_000L, database.customSleepLocationDao().findByKey(key).getUpdatedAt());

        Result<CustomSleepLocationRepository.AddResult> duplicateInactive = new Result<>();
        repository.add("hammock", duplicateInactive);
        assertEquals(CustomSleepLocationRepository.AddStatus.DUPLICATE_INACTIVE, duplicateInactive.value.getStatus());

        Result<CustomSleepLocationEntity> restored = new Result<>();
        clock.setMillis(3_000L);
        repository.reactivate(key, restored);

        assertNotNull(restored.value);
        assertEquals(key, restored.value.getLocationKey());
        assertEquals(1, database.customSleepLocationDao().countActive());
        assertEquals(3_000L, restored.value.getUpdatedAt());
    }

    private static class Result<T> implements CustomSleepLocationRepository.Callback<T> {
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
