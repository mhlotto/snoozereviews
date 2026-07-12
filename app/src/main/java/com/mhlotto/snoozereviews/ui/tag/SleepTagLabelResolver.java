package com.mhlotto.snoozereviews.ui.tag;

import com.mhlotto.snoozereviews.R;
import com.mhlotto.snoozereviews.data.tag.CustomSleepTagKey;
import com.mhlotto.snoozereviews.ui.detail.SleepLogDetailFormatter;
import com.mhlotto.snoozereviews.ui.form.FormOption;
import com.mhlotto.snoozereviews.ui.form.SleepLogFormCatalog;
import com.mhlotto.snoozereviews.ui.form.TagCategory;

import java.util.ArrayList;
import java.util.List;

public final class SleepTagLabelResolver {
    private SleepTagLabelResolver() {
    }

    public static String resolve(String key, SleepLogDetailFormatter.LabelResolver labels) {
        for (TagCategory category : SleepLogFormCatalog.TAG_CATEGORIES) {
            for (FormOption option : category.getOptions()) {
                if (option.getKey().equals(key)) {
                    return labels.getString(option.getLabelResId());
                }
            }
        }
        if (CustomSleepTagKey.isCustomKey(key)) {
            String decoded = CustomSleepTagKey.decode(key);
            if (decoded != null) {
                return decoded;
            }
        }
        return labels.getString(R.string.unknown_tag_detail_format, key);
    }

    public static List<String> builtInDuplicateNames(SleepLogDetailFormatter.LabelResolver labels) {
        List<String> names = new ArrayList<>();
        for (TagCategory category : SleepLogFormCatalog.TAG_CATEGORIES) {
            for (FormOption option : category.getOptions()) {
                names.add(option.getKey());
                names.add(labels.getString(option.getLabelResId()));
            }
        }
        return names;
    }
}
