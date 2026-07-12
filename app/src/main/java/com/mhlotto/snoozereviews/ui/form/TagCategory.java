package com.mhlotto.snoozereviews.ui.form;

import androidx.annotation.StringRes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TagCategory {
    private final String key;
    private final int titleResId;
    private final List<FormOption> options;

    public TagCategory(String key, @StringRes int titleResId, List<FormOption> options) {
        this.key = key;
        this.titleResId = titleResId;
        this.options = Collections.unmodifiableList(new ArrayList<>(options));
    }

    public TagCategory(@StringRes int titleResId, List<FormOption> options) {
        this(null, titleResId, options);
    }

    public String getKey() {
        return key;
    }

    public int getTitleResId() {
        return titleResId;
    }

    public List<FormOption> getOptions() {
        return options;
    }
}
