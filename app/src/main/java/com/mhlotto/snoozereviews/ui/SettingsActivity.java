package com.mhlotto.snoozereviews.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.mhlotto.snoozereviews.R;
import com.mhlotto.snoozereviews.data.entity.CustomSleepLocationEntity;
import com.mhlotto.snoozereviews.data.location.CustomSleepLocationRepository;
import com.mhlotto.snoozereviews.data.location.SleepLocationNameNormalizer;
import com.mhlotto.snoozereviews.databinding.ActivitySettingsBinding;
import com.mhlotto.snoozereviews.ui.detail.SleepLogDetailFormatter;
import com.mhlotto.snoozereviews.ui.form.FormOption;
import com.mhlotto.snoozereviews.ui.form.SleepLogFormCatalog;
import com.mhlotto.snoozereviews.ui.location.SleepLocationLabelResolver;
import com.mhlotto.snoozereviews.ui.navigation.AppNavigation;

import java.util.List;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "SettingsActivity";
    private static final String STATE_LOCATION_TEXT = "locationText";

    private ActivitySettingsBinding binding;
    private CustomSleepLocationRepository repository;
    private boolean destroyed;
    private boolean navigationInProgress;
    private int requestGeneration;

    public static Intent newIntent(Context context) {
        return new Intent(context, SettingsActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        SystemBarInsets.applyToRoot(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(view -> finish());

        SleepLogDetailFormatter.LabelResolver labels = labelResolver();
        repository = new CustomSleepLocationRepository(this, SleepLocationLabelResolver.fixedDuplicateNames(labels));

        renderBuiltInLocations();
        binding.manageTagsButton.setOnClickListener(view -> startActivity(SleepTagSettingsActivity.newIntent(this)));
        binding.addLocationButton.setOnClickListener(view -> addLocation());
        binding.locationNameInput.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addLocation();
                return true;
            }
            return false;
        });

        if (savedInstanceState != null) {
            binding.locationNameInput.setText(savedInstanceState.getString(STATE_LOCATION_TEXT, ""));
        }
        loadCustomLocations();
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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_LOCATION_TEXT, binding.locationNameInput.getText() == null
                ? ""
                : binding.locationNameInput.getText().toString());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_app_navigation, menu);
        AppNavigation.hideActiveDestination(menu, AppNavigation.Destination.SETTINGS);
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

    private void renderBuiltInLocations() {
        binding.builtinLocations.removeAllViews();
        for (FormOption option : SleepLogFormCatalog.LOCATION_OPTIONS) {
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.view_choice_chip, binding.builtinLocations, false);
            chip.setText(option.getLabelResId());
            chip.setCheckable(false);
            chip.setClickable(false);
            chip.setFocusable(false);
            binding.builtinLocations.addView(chip);
        }
    }

    private void loadCustomLocations() {
        int generation = ++requestGeneration;
        repository.listActive(new CustomSleepLocationRepository.Callback<>() {
            @Override
            public void onSuccess(List<CustomSleepLocationEntity> result) {
                if (isInactive(generation)) {
                    return;
                }
                renderCustomLocations(result);
            }

            @Override
            public void onError(Throwable error) {
                if (isInactive(generation)) {
                    return;
                }
                Log.e(TAG, "Failed to load custom sleep locations", error);
                showError();
            }
        });
    }

    private void renderCustomLocations(List<CustomSleepLocationEntity> locations) {
        binding.customLocations.removeAllViews();
        binding.customEmpty.setVisibility(locations.isEmpty() ? View.VISIBLE : View.GONE);
        for (CustomSleepLocationEntity location : locations) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(0, getResources().getDimensionPixelSize(R.dimen.spacing_small), 0, 0);

            TextView label = new TextView(this);
            label.setText(location.getDisplayName());
            label.setTextAppearance(R.style.TextAppearance_SnoozeReviews_Body);
            label.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurface));
            row.addView(label, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            MaterialButton remove = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            remove.setText(R.string.remove);
            remove.setMinHeight(getResources().getDimensionPixelSize(R.dimen.touch_target_min));
            remove.setOnClickListener(view -> confirmRemove(location));
            row.addView(remove);
            binding.customLocations.addView(row);
        }
    }

    private void addLocation() {
        binding.locationNameLayout.setError(null);
        binding.settingsError.setVisibility(View.GONE);
        String value = binding.locationNameInput.getText() == null ? null : binding.locationNameInput.getText().toString();
        try {
            SleepLocationNameNormalizer.clean(value);
        } catch (IllegalArgumentException exception) {
            binding.locationNameLayout.setError(getString(R.string.settings_location_invalid));
            return;
        }

        setAdding(true);
        int generation = ++requestGeneration;
        repository.add(value, new CustomSleepLocationRepository.Callback<>() {
            @Override
            public void onSuccess(CustomSleepLocationRepository.AddResult result) {
                if (isInactive(generation)) {
                    return;
                }
                setAdding(false);
                if (result.getStatus() == CustomSleepLocationRepository.AddStatus.ADDED) {
                    binding.locationNameInput.setText("");
                    loadCustomLocations();
                } else if (result.getStatus() == CustomSleepLocationRepository.AddStatus.DUPLICATE_INACTIVE) {
                    confirmRestore(result.getLocation());
                } else {
                    binding.locationNameLayout.setError(getString(R.string.settings_location_duplicate));
                }
            }

            @Override
            public void onError(Throwable error) {
                if (isInactive(generation)) {
                    return;
                }
                Log.e(TAG, "Failed to add custom sleep location", error);
                setAdding(false);
                showError();
            }
        });
    }

    private void confirmRemove(CustomSleepLocationEntity location) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.settings_remove_location_title_format, location.getDisplayName()))
                .setMessage(R.string.settings_remove_location_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.remove, (dialog, which) -> removeLocation(location))
                .show();
    }

    private void removeLocation(CustomSleepLocationEntity location) {
        int generation = ++requestGeneration;
        repository.remove(location.getLocationKey(), new CustomSleepLocationRepository.Callback<>() {
            @Override
            public void onSuccess(Void result) {
                if (!isInactive(generation)) {
                    loadCustomLocations();
                }
            }

            @Override
            public void onError(Throwable error) {
                if (isInactive(generation)) {
                    return;
                }
                Log.e(TAG, "Failed to remove custom sleep location", error);
                showError();
            }
        });
    }

    private void confirmRestore(CustomSleepLocationEntity location) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.settings_restore_location_title_format, location.getDisplayName()))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.restore, (dialog, which) -> restoreLocation(location))
                .show();
    }

    private void restoreLocation(CustomSleepLocationEntity location) {
        int generation = ++requestGeneration;
        repository.reactivate(location.getLocationKey(), new CustomSleepLocationRepository.Callback<>() {
            @Override
            public void onSuccess(CustomSleepLocationEntity result) {
                if (!isInactive(generation)) {
                    binding.locationNameInput.setText("");
                    loadCustomLocations();
                }
            }

            @Override
            public void onError(Throwable error) {
                if (isInactive(generation)) {
                    return;
                }
                Log.e(TAG, "Failed to restore custom sleep location", error);
                showError();
            }
        });
    }

    private void setAdding(boolean adding) {
        binding.addLocationButton.setEnabled(!adding);
        binding.addLocationButton.setText(adding ? R.string.settings_adding_location : R.string.settings_add_location);
    }

    private void showError() {
        binding.settingsError.setText(R.string.settings_location_error);
        binding.settingsError.setVisibility(View.VISIBLE);
    }

    private boolean isInactive(int generation) {
        return destroyed || isFinishing() || generation != requestGeneration;
    }

    private SleepLogDetailFormatter.LabelResolver labelResolver() {
        return new SleepLogDetailFormatter.LabelResolver() {
            @Override
            public String getString(int resId) {
                return SettingsActivity.this.getString(resId);
            }

            @Override
            public String getString(int resId, Object... args) {
                return SettingsActivity.this.getString(resId, args);
            }
        };
    }

    private int resolveColor(int attr) {
        android.util.TypedValue typedValue = new android.util.TypedValue();
        getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }
}
