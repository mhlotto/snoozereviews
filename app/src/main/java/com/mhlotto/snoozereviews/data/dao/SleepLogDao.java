package com.mhlotto.snoozereviews.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.mhlotto.snoozereviews.data.SleepLogValidator;
import com.mhlotto.snoozereviews.data.SleepLogWithTags;
import com.mhlotto.snoozereviews.data.entity.SleepLogEntity;
import com.mhlotto.snoozereviews.data.entity.SleepLogTagEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Dao
public interface SleepLogDao {
    @Insert
    long insertSleepLog(SleepLogEntity sleepLog);

    @Update
    int updateSleepLog(SleepLogEntity sleepLog);

    @Query("DELETE FROM sleep_logs WHERE id = :id")
    int deleteSleepLogById(long id);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertTags(List<SleepLogTagEntity> tags);

    @Query("DELETE FROM sleep_log_tags WHERE sleep_log_id = :sleepLogId")
    void deleteTagsForSleepLog(long sleepLogId);

    @Query("SELECT * FROM sleep_logs WHERE id = :id")
    SleepLogEntity findEntityById(long id);

    @Transaction
    @Query("SELECT * FROM sleep_logs WHERE id = :id")
    SleepLogWithTags findById(long id);

    @Transaction
    @Query("SELECT * FROM sleep_logs WHERE night_date = :nightDate")
    SleepLogWithTags findByNightDate(String nightDate);

    @Transaction
    @Query("SELECT * FROM sleep_logs ORDER BY night_date DESC")
    List<SleepLogWithTags> listAllNewestFirst();

    @Query("SELECT EXISTS(SELECT 1 FROM sleep_logs WHERE night_date = :nightDate)")
    boolean existsForNightDate(String nightDate);

    @Query("SELECT COUNT(*) FROM sleep_logs")
    int countSleepLogs();

    @Query("SELECT COUNT(*) FROM sleep_log_tags WHERE sleep_log_id = :sleepLogId")
    int countTagsForSleepLog(long sleepLogId);

    @Transaction
    default long createLogWithTags(SleepLogEntity sleepLog, Collection<String> tagKeys) {
        long sleepLogId = insertSleepLog(sleepLog);
        insertTags(toTagRows(sleepLogId, SleepLogValidator.normalizeTagKeys(tagKeys)));
        return sleepLogId;
    }

    @Transaction
    default void updateLogWithTags(SleepLogEntity sleepLog, Collection<String> tagKeys) {
        if (findEntityById(sleepLog.getId()) == null) {
            throw new IllegalArgumentException("sleep log does not exist: " + sleepLog.getId());
        }
        int updatedRows = updateSleepLog(sleepLog);
        if (updatedRows != 1) {
            throw new IllegalStateException("expected to update one sleep log, updated " + updatedRows);
        }
        deleteTagsForSleepLog(sleepLog.getId());
        insertTags(toTagRows(sleepLog.getId(), SleepLogValidator.normalizeTagKeys(tagKeys)));
    }

    default boolean deleteLogById(long id) {
        return deleteSleepLogById(id) > 0;
    }

    private static List<SleepLogTagEntity> toTagRows(long sleepLogId, List<String> tagKeys) {
        List<SleepLogTagEntity> rows = new ArrayList<>(tagKeys.size());
        for (String tagKey : tagKeys) {
            rows.add(new SleepLogTagEntity(sleepLogId, tagKey));
        }
        return rows;
    }
}
