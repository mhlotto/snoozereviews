package com.mhlotto.snoozereviews.data.backup;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.mhlotto.snoozereviews.data.SleepLogWithTags;
import com.mhlotto.snoozereviews.data.dao.CustomSleepLocationDao;
import com.mhlotto.snoozereviews.data.dao.CustomSleepTagDao;
import com.mhlotto.snoozereviews.data.dao.SleepLogDao;
import com.mhlotto.snoozereviews.data.db.SnoozeReviewsDatabase;
import com.mhlotto.snoozereviews.data.entity.CustomSleepLocationEntity;
import com.mhlotto.snoozereviews.data.entity.CustomSleepTagEntity;
import com.mhlotto.snoozereviews.data.entity.SleepLogEntity;
import com.mhlotto.snoozereviews.data.entity.SleepLogTagEntity;
import com.mhlotto.snoozereviews.data.location.CustomLocationKey;
import com.mhlotto.snoozereviews.data.location.SleepLocationNameNormalizer;
import com.mhlotto.snoozereviews.data.tag.BuiltInSleepTagDuplicateNames;
import com.mhlotto.snoozereviews.data.tag.CustomSleepTagKey;
import com.mhlotto.snoozereviews.data.tag.SleepTagCategoryKeys;
import com.mhlotto.snoozereviews.data.tag.SleepTagNameNormalizer;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SleepBackupService {
    public interface Callback<T> {
        void onSuccess(T result);

        void onError(Throwable error);
    }

    public static class ExportResult {
        private final int exportedLogs;

        public ExportResult(int exportedLogs) {
            this.exportedLogs = exportedLogs;
        }

        public int getExportedLogs() {
            return exportedLogs;
        }
    }

    public static class ImportResult {
        private final ImportPlanSummary summary;

        public ImportResult(ImportPlanSummary summary) {
            this.summary = summary;
        }

        public ImportPlanSummary getSummary() {
            return summary;
        }
    }

    private final SnoozeReviewsDatabase database;
    private final SleepLogDao sleepLogDao;
    private final CustomSleepLocationDao customLocationDao;
    private final CustomSleepTagDao customTagDao;
    private final SleepBackupCodec codec;
    private final Executor backgroundExecutor;
    private final Executor callbackExecutor;
    private final Clock clock;

    public SleepBackupService(Context context) {
        this(
                SnoozeReviewsDatabase.getInstance(context),
                new SleepBackupCodec(),
                Executors.newSingleThreadExecutor(),
                new MainThreadExecutor(),
                Clock.systemUTC()
        );
    }

    public SleepBackupService(
            SnoozeReviewsDatabase database,
            SleepBackupCodec codec,
            Executor backgroundExecutor,
            Executor callbackExecutor,
            Clock clock
    ) {
        this.database = database;
        this.sleepLogDao = database.sleepLogDao();
        this.customLocationDao = database.customSleepLocationDao();
        this.customTagDao = database.customSleepTagDao();
        this.codec = codec;
        this.backgroundExecutor = backgroundExecutor;
        this.callbackExecutor = callbackExecutor;
        this.clock = clock;
    }

    public void shutdownBackgroundExecutor() {
        if (backgroundExecutor instanceof ExecutorService) {
            ((ExecutorService) backgroundExecutor).shutdown();
        }
    }

    public void exportBackup(OutputStream outputStream, Callback<ExportResult> callback) {
        execute(callback, () -> {
            try (OutputStream stream = outputStream) {
                SleepBackupDocument document = buildExportDocument();
                codec.write(document, stream);
                return new ExportResult(document.getRecords().size());
            }
        });
    }

    public void parseImportPlan(InputStream inputStream, Callback<ImportPlan> callback) {
        execute(callback, () -> {
            try (InputStream stream = inputStream) {
                SleepBackupDocument document = codec.parse(stream);
                return new ImportPlan(document, calculateSummary(document));
            }
        });
    }

    public void applyImportPlan(ImportPlan plan, Callback<ImportResult> callback) {
        execute(callback, () -> {
            validateDocumentUniqueness(plan.getDocument());
            database.runInTransaction(() -> {
                for (SleepBackupCustomTag customTag : plan.getDocument().getCustomTags()) {
                    applyCustomTagDefinition(customTag.getEntity());
                }
                for (SleepBackupRecord record : plan.getDocument().getRecords()) {
                    SleepLogEntity imported = record.getSleepLog();
                    ensureCustomLocationRow(imported.getSleepLocation());
                    ensureCustomTagRows(record.getTagKeys());
                    SleepLogWithTags existing = sleepLogDao.findByNightDate(imported.getNightDate());
                    if (existing == null) {
                        imported.setId(0L);
                        sleepLogDao.createLogWithTags(imported, record.getTagKeys());
                    } else {
                        imported.setId(existing.getSleepLog().getId());
                        sleepLogDao.updateLogWithTags(imported, record.getTagKeys());
                    }
                }
            });
            return new ImportResult(plan.getSummary());
        });
    }

    private void applyCustomTagDefinition(CustomSleepTagEntity imported) {
        CustomSleepTagEntity existing = customTagDao.findByKey(imported.getTagKey());
        if (existing == null) {
            customTagDao.insert(imported);
            return;
        }
        customTagDao.replaceByKey(
                imported.getTagKey(),
                imported.getDisplayName(),
                imported.getNormalizedName(),
                imported.getCategoryKey(),
                imported.isActive(),
                imported.getCreatedAt(),
                imported.getUpdatedAt()
        );
    }

    private void ensureCustomLocationRow(String sleepLocationKey) {
        if (!CustomLocationKey.isCustomKey(sleepLocationKey)) {
            return;
        }
        String displayName = CustomLocationKey.decode(sleepLocationKey);
        if (displayName == null) {
            return;
        }
        SleepLocationNameNormalizer.CleanedName cleaned = SleepLocationNameNormalizer.clean(displayName);
        CustomSleepLocationEntity existingByKey = customLocationDao.findByKey(sleepLocationKey);
        if (existingByKey != null) {
            return;
        }
        CustomSleepLocationEntity existingByName = customLocationDao.findByNormalizedName(cleaned.getNormalizedName());
        if (existingByName != null) {
            return;
        }
        long now = clock.millis();
        customLocationDao.insert(new CustomSleepLocationEntity(
                sleepLocationKey,
                cleaned.getDisplayName(),
                cleaned.getNormalizedName(),
                true,
                now,
                now
        ));
    }

    private void ensureCustomTagRows(List<String> tagKeys) {
        for (String tagKey : tagKeys) {
            ensureCustomTagRow(tagKey);
        }
    }

    private void ensureCustomTagRow(String tagKey) {
        if (!CustomSleepTagKey.isCustomKey(tagKey)) {
            return;
        }
        String displayName = CustomSleepTagKey.decode(tagKey);
        if (displayName == null) {
            return;
        }
        SleepTagNameNormalizer.CleanedName cleaned = SleepTagNameNormalizer.clean(displayName);
        if (BuiltInSleepTagDuplicateNames.NORMALIZED_NAMES.contains(cleaned.getNormalizedName())) {
            return;
        }
        CustomSleepTagEntity existingByKey = customTagDao.findByKey(tagKey);
        if (existingByKey != null) {
            return;
        }
        CustomSleepTagEntity existingByName = customTagDao.findByNormalizedName(cleaned.getNormalizedName());
        if (existingByName != null) {
            return;
        }
        long now = clock.millis();
        customTagDao.insert(new CustomSleepTagEntity(
                tagKey,
                cleaned.getDisplayName(),
                cleaned.getNormalizedName(),
                SleepTagCategoryKeys.OTHER,
                true,
                now,
                now
        ));
    }

    SleepBackupDocument buildExportDocument() {
        List<SleepLogWithTags> logs = sleepLogDao.listAllNewestFirst();
        List<SleepBackupRecord> records = new ArrayList<>(logs.size());
        for (SleepLogWithTags logWithTags : logs) {
            List<String> tagKeys = new ArrayList<>();
            for (SleepLogTagEntity tag : logWithTags.getTags()) {
                tagKeys.add(tag.getTagKey());
            }
            records.add(new SleepBackupRecord(logWithTags.getSleepLog(), tagKeys));
        }
        List<CustomSleepTagEntity> customTagEntities = customTagDao.listAll();
        List<SleepBackupCustomTag> customTags = new ArrayList<>(customTagEntities.size());
        for (CustomSleepTagEntity entity : customTagEntities) {
            customTags.add(new SleepBackupCustomTag(entity));
        }
        return new SleepBackupDocument(
                SleepBackupDocument.DATABASE_VERSION,
                clock.instant(),
                customTags,
                records
        );
    }

    public ImportPlanSummary calculateSummary(SleepBackupDocument document) throws SleepBackupValidationException {
        validateDocumentUniqueness(document);
        Set<String> importedDates = new HashSet<>();
        for (SleepBackupRecord record : document.getRecords()) {
            importedDates.add(record.getNightDate());
        }

        Set<String> localDates = new HashSet<>();
        for (SleepLogWithTags local : sleepLogDao.listAllNewestFirst()) {
            localDates.add(local.getSleepLog().getNightDate());
        }

        ImportPlanSummary logSummary = ImportPlanSummaryCalculator.calculate(importedDates, localDates);
        int customTagsToAdd = 0;
        int customTagsToUpdate = 0;
        for (SleepBackupCustomTag customTag : document.getCustomTags()) {
            if (customTagDao.findByKey(customTag.getTagKey()) == null) {
                customTagsToAdd++;
            } else {
                customTagsToUpdate++;
            }
        }
        return new ImportPlanSummary(
                logSummary.getTotalRecords(),
                logSummary.getNewRecords(),
                logSummary.getReplacementRecords(),
                logSummary.getRetainedLocalRecords(),
                customTagsToAdd,
                customTagsToUpdate
        );
    }

    private void validateDocumentUniqueness(SleepBackupDocument document) throws SleepBackupValidationException {
        Set<String> nightDates = new HashSet<>();
        for (SleepBackupRecord record : document.getRecords()) {
            if (!nightDates.add(record.getNightDate())) {
                throw new SleepBackupValidationException("Backup contains duplicate night dates.");
            }
        }
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
