package com.mhlotto.snoozereviews.data.tag;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.mhlotto.snoozereviews.data.dao.CustomSleepTagDao;
import com.mhlotto.snoozereviews.data.db.SnoozeReviewsDatabase;
import com.mhlotto.snoozereviews.data.entity.CustomSleepTagEntity;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CustomSleepTagRepository {
    public interface Callback<T> {
        void onSuccess(T result);
        void onError(Throwable error);
    }

    public enum AddStatus { ADDED, DUPLICATE_ACTIVE, DUPLICATE_INACTIVE }

    public static class AddResult {
        private final AddStatus status;
        private final CustomSleepTagEntity tag;

        public AddResult(AddStatus status, CustomSleepTagEntity tag) {
            this.status = status;
            this.tag = tag;
        }

        public AddStatus getStatus() { return status; }
        public CustomSleepTagEntity getTag() { return tag; }
    }

    private final CustomSleepTagDao dao;
    private final Executor backgroundExecutor;
    private final Executor callbackExecutor;
    private final Clock clock;
    private final Set<String> builtInDuplicateNames;

    public CustomSleepTagRepository(Context context, Collection<String> builtInDuplicateNames) {
        this(
                SnoozeReviewsDatabase.getInstance(context).customSleepTagDao(),
                Executors.newSingleThreadExecutor(),
                new MainThreadExecutor(),
                Clock.systemUTC(),
                builtInDuplicateNames
        );
    }

    public CustomSleepTagRepository(CustomSleepTagDao dao, Executor backgroundExecutor, Executor callbackExecutor,
                                    Clock clock, Collection<String> builtInDuplicateNames) {
        this.dao = dao;
        this.backgroundExecutor = backgroundExecutor;
        this.callbackExecutor = callbackExecutor;
        this.clock = clock;
        Set<String> normalized = new HashSet<>();
        if (builtInDuplicateNames != null) {
            for (String name : builtInDuplicateNames) {
                normalized.add(SleepTagNameNormalizer.clean(name).getNormalizedName());
            }
        }
        this.builtInDuplicateNames = Collections.unmodifiableSet(normalized);
    }

    public void shutdownBackgroundExecutor() {
        if (backgroundExecutor instanceof ExecutorService) {
            ((ExecutorService) backgroundExecutor).shutdown();
        }
    }

    public void listActive(Callback<List<CustomSleepTagEntity>> callback) {
        execute(callback, () -> immutableCopy(dao.listActive()));
    }

    public void listAll(Callback<List<CustomSleepTagEntity>> callback) {
        execute(callback, () -> immutableCopy(dao.listAll()));
    }

    public void add(String rawName, String categoryKey, Callback<AddResult> callback) {
        execute(callback, () -> {
            SleepTagNameNormalizer.CleanedName cleaned = SleepTagNameNormalizer.clean(rawName);
            validateCategory(categoryKey);
            if (builtInDuplicateNames.contains(cleaned.getNormalizedName())) {
                return new AddResult(AddStatus.DUPLICATE_ACTIVE, null);
            }
            CustomSleepTagEntity existing = dao.findByNormalizedName(cleaned.getNormalizedName());
            if (existing != null) {
                return new AddResult(existing.isActive() ? AddStatus.DUPLICATE_ACTIVE : AddStatus.DUPLICATE_INACTIVE, existing);
            }
            long now = clock.millis();
            CustomSleepTagEntity tag = new CustomSleepTagEntity(
                    CustomSleepTagKey.encode(cleaned.getDisplayName()),
                    cleaned.getDisplayName(),
                    cleaned.getNormalizedName(),
                    categoryKey,
                    true,
                    now,
                    now
            );
            dao.insert(tag);
            return new AddResult(AddStatus.ADDED, tag);
        });
    }

    public void updateCategory(String tagKey, String categoryKey, Callback<CustomSleepTagEntity> callback) {
        execute(callback, () -> {
            validateCategory(categoryKey);
            int rows = dao.updateCategory(tagKey, categoryKey, clock.millis());
            if (rows != 1) {
                throw new IllegalArgumentException("custom tag does not exist");
            }
            return dao.findByKey(tagKey);
        });
    }

    public void deactivate(String tagKey, Callback<Void> callback) {
        execute(callback, () -> {
            int rows = dao.deactivate(tagKey, clock.millis());
            if (rows != 1) {
                throw new IllegalArgumentException("custom tag does not exist");
            }
            return null;
        });
    }

    public void reactivate(String tagKey, Callback<CustomSleepTagEntity> callback) {
        execute(callback, () -> {
            int rows = dao.reactivate(tagKey, clock.millis());
            if (rows != 1) {
                throw new IllegalArgumentException("custom tag does not exist");
            }
            return dao.findByKey(tagKey);
        });
    }

    private static void validateCategory(String categoryKey) {
        if (!SleepTagCategoryKeys.isValid(categoryKey)) {
            throw new IllegalArgumentException("invalid tag category");
        }
    }

    private static List<CustomSleepTagEntity> immutableCopy(List<CustomSleepTagEntity> rows) {
        return Collections.unmodifiableList(new ArrayList<>(rows));
    }

    private <T> void execute(Callback<T> callback, Operation<T> operation) {
        backgroundExecutor.execute(() -> {
            try {
                T result = operation.run();
                if (callback != null) {
                    callbackExecutor.execute(() -> callback.onSuccess(result));
                }
            } catch (Throwable throwable) {
                if (callback != null) {
                    callbackExecutor.execute(() -> callback.onError(throwable));
                }
            }
        });
    }

    private interface Operation<T> {
        T run() throws Exception;
    }

    private static class MainThreadExecutor implements Executor {
        private final Handler handler = new Handler(Looper.getMainLooper());
        @Override public void execute(Runnable command) { handler.post(command); }
    }
}
