package com.mhlotto.snoozereviews.data.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.mhlotto.snoozereviews.data.dao.SleepLogDao;
import com.mhlotto.snoozereviews.data.entity.SleepLogEntity;
import com.mhlotto.snoozereviews.data.entity.SleepLogTagEntity;

@Database(
        entities = {
                SleepLogEntity.class,
                SleepLogTagEntity.class
        },
        version = 1,
        exportSchema = true
)
public abstract class SnoozeReviewsDatabase extends RoomDatabase {
    public static final String DATABASE_NAME = "snooze-reviews.db";

    private static volatile SnoozeReviewsDatabase instance;

    public abstract SleepLogDao sleepLogDao();

    public static SnoozeReviewsDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (SnoozeReviewsDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    SnoozeReviewsDatabase.class,
                                    DATABASE_NAME
                            )
                            .build();
                }
            }
        }
        return instance;
    }
}
