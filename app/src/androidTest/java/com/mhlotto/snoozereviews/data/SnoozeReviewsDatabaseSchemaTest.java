package com.mhlotto.snoozereviews.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
                .addMigrations(SnoozeReviewsDatabase.MIGRATION_1_2)
                .addMigrations(SnoozeReviewsDatabase.MIGRATION_2_3)
                .addMigrations(SnoozeReviewsDatabase.MIGRATION_3_4)
                .build();

        try {
            assertNotNull(roomDatabase.getOpenHelper().getWritableDatabase());
        } finally {
            roomDatabase.close();
        }
    }

    @Test
    public void migrationOneToTwoPreservesLogsAndCreatesCustomLocationTable() throws Exception {
        SupportSQLiteDatabase sqliteDatabase = helper.createDatabase(TEST_DB, 1);
        sqliteDatabase.execSQL("INSERT INTO sleep_logs "
                + "(id, night_date, sleep_location, fell_asleep_minute, woke_up_minute, "
                + "slept_through_night, had_dreams, sleep_rating, rested_rating, awakening_count, notes, created_at, updated_at) "
                + "VALUES (1, '2026-07-10', 'BED', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 10, 10)");
        sqliteDatabase.close();

        SupportSQLiteDatabase migrated = helper.runMigrationsAndValidate(
                TEST_DB,
                2,
                true,
                SnoozeReviewsDatabase.MIGRATION_1_2
        );

        android.database.Cursor countCursor = migrated.query("SELECT COUNT(*) FROM sleep_logs");
        try {
            assertTrue(countCursor.moveToFirst());
            assertEquals(1, countCursor.getInt(0));
        } finally {
            countCursor.close();
        }
        android.database.Cursor cursor = migrated.query("SELECT sleep_location FROM sleep_logs WHERE id = 1");
        try {
            assertTrue(cursor.moveToFirst());
            assertEquals("BED", cursor.getString(0));
        } finally {
            cursor.close();
        }

        migrated.execSQL("INSERT INTO custom_sleep_locations "
                + "(location_key, display_name, normalized_name, is_active, created_at, updated_at) "
                + "VALUES ('CUSTOM_B64:SGFtbW9jaw', 'Hammock', 'hammock', 1, 20, 20)");
        try {
            migrated.execSQL("INSERT INTO custom_sleep_locations "
                    + "(location_key, display_name, normalized_name, is_active, created_at, updated_at) "
                    + "VALUES ('CUSTOM_B64:aGFtbW9jaw', 'hammock', 'hammock', 1, 20, 20)");
        } catch (android.database.sqlite.SQLiteConstraintException expected) {
            return;
        }
        throw new AssertionError("Expected normalized_name uniqueness constraint");
    }

    @Test
    public void migrationTwoToThreePreservesTagsAndCreatesCustomTagTable() throws Exception {
        SupportSQLiteDatabase sqliteDatabase = helper.createDatabase(TEST_DB, 2);
        sqliteDatabase.execSQL("INSERT INTO sleep_logs "
                + "(id, night_date, sleep_location, fell_asleep_minute, woke_up_minute, "
                + "slept_through_night, had_dreams, sleep_rating, rested_rating, awakening_count, notes, created_at, updated_at) "
                + "VALUES (1, '2026-07-10', 'BED', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 10, 10)");
        sqliteDatabase.execSQL("INSERT INTO sleep_log_tags (sleep_log_id, tag_key) VALUES (1, 'RESTLESS')");
        sqliteDatabase.close();

        SupportSQLiteDatabase migrated = helper.runMigrationsAndValidate(
                TEST_DB,
                3,
                true,
                SnoozeReviewsDatabase.MIGRATION_2_3
        );

        android.database.Cursor tagCursor = migrated.query("SELECT tag_key FROM sleep_log_tags WHERE sleep_log_id = 1");
        try {
            assertTrue(tagCursor.moveToFirst());
            assertEquals("RESTLESS", tagCursor.getString(0));
        } finally {
            tagCursor.close();
        }

        migrated.execSQL("INSERT INTO custom_sleep_tags "
                + "(tag_key, display_name, normalized_name, category_key, is_active, created_at, updated_at) "
                + "VALUES ('CUSTOM_TAG_B64:V2VpZ2h0ZWQgQmxhbmtldA', 'Weighted Blanket', 'weighted blanket', 'OTHER', 1, 20, 20)");
        try {
            migrated.execSQL("INSERT INTO custom_sleep_tags "
                    + "(tag_key, display_name, normalized_name, category_key, is_active, created_at, updated_at) "
                    + "VALUES ('CUSTOM_TAG_B64:d2VpZ2h0ZWQgYmxhbmtldA', 'weighted blanket', 'weighted blanket', 'OTHER', 1, 20, 20)");
        } catch (android.database.sqlite.SQLiteConstraintException expected) {
            return;
        }
        throw new AssertionError("Expected custom tag normalized_name uniqueness constraint");
    }

    @Test
    public void migrationThreeToFourAddsNullableDreamDetails() throws Exception {
        SupportSQLiteDatabase sqliteDatabase = helper.createDatabase(TEST_DB, 3);
        sqliteDatabase.execSQL("INSERT INTO sleep_logs "
                + "(id, night_date, sleep_location, fell_asleep_minute, woke_up_minute, "
                + "slept_through_night, had_dreams, sleep_rating, rested_rating, awakening_count, notes, created_at, updated_at) "
                + "VALUES (1, '2026-07-10', 'BED', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 10, 10)");
        sqliteDatabase.execSQL("INSERT INTO sleep_log_tags (sleep_log_id, tag_key) VALUES (1, 'RESTLESS')");
        sqliteDatabase.close();

        SupportSQLiteDatabase migrated = helper.runMigrationsAndValidate(
                TEST_DB,
                4,
                true,
                SnoozeReviewsDatabase.MIGRATION_3_4
        );

        android.database.Cursor cursor = migrated.query("SELECT dream_details FROM sleep_logs WHERE id = 1");
        try {
            assertTrue(cursor.moveToFirst());
            assertEquals(null, cursor.getString(0));
        } finally {
            cursor.close();
        }
        migrated.execSQL("UPDATE sleep_logs SET had_dreams = 1, dream_details = 'Forest' WHERE id = 1");
        android.database.Cursor tagCursor = migrated.query("SELECT tag_key FROM sleep_log_tags WHERE sleep_log_id = 1");
        try {
            assertTrue(tagCursor.moveToFirst());
            assertEquals("RESTLESS", tagCursor.getString(0));
        } finally {
            tagCursor.close();
        }
    }

    @Test
    public void migrationOneToFourRunsCompleteChain() throws Exception {
        SupportSQLiteDatabase sqliteDatabase = helper.createDatabase(TEST_DB, 1);
        sqliteDatabase.execSQL("INSERT INTO sleep_logs "
                + "(id, night_date, sleep_location, fell_asleep_minute, woke_up_minute, "
                + "slept_through_night, had_dreams, sleep_rating, rested_rating, awakening_count, notes, created_at, updated_at) "
                + "VALUES (1, '2026-07-10', 'BED', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 10, 10)");
        sqliteDatabase.close();

        SupportSQLiteDatabase migrated = helper.runMigrationsAndValidate(
                TEST_DB,
                4,
                true,
                SnoozeReviewsDatabase.MIGRATION_1_2,
                SnoozeReviewsDatabase.MIGRATION_2_3,
                SnoozeReviewsDatabase.MIGRATION_3_4
        );

        android.database.Cursor cursor = migrated.query("SELECT COUNT(*) FROM sleep_logs");
        try {
            assertTrue(cursor.moveToFirst());
            assertEquals(1, cursor.getInt(0));
        } finally {
            cursor.close();
        }
    }
}
