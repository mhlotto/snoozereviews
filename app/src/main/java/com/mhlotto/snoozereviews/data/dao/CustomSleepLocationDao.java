package com.mhlotto.snoozereviews.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.mhlotto.snoozereviews.data.entity.CustomSleepLocationEntity;

import java.util.List;

@Dao
public interface CustomSleepLocationDao {
    @Query("SELECT * FROM custom_sleep_locations WHERE is_active = 1 ORDER BY normalized_name ASC")
    List<CustomSleepLocationEntity> listActive();

    @Query("SELECT * FROM custom_sleep_locations ORDER BY normalized_name ASC")
    List<CustomSleepLocationEntity> listAll();

    @Query("SELECT * FROM custom_sleep_locations WHERE location_key = :key")
    CustomSleepLocationEntity findByKey(String key);

    @Query("SELECT * FROM custom_sleep_locations WHERE normalized_name = :normalizedName")
    CustomSleepLocationEntity findByNormalizedName(String normalizedName);

    @Insert
    long insert(CustomSleepLocationEntity location);

    @Query("UPDATE custom_sleep_locations SET is_active = 1, updated_at = :updatedAt WHERE location_key = :key")
    int reactivate(String key, long updatedAt);

    @Query("UPDATE custom_sleep_locations SET is_active = 0, updated_at = :updatedAt WHERE location_key = :key")
    int deactivate(String key, long updatedAt);

    @Query("SELECT COUNT(*) FROM custom_sleep_locations WHERE is_active = 1")
    int countActive();
}
