package com.mhlotto.snoozereviews.ui.location;

import com.mhlotto.snoozereviews.R;
import com.mhlotto.snoozereviews.data.location.CustomLocationKey;
import com.mhlotto.snoozereviews.ui.detail.SleepLogDetailFormatter;
import com.mhlotto.snoozereviews.ui.form.FormOption;
import com.mhlotto.snoozereviews.ui.form.SleepLogFormCatalog;

import java.util.ArrayList;
import java.util.List;

public final class SleepLocationLabelResolver {
    private SleepLocationLabelResolver() {
    }

    public static String resolve(String key, SleepLogDetailFormatter.LabelResolver labels) {
        if (key == null || key.trim().isEmpty()) {
            return labels.getString(R.string.not_recorded);
        }
        for (FormOption option : SleepLogFormCatalog.LOCATION_OPTIONS) {
            if (key.equals(option.getKey())) {
                return labels.getString(option.getLabelResId());
            }
        }
        if (CustomLocationKey.isCustomKey(key)) {
            String decoded = CustomLocationKey.decode(key);
            if (decoded != null) {
                return decoded;
            }
        }
        return labels.getString(R.string.unknown_location_detail_format, key);
    }

    public static List<String> fixedDuplicateNames(SleepLogDetailFormatter.LabelResolver labels) {
        List<String> names = new ArrayList<>();
        for (FormOption option : SleepLogFormCatalog.LOCATION_OPTIONS) {
            names.add(option.getKey());
            names.add(labels.getString(option.getLabelResId()));
        }
        names.add("Recliner");
        names.add("Hotel bed");
        names.add("Guest bed");
        return names;
    }
}
