package com.mhlotto.snoozereviews.ui.tag;

import com.mhlotto.snoozereviews.ui.detail.TagDisplayItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SleepTagCategoryGroup {
    private final String categoryKey;
    private final String categoryLabel;
    private final List<TagDisplayItem> tags;

    public SleepTagCategoryGroup(String categoryKey, String categoryLabel, List<TagDisplayItem> tags) {
        this.categoryKey = categoryKey;
        this.categoryLabel = categoryLabel;
        this.tags = Collections.unmodifiableList(new ArrayList<>(tags));
    }

    public String getCategoryKey() {
        return categoryKey;
    }

    public String getCategoryLabel() {
        return categoryLabel;
    }

    public List<TagDisplayItem> getTags() {
        return tags;
    }
}
