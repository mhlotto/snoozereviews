package com.mhlotto.snoozereviews.data.backup;

import com.mhlotto.snoozereviews.data.SleepLogValidator;
import com.mhlotto.snoozereviews.data.entity.CustomSleepTagEntity;
import com.mhlotto.snoozereviews.data.entity.SleepLogEntity;
import com.mhlotto.snoozereviews.data.tag.BuiltInSleepTagDuplicateNames;
import com.mhlotto.snoozereviews.data.tag.CustomSleepTagKey;
import com.mhlotto.snoozereviews.data.tag.SleepTagCategoryKeys;
import com.mhlotto.snoozereviews.data.tag.SleepTagNameNormalizer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SleepBackupCodec {
    public static final int MAX_BACKUP_BYTES = 25 * 1024 * 1024;
    public static final int MAX_LOG_RECORDS = 100_000;

    public String serialize(SleepBackupDocument document) {
        try {
            JSONObject root = new JSONObject();
            root.put("format", SleepBackupDocument.FORMAT);
            root.put("version", SleepBackupDocument.VERSION);
            root.put("databaseVersion", document.getDatabaseVersion());
            root.put("exportedAt", document.getExportedAt().toString());

            List<SleepBackupCustomTag> customTags = new ArrayList<>(document.getCustomTags());
            customTags.sort(Comparator.comparing(SleepBackupCustomTag::getNormalizedName));
            JSONArray customTagArray = new JSONArray();
            for (SleepBackupCustomTag customTag : customTags) {
                customTagArray.put(toJson(customTag));
            }
            root.put("customTags", customTagArray);

            List<SleepBackupRecord> records = new ArrayList<>(document.getRecords());
            records.sort(Comparator.comparing(SleepBackupRecord::getNightDate));
            JSONArray logs = new JSONArray();
            for (SleepBackupRecord record : records) {
                logs.put(toJson(record));
            }
            root.put("logs", logs);
            return root.toString(2) + "\n";
        } catch (JSONException exception) {
            throw new IllegalStateException("failed to serialize backup", exception);
        }
    }

    public void write(SleepBackupDocument document, OutputStream outputStream) throws IOException {
        outputStream.write(serialize(document).getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }

    public SleepBackupDocument parse(InputStream inputStream) throws IOException, SleepBackupValidationException {
        return parse(readLimited(inputStream));
    }

    public SleepBackupDocument parse(String json) throws SleepBackupValidationException {
        try {
            JSONObject root = new JSONObject(json);
            requireString(root, "format");
            if (!SleepBackupDocument.FORMAT.equals(root.getString("format"))) {
                throw new SleepBackupValidationException("Wrong backup format.");
            }
            int version = requireInt(root, "version");
            if (version > SleepBackupDocument.VERSION) {
                throw new SleepBackupValidationException("The backup version is newer than this app supports.");
            }
            if (version < 1) {
                throw new SleepBackupValidationException("Unsupported backup version.");
            }
            int databaseVersion = requireInt(root, "databaseVersion");
            if (databaseVersion <= 0) {
                throw new SleepBackupValidationException("Invalid database version.");
            }
            Instant exportedAt = parseInstant(requireString(root, "exportedAt"));
            List<SleepBackupCustomTag> customTags = parseCustomTags(root, version);
            JSONArray logs = requireArray(root, "logs");
            if (logs.length() > MAX_LOG_RECORDS) {
                throw new SleepBackupValidationException("Backup contains too many records.");
            }

            List<SleepBackupRecord> records = new ArrayList<>(logs.length());
            Set<String> nightDates = new HashSet<>();
            for (int i = 0; i < logs.length(); i++) {
                JSONObject log = logs.optJSONObject(i);
                if (log == null) {
                    throw new SleepBackupValidationException("Log record must be an object.");
                }
                SleepBackupRecord record = parseRecord(log, version);
                if (!nightDates.add(record.getNightDate())) {
                    throw new SleepBackupValidationException("Backup contains duplicate night dates.");
                }
                records.add(record);
            }
            return new SleepBackupDocument(databaseVersion, exportedAt, customTags, records);
        } catch (JSONException exception) {
            throw new SleepBackupValidationException("Malformed JSON.", exception);
        }
    }

    private JSONObject toJson(SleepBackupCustomTag customTag) throws JSONException {
        CustomSleepTagEntity entity = customTag.getEntity();
        JSONObject object = new JSONObject();
        object.put("tagKey", entity.getTagKey());
        object.put("displayName", entity.getDisplayName());
        object.put("categoryKey", entity.getCategoryKey());
        object.put("isActive", entity.isActive());
        object.put("createdAt", entity.getCreatedAt());
        object.put("updatedAt", entity.getUpdatedAt());
        return object;
    }

    private JSONObject toJson(SleepBackupRecord record) throws JSONException {
        SleepLogEntity log = record.getSleepLog();
        JSONObject object = new JSONObject();
        object.put("nightDate", log.getNightDate());
        putNullable(object, "sleepLocation", log.getSleepLocation());
        putNullable(object, "fellAsleepMinute", log.getFellAsleepMinute());
        putNullable(object, "wokeUpMinute", log.getWokeUpMinute());
        putNullable(object, "sleptThroughNight", log.getSleptThroughNight());
        putNullable(object, "hadDreams", log.getHadDreams());
        putNullable(object, "dreamDetails", Boolean.TRUE.equals(log.getHadDreams()) ? log.getDreamDetails() : null);
        putNullable(object, "sleepRating", log.getSleepRating());
        putNullable(object, "restedRating", log.getRestedRating());
        putNullable(object, "awakeningCount", log.getAwakeningCount());
        putNullable(object, "notes", log.getNotes());
        object.put("createdAt", log.getCreatedAt());
        object.put("updatedAt", log.getUpdatedAt());

        List<String> tagKeys = SleepLogValidator.normalizeTagKeys(record.getTagKeys());
        JSONArray tags = new JSONArray();
        for (String tagKey : tagKeys) {
            tags.put(tagKey);
        }
        object.put("tags", tags);
        return object;
    }

    private List<SleepBackupCustomTag> parseCustomTags(JSONObject root, int version)
            throws JSONException, SleepBackupValidationException {
        if (version == 1) {
            return new ArrayList<>();
        }
        JSONArray array = requireArray(root, "customTags");
        List<SleepBackupCustomTag> customTags = new ArrayList<>(array.length());
        Set<String> keys = new HashSet<>();
        Set<String> names = new HashSet<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject object = array.optJSONObject(i);
            if (object == null) {
                throw new SleepBackupValidationException("Custom tag definition must be an object.");
            }
            CustomSleepTagEntity entity = parseCustomTag(object);
            if (!keys.add(entity.getTagKey())) {
                throw new SleepBackupValidationException("Backup contains duplicate custom tag keys.");
            }
            if (!names.add(entity.getNormalizedName())) {
                throw new SleepBackupValidationException("Backup contains duplicate custom tag names.");
            }
            customTags.add(new SleepBackupCustomTag(entity));
        }
        return customTags;
    }

    private CustomSleepTagEntity parseCustomTag(JSONObject object)
            throws JSONException, SleepBackupValidationException {
        String tagKey = requireString(object, "tagKey");
        String displayName = requireString(object, "displayName");
        String categoryKey = requireString(object, "categoryKey");
        Boolean active = requireBoolean(object, "isActive");
        long createdAt = requireLong(object, "createdAt");
        long updatedAt = requireLong(object, "updatedAt");

        if (!CustomSleepTagKey.isCustomKey(tagKey)) {
            throw new SleepBackupValidationException("Invalid custom tag key.");
        }
        String decodedName = CustomSleepTagKey.decode(tagKey);
        if (decodedName == null) {
            throw new SleepBackupValidationException("Invalid custom tag key.");
        }
        SleepTagNameNormalizer.CleanedName cleaned;
        try {
            cleaned = SleepTagNameNormalizer.clean(displayName);
        } catch (IllegalArgumentException exception) {
            throw new SleepBackupValidationException("Invalid custom tag name.", exception);
        }
        if (!cleaned.getDisplayName().equals(decodedName)) {
            throw new SleepBackupValidationException("Custom tag key does not match display name.");
        }
        if (BuiltInSleepTagDuplicateNames.NORMALIZED_NAMES.contains(cleaned.getNormalizedName())) {
            throw new SleepBackupValidationException("Custom tag duplicates a built-in tag.");
        }
        if (!SleepTagCategoryKeys.isValid(categoryKey)) {
            throw new SleepBackupValidationException("Invalid custom tag category.");
        }
        if (createdAt < 0L || updatedAt < 0L || updatedAt < createdAt) {
            throw new SleepBackupValidationException("Invalid custom tag timestamps.");
        }
        return new CustomSleepTagEntity(
                tagKey,
                cleaned.getDisplayName(),
                cleaned.getNormalizedName(),
                categoryKey,
                active,
                createdAt,
                updatedAt
        );
    }

    private SleepBackupRecord parseRecord(JSONObject object, int version) throws JSONException, SleepBackupValidationException {
        SleepLogEntity entity = new SleepLogEntity();
        entity.setNightDate(requireString(object, "nightDate"));
        entity.setSleepLocation(optionalString(object, "sleepLocation"));
        entity.setFellAsleepMinute(optionalInt(object, "fellAsleepMinute"));
        entity.setWokeUpMinute(optionalInt(object, "wokeUpMinute"));
        entity.setSleptThroughNight(optionalBoolean(object, "sleptThroughNight"));
        entity.setHadDreams(optionalBoolean(object, "hadDreams"));
        entity.setDreamDetails(version >= 3 ? optionalString(object, "dreamDetails") : null);
        if (version >= 3 && !Boolean.TRUE.equals(entity.getHadDreams()) && entity.getDreamDetails() != null
                && !entity.getDreamDetails().trim().isEmpty()) {
            throw new SleepBackupValidationException("Dream details require hadDreams to be true.");
        }
        entity.setSleepRating(optionalInt(object, "sleepRating"));
        entity.setRestedRating(optionalInt(object, "restedRating"));
        if (version < 4 && (Integer.valueOf(0).equals(entity.getSleepRating())
                || Integer.valueOf(0).equals(entity.getRestedRating()))) {
            throw new SleepBackupValidationException("Rating zero requires backup version 4.");
        }
        entity.setAwakeningCount(optionalInt(object, "awakeningCount"));
        entity.setNotes(optionalString(object, "notes"));
        entity.setCreatedAt(requireLong(object, "createdAt"));
        entity.setUpdatedAt(requireLong(object, "updatedAt"));
        if (entity.getCreatedAt() < 0L || entity.getUpdatedAt() < 0L || entity.getUpdatedAt() < entity.getCreatedAt()) {
            throw new SleepBackupValidationException("Invalid timestamps.");
        }

        SleepLogEntity validated;
        try {
            validated = SleepLogValidator.validatedCopyForWrite(entity);
            validated.setCreatedAt(entity.getCreatedAt());
            validated.setUpdatedAt(entity.getUpdatedAt());
        } catch (IllegalArgumentException exception) {
            throw new SleepBackupValidationException("Invalid sleep log record.", exception);
        }

        JSONArray tags = requireArray(object, "tags");
        List<String> tagKeys = new ArrayList<>(tags.length());
        for (int i = 0; i < tags.length(); i++) {
            Object value = tags.get(i);
            if (!(value instanceof String)) {
                throw new SleepBackupValidationException("Tag key must be a string.");
            }
            tagKeys.add((String) value);
        }
        try {
            return new SleepBackupRecord(validated, SleepLogValidator.normalizeTagKeys(tagKeys));
        } catch (IllegalArgumentException exception) {
            throw new SleepBackupValidationException("Invalid tag key.", exception);
        }
    }

    private String readLimited(InputStream inputStream) throws IOException, SleepBackupValidationException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            total += read;
            if (total > MAX_BACKUP_BYTES) {
                throw new SleepBackupValidationException("Backup file is too large.");
            }
            output.write(buffer, 0, read);
        }
        return output.toString(StandardCharsets.UTF_8.name());
    }

    private void putNullable(JSONObject object, String key, Object value) throws JSONException {
        object.put(key, value == null ? JSONObject.NULL : value);
    }

    private String requireString(JSONObject object, String key) throws JSONException, SleepBackupValidationException {
        if (!object.has(key) || object.isNull(key) || !(object.get(key) instanceof String)) {
            throw new SleepBackupValidationException("Missing or invalid field: " + key);
        }
        return object.getString(key);
    }

    private int requireInt(JSONObject object, String key) throws JSONException, SleepBackupValidationException {
        if (!object.has(key) || object.isNull(key) || !(object.get(key) instanceof Integer)) {
            throw new SleepBackupValidationException("Missing or invalid integer field: " + key);
        }
        return object.getInt(key);
    }

    private Boolean requireBoolean(JSONObject object, String key) throws JSONException, SleepBackupValidationException {
        if (!object.has(key) || object.isNull(key) || !(object.get(key) instanceof Boolean)) {
            throw new SleepBackupValidationException("Missing or invalid Boolean field: " + key);
        }
        return object.getBoolean(key);
    }

    private long requireLong(JSONObject object, String key) throws JSONException, SleepBackupValidationException {
        if (!object.has(key) || object.isNull(key) || !(object.get(key) instanceof Number)) {
            throw new SleepBackupValidationException("Missing or invalid integer field: " + key);
        }
        return object.getLong(key);
    }

    private JSONArray requireArray(JSONObject object, String key) throws JSONException, SleepBackupValidationException {
        if (!object.has(key) || object.isNull(key) || !(object.get(key) instanceof JSONArray)) {
            throw new SleepBackupValidationException("Missing or invalid array field: " + key);
        }
        return object.getJSONArray(key);
    }

    private String optionalString(JSONObject object, String key) throws JSONException, SleepBackupValidationException {
        if (!object.has(key) || object.isNull(key)) {
            return null;
        }
        if (!(object.get(key) instanceof String)) {
            throw new SleepBackupValidationException("Invalid string field: " + key);
        }
        return object.getString(key);
    }

    private Integer optionalInt(JSONObject object, String key) throws JSONException, SleepBackupValidationException {
        if (!object.has(key) || object.isNull(key)) {
            return null;
        }
        if (!(object.get(key) instanceof Integer)) {
            throw new SleepBackupValidationException("Invalid integer field: " + key);
        }
        return object.getInt(key);
    }

    private Boolean optionalBoolean(JSONObject object, String key) throws JSONException, SleepBackupValidationException {
        if (!object.has(key) || object.isNull(key)) {
            return null;
        }
        if (!(object.get(key) instanceof Boolean)) {
            throw new SleepBackupValidationException("Invalid Boolean field: " + key);
        }
        return object.getBoolean(key);
    }

    private Instant parseInstant(String value) throws SleepBackupValidationException {
        try {
            return Instant.parse(value);
        } catch (RuntimeException exception) {
            throw new SleepBackupValidationException("Invalid export timestamp.", exception);
        }
    }
}
