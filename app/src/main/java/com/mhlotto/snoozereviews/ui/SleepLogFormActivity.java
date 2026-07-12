package com.mhlotto.snoozereviews.ui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteConstraintException;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputLayout;
import com.mhlotto.snoozereviews.R;
import com.mhlotto.snoozereviews.data.SleepLogRepository;
import com.mhlotto.snoozereviews.data.SleepLogWithTags;
import com.mhlotto.snoozereviews.data.entity.CustomSleepLocationEntity;
import com.mhlotto.snoozereviews.data.entity.SleepLogEntity;
import com.mhlotto.snoozereviews.data.location.CustomLocationKey;
import com.mhlotto.snoozereviews.data.location.CustomSleepLocationRepository;
import com.mhlotto.snoozereviews.databinding.ActivitySleepLogFormBinding;
import com.mhlotto.snoozereviews.ui.detail.SleepLogDetailFormatter;
import com.mhlotto.snoozereviews.ui.form.FormOption;
import com.mhlotto.snoozereviews.ui.form.FormInputParser;
import com.mhlotto.snoozereviews.ui.form.NightDatePolicy;
import com.mhlotto.snoozereviews.ui.form.SleepLogFormCatalog;
import com.mhlotto.snoozereviews.ui.form.SleepLogFormState;
import com.mhlotto.snoozereviews.ui.form.TagCategory;
import com.mhlotto.snoozereviews.ui.form.TimeOfDayHelper;
import com.mhlotto.snoozereviews.ui.location.SleepLocationLabelResolver;
import com.mhlotto.snoozereviews.ui.navigation.AppNavigation;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SleepLogFormActivity extends AppCompatActivity {
    public static final String EXTRA_NIGHT_DATE = "com.mhlotto.snoozereviews.extra.NIGHT_DATE";
    public static final String EXTRA_SLEEP_LOG_ID = "com.mhlotto.snoozereviews.extra.SLEEP_LOG_ID";
    public static final String EXTRA_RESULT_SLEEP_LOG_ID = "com.mhlotto.snoozereviews.extra.RESULT_SLEEP_LOG_ID";
    public static final String EXTRA_RESULT_NIGHT_DATE = "com.mhlotto.snoozereviews.extra.RESULT_NIGHT_DATE";

    private static final String TAG = "SleepLogFormActivity";
    private static final String STATE_MODE = "mode";
    private static final String STATE_LOAD_GENERATION = "loadGeneration";
    private static final String STATE_SAVE_GENERATION = "saveGeneration";
    private static final String STATE_IS_SAVING = "isSaving";
    private static final String STATE_CURRENT_PREFIX = "current.";
    private static final String STATE_INITIAL_PREFIX = "initial.";
    private static final String MODE_CREATE = "create";
    private static final String MODE_EDIT = "edit";

    private ActivitySleepLogFormBinding binding;
    private SleepLogRepository repository;
    private CustomSleepLocationRepository customLocationRepository;
    private NightDatePolicy nightDatePolicy;
    private SleepLogFormState currentState;
    private SleepLogFormState initialState;
    private String mode;
    private long editSleepLogId;
    private int loadGeneration;
    private int saveGeneration;
    private boolean isSaving;
    private boolean destroyed;
    private boolean suppressChangeCallbacks;
    private boolean navigationInProgress;
    private boolean discardDialogShowing;
    private List<CustomSleepLocationEntity> activeCustomLocations = new ArrayList<>();

    public static Intent newCreateIntent(Context context, String nightDate) {
        Intent intent = new Intent(context, SleepLogFormActivity.class);
        intent.putExtra(EXTRA_NIGHT_DATE, nightDate);
        return intent;
    }

    public static Intent newEditIntent(Context context, long sleepLogId) {
        Intent intent = new Intent(context, SleepLogFormActivity.class);
        intent.putExtra(EXTRA_SLEEP_LOG_ID, sleepLogId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySleepLogFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        SystemBarInsets.applyToRoot(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        repository = new SleepLogRepository(this);
        SleepLogDetailFormatter.LabelResolver labels = labelResolver();
        customLocationRepository = new CustomSleepLocationRepository(this, SleepLocationLabelResolver.fixedDuplicateNames(labels));
        nightDatePolicy = new NightDatePolicy(Clock.systemDefaultZone());

        buildChoiceControls();
        loadCustomLocations();
        wireEvents();
        configureBackHandling();

        if (savedInstanceState != null) {
            restoreState(savedInstanceState);
            renderFromState();
            showForm();
            setSaving(isSaving);
        } else {
            initializeFromIntent();
        }
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        if (repository != null) {
            repository.shutdownBackgroundExecutor();
        }
        if (customLocationRepository != null) {
            customLocationRepository.shutdownBackgroundExecutor();
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
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        AppNavigation.Destination destination = AppNavigation.destinationForMenuItem(item.getItemId());
        if (destination != null) {
            openDestinationWithUnsavedProtection(destination);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_MODE, mode);
        outState.putLong(EXTRA_SLEEP_LOG_ID, editSleepLogId);
        outState.putInt(STATE_LOAD_GENERATION, loadGeneration);
        outState.putInt(STATE_SAVE_GENERATION, saveGeneration);
        outState.putBoolean(STATE_IS_SAVING, isSaving);
        putState(outState, STATE_CURRENT_PREFIX, collectStateFromViews(false));
        putState(outState, STATE_INITIAL_PREFIX, initialState);
    }

    private void initializeFromIntent() {
        editSleepLogId = getIntent().getLongExtra(EXTRA_SLEEP_LOG_ID, 0L);
        String nightDate = getIntent().getStringExtra(EXTRA_NIGHT_DATE);
        if (editSleepLogId > 0L) {
            mode = MODE_EDIT;
            binding.toolbar.setTitle(R.string.edit_sleep_log);
            loadEditRecord();
        } else if (isValidNightDate(nightDate)) {
            mode = MODE_CREATE;
            binding.toolbar.setTitle(R.string.log_sleep);
            currentState = SleepLogFormState.create(nightDate);
            initialState = new SleepLogFormState(currentState);
            renderFromState();
            showForm();
        } else {
            Log.e(TAG, "Invalid form launch extras");
            mode = MODE_CREATE;
            showLoadError(getString(R.string.error_invalid_mode), false);
        }
    }

    private void loadEditRecord() {
        int requestGeneration = ++loadGeneration;
        showLoading();
        repository.findSleepLogById(editSleepLogId, new SleepLogRepository.Callback<>() {
            @Override
            public void onSuccess(SleepLogWithTags result) {
                if (isInactive() || requestGeneration != loadGeneration) {
                    return;
                }
                if (result == null) {
                    showLoadError(getString(R.string.form_missing_log_message), true);
                    return;
                }
                currentState = SleepLogFormState.fromSleepLogWithTags(result);
                initialState = new SleepLogFormState(currentState);
                renderFromState();
                showForm();
            }

            @Override
            public void onError(Throwable error) {
                if (isInactive() || requestGeneration != loadGeneration) {
                    return;
                }
                Log.e(TAG, "Failed to load sleep log", error);
                showLoadError(getString(R.string.form_load_error_message), true);
            }
        });
    }

    private void buildChoiceControls() {
        addLocationChips(null);
        addTriStateChips(binding.sleptThroughChipGroup);
        addTriStateChips(binding.hadDreamsChipGroup);
        addRatingChips(binding.sleepRatingChipGroup);
        addRatingChips(binding.restedRatingChipGroup);
        addTagSections(new HashSet<>());
    }

    private void addLocationChips(String unknownLocationKey) {
        binding.locationChipGroup.removeAllViews();
        addChoiceChip(binding.locationChipGroup, null, getString(R.string.sleep_location_not_specified), true);
        for (FormOption option : SleepLogFormCatalog.LOCATION_OPTIONS) {
            addChoiceChip(binding.locationChipGroup, option.getKey(), getString(option.getLabelResId()), true);
        }
        for (CustomSleepLocationEntity customLocation : activeCustomLocations) {
            addChoiceChip(binding.locationChipGroup, customLocation.getLocationKey(), customLocation.getDisplayName(), true);
        }
        if (unknownLocationKey != null) {
            String label = CustomLocationKey.isCustomKey(unknownLocationKey)
                    ? customLocationFallbackLabel(unknownLocationKey)
                    : getString(R.string.unknown_location_format, unknownLocationKey);
            addChoiceChip(binding.locationChipGroup, unknownLocationKey, label, true);
        }
    }

    private void loadCustomLocations() {
        customLocationRepository.listActive(new CustomSleepLocationRepository.Callback<>() {
            @Override
            public void onSuccess(List<CustomSleepLocationEntity> result) {
                if (isInactive()) {
                    return;
                }
                activeCustomLocations = new ArrayList<>(result);
                if (currentState != null) {
                    renderFromState();
                }
            }

            @Override
            public void onError(Throwable error) {
                if (!isInactive()) {
                    Log.e(TAG, "Failed to load custom sleep locations", error);
                }
            }
        });
    }

    private void addTriStateChips(ChipGroup chipGroup) {
        addChoiceChip(chipGroup, null, getString(R.string.answer_not_answered), true);
        addChoiceChip(chipGroup, Boolean.TRUE, getString(R.string.answer_yes), true);
        addChoiceChip(chipGroup, Boolean.FALSE, getString(R.string.answer_no), true);
    }

    private void addRatingChips(ChipGroup chipGroup) {
        addChoiceChip(chipGroup, null, getString(R.string.not_rated), true);
        for (int rating = 1; rating <= 5; rating++) {
            addChoiceChip(chipGroup, rating, getString(R.string.rating_value, rating), true);
        }
    }

    private void addTagSections(Set<String> unknownSelectedTags) {
        binding.tagSectionsContainer.removeAllViews();
        Set<String> knownKeys = new HashSet<>();
        for (TagCategory category : SleepLogFormCatalog.TAG_CATEGORIES) {
            TextView heading = new TextView(this);
            heading.setText(category.getTitleResId());
            heading.setTextAppearance(R.style.TextAppearance_SnoozeReviews_Label);
            heading.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurface));
            heading.setPadding(0, getResources().getDimensionPixelSize(R.dimen.spacing_small), 0, 0);
            binding.tagSectionsContainer.addView(heading);

            ChipGroup chipGroup = new ChipGroup(this);
            chipGroup.setSingleSelection(false);
            applyChipGroupSpacing(chipGroup);
            chipGroup.setTag("tagGroup");
            binding.tagSectionsContainer.addView(chipGroup, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            for (FormOption option : category.getOptions()) {
                knownKeys.add(option.getKey());
                addChoiceChip(chipGroup, option.getKey(), getString(option.getLabelResId()), false);
            }
        }

        for (String tagKey : unknownSelectedTags) {
            if (!knownKeys.contains(tagKey)) {
                ChipGroup chipGroup = findOrCreateUnknownTagGroup();
                addChoiceChip(chipGroup, tagKey, getString(R.string.unknown_tag_format, tagKey), false);
            }
        }
    }

    private ChipGroup findOrCreateUnknownTagGroup() {
        int childCount = binding.tagSectionsContainer.getChildCount();
        if (childCount > 0) {
            View last = binding.tagSectionsContainer.getChildAt(childCount - 1);
            if (last instanceof ChipGroup && "unknownTagGroup".equals(last.getTag())) {
                return (ChipGroup) last;
            }
        }
        TextView heading = new TextView(this);
        heading.setText(getString(R.string.unknown_tag_format, ""));
        heading.setTextAppearance(R.style.TextAppearance_SnoozeReviews_Label);
        heading.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurface));
        binding.tagSectionsContainer.addView(heading);
        ChipGroup chipGroup = new ChipGroup(this);
        chipGroup.setSingleSelection(false);
        applyChipGroupSpacing(chipGroup);
        chipGroup.setTag("unknownTagGroup");
        binding.tagSectionsContainer.addView(chipGroup);
        return chipGroup;
    }

    private Chip addChoiceChip(ChipGroup chipGroup, Object value, String label, boolean singleChoice) {
        Chip chip = (Chip) getLayoutInflater().inflate(R.layout.view_choice_chip, chipGroup, false);
        chip.setId(View.generateViewId());
        chip.setText(label);
        chip.setCheckable(true);
        chip.setClickable(true);
        chip.setTag(value);
        chip.setMinHeight(getResources().getDimensionPixelSize(R.dimen.touch_target_min));
        chipGroup.addView(chip);
        if (singleChoice && chipGroup.getCheckedChipId() == View.NO_ID) {
            chip.setChecked(true);
        }
        return chip;
    }

    private void applyChipGroupSpacing(ChipGroup chipGroup) {
        chipGroup.setChipSpacingHorizontalResource(R.dimen.chip_spacing_horizontal);
        chipGroup.setChipSpacingVerticalResource(R.dimen.chip_spacing_vertical);
    }

    private void wireEvents() {
        binding.toolbar.setNavigationOnClickListener(view -> handleBack());
        binding.retryButton.setOnClickListener(view -> loadEditRecord());
        binding.saveButton.setOnClickListener(view -> save());
        binding.nightDateInput.setOnClickListener(view -> showDatePicker());
        binding.fellAsleepButton.setOnClickListener(view -> showTimePicker(true));
        binding.wokeUpButton.setOnClickListener(view -> showTimePicker(false));
        binding.clearFellAsleepButton.setOnClickListener(view -> {
            currentState.setFellAsleepMinute(null);
            updateTimeButtons();
        });
        binding.clearWokeUpButton.setOnClickListener(view -> {
            currentState.setWokeUpMinute(null);
            updateTimeButtons();
        });
        TextWatcher watcher = new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                if (!suppressChangeCallbacks) {
                    binding.awakeningCountLayout.setError(null);
                    binding.saveError.setVisibility(View.GONE);
                }
            }
        };
        binding.awakeningCountInput.addTextChangedListener(watcher);
        binding.awakeningCountInput.setImeOptions(EditorInfo.IME_ACTION_DONE);
    }

    private void configureBackHandling() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBack();
            }
        });
    }

    private void showDatePicker() {
        LocalDate selected = LocalDate.parse(currentState.getNightDate());
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    LocalDate date = LocalDate.of(year, month + 1, dayOfMonth);
                    currentState.setNightDate(date.toString());
                    updateDateInput();
                    binding.nightDateLayout.setError(null);
                },
                selected.getYear(),
                selected.getMonthValue() - 1,
                selected.getDayOfMonth()
        );
        dialog.getDatePicker().setMaxDate(LocalDate.now(Clock.systemDefaultZone())
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli());
        dialog.show();
    }

    private void showTimePicker(boolean fellAsleep) {
        Integer value = fellAsleep ? currentState.getFellAsleepMinute() : currentState.getWokeUpMinute();
        int hour = value == null ? 22 : TimeOfDayHelper.hourOfDay(value);
        int minute = value == null ? 0 : TimeOfDayHelper.minute(value);
        boolean use24Hour = DateFormat.is24HourFormat(this);
        TimePickerDialog dialog = new TimePickerDialog(
                this,
                (view, hourOfDay, selectedMinute) -> {
                    int minuteOfDay = TimeOfDayHelper.toMinuteOfDay(hourOfDay, selectedMinute);
                    if (fellAsleep) {
                        currentState.setFellAsleepMinute(minuteOfDay);
                    } else {
                        currentState.setWokeUpMinute(minuteOfDay);
                    }
                    updateTimeButtons();
                },
                hour,
                minute,
                use24Hour
        );
        dialog.show();
    }

    private void save() {
        if (isSaving) {
            return;
        }

        SleepLogFormState state;
        try {
            state = collectStateFromViews(true);
            validateFormState(state);
        } catch (IllegalArgumentException exception) {
            return;
        }

        setSaving(true);
        binding.saveError.setVisibility(View.GONE);
        SleepLogEntity entity = state.toEntityForSave();
        List<String> tagKeys = state.getSelectedTagKeys();
        int requestGeneration = ++saveGeneration;

        if (MODE_CREATE.equals(mode)) {
            repository.createSleepLog(entity, tagKeys, new SleepLogRepository.Callback<>() {
                @Override
                public void onSuccess(Long result) {
                    if (isInactive() || requestGeneration != saveGeneration) {
                        return;
                    }
                    setSaving(false);
                    setResultOk(result, state.getNightDate());
                    startActivity(SleepLogDetailActivity.newIntent(SleepLogFormActivity.this, result, state.getNightDate()));
                    finish();
                }

                @Override
                public void onError(Throwable error) {
                    handleSaveError(error, requestGeneration);
                }
            });
        } else {
            entity.setId(editSleepLogId);
            repository.updateSleepLog(entity, tagKeys, new SleepLogRepository.Callback<>() {
                @Override
                public void onSuccess(Void result) {
                    if (isInactive() || requestGeneration != saveGeneration) {
                        return;
                    }
                    setSaving(false);
                    setResultOk(editSleepLogId, state.getNightDate());
                    finish();
                }

                @Override
                public void onError(Throwable error) {
                    handleSaveError(error, requestGeneration);
                }
            });
        }
    }

    private void handleSaveError(Throwable error, int requestGeneration) {
        if (isInactive() || requestGeneration != saveGeneration) {
            return;
        }
        Log.e(TAG, "Failed to save sleep log", error);
        setSaving(false);
        binding.saveError.setText(isUniqueConstraintFailure(error)
                ? R.string.error_duplicate_night_date
                : R.string.error_save_failed);
        binding.saveError.setVisibility(View.VISIBLE);
    }

    private boolean isUniqueConstraintFailure(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SQLiteConstraintException) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && message.toLowerCase(Locale.US).contains("unique")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private SleepLogFormState collectStateFromViews(boolean showErrors) {
        SleepLogFormState state = new SleepLogFormState(currentState);
        state.setNightDate(currentState.getNightDate());
        state.setSleepLocationKey(getCheckedTag(binding.locationChipGroup, String.class));
        state.setSleptThroughNight(getCheckedTag(binding.sleptThroughChipGroup, Boolean.class));
        state.setHadDreams(getCheckedTag(binding.hadDreamsChipGroup, Boolean.class));
        state.setSleepRating(getCheckedTag(binding.sleepRatingChipGroup, Integer.class));
        state.setRestedRating(getCheckedTag(binding.restedRatingChipGroup, Integer.class));
        state.setAwakeningCount(parseAwakeningCount(showErrors));
        state.setNotes(getText(binding.notesInput.getText()));
        state.setSelectedTagKeys(collectSelectedTagKeys());
        return state;
    }

    private Integer parseAwakeningCount(boolean showErrors) {
        String text = getText(binding.awakeningCountInput.getText());
        try {
            Integer value = FormInputParser.parseAwakeningCount(text);
            binding.awakeningCountLayout.setError(null);
            return value;
        } catch (IllegalArgumentException exception) {
            if (showErrors) {
                binding.awakeningCountLayout.setError(getString(R.string.error_invalid_awakening_count));
                focusField(binding.awakeningCountLayout);
            }
            throw new IllegalArgumentException("invalid awakening count", exception);
        }
    }

    private void validateFormState(SleepLogFormState state) {
        binding.nightDateLayout.setError(null);
        if (!nightDatePolicy.isValidIsoDate(state.getNightDate())) {
            binding.nightDateLayout.setError(getString(R.string.error_invalid_date));
            focusField(binding.nightDateLayout);
            throw new IllegalArgumentException("invalid date");
        }
        if (!nightDatePolicy.isTodayOrPast(state.getNightDate())) {
            binding.nightDateLayout.setError(getString(R.string.error_future_date));
            focusField(binding.nightDateLayout);
            throw new IllegalArgumentException("future date");
        }
        state.toEntityForSave();
    }

    private void renderFromState() {
        if (currentState == null) {
            return;
        }
        suppressChangeCallbacks = true;
        addLocationChips(isAvailableLocation(currentState.getSleepLocationKey()) ? null : currentState.getSleepLocationKey());
        addTagSections(unknownTagKeys(currentState.getSelectedTagKeys()));
        updateDateInput();
        updateTimeButtons();
        checkChipForTag(binding.locationChipGroup, currentState.getSleepLocationKey());
        checkChipForTag(binding.sleptThroughChipGroup, currentState.getSleptThroughNight());
        checkChipForTag(binding.hadDreamsChipGroup, currentState.getHadDreams());
        checkChipForTag(binding.sleepRatingChipGroup, currentState.getSleepRating());
        checkChipForTag(binding.restedRatingChipGroup, currentState.getRestedRating());
        binding.awakeningCountInput.setText(currentState.getAwakeningCount() == null ? "" : String.valueOf(currentState.getAwakeningCount()));
        binding.notesInput.setText(currentState.getNotes() == null ? "" : currentState.getNotes());
        checkSelectedTags(currentState.getSelectedTagKeys());
        suppressChangeCallbacks = false;
    }

    private void updateDateInput() {
        LocalDate date = LocalDate.parse(currentState.getNightDate());
        String formatted = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()));
        binding.nightDateInput.setText(formatted);
    }

    private void updateTimeButtons() {
        updateTimeButton(binding.fellAsleepButton, R.string.fell_asleep_time, currentState.getFellAsleepMinute());
        updateTimeButton(binding.wokeUpButton, R.string.woke_up_time, currentState.getWokeUpMinute());
    }

    private void updateTimeButton(com.google.android.material.button.MaterialButton button, int labelResId, Integer minute) {
        String label = getString(labelResId);
        if (minute == null) {
            button.setText(getString(R.string.time_not_set, label));
        } else {
            button.setText(getString(R.string.time_set_format, label,
                    TimeOfDayHelper.formatMinuteOfDay(minute, Locale.getDefault(), DateFormat.is24HourFormat(this))));
        }
    }

    private void showLoading() {
        binding.loadingContainer.setVisibility(View.VISIBLE);
        binding.errorContainer.setVisibility(View.GONE);
        binding.formScroll.setVisibility(View.GONE);
        binding.saveButton.setEnabled(false);
    }

    private void showForm() {
        binding.loadingContainer.setVisibility(View.GONE);
        binding.errorContainer.setVisibility(View.GONE);
        binding.formScroll.setVisibility(View.VISIBLE);
        binding.saveButton.setEnabled(true);
    }

    private void showLoadError(String message, boolean allowRetry) {
        binding.loadingContainer.setVisibility(View.GONE);
        binding.formScroll.setVisibility(View.GONE);
        binding.errorContainer.setVisibility(View.VISIBLE);
        binding.errorMessage.setText(message);
        binding.retryButton.setVisibility(allowRetry ? View.VISIBLE : View.GONE);
    }

    private void setSaving(boolean saving) {
        isSaving = saving;
        binding.saveButton.setEnabled(!saving);
        binding.saveButton.setText(saving ? R.string.saving_sleep_log : R.string.save_sleep_log);
        binding.saveProgress.setVisibility(saving ? View.VISIBLE : View.GONE);
    }

    private void handleBack() {
        if (isSaving) {
            return;
        }
        confirmDiscardIfNeeded(this::finish);
    }

    private void openDestinationWithUnsavedProtection(AppNavigation.Destination destination) {
        if (isSaving) {
            return;
        }
        confirmDiscardIfNeeded(() -> {
            if (!navigationInProgress) {
                navigationInProgress = AppNavigation.openDestination(this, destination);
            }
        });
    }

    private void confirmDiscardIfNeeded(Runnable onDiscardOrClean) {
        if (discardDialogShowing) {
            return;
        }
        SleepLogFormState latest = currentState;
        if (binding.formScroll.getVisibility() == View.VISIBLE) {
            try {
                latest = collectStateFromViews(false);
            } catch (IllegalArgumentException exception) {
                latest = null;
            }
        }
        if (latest != null && initialState != null && latest.isDirtyComparedTo(initialState)) {
            discardDialogShowing = true;
            new AlertDialog.Builder(this)
                    .setTitle(R.string.discard_changes_title)
                    .setMessage(R.string.discard_changes_message)
                    .setPositiveButton(R.string.discard, (dialog, which) -> {
                        discardDialogShowing = false;
                        onDiscardOrClean.run();
                    })
                    .setNegativeButton(R.string.keep_editing, null)
                    .setOnDismissListener(dialog -> discardDialogShowing = false)
                    .show();
        } else {
            onDiscardOrClean.run();
        }
    }

    private void setResultOk(long sleepLogId, String nightDate) {
        Intent result = new Intent()
                .putExtra(EXTRA_RESULT_SLEEP_LOG_ID, sleepLogId)
                .putExtra(EXTRA_RESULT_NIGHT_DATE, nightDate);
        setResult(RESULT_OK, result);
    }

    private void restoreState(Bundle bundle) {
        mode = bundle.getString(STATE_MODE, MODE_CREATE);
        editSleepLogId = bundle.getLong(EXTRA_SLEEP_LOG_ID, 0L);
        loadGeneration = bundle.getInt(STATE_LOAD_GENERATION, 0);
        saveGeneration = bundle.getInt(STATE_SAVE_GENERATION, 0);
        isSaving = false;
        currentState = getState(bundle, STATE_CURRENT_PREFIX);
        initialState = getState(bundle, STATE_INITIAL_PREFIX);
        binding.toolbar.setTitle(MODE_EDIT.equals(mode) ? R.string.edit_sleep_log : R.string.log_sleep);
    }

    private void putState(Bundle bundle, String prefix, SleepLogFormState state) {
        if (state == null) {
            return;
        }
        bundle.putLong(prefix + "id", state.getId());
        bundle.putString(prefix + "nightDate", state.getNightDate());
        bundle.putString(prefix + "sleepLocationKey", state.getSleepLocationKey());
        putNullableInteger(bundle, prefix + "fellAsleepMinute", state.getFellAsleepMinute());
        putNullableInteger(bundle, prefix + "wokeUpMinute", state.getWokeUpMinute());
        putNullableBoolean(bundle, prefix + "sleptThroughNight", state.getSleptThroughNight());
        putNullableBoolean(bundle, prefix + "hadDreams", state.getHadDreams());
        putNullableInteger(bundle, prefix + "sleepRating", state.getSleepRating());
        putNullableInteger(bundle, prefix + "restedRating", state.getRestedRating());
        putNullableInteger(bundle, prefix + "awakeningCount", state.getAwakeningCount());
        bundle.putString(prefix + "notes", state.getNotes());
        bundle.putStringArrayList(prefix + "selectedTagKeys", state.getSelectedTagKeys());
    }

    private SleepLogFormState getState(Bundle bundle, String prefix) {
        SleepLogFormState state = new SleepLogFormState();
        state.setId(bundle.getLong(prefix + "id", 0L));
        state.setNightDate(bundle.getString(prefix + "nightDate"));
        state.setSleepLocationKey(bundle.getString(prefix + "sleepLocationKey"));
        state.setFellAsleepMinute(getNullableInteger(bundle, prefix + "fellAsleepMinute"));
        state.setWokeUpMinute(getNullableInteger(bundle, prefix + "wokeUpMinute"));
        state.setSleptThroughNight(getNullableBoolean(bundle, prefix + "sleptThroughNight"));
        state.setHadDreams(getNullableBoolean(bundle, prefix + "hadDreams"));
        state.setSleepRating(getNullableInteger(bundle, prefix + "sleepRating"));
        state.setRestedRating(getNullableInteger(bundle, prefix + "restedRating"));
        state.setAwakeningCount(getNullableInteger(bundle, prefix + "awakeningCount"));
        state.setNotes(bundle.getString(prefix + "notes"));
        state.setSelectedTagKeysFromRestored(bundle.getStringArrayList(prefix + "selectedTagKeys"));
        return state;
    }

    private void putNullableInteger(Bundle bundle, String key, Integer value) {
        bundle.putBoolean(key + ".present", value != null);
        if (value != null) {
            bundle.putInt(key, value);
        }
    }

    private Integer getNullableInteger(Bundle bundle, String key) {
        return bundle.getBoolean(key + ".present", false) ? bundle.getInt(key) : null;
    }

    private void putNullableBoolean(Bundle bundle, String key, Boolean value) {
        bundle.putBoolean(key + ".present", value != null);
        if (value != null) {
            bundle.putBoolean(key, value);
        }
    }

    private Boolean getNullableBoolean(Bundle bundle, String key) {
        return bundle.getBoolean(key + ".present", false) ? bundle.getBoolean(key) : null;
    }

    private List<String> collectSelectedTagKeys() {
        List<String> selected = new ArrayList<>();
        collectSelectedTags(binding.tagSectionsContainer, selected);
        return selected;
    }

    private void collectSelectedTags(LinearLayout container, List<String> selected) {
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child instanceof ChipGroup) {
                ChipGroup chipGroup = (ChipGroup) child;
                for (int j = 0; j < chipGroup.getChildCount(); j++) {
                    View chipView = chipGroup.getChildAt(j);
                    if (chipView instanceof Chip && ((Chip) chipView).isChecked()) {
                        Object tag = chipView.getTag();
                        if (tag instanceof String) {
                            selected.add((String) tag);
                        }
                    }
                }
            }
        }
    }

    private void checkSelectedTags(List<String> selectedTagKeys) {
        Set<String> selected = new HashSet<>(selectedTagKeys);
        for (int i = 0; i < binding.tagSectionsContainer.getChildCount(); i++) {
            View child = binding.tagSectionsContainer.getChildAt(i);
            if (child instanceof ChipGroup) {
                ChipGroup chipGroup = (ChipGroup) child;
                for (int j = 0; j < chipGroup.getChildCount(); j++) {
                    View chipView = chipGroup.getChildAt(j);
                    if (chipView instanceof Chip) {
                        ((Chip) chipView).setChecked(selected.contains(chipView.getTag()));
                    }
                }
            }
        }
    }

    private <T> T getCheckedTag(ChipGroup chipGroup, Class<T> type) {
        int checkedId = chipGroup.getCheckedChipId();
        if (checkedId == View.NO_ID) {
            return null;
        }
        View checked = chipGroup.findViewById(checkedId);
        Object tag = checked == null ? null : checked.getTag();
        return type.isInstance(tag) ? type.cast(tag) : null;
    }

    private void checkChipForTag(ChipGroup chipGroup, Object tag) {
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            View child = chipGroup.getChildAt(i);
            if (child instanceof Chip && (tag == null ? child.getTag() == null : tag.equals(child.getTag()))) {
                ((Chip) child).setChecked(true);
                return;
            }
        }
    }

    private boolean isAvailableLocation(String key) {
        if (key == null) {
            return true;
        }
        for (FormOption option : SleepLogFormCatalog.LOCATION_OPTIONS) {
            if (key.equals(option.getKey())) {
                return true;
            }
        }
        for (CustomSleepLocationEntity location : activeCustomLocations) {
            if (key.equals(location.getLocationKey())) {
                return true;
            }
        }
        return false;
    }

    private String customLocationFallbackLabel(String key) {
        String decoded = CustomLocationKey.decode(key);
        if (decoded == null) {
            return getString(R.string.unknown_location_format, key);
        }
        return getString(R.string.removed_location_format, decoded);
    }

    private SleepLogDetailFormatter.LabelResolver labelResolver() {
        return new SleepLogDetailFormatter.LabelResolver() {
            @Override
            public String getString(int resId) {
                return SleepLogFormActivity.this.getString(resId);
            }

            @Override
            public String getString(int resId, Object... args) {
                return SleepLogFormActivity.this.getString(resId, args);
            }
        };
    }

    private Set<String> unknownTagKeys(List<String> selectedTags) {
        Set<String> known = new HashSet<>();
        for (TagCategory category : SleepLogFormCatalog.TAG_CATEGORIES) {
            for (FormOption option : category.getOptions()) {
                known.add(option.getKey());
            }
        }
        Set<String> unknown = new HashSet<>(selectedTags);
        unknown.removeAll(known);
        return unknown;
    }

    private String getText(Editable editable) {
        if (editable == null) {
            return null;
        }
        String value = editable.toString();
        return value.trim().isEmpty() ? null : value;
    }

    private void focusField(TextInputLayout textInputLayout) {
        binding.formScroll.post(() -> {
            binding.formScroll.smoothScrollTo(0, textInputLayout.getTop());
            textInputLayout.requestFocus();
        });
    }

    private int resolveColor(int attr) {
        android.util.TypedValue typedValue = new android.util.TypedValue();
        getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }

    public static boolean isValidNightDate(String nightDate) {
        if (nightDate == null) {
            return false;
        }
        try {
            return LocalDate.parse(nightDate).toString().equals(nightDate);
        } catch (DateTimeParseException exception) {
            return false;
        }
    }

    private boolean isInactive() {
        return destroyed || isFinishing();
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }
}
