package com.mhlotto.snoozereviews.data.backup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.mhlotto.snoozereviews.data.SleepLocationKeys;
import com.mhlotto.snoozereviews.data.entity.SleepLogEntity;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;

public class SleepBackupCodecTest {
    private final SleepBackupCodec codec = new SleepBackupCodec();

    @Test
    public void serializesEmptyBackupWithNewline() {
        String json = codec.serialize(document());

        assertTrue(json.contains("\"format\": \"snooze-reviews-backup\""));
        assertTrue(json.contains("\"version\": 1"));
        assertTrue(json.contains("\"logs\": []"));
        assertTrue(json.endsWith("\n"));
    }

    @Test
    public void serializesMinimalAndFullLogsWithNullsAndPrivateFields() {
        SleepLogEntity minimal = entity("2026-07-10");
        SleepLogEntity full = entity("2026-07-09");
        full.setSleepLocation(SleepLocationKeys.BED);
        full.setFellAsleepMinute(1350);
        full.setWokeUpMinute(435);
        full.setSleptThroughNight(false);
        full.setHadDreams(true);
        full.setSleepRating(4);
        full.setRestedRating(3);
        full.setAwakeningCount(2);
        full.setNotes("Line one\nLine two");

        String json = codec.serialize(document(
                new SleepBackupRecord(minimal, Collections.emptyList()),
                new SleepBackupRecord(full, Arrays.asList("GROGGY", "NOISY"))
        ));

        assertTrue(json.indexOf("\"nightDate\": \"2026-07-09\"") < json.indexOf("\"nightDate\": \"2026-07-10\""));
        assertTrue(json.contains("\"sleepLocation\": null"));
        assertTrue(json.contains("\"sleepLocation\": \"BED\""));
        assertTrue(json.contains("\"sleptThroughNight\": false"));
        assertTrue(json.contains("\"hadDreams\": true"));
        assertTrue(json.contains("Line one\\nLine two"));
        assertFalse(json.contains("\"id\""));
    }

    @Test
    public void sortsAndDeduplicatesTagsLexicographically() {
        SleepLogEntity entity = entity("2026-07-10");

        String json = codec.serialize(document(new SleepBackupRecord(entity, Arrays.asList("Z_TAG", "A_TAG", "A_TAG"))));

        assertTrue(json.indexOf("\"A_TAG\"") < json.indexOf("\"Z_TAG\""));
        assertEquals(json.indexOf("\"A_TAG\""), json.lastIndexOf("\"A_TAG\""));
    }

    @Test
    public void parsesAndRoundTripsVersionOneBackup() throws Exception {
        SleepBackupDocument parsed = codec.parse(codec.serialize(document(new SleepBackupRecord(entity("2026-07-10"), Arrays.asList("UNKNOWN_TAG")))));

        assertEquals(1, parsed.getRecords().size());
        assertEquals("2026-07-10", parsed.getRecords().get(0).getNightDate());
        assertEquals(Collections.singletonList("UNKNOWN_TAG"), parsed.getRecords().get(0).getTagKeys());
    }

    @Test
    public void preservesTriStateNullsUnknownKeysAndMultilineNotes() throws Exception {
        SleepLogEntity entity = entity("2026-07-10");
        entity.setSleepLocation("FUTURE_LOCATION");
        entity.setSleptThroughNight(null);
        entity.setHadDreams(false);
        entity.setNotes("A\nB");

        SleepBackupRecord record = codec.parse(codec.serialize(document(new SleepBackupRecord(entity, Arrays.asList("FUTURE_TAG")))))
                .getRecords()
                .get(0);

        assertEquals("FUTURE_LOCATION", record.getSleepLog().getSleepLocation());
        assertEquals(null, record.getSleepLog().getSleptThroughNight());
        assertEquals(Boolean.FALSE, record.getSleepLog().getHadDreams());
        assertEquals("A\nB", record.getSleepLog().getNotes());
        assertEquals(Collections.singletonList("FUTURE_TAG"), record.getTagKeys());
    }

    @Test
    public void ignoresUnknownExtraFields() throws Exception {
        String json = codec.serialize(document()).replace("\"logs\": []", "\"extra\": true,\n  \"logs\": []");

        assertEquals(0, codec.parse(json).getRecords().size());
    }

