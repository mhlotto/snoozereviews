package com.mhlotto.snoozereviews.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.chip.Chip;
import com.mhlotto.snoozereviews.R;
import com.mhlotto.snoozereviews.data.SleepLogRepository;
import com.mhlotto.snoozereviews.data.SleepLogWithTags;
import com.mhlotto.snoozereviews.databinding.ActivitySleepLogDetailBinding;
import com.mhlotto.snoozereviews.ui.detail.SleepLogDetailFormatter;
import com.mhlotto.snoozereviews.ui.detail.SleepLogDetailViewState;
import com.mhlotto.snoozereviews.ui.detail.TagDisplayItem;
import com.mhlotto.snoozereviews.ui.navigation.AppNavigation;

import java.util.Locale;

public class SleepLogDetailActivity extends AppCompatActivity {
    public static final String EXTRA_SLEEP_LOG_ID = "com.mhlotto.snoozereviews.extra.SLEEP_LOG_ID";
    public static final String EXTRA_NIGHT_DATE = "com.mhlotto.snoozereviews.extra.NIGHT_DATE";

    private static final String TAG = "SleepLogDetailActivity";

    private ActivitySleepLogDetailBinding binding;
    private SleepLogRepository repository;
    private SleepLogDetailFormatter formatter;
    private long sleepLogId;
    private String nightDate;
    private int loadGeneration;
    private boolean destroyed;
    private boolean hasLoadedReport;
    private boolean navigationInProgress;
    private MenuItem editItem;
    private SleepLogDetailViewState currentViewState;
    private ActivityResultLauncher<Intent> editLauncher;

