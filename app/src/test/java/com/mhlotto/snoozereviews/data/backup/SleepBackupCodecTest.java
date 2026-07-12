package com.mhlotto.snoozereviews.data.backup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.mhlotto.snoozereviews.data.SleepLocationKeys;
import com.mhlotto.snoozereviews.data.entity.CustomSleepTagEntity;
import com.mhlotto.snoozereviews.data.entity.SleepLogEntity;
import com.mhlotto.snoozereviews.data.tag.CustomSleepTagKey;
import com.mhlotto.snoozereviews.data.tag.SleepTagCategoryKeys;

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
        assertTrue(json.contains("\"version\": 3"));
        assertTrue(json.contains("\"databaseVersion\": 4"));
        assertTrue(json.contains("\"customTags\": []"));
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
        full.setDreamDetails("Dream line\nCafé");
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
        assertTrue(json.contains("\"dreamDetails\": \"Dream line\\nCafé\""));
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
        SleepBackupDocument parsed = codec.parse(base("\"logs\":[{\"nightDate\":\"2026-07-10\",\"createdAt\":1,\"updatedAt\":1,\"tags\":[\"UNKNOWN_TAG\"]}]", 1, 1));

        assertEquals(1, parsed.getRecords().size());
        assertEquals("2026-07-10", parsed.getRecords().get(0).getNightDate());
        assertEquals(Collections.singletonList("UNKNOWN_TAG"), parsed.getRecords().get(0).getTagKeys());
        assertEquals(0, parsed.getCustomTags().size());
        assertNull(parsed.getRecords().get(0).getSleepLog().getDreamDetails());
    }

    @Test
    public void serializesAndParsesVersionTwoCustomTagDefinitions() throws Exception {
        String key = CustomSleepTagKey.encode("Weighted Blanket");
        SleepBackupDocument document = new SleepBackupDocument(
                3,
                Instant.parse("2026-07-11T14:30:00Z"),
                Collections.singletonList(new SleepBackupCustomTag(new CustomSleepTagEntity(
                        key,
                        "Weighted Blanket",
                        "weighted blanket",
                        SleepTagCategoryKeys.ENVIRONMENT,
                        false,
                        10L,
                        20L
                ))),
                Collections.emptyList()
        );

        String json = codec.serialize(document);
        assertTrue(json.contains("\"customTags\""));
        assertTrue(json.contains("\"categoryKey\": \"ENVIRONMENT\""));
        assertTrue(json.contains("\"isActive\": false"));

        SleepBackupCustomTag parsed = codec.parse(json).getCustomTags().get(0);
        assertEquals(key, parsed.getEntity().getTagKey());
        assertEquals("Weighted Blanket", parsed.getEntity().getDisplayName());
        assertEquals(SleepTagCategoryKeys.ENVIRONMENT, parsed.getEntity().getCategoryKey());
        assertFalse(parsed.getEntity().isActive());
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
    public void roundTripsVersionThreeDreamDetailsAndRejectsInconsistentDreams() throws Exception {
        SleepLogEntity entity = entity("2026-07-10");
        entity.setHadDreams(Boolean.TRUE);
        entity.setDreamDetails("Forest\n雪");

        SleepBackupRecord record = codec.parse(codec.serialize(document(new SleepBackupRecord(entity, Collections.emptyList()))))
                .getRecords()
                .get(0);

        assertEquals("Forest\n雪", record.getSleepLog().getDreamDetails());
        assertInvalid(withLog("\"nightDate\":\"2026-07-10\",\"hadDreams\":false,\"dreamDetails\":\"Hidden\",\"createdAt\":1,\"updatedAt\":1,\"tags\":[]"));
        assertInvalid(withLog("\"nightDate\":\"2026-07-10\",\"hadDreams\":null,\"dreamDetails\":\"Hidden\",\"createdAt\":1,\"updatedAt\":1,\"tags\":[]"));
        assertInvalid(withLog("\"nightDate\":\"2026-07-10\",\"hadDreams\":true,\"dreamDetails\":\"" + repeat("a", com.mhlotto.snoozereviews.data.SleepLogValidator.MAX_DREAM_DETAILS_CHARS + 1) + "\",\"createdAt\":1,\"updatedAt\":1,\"tags\":[]"));
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
        assertInvalid("{\"format\":\"snooze-reviews-backup\",\"version\":4,\"databaseVersion\":4,\"exportedAt\":\"2026-07-11T00:00:00Z\",\"customTags\":[],\"logs\":[]}");
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
        assertInvalid(base("\"customTags\":[],\"logs\":[{\"nightDate\":\"2026-07-10\",\"createdAt\":1,\"updatedAt\":1,\"tags\":[]},{\"nightDate\":\"2026-07-10\",\"createdAt\":1,\"updatedAt\":1,\"tags\":[]}]"));
    }

    @Test
    public void rejectsInvalidVersionTwoCustomTags() {
        String key = CustomSleepTagKey.encode("Weighted Blanket");
        assertInvalid(base("\"customTags\":[{\"tagKey\":\"" + key + "\",\"displayName\":\"Other Name\",\"categoryKey\":\"OTHER\",\"isActive\":true,\"createdAt\":1,\"updatedAt\":1}],\"logs\":[]", 2, 3));
        assertInvalid(base("\"customTags\":[{\"tagKey\":\"" + key + "\",\"displayName\":\"Weighted Blanket\",\"categoryKey\":\"BAD\",\"isActive\":true,\"createdAt\":1,\"updatedAt\":1}],\"logs\":[]", 2, 3));
        assertInvalid(base("\"customTags\":[{\"tagKey\":\"" + key + "\",\"displayName\":\"Weighted Blanket\",\"categoryKey\":\"OTHER\",\"isActive\":true,\"createdAt\":2,\"updatedAt\":1}],\"logs\":[]", 2, 3));
        assertInvalid(base("\"customTags\":[{\"tagKey\":\"" + key + "\",\"displayName\":\"Weighted Blanket\",\"categoryKey\":\"OTHER\",\"isActive\":true,\"createdAt\":1,\"updatedAt\":1},{\"tagKey\":\"" + key + "\",\"displayName\":\"Weighted Blanket\",\"categoryKey\":\"OTHER\",\"isActive\":true,\"createdAt\":1,\"updatedAt\":1}],\"logs\":[]", 2, 3));
        String builtInDuplicate = CustomSleepTagKey.encode("Too hot");
        assertInvalid(base("\"customTags\":[{\"tagKey\":\"" + builtInDuplicate + "\",\"displayName\":\"Too hot\",\"categoryKey\":\"OTHER\",\"isActive\":true,\"createdAt\":1,\"updatedAt\":1}],\"logs\":[]", 2, 3));
    }

    @Test
    public void rejectsExcessiveRecordCountAndOversizedInput() {
        StringBuilder logs = new StringBuilder("[");
        for (int i = 0; i <= SleepBackupCodec.MAX_LOG_RECORDS; i++) {
            if (i > 0) logs.append(',');
            logs.append("{\"nightDate\":\"").append(String.format("2300-%02d-%02d", ((i / 28) % 12) + 1, (i % 28) + 1)).append("\",\"createdAt\":1,\"updatedAt\":1,\"tags\":[]}");
        }
        logs.append(']');
        assertInvalid(base("\"customTags\":[],\"logs\":" + logs));

        byte[] bytes = new byte[SleepBackupCodec.MAX_BACKUP_BYTES + 1];
        try {
            codec.parse(new ByteArrayInputStream(bytes));
            fail("Expected oversized input to fail");
        } catch (Exception expected) {
            assertTrue(expected instanceof SleepBackupValidationException);
        }
    }

    private SleepBackupDocument document(SleepBackupRecord... records) {
        return new SleepBackupDocument(4, Instant.parse("2026-07-11T14:30:00Z"), Arrays.asList(records));
    }

    private SleepLogEntity entity(String nightDate) {
        SleepLogEntity entity = new SleepLogEntity(nightDate);
        entity.setId(99L);
        entity.setCreatedAt(10L);
        entity.setUpdatedAt(20L);
        return entity;
    }

    private String withLog(String fields) {
        return base("\"customTags\":[],\"logs\":[{" + fields + "}]");
    }

    private String base(String tail) {
        return base(tail, 3, 4);
    }

    private String base(String tail, int version, int databaseVersion) {
        return "{\"format\":\"snooze-reviews-backup\",\"version\":" + version + ",\"databaseVersion\":" + databaseVersion + ",\"exportedAt\":\"2026-07-11T00:00:00Z\"," + tail + "}";
    }

    private void assertInvalid(String json) {
        try {
            codec.parse(json);
            fail("Expected invalid backup");
        } catch (SleepBackupValidationException expected) {
            assertTrue(expected.getMessage().length() > 0);
        }
    }

    private String repeat(String value, int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            builder.append(value);
        }
        return builder.toString();
    }
}
