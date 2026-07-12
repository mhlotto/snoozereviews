package com.mhlotto.snoozereviews.data.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.mhlotto.snoozereviews.data.dao.CustomSleepLocationDao;
import com.mhlotto.snoozereviews.data.dao.SleepLogDao;
import com.mhlotto.snoozereviews.data.entity.CustomSleepLocationEntity;
import com.mhlotto.snoozereviews.data.entity.SleepLogEntity;
import com.mhlotto.snoozereviews.data.entity.SleepLogTagEntity;

@Database(
        entities = {
                SleepLogEntity.class,
                SleepLogTagEntity.class,
                CustomSleepLocationEntity.class
        },
        version = 2,
        exportSchema = true
)
public abstract class SnoozeReviewsDatabase extends RoomDatabase {
    public static final String DATABASE_NAME = "snooze-reviews.db";

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `custom_sleep_locations` ("
                    + "`location_key` TEXT NOT NULL, "
                    + "`display_name` TEXT NOT NULL, "
                    + "`normalized_name` TEXT NOT NULL, "
                    + "`is_active` INTEGER NOT NULL, "
                    + "`created_at` INTEGER NOT NULL, "
                    + "`updated_at` INTEGER NOT NULL, "
                    + "PRIMARY KEY(`location_key`))");
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS "
                    + "`index_custom_sleep_locations_normalized_name` "
                    + "ON `custom_sleep_locations` (`normalized_name`)");
        }
    };

    private static volatile SnoozeReviewsDatabase instance;

    public abstract SleepLogDao sleepLogDao();

    public abstract CustomSleepLocationDao customSleepLocationDao();

    public static SnoozeReviewsDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (SnoozeReviewsDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    SnoozeReviewsDatabase.class,
                                    DATABASE_NAME
                            )
                            .addMigrations(MIGRATION_1_2)
                            .build();
                }
            }
        }
        return instance;
    }
}