    public static Intent newIntent(Context context, long sleepLogId, String nightDate) {
        Intent intent = new Intent(context, SleepLogDetailActivity.class);
        intent.putExtra(EXTRA_SLEEP_LOG_ID, sleepLogId);
        intent.putExtra(EXTRA_NIGHT_DATE, nightDate);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySleepLogDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(view -> finish());

        repository = new SleepLogRepository(this);
        formatter = new SleepLogDetailFormatter(new AndroidLabelResolver(), Locale.getDefault(), DateFormat.is24HourFormat(this));

        editLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        loadReport(true);
                    }
                }
        );

        sleepLogId = getIntent().getLongExtra(EXTRA_SLEEP_LOG_ID, 0L);
        nightDate = getIntent().getStringExtra(EXTRA_NIGHT_DATE);
        if (sleepLogId <= 0L || !SleepLogFormActivity.isValidNightDate(nightDate)) {
            Log.e(TAG, "Invalid or missing sleep report destination extras");
            showError(getString(R.string.detail_invalid_intent_message), false);
            return;
        }

        binding.retryButton.setOnClickListener(view -> loadReport(hasLoadedReport));
        loadReport(false);
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
    protected void onResume() {
        super.onResume();
        navigationInProgress = false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_sleep_log_detail, menu);
        getMenuInflater().inflate(R.menu.menu_app_navigation, menu);
        editItem = menu.findItem(R.id.action_edit);
        updateEditAvailability();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_edit && hasLoadedReport) {
            editLauncher.launch(SleepLogFormActivity.newEditIntent(this, sleepLogId));
            return true;
        }
        AppNavigation.Destination destination = AppNavigation.destinationForMenuItem(item.getItemId());
        if (destination != null) {
            if (!navigationInProgress) {
                navigationInProgress = AppNavigation.openDestination(this, destination);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadReport(boolean refreshExistingReport) {
        int requestGeneration = ++loadGeneration;
        if (refreshExistingReport && hasLoadedReport) {
            binding.refreshError.setVisibility(View.GONE);
            setEditEnabled(false);
        } else {
            showLoading();
        }

        repository.findSleepLogById(sleepLogId, new SleepLogRepository.Callback<>() {
            @Override
            public void onSuccess(SleepLogWithTags result) {
                if (destroyed || requestGeneration != loadGeneration) {
                    return;
                }
                if (result == null) {
                    hasLoadedReport = false;
                    showError(getString(R.string.detail_not_found_message), true);
                    return;
                }
                try {
                    currentViewState = formatter.format(result);
                    nightDate = currentViewState.getNightDate();
                    renderReport(currentViewState);
                    hasLoadedReport = true;
                    showReport();
                } catch (RuntimeException exception) {
                    Log.e(TAG, "Failed to format sleep report", exception);
                    showLoadFailure(refreshExistingReport);
                }
            }

            @Override
            public void onError(Throwable error) {
                if (destroyed || requestGeneration != loadGeneration) {
                    return;
                }
                Log.e(TAG, "Failed to load sleep report", error);
                showLoadFailure(refreshExistingReport);
            }
        });
    }

    private void showLoadFailure(boolean refreshExistingReport) {
        if (refreshExistingReport && hasLoadedReport) {
            binding.refreshError.setVisibility(View.VISIBLE);
            setEditEnabled(true);
            return;
        }
        showError(getString(R.string.detail_load_error_message), true);
    }

    private void renderReport(SleepLogDetailViewState state) {
        binding.nightDate.setText(state.getFormattedNightDate());
        binding.fellAsleep.setText(getString(R.string.time_set_format, getString(R.string.fell_asleep_label), state.getFellAsleepTime()));
        binding.wokeUp.setText(getString(R.string.time_set_format, getString(R.string.woke_up_label), state.getWokeUpTime()));
        binding.duration.setText(getString(R.string.time_set_format, getString(R.string.duration_label), state.getDuration()));
        binding.location.setText(getString(R.string.time_set_format, getString(R.string.location_label), state.getLocation()));
        binding.sleepRating.setText(getString(R.string.time_set_format, getString(R.string.sleep_quality_label), state.getSleepRating()));
        binding.restedRating.setText(getString(R.string.time_set_format, getString(R.string.rested_after_waking_label), state.getRestedRating()));
        binding.sleptThrough.setText(getString(R.string.time_set_format, getString(R.string.slept_through_label), state.getSleptThroughNight()));
        binding.hadDreams.setText(getString(R.string.time_set_format, getString(R.string.dreams_label), state.getHadDreams()));
        binding.awakeningCount.setText(getString(R.string.time_set_format, getString(R.string.awakening_count_report_label), state.getAwakeningCount()));
        binding.notes.setText(state.getNotes());

        binding.tags.removeAllViews();
        if (state.getTags().isEmpty()) {
            binding.noTags.setVisibility(View.VISIBLE);
            binding.tags.setVisibility(View.GONE);
        } else {
            binding.noTags.setVisibility(View.GONE);
            binding.tags.setVisibility(View.VISIBLE);
            for (TagDisplayItem item : state.getTags()) {
                Chip chip = new Chip(this);
                chip.setText(item.getLabel());
                chip.setCheckable(false);
                chip.setClickable(false);
                chip.setFocusable(false);
                binding.tags.addView(chip);
            }
        }
    }

    private void showLoading() {
        binding.loadingContainer.setVisibility(View.VISIBLE);
        binding.errorContainer.setVisibility(View.GONE);
        binding.reportScroll.setVisibility(View.GONE);
        setEditEnabled(false);
    }

    private void showReport() {
        binding.loadingContainer.setVisibility(View.GONE);
        binding.errorContainer.setVisibility(View.GONE);
        binding.reportScroll.setVisibility(View.VISIBLE);
        binding.refreshError.setVisibility(View.GONE);
        setEditEnabled(true);
    }

    private void showError(String message, boolean allowRetry) {
        binding.loadingContainer.setVisibility(View.GONE);
        binding.reportScroll.setVisibility(View.GONE);
        binding.errorContainer.setVisibility(View.VISIBLE);
        binding.errorMessage.setText(message);
        binding.retryButton.setVisibility(allowRetry ? View.VISIBLE : View.GONE);
        setEditEnabled(false);
    }

    private void updateEditAvailability() {
        setEditEnabled(hasLoadedReport);
    }

    private void setEditEnabled(boolean enabled) {
        if (editItem != null) {
            editItem.setEnabled(enabled);
            editItem.setVisible(enabled);
        }
    }

    private class AndroidLabelResolver implements SleepLogDetailFormatter.LabelResolver {
        @Override
        public String getString(int resId) {
            return SleepLogDetailActivity.this.getString(resId);
        }

        @Override
        public String getString(int resId, Object... args) {
            return SleepLogDetailActivity.this.getString(resId, args);
        }
    }
}
