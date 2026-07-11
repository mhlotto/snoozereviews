package com.mhlotto.snoozereviews.ui.detail;

public class TagDisplayItem {
    private final String key;
    private final String label;

    public TagDisplayItem(String key, String label) {
        this.key = key;
        this.label = label;
    }

    public String getKey() {
        return key;
    }

    public String getLabel() {
        return label;
    }
}
