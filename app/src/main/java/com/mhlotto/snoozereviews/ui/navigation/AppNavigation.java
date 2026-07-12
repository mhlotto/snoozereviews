package com.mhlotto.snoozereviews.ui.navigation;

import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.Nullable;

import com.mhlotto.snoozereviews.R;
import com.mhlotto.snoozereviews.ui.AddSleepByDateActivity;
import com.mhlotto.snoozereviews.ui.BackupRestoreActivity;
import com.mhlotto.snoozereviews.ui.SettingsActivity;
import com.mhlotto.snoozereviews.ui.SleepHistoryActivity;
import com.mhlotto.snoozereviews.ui.SleepStatsActivity;

public final class AppNavigation {
    public enum Destination {
        HISTORY,
        STATS,
        ADD_BY_DATE,
        BACKUP_RESTORE,
        SETTINGS
    }

    private AppNavigation() {
    }

    @Nullable
    public static Destination destinationForMenuItem(int itemId) {
        if (itemId == R.id.action_history) {
            return Destination.HISTORY;
        }
        if (itemId == R.id.action_stats) {
            return Destination.STATS;
        }
        if (itemId == R.id.action_add_by_date) {
            return Destination.ADD_BY_DATE;
        }
        if (itemId == R.id.action_backup_restore) {
            return Destination.BACKUP_RESTORE;
        }
        if (itemId == R.id.action_settings) {
            return Destination.SETTINGS;
        }
        return null;
    }

    public static boolean openDestination(Activity activity, Destination destination) {
        Intent intent;
        switch (destination) {
            case HISTORY:
                intent = SleepHistoryActivity.newIntent(activity);
                break;
            case STATS:
                intent = SleepStatsActivity.newIntent(activity);
                break;
            case ADD_BY_DATE:
                intent = AddSleepByDateActivity.newIntent(activity);
                break;
            case BACKUP_RESTORE:
                intent = BackupRestoreActivity.newIntent(activity);
                break;
            case SETTINGS:
                intent = SettingsActivity.newIntent(activity);
                break;
            default:
                return false;
        }
        activity.startActivity(intent);
        return true;
    }

    public static boolean handleMenuItem(Activity activity, MenuItem item) {
        Destination destination = destinationForMenuItem(item.getItemId());
        if (destination == null) {
            return false;
        }
        return openDestination(activity, destination);
    }

    public static void hideActiveDestination(Menu menu, Destination activeDestination) {
        MenuItem item = menu.findItem(menuItemIdForDestination(activeDestination));
        if (item != null) {
            item.setVisible(false);
        }
    }

    public static int menuItemIdForDestination(Destination destination) {
        switch (destination) {
            case HISTORY:
                return R.id.action_history;
            case STATS:
                return R.id.action_stats;
            case ADD_BY_DATE:
                return R.id.action_add_by_date;
            case BACKUP_RESTORE:
                return R.id.action_backup_restore;
            case SETTINGS:
                return R.id.action_settings;
            default:
                return 0;
        }
    }
}
