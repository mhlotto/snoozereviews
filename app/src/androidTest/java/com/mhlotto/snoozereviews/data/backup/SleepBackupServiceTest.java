package com.mhlotto.snoozereviews.data.backup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.mhlotto.snoozereviews.data.SleepLogWithTags;
import com.mhlotto.snoozereviews.data.dao.SleepLogDao;
import com.mhlotto.snoozereviews.data.db.SnoozeReviewsDatabase;
import com.mhlotto.snoozereviews.data.entity.SleepLogEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;

@RunWith(AndroidJUnit4.class)
public class SleepBackupServiceTest {
    private SnoozeReviewsDatabase database;
    private SleepLogDao dao;
    private SleepBackupService service;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, SnoozeReviewsDatabase.class).build();
        dao = database.sleepLogDao();
        service = new SleepBackupService(
                database,
                new SleepBackupCodec(),
                Runnable::run,
                Runnable::run,
                Clock.fixed(Instant.parse("2026-07-11T14:30:00Z"), ZoneOffset.UTC)
        );
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void importsOneRecordIntoEmptyDatabase() throws Exception {
        ImportPlan plan = plan(record(entity("2026-07-10"), "NOISY"));

        apply(plan);

        SleepLogWithTags result = dao.findByNightDate("2026-07-10");
        assertEquals("2026-07-10", result.getSleepLog().getNightDate());
        assertEquals(1, result.getTags().size());
        assertEquals("NOISY", result.getTags().get(0).getTagKey());
    }

    @Test
    public void replacesMatchingDatePreservesLocalIdAndReplacesFieldsTagsAndTimestamps() throws Exception {
        SleepLogEntity local = entity("2026-07-10");
        local.setSleepRating(1);
        long localId = dao.createLogWithTags(local, Collections.singletonList("OLD_TAG"));
        SleepLogEntity imported = entity("2026-07-10");
        imported.setSleepRating(5);
        imported.setSleepLocation("FUTURE_LOCATION");
        imported.setCreatedAt(100L);
        imported.setUpdatedAt(200L);

        apply(plan(record(imported, "NEW_TAG", "FUTURE_TAG")));

        SleepLogWithTags result = dao.findByNightDate("2026-07-10");
        assertEquals(localId, result.getSleepLog().getId());
        assertEquals(Integer.valueOf(5), result.getSleepLog().getSleepRating());
        assertEquals("FUTURE_LOCATION", result.getSleepLog().getSleepLocation());
        assertEquals(100L, result.getSleepLog().getCreatedAt());
        assertEquals(200L, result.getSleepLog().getUpdatedAt());
        assertEquals(2, result.getTags().size());
    }

    @Test
    public void newRecordsReceiveIdsAndLocalOnlyRecordsRemain() throws Exception {
        long localOnlyId = dao.createLogWithTags(entity("2026-07-09"), Collections.singletonList("LOCAL"));

        apply(plan(record(entity("2026-07-10"), "IMPORTED")));

        assertEquals(localOnlyId, dao.findByNightDate("2026-07-09").getSleepLog().getId());
        assertNotEquals(0L, dao.findByNightDate("2026-07-10").getSleepLog().getId());
        assertEquals(2, dao.countSleepLogs());
    }

    @Test
    public void failedTagOperationRollsBackCompleteImport() throws Exception {
        ImportPlan plan = plan(
                record(entity("2026-07-10"), "GOOD"),
                new SleepBackupRecord(entity("2026-07-11"), Arrays.asList("GOOD", null))
        );

        try {
            apply(plan);
        } catch (RuntimeException expected) {
            // Expected from tag validation inside the Room transaction.
        }

        assertNull(dao.findByNightDate("2026-07-10"));
        assertNull(dao.findByNightDate("2026-07-11"));
        assertEquals(0, dao.countSleepLogs());
    }

    @Test
    public void duplicateImportedDatesAreRejectedBeforeModification() throws Exception {
        ImportPlan plan = plan(record(entity("2026-07-10")), record(entity("2026-07-10")));

        try {
            apply(plan);
        } catch (RuntimeException expected) {
            // Expected validation failure.
        }

        assertEquals(0, dao.countSleepLogs());
    }

    @Test
    public void exportReadsAllRecordsAndEmptyExportSucceeds() throws Exception {
        ByteArrayOutputStream emptyOutput = new ByteArrayOutputStream();
        export(emptyOutput);
        String emptyJson = emptyOutput.toString("UTF-8");
        assertEquals(0, new SleepBackupCodec().parse(emptyJson).getRecords().size());

        dao.createLogWithTags(entity("2026-07-10"), Collections.singletonList("TAG"));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        SleepBackupService.ExportResult result = export(output);
        SleepBackupDocument parsed = new SleepBackupCodec().parse(output.toString("UTF-8"));

        assertEquals(1, result.getExportedLogs());
        assertEquals(1, parsed.getRecords().size());
        assertEquals("TAG", parsed.getRecords().get(0).getTagKeys().get(0));
    }

    private ImportPlan plan(SleepBackupRecord... records) throws SleepBackupValidationException {
        SleepBackupDocument document = new SleepBackupDocument(1, Instant.parse("2026-07-11T00:00:00Z"), Arrays.asList(records));
        return new ImportPlan(document, service.calculateSummary(document));
    }

    private SleepBackupRecord record(SleepLogEntity entity, String... tags) {
        return new SleepBackupRecord(entity, Arrays.asList(tags));
    }

    private SleepLogEntity entity(String nightDate) {
        SleepLogEntity entity = new SleepLogEntity(nightDate);
        entity.setCreatedAt(1L);
        entity.setUpdatedAt(2L);
        return entity;
    }

    private void apply(ImportPlan plan) {
        final RuntimeException[] error = new RuntimeException[1];
        service.applyImportPlan(plan, new SleepBackupService.Callback<>() {
            @Override
            public void onSuccess(SleepBackupService.ImportResult result) {
            }

            @Override
            public void onError(Throwable throwable) {
                error[0] = new RuntimeException(throwable);
            }
        });
        if (error[0] != null) {
            throw error[0];
        }
    }

    private SleepBackupService.ExportResult export(ByteArrayOutputStream outputStream) {
        final SleepBackupService.ExportResult[] result = new SleepBackupService.ExportResult[1];
        final RuntimeException[] error = new RuntimeException[1];
        service.exportBackup(outputStream, new SleepBackupService.Callback<>() {
            @Override
            public void onSuccess(SleepBackupService.ExportResult exportResult) {
                result[0] = exportResult;
            }

            @Override
            public void onError(Throwable throwable) {
                error[0] = new RuntimeException(throwable);
            }
        });
        if (error[0] != null) {
            throw error[0];
        }
        return result[0];
    }
}
