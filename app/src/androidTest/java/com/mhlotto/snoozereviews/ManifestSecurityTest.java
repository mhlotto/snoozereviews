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
import android.content.res.XmlResourceParser;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.material.chip.Chip;
import com.mhlotto.snoozereviews.ui.AddSleepByDateActivity;
import com.mhlotto.snoozereviews.ui.BackupRestoreActivity;
import com.mhlotto.snoozereviews.ui.SettingsActivity;
import com.mhlotto.snoozereviews.ui.SleepHistoryActivity;
import com.mhlotto.snoozereviews.ui.SleepTagSettingsActivity;
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
        assertFalse(activities.get(SettingsActivity.class.getName()).exported);
        assertFalse(activities.get(SleepTagSettingsActivity.class.getName()).exported);
    }

    @Test
    public void manifestContainsExpectedActivityCount() throws Exception {
        assertEquals(9, activitiesByName().size());
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
        assertTrue(context.getResources().getResourceName(R.drawable.snooze_splash_full).contains("snooze_splash_full"));
        assertTrue(context.getResources().getResourceName(R.color.snooze_splash_background).contains("snooze_splash_background"));
        assertTrue(context.getResources().getResourceName(R.layout.activity_splash).contains("activity_splash"));
    }

    @Test
    public void themeTypographyAndChoiceChipResourcesResolve() {
        Context context = ApplicationProvider.getApplicationContext();

        assertTrue(context.getResources().getResourceName(R.style.TextAppearance_SnoozeReviews_Headline).contains("TextAppearance.SnoozeReviews.Headline"));
        assertTrue(context.getResources().getResourceName(R.style.TextAppearance_SnoozeReviews_SectionTitle).contains("TextAppearance.SnoozeReviews.SectionTitle"));
        assertTrue(context.getResources().getResourceName(R.style.TextAppearance_SnoozeReviews_Body).contains("TextAppearance.SnoozeReviews.Body"));
        assertTrue(context.getResources().getResourceName(R.style.TextAppearance_SnoozeReviews_Chip).contains("TextAppearance.SnoozeReviews.Chip"));
        assertTrue(context.getResources().getDimensionPixelSize(R.dimen.touch_target_min) >= 48);
        assertTrue(context.getResources().getResourceName(R.layout.view_choice_chip).contains("view_choice_chip"));
        assertTrue(context.getResources().getResourceName(R.layout.view_form_choice_chip_no_icon).contains("view_form_choice_chip_no_icon"));
        assertTrue(context.getResources().getResourceName(R.style.Widget_SnoozeReviews_FormChoiceChip_NoIcon).contains("Widget.SnoozeReviews.FormChoiceChip.NoIcon"));
        assertTrue(context.getResources().getResourceName(R.color.snooze_choice_chip_background).contains("snooze_choice_chip_background"));
        assertTrue(context.getResources().getResourceName(R.color.snooze_choice_chip_text).contains("snooze_choice_chip_text"));
        assertTrue(context.getResources().getResourceName(R.color.snooze_choice_chip_stroke).contains("snooze_choice_chip_stroke"));
    }

    @Test
    public void formNoIconChoiceChipDisablesCheckedIconWithoutChangingDefaultChip() {
        Context context = new ContextThemeWrapper(ApplicationProvider.getApplicationContext(), R.style.Theme_SnoozeReviews);
        LayoutInflater inflater = LayoutInflater.from(context);

        Chip defaultChip = (Chip) inflater.inflate(R.layout.view_choice_chip, null, false);
        Chip noIconChip = (Chip) inflater.inflate(R.layout.view_form_choice_chip_no_icon, null, false);

        assertTrue(defaultChip.isCheckedIconVisible());
        assertFalse(noIconChip.isCheckedIconVisible());
    }

    @Test
    public void backupRuleResourcesResolve() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();

        try (XmlResourceParser ignored = context.getResources().getXml(R.xml.backup_rules)) {
            assertTrue(context.getResources().getResourceName(R.xml.backup_rules).contains("backup_rules"));
        }
        try (XmlResourceParser ignored = context.getResources().getXml(R.xml.data_extraction_rules)) {
            assertTrue(context.getResources().getResourceName(R.xml.data_extraction_rules).contains("data_extraction_rules"));
        }
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
