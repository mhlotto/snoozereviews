package com.mhlotto.snoozereviews.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.mhlotto.snoozereviews.data.entity.CustomSleepTagEntity;

import java.util.List;

@Dao
public interface CustomSleepTagDao {
    @Query("SELECT * FROM custom_sleep_tags WHERE is_active = 1 ORDER BY normalized_name ASC")
    List<CustomSleepTagEntity> listActive();

    @Query("SELECT * FROM custom_sleep_tags ORDER BY normalized_name ASC")
    List<CustomSleepTagEntity> listAll();

    @Query("SELECT * FROM custom_sleep_tags WHERE tag_key = :key")
    CustomSleepTagEntity findByKey(String key);

    @Query("SELECT * FROM custom_sleep_tags WHERE normalized_name = :normalizedName")
    CustomSleepTagEntity findByNormalizedName(String normalizedName);

    @Insert
    long insert(CustomSleepTagEntity tag);

    @Query("UPDATE custom_sleep_tags SET category_key = :categoryKey, updated_at = :updatedAt WHERE tag_key = :key")
    int updateCategory(String key, String categoryKey, long updatedAt);

    @Query("UPDATE custom_sleep_tags SET display_name = :displayName, normalized_name = :normalizedName, category_key = :categoryKey, is_active = :active, created_at = :createdAt, updated_at = :updatedAt WHERE tag_key = :key")
    int replaceByKey(String key, String displayName, String normalizedName, String categoryKey, boolean active, long createdAt, long updatedAt);

    @Query("UPDATE custom_sleep_tags SET is_active = 0, updated_at = :updatedAt WHERE tag_key = :key")
    int deactivate(String key, long updatedAt);

    @Query("UPDATE custom_sleep_tags SET is_active = 1, updated_at = :updatedAt WHERE tag_key = :key")
    int reactivate(String key, long updatedAt);

    @Query("SELECT COUNT(*) FROM custom_sleep_tags WHERE is_active = 1")
    int countActive();
}
