package com.mhlotto.snoozereviews.ui;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.mhlotto.snoozereviews.R;
import com.mhlotto.snoozereviews.data.SleepLogRepository;
import com.mhlotto.snoozereviews.data.SleepLogWithTags;
import com.mhlotto.snoozereviews.databinding.ActivityAddSleepByDateBinding;
import com.mhlotto.snoozereviews.ui.addbydate.AddByDatePolicy;
import com.mhlotto.snoozereviews.ui.navigation.AppNavigation;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

public class AddSleepByDateActivity extends AppCompatActivity {
    private static final String TAG = "AddSleepByDateActivity";
    private static final String STATE_SELECTED_NIGHT_DATE = "selectedNightDate";
    private static final String STATE_ERROR_VISIBLE = "errorVisible";
    private static final String STATE_DUPLICATE_VISIBLE = "duplicateVisible";

    private ActivityAddSleepByDateBinding binding;
    private SleepLogRepository repository;
    private AddByDatePolicy datePolicy;
    private String selectedNightDate;
    private int lookupGeneration;
    private boolean destroyed;
    private boolean lookupInProgress;
    private boolean navigationInProgress;
    private SleepLogWithTags existingLog;

    public static Intent newIntent(Context context) {
        return new Intent(context, AddSleepByDateActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddSleepByDateBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        SystemBarInsets.applyToRoot(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(view -> finish());

        repository = new SleepLogRepository(this);
        datePolicy = new AddByDatePolicy(Clock.systemDefaultZone());

        selectedNightDate = savedInstanceState == null
                ? datePolicy.defaultNightDate()
                : savedInstanceState.getString(STATE_SELECTED_NIGHT_DATE, datePolicy.defaultNightDate());

        binding.dateButton.setOnClickListener(view -> showDatePicker());
        binding.continueButton.setOnClickListener(view -> continueWithSelectedDate());
        binding.retryButton.setOnClickListener(view -> continueWithSelectedDate());
        binding.viewLogButton.setOnClickListener(view -> openExistingLog());
        binding.chooseAnotherDateButton.setOnClickListener(view -> {
            clearMessages();
            showDatePicker();
        });

        updateDateText();
        if (savedInstanceState != null) {
            binding.lookupError.setVisibility(savedInstanceState.getBoolean(STATE_ERROR_VISIBLE) ? View.VISIBLE : View.GONE);
            binding.retryButton.setVisibility(savedInstanceState.getBoolean(STATE_ERROR_VISIBLE) ? View.VISIBLE : View.GONE);
            binding.existingLogContainer.setVisibility(savedInstanceState.getBoolean(STATE_DUPLICATE_VISIBLE) ? View.VISIBLE : View.GONE);
        }
        updateBusy(false);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_SELECTED_NIGHT_DATE, selectedNightDate);
        outState.putBoolean(STATE_ERROR_VISIBLE, binding.lookupError.getVisibility() == View.VISIBLE);
        outState.putBoolean(STATE_DUPLICATE_VISIBLE, binding.existingLogContainer.getVisibility() == View.VISIBLE);
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
        getMenuInflater().inflate(R.menu.menu_app_navigation, menu);
        AppNavigation.hideActiveDestination(menu, AppNavigation.Destination.ADD_BY_DATE);
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

    private void showDatePicker() {
        LocalDate selected = LocalDate.parse(selectedNightDate);
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedNightDate = LocalDate.of(year, month + 1, dayOfMonth).toString();
                    lookupGeneration++;
                    existingLog = null;
                    clearMessages();
                    updateDateText();
                    updateBusy(false);
                },
                selected.getYear(),
                selected.getMonthValue() - 1,
                selected.getDayOfMonth()
        );
        dialog.getDatePicker().setMaxDate(datePolicy.yesterday()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli());
        dialog.show();
    }

    private void continueWithSelectedDate() {
        if (lookupInProgress) {
            return;
        }
        if (!datePolicy.isAllowedNightDate(selectedNightDate)) {
            Log.e(TAG, "Invalid Add by Date night date");
            showLookupError();
            return;
        }

        clearMessages();
        updateBusy(true);
        int requestGeneration = ++lookupGeneration;
        String requestNightDate = selectedNightDate;
        repository.findSleepLogByNightDate(requestNightDate, new SleepLogRepository.Callback<>() {
            @Override
            public void onSuccess(SleepLogWithTags result) {
                if (destroyed || requestGeneration != lookupGeneration || !requestNightDate.equals(selectedNightDate)) {
                    return;
                }
                updateBusy(false);
                if (result == null) {
                    startActivity(SleepLogFormActivity.newCreateIntent(AddSleepByDateActivity.this, requestNightDate));
                } else {
                    existingLog = result;
                    showExistingLogMessage();
                }
            }

            @Override
            public void onError(Throwable error) {
                if (destroyed || requestGeneration != lookupGeneration || !requestNightDate.equals(selectedNightDate)) {
                    return;
                }
                Log.e(TAG, "Failed to check selected sleep date", error);
                updateBusy(false);
                showLookupError();
            }
        });
    }

    private void openExistingLog() {
        if (existingLog == null || existingLog.getSleepLog() == null) {
            continueWithSelectedDate();
            return;
        }
        if (!navigationInProgress) {
            navigationInProgress = true;
            startActivity(SleepLogDetailActivity.newIntent(
                    this,
                    existingLog.getSleepLog().getId(),
                    existingLog.getSleepLog().getNightDate()
            ));
        }
    }

    private void updateDateText() {
        LocalDate date = LocalDate.parse(selectedNightDate);
        binding.dateButton.setText(date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(Locale.getDefault())));
    }

    private void updateBusy(boolean busy) {
        lookupInProgress = busy;
        binding.continueButton.setEnabled(!busy);
        binding.retryButton.setEnabled(!busy);
        binding.dateButton.setEnabled(!busy);
        binding.progress.setVisibility(busy ? View.VISIBLE : View.GONE);
    }

    private void clearMessages() {
        binding.lookupError.setVisibility(View.GONE);
        binding.retryButton.setVisibility(View.GONE);
        binding.existingLogContainer.setVisibility(View.GONE);
    }

    private void showLookupError() {
        binding.lookupError.setVisibility(View.VISIBLE);
        binding.retryButton.setVisibility(View.VISIBLE);
        binding.existingLogContainer.setVisibility(View.GONE);
    }

    private void showExistingLogMessage() {
        binding.lookupError.setVisibility(View.GONE);
        binding.existingLogContainer.setVisibility(View.VISIBLE);
    }
}
