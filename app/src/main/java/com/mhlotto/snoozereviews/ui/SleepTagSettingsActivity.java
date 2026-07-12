package com.mhlotto.snoozereviews.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.mhlotto.snoozereviews.R;
import com.mhlotto.snoozereviews.data.entity.CustomSleepTagEntity;
import com.mhlotto.snoozereviews.data.tag.CustomSleepTagRepository;
import com.mhlotto.snoozereviews.data.tag.SleepTagCategoryKeys;
import com.mhlotto.snoozereviews.data.tag.SleepTagNameNormalizer;
import com.mhlotto.snoozereviews.databinding.ActivitySleepTagSettingsBinding;
import com.mhlotto.snoozereviews.ui.detail.SleepLogDetailFormatter;
import com.mhlotto.snoozereviews.ui.form.FormOption;
import com.mhlotto.snoozereviews.ui.form.SleepLogFormCatalog;
import com.mhlotto.snoozereviews.ui.form.TagCategory;
import com.mhlotto.snoozereviews.ui.tag.SleepTagCategoryCatalog;
import com.mhlotto.snoozereviews.ui.tag.SleepTagLabelResolver;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SleepTagSettingsActivity extends AppCompatActivity {
    private static final String TAG = "SleepTagSettings";
    private static final String STATE_TAG_TEXT = "tagText";
    private static final String STATE_CATEGORY = "category";

    private ActivitySleepTagSettingsBinding binding;
    private CustomSleepTagRepository repository;
    private String selectedCategory = SleepTagCategoryKeys.OTHER;
    private boolean destroyed;
    private int requestGeneration;

    public static Intent newIntent(Context context) {
        return new Intent(context, SleepTagSettingsActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySleepTagSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        SystemBarInsets.applyToRoot(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(view -> finish());

        repository = new CustomSleepTagRepository(this, SleepTagLabelResolver.builtInDuplicateNames(labelResolver()));
        renderBuiltInTags();
        if (savedInstanceState != null) {
            binding.tagNameInput.setText(savedInstanceState.getString(STATE_TAG_TEXT, ""));
            selectedCategory = savedInstanceState.getString(STATE_CATEGORY, SleepTagCategoryKeys.OTHER);
        }
        updateCategoryButton();
        binding.categoryButton.setOnClickListener(view -> chooseCategory(selectedCategory, category -> {
            selectedCategory = category;
            updateCategoryButton();
        }));
        binding.addTagButton.setOnClickListener(view -> addTag());
        loadTags();
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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_TAG_TEXT, binding.tagNameInput.getText() == null ? "" : binding.tagNameInput.getText().toString());
        outState.putString(STATE_CATEGORY, selectedCategory);
    }

    private void renderBuiltInTags() {
        binding.builtinTags.removeAllViews();
        for (TagCategory category : SleepLogFormCatalog.TAG_CATEGORIES) {
            TextView heading = heading(getString(category.getTitleResId()));
            binding.builtinTags.addView(heading);
            com.google.android.material.chip.ChipGroup group = new com.google.android.material.chip.ChipGroup(this);
            group.setChipSpacingHorizontalResource(R.dimen.chip_spacing_horizontal);
            group.setChipSpacingVerticalResource(R.dimen.chip_spacing_vertical);
            for (FormOption option : category.getOptions()) {
                Chip chip = (Chip) getLayoutInflater().inflate(R.layout.view_choice_chip, group, false);
                chip.setText(option.getLabelResId());
                chip.setCheckable(false);
                chip.setClickable(false);
                chip.setFocusable(false);
                group.addView(chip);
            }
            binding.builtinTags.addView(group);
        }
    }

    private void loadTags() {
        int generation = ++requestGeneration;
        repository.listAll(new CustomSleepTagRepository.Callback<>() {
            @Override
            public void onSuccess(List<CustomSleepTagEntity> result) {
                if (isInactive(generation)) {
                    return;
                }
                renderCustomTags(result);
            }

            @Override
            public void onError(Throwable error) {
                if (!isInactive(generation)) {
                    Log.e(TAG, "Failed to load custom sleep tags", error);
                    showError();
                }
            }
        });
    }

    private void renderCustomTags(List<CustomSleepTagEntity> tags) {
        binding.activeCustomTags.removeAllViews();
        binding.removedCustomTags.removeAllViews();
        List<CustomSleepTagEntity> active = new ArrayList<>();
        List<CustomSleepTagEntity> removed = new ArrayList<>();
        for (CustomSleepTagEntity tag : tags) {
            (tag.isActive() ? active : removed).add(tag);
        }
        active.sort(Comparator
                .comparingInt((CustomSleepTagEntity tag) -> SleepTagCategoryKeys.orderOf(tag.getCategoryKey()))
                .thenComparing(CustomSleepTagEntity::getNormalizedName));
        removed.sort(Comparator.comparing(CustomSleepTagEntity::getNormalizedName));
        binding.activeEmpty.setVisibility(active.isEmpty() ? View.VISIBLE : View.GONE);
        binding.removedEmpty.setVisibility(removed.isEmpty() ? View.VISIBLE : View.GONE);
        for (CustomSleepTagEntity tag : active) {
            binding.activeCustomTags.addView(row(tag, true));
        }
        for (CustomSleepTagEntity tag : removed) {
            binding.removedCustomTags.addView(row(tag, false));
        }
    }

    private View row(CustomSleepTagEntity tag, boolean active) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, getResources().getDimensionPixelSize(R.dimen.spacing_small), 0, 0);
        TextView label = new TextView(this);
        label.setText(tag.getDisplayName() + " • " + getString(SleepTagCategoryCatalog.labelFor(tag.getCategoryKey())));
        label.setTextAppearance(R.style.TextAppearance_SnoozeReviews_Body);
        label.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurface));
        row.addView(label);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        if (active) {
            MaterialButton edit = outlinedButton(R.string.edit_category);
            edit.setOnClickListener(view -> chooseCategory(tag.getCategoryKey(), category -> updateCategory(tag, category)));
            buttons.addView(edit);
            MaterialButton remove = outlinedButton(R.string.remove);
            remove.setOnClickListener(view -> confirmRemove(tag));
            buttons.addView(remove);
        } else {
            MaterialButton restore = outlinedButton(R.string.restore);
            restore.setOnClickListener(view -> restore(tag));
            buttons.addView(restore);
        }
        row.addView(buttons);
        return row;
    }

    private MaterialButton outlinedButton(int textRes) {
        MaterialButton button = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        button.setText(textRes);
        button.setMinHeight(getResources().getDimensionPixelSize(R.dimen.touch_target_min));
        return button;
    }

    private void addTag() {
        binding.tagNameLayout.setError(null);
        String raw = binding.tagNameInput.getText() == null ? null : binding.tagNameInput.getText().toString();
        try {
            SleepTagNameNormalizer.clean(raw);
        } catch (IllegalArgumentException exception) {
            binding.tagNameLayout.setError(getString(R.string.settings_tag_invalid));
            return;
        }
        setAdding(true);
        int generation = ++requestGeneration;
        repository.add(raw, selectedCategory, new CustomSleepTagRepository.Callback<>() {
            @Override
            public void onSuccess(CustomSleepTagRepository.AddResult result) {
                if (isInactive(generation)) {
                    return;
                }
                setAdding(false);
                if (result.getStatus() == CustomSleepTagRepository.AddStatus.ADDED) {
                    binding.tagNameInput.setText("");
                    loadTags();
                } else if (result.getStatus() == CustomSleepTagRepository.AddStatus.DUPLICATE_INACTIVE) {
                    confirmRestore(result.getTag());
                } else {
                    binding.tagNameLayout.setError(getString(R.string.settings_tag_duplicate));
                }
            }

            @Override
            public void onError(Throwable error) {
                if (!isInactive(generation)) {
                    Log.e(TAG, "Failed to add custom sleep tag", error);
                    setAdding(false);
                    showError();
                }
            }
        });
    }

    private void updateCategory(CustomSleepTagEntity tag, String category) {
        int generation = ++requestGeneration;
        repository.updateCategory(tag.getTagKey(), category, callbackReload(generation, "Failed to update custom sleep tag category"));
    }

    private void confirmRemove(CustomSleepTagEntity tag) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.settings_remove_tag_title_format, tag.getDisplayName()))
                .setMessage(R.string.settings_remove_tag_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.remove, (dialog, which) -> {
                    int generation = ++requestGeneration;
                    repository.deactivate(tag.getTagKey(), new CustomSleepTagRepository.Callback<>() {
                        @Override public void onSuccess(Void result) { if (!isInactive(generation)) loadTags(); }
                        @Override public void onError(Throwable error) { if (!isInactive(generation)) { Log.e(TAG, "Failed to remove custom sleep tag", error); showError(); } }
                    });
                })
                .show();
    }

    private void confirmRestore(CustomSleepTagEntity tag) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.settings_restore_tag_title_format, tag.getDisplayName()))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.restore, (dialog, which) -> restore(tag))
                .show();
    }

    private void restore(CustomSleepTagEntity tag) {
        int generation = ++requestGeneration;
        repository.reactivate(tag.getTagKey(), callbackReload(generation, "Failed to restore custom sleep tag"));
    }

    private CustomSleepTagRepository.Callback<CustomSleepTagEntity> callbackReload(int generation, String logMessage) {
        return new CustomSleepTagRepository.Callback<>() {
            @Override public void onSuccess(CustomSleepTagEntity result) { if (!isInactive(generation)) loadTags(); }
            @Override public void onError(Throwable error) { if (!isInactive(generation)) { Log.e(TAG, logMessage, error); showError(); } }
        };
    }

    private void chooseCategory(String current, CategorySelection selection) {
        String[] labels = new String[SleepTagCategoryCatalog.CATEGORIES.size()];
        int checked = 0;
        for (int i = 0; i < SleepTagCategoryCatalog.CATEGORIES.size(); i++) {
            FormOption option = SleepTagCategoryCatalog.CATEGORIES.get(i);
            labels[i] = getString(option.getLabelResId());
            if (option.getKey().equals(current)) {
                checked = i;
            }
        }
        final int[] selected = {checked};
        new AlertDialog.Builder(this)
                .setTitle(R.string.settings_tag_category_label)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> selected[0] = which)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) ->
                        selection.onSelected(SleepTagCategoryCatalog.CATEGORIES.get(selected[0]).getKey()))
                .show();
    }

    private void updateCategoryButton() {
        binding.categoryButton.setText(getString(R.string.settings_tag_category_label) + ": " + getString(SleepTagCategoryCatalog.labelFor(selectedCategory)));
    }

    private void setAdding(boolean adding) {
        binding.addTagButton.setEnabled(!adding);
        binding.addTagButton.setText(adding ? R.string.settings_adding_tag : R.string.settings_add_tag);
    }

    private TextView heading(String text) {
        TextView heading = new TextView(this);
        heading.setText(text);
        heading.setTextAppearance(R.style.TextAppearance_SnoozeReviews_Label);
        heading.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurface));
        heading.setPadding(0, getResources().getDimensionPixelSize(R.dimen.spacing_small), 0, 0);
        return heading;
    }

    private void showError() {
        binding.tagSettingsError.setText(R.string.settings_tag_error);
        binding.tagSettingsError.setVisibility(View.VISIBLE);
    }

    private boolean isInactive(int generation) {
        return destroyed || isFinishing() || generation != requestGeneration;
    }

    private SleepLogDetailFormatter.LabelResolver labelResolver() {
        return new SleepLogDetailFormatter.LabelResolver() {
            @Override public String getString(int resId) { return SleepTagSettingsActivity.this.getString(resId); }
            @Override public String getString(int resId, Object... args) { return SleepTagSettingsActivity.this.getString(resId, args); }
        };
    }

    private int resolveColor(int attr) {
        android.util.TypedValue typedValue = new android.util.TypedValue();
        getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }

    private interface CategorySelection {
        void onSelected(String category);
    }
}
