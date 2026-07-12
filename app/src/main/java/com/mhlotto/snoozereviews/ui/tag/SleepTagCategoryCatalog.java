package com.mhlotto.snoozereviews.ui.tag;

import com.mhlotto.snoozereviews.R;
import com.mhlotto.snoozereviews.data.tag.SleepTagCategoryKeys;
import com.mhlotto.snoozereviews.ui.form.FormOption;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class SleepTagCategoryCatalog {
    public static final List<FormOption> CATEGORIES = Collections.unmodifiableList(Arrays.asList(
            new FormOption(SleepTagCategoryKeys.ENVIRONMENT, R.string.tag_category_temperature_environment),
            new FormOption(SleepTagCategoryKeys.PHYSICAL, R.string.tag_category_physical_condition),
            new FormOption(SleepTagCategoryKeys.SLEEP_PATTERN, R.string.tag_category_sleep_pattern),
            new FormOption(SleepTagCategoryKeys.MIND_AND_DREAMS, R.string.tag_category_mind_dreams),
            new FormOption(SleepTagCategoryKeys.MORNING_RESULT, R.string.tag_category_morning_result),
            new FormOption(SleepTagCategoryKeys.OTHER, R.string.tag_category_other)
    ));

    private SleepTagCategoryCatalog() {
    }

    public static int labelFor(String categoryKey) {
        for (FormOption option : CATEGORIES) {
            if (option.getKey().equals(categoryKey)) {
                return option.getLabelResId();
            }
        }
        return R.string.tag_category_other;
    }
}
