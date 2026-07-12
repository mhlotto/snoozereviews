package com.mhlotto.snoozereviews.ui.tag;

import com.mhlotto.snoozereviews.data.entity.CustomSleepTagEntity;
import com.mhlotto.snoozereviews.data.tag.SleepTagCategoryKeys;
import com.mhlotto.snoozereviews.ui.detail.SleepLogDetailFormatter;
import com.mhlotto.snoozereviews.ui.detail.TagDisplayItem;
import com.mhlotto.snoozereviews.ui.form.FormOption;
import com.mhlotto.snoozereviews.ui.form.SleepLogFormCatalog;
import com.mhlotto.snoozereviews.ui.form.TagCategory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SleepTagCategoryGrouper {
    private final SleepLogDetailFormatter.LabelResolver labels;
    private final Locale locale;

    public SleepTagCategoryGrouper(SleepLogDetailFormatter.LabelResolver labels, Locale locale) {
        this.labels = labels;
        this.locale = locale;
    }

    public List<SleepTagCategoryGroup> group(
            Collection<String> selectedTagKeys,
            Collection<CustomSleepTagEntity> customTags
    ) {
        if (selectedTagKeys == null || selectedTagKeys.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, BuiltInTagInfo> builtIns = builtInTagInfo();
        Map<String, CustomSleepTagEntity> customByKey = new HashMap<>();
        if (customTags != null) {
            for (CustomSleepTagEntity customTag : customTags) {
                customByKey.put(customTag.getTagKey(), customTag);
            }
        }

        Map<String, List<TagInfo>> grouped = new HashMap<>();
        LinkedHashSet<String> uniqueKeys = new LinkedHashSet<>(selectedTagKeys);
        for (String key : uniqueKeys) {
            BuiltInTagInfo builtIn = builtIns.get(key);
            CustomSleepTagEntity custom = customByKey.get(key);
            String categoryKey;
            int builtInOrder = Integer.MAX_VALUE;
            if (builtIn != null) {
                categoryKey = builtIn.categoryKey;
                builtInOrder = builtIn.order;
            } else if (custom != null && SleepTagCategoryKeys.isValid(custom.getCategoryKey())) {
                categoryKey = custom.getCategoryKey();
            } else {
                categoryKey = SleepTagCategoryKeys.OTHER;
            }
            String label = SleepTagLabelResolver.resolve(key, labels);
            grouped.computeIfAbsent(categoryKey, ignored -> new ArrayList<>())
                    .add(new TagInfo(key, label, builtInOrder));
        }

        List<SleepTagCategoryGroup> result = new ArrayList<>();
        for (String categoryKey : SleepTagCategoryKeys.ORDERED_KEYS) {
            List<TagInfo> tags = grouped.get(categoryKey);
            if (tags == null || tags.isEmpty()) {
                continue;
            }
            tags.sort(Comparator
                    .comparingInt((TagInfo tag) -> tag.builtInOrder)
                    .thenComparing(tag -> tag.label.toLowerCase(locale))
                    .thenComparing(tag -> tag.key));
            List<TagDisplayItem> displayItems = new ArrayList<>(tags.size());
            for (TagInfo tag : tags) {
                displayItems.add(new TagDisplayItem(tag.key, tag.label));
            }
            result.add(new SleepTagCategoryGroup(
                    categoryKey,
                    labels.getString(SleepTagCategoryCatalog.labelFor(categoryKey)),
                    displayItems
            ));
        }
        return Collections.unmodifiableList(result);
    }

    private Map<String, BuiltInTagInfo> builtInTagInfo() {
        Map<String, BuiltInTagInfo> result = new HashMap<>();
        int order = 0;
        for (TagCategory category : SleepLogFormCatalog.TAG_CATEGORIES) {
            for (FormOption option : category.getOptions()) {
                result.put(option.getKey(), new BuiltInTagInfo(category.getKey(), order++));
            }
        }
        return result;
    }

    private static final class BuiltInTagInfo {
        private final String categoryKey;
        private final int order;

        private BuiltInTagInfo(String categoryKey, int order) {
            this.categoryKey = categoryKey;
            this.order = order;
        }
    }

    private static final class TagInfo {
        private final String key;
        private final String label;
        private final int builtInOrder;

        private TagInfo(String key, String label, int builtInOrder) {
            this.key = key;
            this.label = label;
            this.builtInOrder = builtInOrder;
        }
    }
}