    @Test
    public void rejectsMalformedTopLevelAndVersions() {
        assertInvalid("not json");
        assertInvalid("{\"version\":1,\"databaseVersion\":1,\"exportedAt\":\"2026-07-11T00:00:00Z\",\"logs\":[]}");
        assertInvalid("{\"format\":\"wrong\",\"version\":1,\"databaseVersion\":1,\"exportedAt\":\"2026-07-11T00:00:00Z\",\"logs\":[]}");
        assertInvalid("{\"format\":\"snooze-reviews-backup\",\"version\":2,\"databaseVersion\":1,\"exportedAt\":\"2026-07-11T00:00:00Z\",\"logs\":[]}");
        assertInvalid("{\"format\":\"snooze-reviews-backup\",\"version\":0,\"databaseVersion\":1,\"exportedAt\":\"2026-07-11T00:00:00Z\",\"logs\":[]}");
        assertInvalid("{\"format\":\"snooze-reviews-backup\",\"version\":1,\"databaseVersion\":0,\"exportedAt\":\"2026-07-11T00:00:00Z\",\"logs\":[]}");
        assertInvalid("{\"format\":\"snooze-reviews-backup\",\"version\":1,\"databaseVersion\":1,\"exportedAt\":\"bad\",\"logs\":[]}");
        assertInvalid("{\"format\":\"snooze-reviews-backup\",\"version\":1,\"databaseVersion\":1,\"exportedAt\":\"2026-07-11T00:00:00Z\"}");
    }

    @Test
    public void rejectsInvalidRecordsAndDuplicateDates() {
        assertInvalid(withLog("\"nightDate\":\"bad\",\"createdAt\":1,\"updatedAt\":1,\"tags\":[]"));
        assertInvalid(withLog("\"nightDate\":\"2026-07-10\",\"fellAsleepMinute\":1440,\"createdAt\":1,\"updatedAt\":1,\"tags\":[]"));
        assertInvalid(withLog("\"nightDate\":\"2026-07-10\",\"sleepRating\":0,\"createdAt\":1,\"updatedAt\":1,\"tags\":[]"));
        assertInvalid(withLog("\"nightDate\":\"2026-07-10\",\"awakeningCount\":-1,\"createdAt\":1,\"updatedAt\":1,\"tags\":[]"));
        assertInvalid(withLog("\"nightDate\":\"2026-07-10\",\"createdAt\":2,\"updatedAt\":1,\"tags\":[]"));
        assertInvalid(withLog("\"nightDate\":\"2026-07-10\",\"createdAt\":1,\"updatedAt\":1,\"tags\":[\"\"]"));
        assertInvalid(base("\"logs\":[{\"nightDate\":\"2026-07-10\",\"createdAt\":1,\"updatedAt\":1,\"tags\":[]},{\"nightDate\":\"2026-07-10\",\"createdAt\":1,\"updatedAt\":1,\"tags\":[]}]"));
    }

    @Test
    public void rejectsExcessiveRecordCountAndOversizedInput() {
        StringBuilder logs = new StringBuilder("[");
        for (int i = 0; i <= SleepBackupCodec.MAX_LOG_RECORDS; i++) {
            if (i > 0) logs.append(',');
            logs.append("{\"nightDate\":\"").append(String.format("2300-%02d-%02d", ((i / 28) % 12) + 1, (i % 28) + 1)).append("\",\"createdAt\":1,\"updatedAt\":1,\"tags\":[]}");
        }
        logs.append(']');
        assertInvalid(base("\"logs\":" + logs));

        byte[] bytes = new byte[SleepBackupCodec.MAX_BACKUP_BYTES + 1];
        try {
            codec.parse(new ByteArrayInputStream(bytes));
            fail("Expected oversized input to fail");
        } catch (Exception expected) {
            assertTrue(expected instanceof SleepBackupValidationException);
        }
    }

    private SleepBackupDocument document(SleepBackupRecord... records) {
        return new SleepBackupDocument(1, Instant.parse("2026-07-11T14:30:00Z"), Arrays.asList(records));
    }

    private SleepLogEntity entity(String nightDate) {
        SleepLogEntity entity = new SleepLogEntity(nightDate);
        entity.setId(99L);
        entity.setCreatedAt(10L);
        entity.setUpdatedAt(20L);
        return entity;
    }

    private String withLog(String fields) {
        return base("\"logs\":[{" + fields + "}]");
    }

    private String base(String tail) {
        return "{\"format\":\"snooze-reviews-backup\",\"version\":1,\"databaseVersion\":1,\"exportedAt\":\"2026-07-11T00:00:00Z\"," + tail + "}";
    }

    private void assertInvalid(String json) {
        try {
            codec.parse(json);
            fail("Expected invalid backup");
        } catch (SleepBackupValidationException expected) {
            assertTrue(expected.getMessage().length() > 0);
        }
    }
}
