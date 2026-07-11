package com.mhlotto.snoozereviews.ui.form;

import androidx.annotation.StringRes;

public class FormOption {
    private final String key;
    private final int labelResId;

    public FormOption(String key, @StringRes int labelResId) {
        this.key = key;
        this.labelResId = labelResId;
    }

    public String getKey() {
        return key;
    }

    public int getLabelResId() {
        return labelResId;
    }
}
