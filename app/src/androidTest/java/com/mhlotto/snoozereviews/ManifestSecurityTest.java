package com.mhlotto.snoozereviews;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.mhlotto.snoozereviews.ui.AddSleepByDateActivity;
import com.mhlotto.snoozereviews.ui.BackupRestoreActivity;
import com.mhlotto.snoozereviews.ui.SleepHistoryActivity;
import com.mhlotto.snoozereviews.ui.SleepLogDetailActivity;
import com.mhlotto.snoozereviews.ui.SleepLogFormActivity;
import com.mhlotto.snoozereviews.ui.SleepStatsActivity;
import com.mhlotto.snoozereviews.ui.SplashActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class ManifestSecurityTest {
    @Test
    public void appRequestsNoPermissions() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS);

        assertNull(packageInfo.requestedPermissions);
    }

    @Test
    public void onlySplashActivityIsExported() throws Exception {
        Map<String, ActivityInfo> activities = activitiesByName();

        assertTrue(activities.get(SplashActivity.class.getName()).exported);
        assertFalse(activities.get(SleepLogFormActivity.class.getName()).exported);
        assertFalse(activities.get(SleepLogDetailActivity.class.getName()).exported);
        assertFalse(activities.get(SleepHistoryActivity.class.getName()).exported);
        assertFalse(activities.get(SleepStatsActivity.class.getName()).exported);
        assertFalse(activities.get(AddSleepByDateActivity.class.getName()).exported);
        assertFalse(activities.get(BackupRestoreActivity.class.getName()).exported);
    }

    @Test
    public void manifestContainsExpectedActivityCount() throws Exception {
        assertEquals(7, activitiesByName().size());
    }

    @Test
    public void splashActivityIsTheLauncherAndUsesSplashTheme() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());

        assertEquals(SplashActivity.class.getName(), launchIntent.getComponent().getClassName());
        assertEquals(R.style.Theme_SnoozeReviews_Splash, activitiesByName().get(SplashActivity.class.getName()).theme);
    }

    @Test
    public void launcherAndSplashVisualResourcesResolve() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        ApplicationInfo applicationInfo = context.getPackageManager()
                .getApplicationInfo(context.getPackageName(), 0);

        assertEquals(R.mipmap.ic_launcher, applicationInfo.icon);
        assertTrue(context.getResources().getResourceName(R.mipmap.ic_launcher).contains("ic_launcher"));
        assertTrue(context.getResources().getResourceName(R.mipmap.ic_launcher_round).contains("ic_launcher_round"));
        assertTrue(context.getResources().getResourceName(R.mipmap.ic_launcher_foreground).contains("ic_launcher_foreground"));
        assertTrue(context.getResources().getResourceName(R.drawable.ic_launcher_monochrome).contains("ic_launcher_monochrome"));
        assertTrue(context.getResources().getResourceName(R.drawable.snooze_splash_icon).contains("snooze_splash_icon"));
        assertTrue(context.getResources().getResourceName(R.color.snooze_splash_background).contains("snooze_splash_background"));
    }

    private Map<String, ActivityInfo> activitiesByName() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_ACTIVITIES);
        Map<String, ActivityInfo> result = new HashMap<>();
        for (ActivityInfo activityInfo : packageInfo.activities) {
            result.put(activityInfo.name, activityInfo);
        }
        return result;
    }
}
