package com.mhlotto.snoozereviews.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.mhlotto.snoozereviews.data.dao.SleepLogDao;
import com.mhlotto.snoozereviews.data.db.SnoozeReviewsDatabase;
import com.mhlotto.snoozereviews.data.entity.SleepLogEntity;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SleepLogRepository {
    public interface Callback<T> {
        void onSuccess(T result);

        void onError(Throwable error);
    }

    private final SnoozeReviewsDatabase database;
    private final SleepLogDao sleepLogDao;
    private final Executor backgroundExecutor;
    private final Executor callbackExecutor;
    private final Clock clock;

    public SleepLogRepository(Context context) {
        this(
                SnoozeReviewsDatabase.getInstance(context),
                Executors.newSingleThreadExecutor(),
                new MainThreadExecutor(),
                Clock.systemUTC()
        );
    }

    public SleepLogRepository(
            SnoozeReviewsDatabase database,
            Executor backgroundExecutor,
            Executor callbackExecutor,
            Clock clock
    ) {
        this.database = database;
        this.sleepLogDao = database.sleepLogDao();
        this.backgroundExecutor = backgroundExecutor;
        this.callbackExecutor = callbackExecutor;
        this.clock = clock;
    }

    public void shutdownBackgroundExecutor() {
        if (backgroundExecutor instanceof ExecutorService) {
            ((ExecutorService) backgroundExecutor).shutdown();
        }
    }

    public void createSleepLog(
            SleepLogEntity sleepLog,
            Collection<String> tagKeys,
            Callback<Long> callback
    ) {
        execute(callback, () -> {
            SleepLogEntity prepared = SleepLogValidator.validatedCopyForWrite(sleepLog);
            long now = clock.millis();
            prepared.setId(0L);
            prepared.setCreatedAt(now);
            prepared.setUpdatedAt(now);
            List<String> normalizedTags = SleepLogValidator.normalizeTagKeys(tagKeys);
            return sleepLogDao.createLogWithTags(prepared, normalizedTags);
        });
    }

    public void updateSleepLog(
            SleepLogEntity sleepLog,
            Collection<String> tagKeys,
            Callback<Void> callback
    ) {
        execute(callback, () -> {
            SleepLogEntity prepared = SleepLogValidator.validatedCopyForWrite(sleepLog);
            if (prepared.getId() <= 0) {
                throw new IllegalArgumentException("sleep log id must be greater than zero");
            }
            List<String> normalizedTags = SleepLogValidator.normalizeTagKeys(tagKeys);
            database.runInTransaction(() -> {
                SleepLogEntity existing = sleepLogDao.findEntityById(prepared.getId());
                if (existing == null) {
                    throw new IllegalArgumentException("sleep log does not exist: " + prepared.getId());
                }
                prepared.setCreatedAt(existing.getCreatedAt());
                prepared.setUpdatedAt(clock.millis());
                sleepLogDao.updateLogWithTags(prepared, normalizedTags);
            });
            return null;
        });
    }

    public void deleteSleepLog(long id, Callback<Boolean> callback) {
        execute(callback, () -> sleepLogDao.deleteLogById(id));
    }

    public void findSleepLogById(long id, Callback<SleepLogWithTags> callback) {
        execute(callback, () -> sleepLogDao.findById(id));
    }

    public void findSleepLogByNightDate(String nightDate, Callback<SleepLogWithTags> callback) {
        execute(callback, () -> sleepLogDao.findByNightDate(SleepLogValidator.validatedCopyForWrite(new SleepLogEntity(nightDate)).getNightDate()));
    }

    public void listAllSleepLogs(Callback<List<SleepLogWithTags>> callback) {
        execute(callback, () -> Collections.unmodifiableList(new ArrayList<>(sleepLogDao.listAllNewestFirst())));
    }

    public void existsForNightDate(String nightDate, Callback<Boolean> callback) {
        execute(callback, () -> sleepLogDao.existsForNightDate(SleepLogValidator.validatedCopyForWrite(new SleepLogEntity(nightDate)).getNightDate()));
    }

    public void countSleepLogs(Callback<Integer> callback) {
        execute(callback, sleepLogDao::countSleepLogs);
    }

    private <T> void execute(Callback<T> callback, Operation<T> operation) {
        backgroundExecutor.execute(() -> {
            try {
                T result = operation.run();
                deliverSuccess(callback, result);
            } catch (Throwable throwable) {
                deliverError(callback, throwable);
            }
        });
    }

    private <T> void deliverSuccess(Callback<T> callback, T result) {
        if (callback != null) {
            callbackExecutor.execute(() -> callback.onSuccess(result));
        }
    }

    private <T> void deliverError(Callback<T> callback, Throwable throwable) {
        if (callback != null) {
            callbackExecutor.execute(() -> callback.onError(throwable));
        }
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
