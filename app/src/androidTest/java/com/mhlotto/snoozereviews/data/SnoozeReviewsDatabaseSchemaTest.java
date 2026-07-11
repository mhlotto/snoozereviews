package com.mhlotto.snoozereviews.data;

import static org.junit.Assert.assertNotNull;

import android.content.Context;

import androidx.room.Room;
import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.mhlotto.snoozereviews.data.db.SnoozeReviewsDatabase;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SnoozeReviewsDatabaseSchemaTest {
    private static final String TEST_DB = "snooze-reviews-migration-test.db";

    @Rule
    public MigrationTestHelper helper = new MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            SnoozeReviewsDatabase.class
    );

    @After
    public void tearDown() {
        Context context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase(TEST_DB);
    }

    @Test
    public void versionOneSchemaCanBeCreatedAndValidated() throws Exception {
        SupportSQLiteDatabase sqliteDatabase = helper.createDatabase(TEST_DB, 1);
        sqliteDatabase.close();

        Context context = ApplicationProvider.getApplicationContext();
        SnoozeReviewsDatabase roomDatabase = Room.databaseBuilder(
                        context,
                        SnoozeReviewsDatabase.class,
                        TEST_DB
                )
                .build();

        try {
            assertNotNull(roomDatabase.getOpenHelper().getWritableDatabase());
        } finally {
            roomDatabase.close();
        }
    }
}
