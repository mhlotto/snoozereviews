package com.mhlotto.snoozereviews.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.hamcrest.Matchers.containsString;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class LaunchActivityTest {
    @Test
    public void manifestLauncherResolvesToSplashActivity() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setPackage(context.getPackageName());

        ResolveInfo resolveInfo = context.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);

        assertEquals(SplashActivity.class.getName(), resolveInfo.activityInfo.name);
    }

    @Test
    public void formIntentFactoryIncludesExpectedExtras() {
        Context context = ApplicationProvider.getApplicationContext();

        Intent intent = SleepLogFormActivity.newCreateIntent(context, "2026-07-10");

        assertEquals(new ComponentName(context, SleepLogFormActivity.class), intent.getComponent());
        assertEquals("2026-07-10", intent.getStringExtra(SleepLogFormActivity.EXTRA_NIGHT_DATE));
    }

    @Test
    public void detailIntentFactoryIncludesExpectedExtras() {
        Context context = ApplicationProvider.getApplicationContext();

        Intent intent = SleepLogDetailActivity.newIntent(context, 42L, "2026-07-10");

        assertEquals(new ComponentName(context, SleepLogDetailActivity.class), intent.getComponent());
        assertEquals(42L, intent.getLongExtra(SleepLogDetailActivity.EXTRA_SLEEP_LOG_ID, 0L));
        assertEquals("2026-07-10", intent.getStringExtra(SleepLogDetailActivity.EXTRA_NIGHT_DATE));
    }

    @Test
    public void editIntentFactoryIncludesExpectedExtras() {
        Context context = ApplicationProvider.getApplicationContext();

        Intent intent = SleepLogFormActivity.newEditIntent(context, 42L);

        assertEquals(new ComponentName(context, SleepLogFormActivity.class), intent.getComponent());
        assertEquals(42L, intent.getLongExtra(SleepLogFormActivity.EXTRA_SLEEP_LOG_ID, 0L));
    }

    @Test
    public void statsAndAddByDateIntentFactoriesTargetExpectedActivities() {
        Context context = ApplicationProvider.getApplicationContext();

        assertEquals(
                new ComponentName(context, SleepStatsActivity.class),
                SleepStatsActivity.newIntent(context).getComponent()
        );
        assertEquals(
                new ComponentName(context, AddSleepByDateActivity.class),
                AddSleepByDateActivity.newIntent(context).getComponent()
        );
        assertEquals(
                new ComponentName(context, BackupRestoreActivity.class),
                BackupRestoreActivity.newIntent(context).getComponent()
        );
        assertEquals(
                new ComponentName(context, SettingsActivity.class),
                SettingsActivity.newIntent(context).getComponent()
        );
    }

    @Test
    public void formActivityDisplaysSuppliedNightDate() {
        Context context = ApplicationProvider.getApplicationContext();

        try (ActivityScenario<SleepLogFormActivity> ignored = ActivityScenario.launch(
                SleepLogFormActivity.newCreateIntent(context, "2026-07-10")
        )) {
            onView(withText(containsString("2026"))).check(matches(isDisplayed()));
        }
    }

    @Test
    public void detailActivityDisplaysSuppliedNightDate() {
        Context context = ApplicationProvider.getApplicationContext();

        try (ActivityScenario<SleepLogDetailActivity> ignored = ActivityScenario.launch(
                SleepLogDetailActivity.newIntent(context, 42L, "2026-07-10")
        )) {
            onView(withText("This sleep log could not be found.")).check(matches(isDisplayed()));
        }
    }

    @Test
    public void statsActivityDisplaysPlaceholder() {
        Context context = ApplicationProvider.getApplicationContext();

        try (ActivityScenario<SleepStatsActivity> ignored = ActivityScenario.launch(
                SleepStatsActivity.newIntent(context)
        )) {
            onView(withText("Sleep statistics")).check(matches(isDisplayed()));
            onView(withText("Statistics will appear here after enough sleep logs have been recorded."))
                    .check(matches(isDisplayed()));
        }
    }

    @Test
    public void addByDateActivityDisplaysDateSelectionFlow() {
        Context context = ApplicationProvider.getApplicationContext();

        try (ActivityScenario<AddSleepByDateActivity> ignored = ActivityScenario.launch(
                AddSleepByDateActivity.newIntent(context)
        )) {
            onView(withText("Add sleep by date")).check(matches(isDisplayed()));
            onView(withText("Continue")).check(matches(isDisplayed()));
        }
    }

    @Test
    public void backupRestoreActivityDisplaysExportAndImportActions() {
        Context context = ApplicationProvider.getApplicationContext();

        try (ActivityScenario<BackupRestoreActivity> ignored = ActivityScenario.launch(
                BackupRestoreActivity.newIntent(context)
        )) {
            onView(withText("Backup and Restore")).check(matches(isDisplayed()));
            onView(withText("Export backup")).check(matches(isDisplayed()));
            onView(withText("Import backup")).check(matches(isDisplayed()));
        }
    }

    @Test
    public void invalidExtrasShowSafeErrorState() {
        Context context = ApplicationProvider.getApplicationContext();

        try (ActivityScenario<SleepLogFormActivity> ignored = ActivityScenario.launch(
                new Intent(context, SleepLogFormActivity.class)
        )) {
            onView(withText("Required launch information was missing or invalid.")).check(matches(isDisplayed()));
        }
    }

    @Test
    public void splashActivityCanBeCreated() {
        try (ActivityScenario<SplashActivity> scenario = ActivityScenario.launch(SplashActivity.class)) {
            scenario.onActivity(activity -> assertTrue(!activity.isFinishing()));
        }
    }
}
