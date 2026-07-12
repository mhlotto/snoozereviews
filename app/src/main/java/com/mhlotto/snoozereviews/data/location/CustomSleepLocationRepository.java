package com.mhlotto.snoozereviews.data.location;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.mhlotto.snoozereviews.data.dao.CustomSleepLocationDao;
import com.mhlotto.snoozereviews.data.db.SnoozeReviewsDatabase;
import com.mhlotto.snoozereviews.data.entity.CustomSleepLocationEntity;

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

public class CustomSleepLocationRepository {
    public interface Callback<T> {
        void onSuccess(T result);

        void onError(Throwable error);
    }

    public enum AddStatus {
        ADDED,
        DUPLICATE_ACTIVE,
        DUPLICATE_INACTIVE
    }

    public static class AddResult {
        private final AddStatus status;
        private final CustomSleepLocationEntity location;

        public AddResult(AddStatus status, CustomSleepLocationEntity location) {
            this.status = status;
            this.location = location;
        }

        public AddStatus getStatus() {
            return status;
        }

        public CustomSleepLocationEntity getLocation() {
            return location;
        }
    }

    private final CustomSleepLocationDao dao;
    private final Executor backgroundExecutor;
    private final Executor callbackExecutor;
    private final Clock clock;
    private final Set<String> fixedDuplicateNames;

    public CustomSleepLocationRepository(Context context, Collection<String> fixedDuplicateNames) {
        this(
                SnoozeReviewsDatabase.getInstance(context).customSleepLocationDao(),
                Executors.newSingleThreadExecutor(),
                new MainThreadExecutor(),
                Clock.systemUTC(),
                fixedDuplicateNames
        );
    }

    public CustomSleepLocationRepository(
            CustomSleepLocationDao dao,
            Executor backgroundExecutor,
            Executor callbackExecutor,
            Clock clock,
            Collection<String> fixedDuplicateNames
    ) {
        this.dao = dao;
        this.backgroundExecutor = backgroundExecutor;
        this.callbackExecutor = callbackExecutor;
        this.clock = clock;
        Set<String> normalized = new HashSet<>();
        if (fixedDuplicateNames != null) {
            for (String name : fixedDuplicateNames) {
                normalized.add(SleepLocationNameNormalizer.clean(name).getNormalizedName());
            }
        }
        this.fixedDuplicateNames = Collections.unmodifiableSet(normalized);
    }

    public void shutdownBackgroundExecutor() {
        if (backgroundExecutor instanceof ExecutorService) {
            ((ExecutorService) backgroundExecutor).shutdown();
        }
    }

    public void listActive(Callback<List<CustomSleepLocationEntity>> callback) {
        execute(callback, () -> immutableCopy(dao.listActive()));
    }

    public void listAll(Callback<List<CustomSleepLocationEntity>> callback) {
        execute(callback, () -> immutableCopy(dao.listAll()));
    }

    public void add(String rawName, Callback<AddResult> callback) {
        execute(callback, () -> {
            SleepLocationNameNormalizer.CleanedName cleaned = SleepLocationNameNormalizer.clean(rawName);
            if (fixedDuplicateNames.contains(cleaned.getNormalizedName())) {
                return new AddResult(AddStatus.DUPLICATE_ACTIVE, null);
            }
            CustomSleepLocationEntity existing = dao.findByNormalizedName(cleaned.getNormalizedName());
            if (existing != null) {
                return new AddResult(existing.isActive() ? AddStatus.DUPLICATE_ACTIVE : AddStatus.DUPLICATE_INACTIVE, existing);
            }
            long now = clock.millis();
            CustomSleepLocationEntity entity = new CustomSleepLocationEntity(
                    CustomLocationKey.encode(cleaned.getDisplayName()),
                    cleaned.getDisplayName(),
                    cleaned.getNormalizedName(),
                    true,
                    now,
                    now
            );
            dao.insert(entity);
            return new AddResult(AddStatus.ADDED, entity);
        });
    }

    public void remove(String locationKey, Callback<Void> callback) {
        execute(callback, () -> {
            if (!CustomLocationKey.isCustomKey(locationKey)) {
                throw new IllegalArgumentException("custom location key is required");
            }
            int rows = dao.deactivate(locationKey, clock.millis());
            if (rows != 1) {
                throw new IllegalArgumentException("custom location does not exist");
            }
            return null;
        });
    }

    public void reactivate(String locationKey, Callback<CustomSleepLocationEntity> callback) {
        execute(callback, () -> {
            CustomSleepLocationEntity existing = dao.findByKey(locationKey);
            if (existing == null) {
                throw new IllegalArgumentException("custom location does not exist");
            }
            dao.reactivate(locationKey, clock.millis());
            return dao.findByKey(locationKey);
        });
    }

    private static List<CustomSleepLocationEntity> immutableCopy(List<CustomSleepLocationEntity> rows) {
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

        @Override
        public void execute(Runnable command) {
            handler.post(command);
        }
    }
}
