package com.mhlotto.snoozereviews.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.mhlotto.snoozereviews.R;
import com.mhlotto.snoozereviews.data.SleepLogRepository;
import com.mhlotto.snoozereviews.data.SleepLogWithTags;
import com.mhlotto.snoozereviews.databinding.ActivitySleepHistoryBinding;
import com.mhlotto.snoozereviews.ui.detail.SleepLogDetailFormatter;
import com.mhlotto.snoozereviews.ui.history.SleepHistoryAdapter;
import com.mhlotto.snoozereviews.ui.history.SleepHistoryItem;
import com.mhlotto.snoozereviews.ui.history.SleepHistoryItemFormatter;
import com.mhlotto.snoozereviews.ui.navigation.AppNavigation;

import java.util.List;
import java.util.Locale;

public class SleepHistoryActivity extends AppCompatActivity {
    private static final String TAG = "SleepHistoryActivity";

    private ActivitySleepHistoryBinding binding;
    private SleepLogRepository repository;
    private SleepHistoryItemFormatter formatter;
    private SleepHistoryAdapter adapter;
    private int loadGeneration;
    private boolean destroyed;
    private boolean loadedOnce;
    private boolean navigationInProgress;

    public static Intent newIntent(Context context) {
        return new Intent(context, SleepHistoryActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySleepHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        SystemBarInsets.applyToRoot(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(view -> finish());

        repository = new SleepLogRepository(this);
        formatter = new SleepHistoryItemFormatter(new AndroidLabelResolver(), Locale.getDefault(), DateFormat.is24HourFormat(this));
        adapter = new SleepHistoryAdapter(this::openDetail);
        binding.historyList.setLayoutManager(new LinearLayoutManager(this));
        binding.historyList.setAdapter(adapter);
        binding.retryButton.setOnClickListener(view -> loadHistory(false));

        loadHistory(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        navigationInProgress = false;
        if (loadedOnce) {
            loadHistory(true);
        }
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        if (repository != null) {
            repository.shutdownBackgroundExecutor();
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_app_navigation, menu);
        AppNavigation.hideActiveDestination(menu, AppNavigation.Destination.HISTORY);
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

    private void loadHistory(boolean refreshExistingContent) {
        int requestGeneration = ++loadGeneration;
        if (!refreshExistingContent || adapter.getCurrentList().isEmpty()) {
            showLoading();
        }
        binding.retryButton.setEnabled(false);

        repository.listAllSleepLogs(new SleepLogRepository.Callback<>() {
            @Override
            public void onSuccess(List<SleepLogWithTags> result) {
                if (destroyed || requestGeneration != loadGeneration) {
                    return;
                }
                loadedOnce = true;
                binding.retryButton.setEnabled(true);
                List<SleepHistoryItem> items = formatter.formatList(result);
                adapter.submitList(items);
                if (items.isEmpty()) {
                    showEmpty();
                } else {
                    showContent();
                }
            }

            @Override
            public void onError(Throwable error) {
                if (destroyed || requestGeneration != loadGeneration) {
                    return;
                }
                Log.e(TAG, "Failed to load sleep history", error);
                binding.retryButton.setEnabled(true);
                if (refreshExistingContent && !adapter.getCurrentList().isEmpty()) {
                    Snackbar.make(binding.historyRoot, R.string.history_load_error, Snackbar.LENGTH_LONG)
                            .setAction(R.string.retry, view -> loadHistory(true))
                            .show();
                    showContent();
                } else {
                    showError();
                }
            }
        });
    }

    private void openDetail(SleepHistoryItem item) {
        if (navigationInProgress) {
            return;
        }
        if (item.getSleepLogId() <= 0L || item.getNightDate() == null) {
            Log.e(TAG, "Invalid history row navigation state");
            return;
        }
        navigationInProgress = true;
        startActivity(SleepLogDetailActivity.newIntent(this, item.getSleepLogId(), item.getNightDate()));
    }

    private void showLoading() {
        binding.loadingContainer.setVisibility(View.VISIBLE);
        binding.emptyContainer.setVisibility(View.GONE);
        binding.errorContainer.setVisibility(View.GONE);
        binding.historyList.setVisibility(View.GONE);
    }

    private void showEmpty() {
        binding.loadingContainer.setVisibility(View.GONE);
        binding.emptyContainer.setVisibility(View.VISIBLE);
        binding.errorContainer.setVisibility(View.GONE);
        binding.historyList.setVisibility(View.GONE);
    }

    private void showContent() {
        binding.loadingContainer.setVisibility(View.GONE);
        binding.emptyContainer.setVisibility(View.GONE);
        binding.errorContainer.setVisibility(View.GONE);
        binding.historyList.setVisibility(View.VISIBLE);
    }

    private void showError() {
        binding.loadingContainer.setVisibility(View.GONE);
        binding.emptyContainer.setVisibility(View.GONE);
        binding.errorContainer.setVisibility(View.VISIBLE);
        binding.historyList.setVisibility(View.GONE);
    }

    private class AndroidLabelResolver implements SleepLogDetailFormatter.LabelResolver {
        @Override
        public String getString(int resId) {
            return SleepHistoryActivity.this.getString(resId);
        }

        @Override
        public String getString(int resId, Object... args) {
            return SleepHistoryActivity.this.getString(resId, args);
        }
    }
}
