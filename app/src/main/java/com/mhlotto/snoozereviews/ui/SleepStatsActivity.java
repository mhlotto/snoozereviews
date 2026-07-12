package com.mhlotto.snoozereviews.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;

import com.mhlotto.snoozereviews.R;
import com.mhlotto.snoozereviews.databinding.ActivitySleepStatsBinding;
import com.mhlotto.snoozereviews.ui.navigation.AppNavigation;

public class SleepStatsActivity extends AppCompatActivity {
    private ActivitySleepStatsBinding binding;
    private boolean navigationInProgress;

    public static Intent newIntent(Context context) {
        return new Intent(context, SleepStatsActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySleepStatsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        SystemBarInsets.applyToRoot(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(view -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        navigationInProgress = false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_app_navigation, menu);
        AppNavigation.hideActiveDestination(menu, AppNavigation.Destination.STATS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        AppNavigation.Destination destination = AppNavigation.destinationForMenuItem(item.getItemId());
        if (destination != null) {
            if (!navigationInProgress) {
                navigationInProgress = AppNavigation.openDestination(this, destination);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
