package com.mhlotto.snoozereviews.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.mhlotto.snoozereviews.R;
import com.mhlotto.snoozereviews.data.backup.ImportPlan;
import com.mhlotto.snoozereviews.data.backup.ImportPlanSummary;
import com.mhlotto.snoozereviews.data.backup.SleepBackupService;
import com.mhlotto.snoozereviews.data.backup.SleepBackupValidationException;
import com.mhlotto.snoozereviews.databinding.ActivityBackupRestoreBinding;
import com.mhlotto.snoozereviews.ui.navigation.AppNavigation;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class BackupRestoreActivity extends AppCompatActivity {
    private static final String TAG = "BackupRestoreActivity";

    private ActivityBackupRestoreBinding binding;
    private SleepBackupService backupService;
    private ActivityResultLauncher<String> exportLauncher;
    private ActivityResultLauncher<String[]> importLauncher;
    private ImportPlan pendingImportPlan;
    private int requestGeneration;
    private boolean destroyed;
    private boolean busy;
    private boolean navigationInProgress;

    public static Intent newIntent(Context context) {
        return new Intent(context, BackupRestoreActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBackupRestoreBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(view -> finish());

        backupService = new SleepBackupService(this);
        exportLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("application/json"),
                this::handleExportUri
        );
        importLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                this::handleImportUri
        );

        binding.exportButton.setOnClickListener(view -> exportLauncher.launch(defaultBackupFilename()));
        binding.importButton.setOnClickListener(view -> importLauncher.launch(new String[]{
                "application/json",
                "text/json",
                "text/plain",
                "*/*"
        }));
        setBusy(false);
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        if (backupService != null) {
            backupService.shutdownBackgroundExecutor();
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
        AppNavigation.hideActiveDestination(menu, AppNavigation.Destination.BACKUP_RESTORE);
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

    private void handleExportUri(Uri uri) {
        if (uri == null || busy) {
            return;
        }
        int generation = ++requestGeneration;
        clearMessage();
        setBusy(true);
        try {
            OutputStream outputStream = getContentResolver().openOutputStream(uri);
            if (outputStream == null) {
                throw new IllegalStateException("Document provider returned no output stream.");
            }
            backupService.exportBackup(outputStream, new SleepBackupService.Callback<>() {
                @Override
                public void onSuccess(SleepBackupService.ExportResult result) {
                    if (destroyed || generation != requestGeneration) {
                        return;
                    }
                    setBusy(false);
                    showMessage(getString(R.string.backup_export_success_format, result.getExportedLogs()), false);
                }

                @Override
                public void onError(Throwable error) {
                    if (destroyed || generation != requestGeneration) {
                        return;
                    }
                    Log.e(TAG, "Failed to export backup", error);
                    setBusy(false);
                    showMessage(getString(R.string.backup_export_error), true);
                }
            });
        } catch (RuntimeException | java.io.IOException exception) {
            Log.e(TAG, "Failed to open backup export stream", exception);
            setBusy(false);
            showMessage(getString(R.string.backup_export_error), true);
        }
    }

    private void handleImportUri(Uri uri) {
        if (uri == null || busy) {
            return;
        }
        int generation = ++requestGeneration;
        clearMessage();
        setBusy(true);
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                throw new IllegalStateException("Document provider returned no input stream.");
            }
            backupService.parseImportPlan(inputStream, new SleepBackupService.Callback<>() {
                @Override
                public void onSuccess(ImportPlan result) {
                    if (destroyed || generation != requestGeneration) {
                        return;
                    }
                    setBusy(false);
                    pendingImportPlan = result;
                    showImportConfirmation(result);
                }

                @Override
                public void onError(Throwable error) {
                    if (destroyed || generation != requestGeneration) {
                        return;
                    }
                    Log.e(TAG, "Failed to read or validate backup", error);
                    setBusy(false);
                    showImportParseError(error);
                }
            });
        } catch (RuntimeException | java.io.IOException exception) {
            Log.e(TAG, "Failed to open backup import stream", exception);
            setBusy(false);
            showMessage(getString(R.string.backup_read_error), true);
        }
    }

    private void showImportConfirmation(ImportPlan plan) {
        ImportPlanSummary summary = plan.getSummary();
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.backup_import_confirm_title_format, summary.getTotalRecords()))
                .setMessage(getString(
                        R.string.backup_import_confirm_message_format,
                        summary.getNewRecords(),
                        summary.getReplacementRecords(),
                        summary.getRetainedLocalRecords()
                ))
                .setPositiveButton(R.string.import_action, (dialog, which) -> applyPendingImport())
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> pendingImportPlan = null)
                .show();
    }

    private void applyPendingImport() {
        if (pendingImportPlan == null || busy) {
            return;
        }
        int generation = ++requestGeneration;
        ImportPlan plan = pendingImportPlan;
        pendingImportPlan = null;
        clearMessage();
        setBusy(true);
        backupService.applyImportPlan(plan, new SleepBackupService.Callback<>() {
            @Override
            public void onSuccess(SleepBackupService.ImportResult result) {
                if (destroyed || generation != requestGeneration) {
                    return;
                }
                setBusy(false);
                ImportPlanSummary summary = result.getSummary();
                showMessage(getString(
                        R.string.backup_import_success_format,
                        summary.getNewRecords(),
                        summary.getReplacementRecords()
                ), false);
            }

            @Override
            public void onError(Throwable error) {
                if (destroyed || generation != requestGeneration) {
                    return;
                }
                Log.e(TAG, "Failed to apply backup import", error);
                setBusy(false);
                showMessage(getString(R.string.backup_database_error), true);
            }
        });
    }

    private void showImportParseError(Throwable error) {
        if (error instanceof SleepBackupValidationException) {
            showMessage(getString(R.string.backup_invalid_document_with_reason_format, error.getMessage()), true);
        } else if (error instanceof java.io.IOException) {
            showMessage(getString(R.string.backup_read_error), true);
        } else {
            showMessage(getString(R.string.backup_invalid_document), true);
        }
    }

    private void setBusy(boolean busy) {
        this.busy = busy;
        binding.exportButton.setEnabled(!busy);
        binding.importButton.setEnabled(!busy);
        binding.progress.setVisibility(busy ? View.VISIBLE : View.GONE);
    }

    private void showMessage(String message, boolean error) {
        binding.resultMessage.setText(message);
        binding.resultMessage.setTextColor(getColor(error ? com.google.android.material.R.color.design_default_color_error : android.R.color.holo_green_dark));
        binding.resultMessage.setVisibility(View.VISIBLE);
    }

    private void clearMessage() {
        binding.resultMessage.setVisibility(View.GONE);
    }

    private String defaultBackupFilename() {
        String date = LocalDate.now(Clock.systemDefaultZone()).format(DateTimeFormatter.ISO_LOCAL_DATE);
        return "snooze-reviews-backup-" + date + ".json";
    }
}
