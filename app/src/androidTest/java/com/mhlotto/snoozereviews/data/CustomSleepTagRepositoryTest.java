package com.mhlotto.snoozereviews.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.mhlotto.snoozereviews.data.db.SnoozeReviewsDatabase;
import com.mhlotto.snoozereviews.data.entity.CustomSleepTagEntity;
import com.mhlotto.snoozereviews.data.tag.CustomSleepTagRepository;
import com.mhlotto.snoozereviews.data.tag.SleepTagCategoryKeys;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;

@RunWith(AndroidJUnit4.class)
public class CustomSleepTagRepositoryTest {
    private SnoozeReviewsDatabase database;
    private MutableClock clock;
    private CustomSleepTagRepository repository;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, SnoozeReviewsDatabase.class).build();
        clock = new MutableClock(1_000L);
        repository = new CustomSleepTagRepository(
                database.customSleepTagDao(),
                Runnable::run,
                Runnable::run,
                clock,
                Arrays.asList("TOO_HOT", "Too hot", "Restless")
        );
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void addAssignsTimestampsAndRejectsDuplicates() {
        Result<CustomSleepTagRepository.AddResult> first = new Result<>();
        repository.add("  Weighted   Blanket  ", SleepTagCategoryKeys.OTHER, first);

        assertNull(first.error);
        assertEquals(CustomSleepTagRepository.AddStatus.ADDED, first.value.getStatus());
        CustomSleepTagEntity row = database.customSleepTagDao().findByNormalizedName("weighted blanket");
        assertEquals("Weighted Blanket", row.getDisplayName());
        assertEquals(SleepTagCategoryKeys.OTHER, row.getCategoryKey());
        assertEquals(1_000L, row.getCreatedAt());
        assertEquals(1_000L, row.getUpdatedAt());

        Result<CustomSleepTagRepository.AddResult> duplicate = new Result<>();
        repository.add("weighted blanket", SleepTagCategoryKeys.ENVIRONMENT, duplicate);

        assertEquals(CustomSleepTagRepository.AddStatus.DUPLICATE_ACTIVE, duplicate.value.getStatus());
        assertEquals(1, database.customSleepTagDao().countActive());
    }

    @Test
    public void builtInDuplicateAndInvalidCategoryFailClearly() {
        Result<CustomSleepTagRepository.AddResult> duplicateBuiltIn = new Result<>();
        repository.add("too hot", SleepTagCategoryKeys.OTHER, duplicateBuiltIn);
        assertEquals(CustomSleepTagRepository.AddStatus.DUPLICATE_ACTIVE, duplicateBuiltIn.value.getStatus());

        Result<CustomSleepTagRepository.AddResult> invalidCategory = new Result<>();
        repository.add("Weighted Blanket", "BAD", invalidCategory);
        assertNotNull(invalidCategory.error);
    }

    @Test
    public void categoryEditRemoveAndRestorePreserveKey() {
        Result<CustomSleepTagRepository.AddResult> added = new Result<>();
        repository.add("Weighted Blanket", SleepTagCategoryKeys.OTHER, added);
        String key = added.value.getTag().getTagKey();

        Result<CustomSleepTagEntity> moved = new Result<>();
        clock.setMillis(2_000L);
        repository.updateCategory(key, SleepTagCategoryKeys.ENVIRONMENT, moved);
        assertEquals(key, moved.value.getTagKey());
        assertEquals(SleepTagCategoryKeys.ENVIRONMENT, moved.value.getCategoryKey());
        assertEquals(2_000L, moved.value.getUpdatedAt());

        Result<Void> removed = new Result<>();
        clock.setMillis(3_000L);
        repository.deactivate(key, removed);
        assertNull(removed.error);
        assertEquals(0, database.customSleepTagDao().countActive());

        Result<CustomSleepTagRepository.AddResult> inactiveDuplicate = new Result<>();
        repository.add("weighted blanket", SleepTagCategoryKeys.PHYSICAL, inactiveDuplicate);
        assertEquals(CustomSleepTagRepository.AddStatus.DUPLICATE_INACTIVE, inactiveDuplicate.value.getStatus());

        Result<CustomSleepTagEntity> restored = new Result<>();
        clock.setMillis(4_000L);
        repository.reactivate(key, restored);
        assertEquals(key, restored.value.getTagKey());
        assertEquals(SleepTagCategoryKeys.ENVIRONMENT, restored.value.getCategoryKey());
        assertEquals(1, database.customSleepTagDao().countActive());
    }

    private static class Result<T> implements CustomSleepTagRepository.Callback<T> {
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
