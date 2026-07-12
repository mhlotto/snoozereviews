package com.mhlotto.snoozereviews.ui.location;

import static org.junit.Assert.assertEquals;

import com.mhlotto.snoozereviews.R;
import com.mhlotto.snoozereviews.data.SleepLocationKeys;
import com.mhlotto.snoozereviews.data.location.CustomLocationKey;
import com.mhlotto.snoozereviews.ui.detail.SleepLogDetailFormatter;

import org.junit.Test;

public class SleepLocationLabelResolverTest {
    private final SleepLogDetailFormatter.LabelResolver labels = new SleepLogDetailFormatter.LabelResolver() {
        @Override
        public String getString(int resId) {
            if (resId == R.string.not_recorded) {
                return "Not recorded";
            }
            if (resId == R.string.location_bed) {
                return "Bed";
            }
            return "res-" + resId;
        }

        @Override
        public String getString(int resId, Object... args) {
            if (resId == R.string.unknown_location_detail_format) {
                return "Unknown location (" + args[0] + ")";
            }
            return getString(resId);
        }
    };

    @Test
    public void fixedKeyResolvesToKnownLabel() {
        assertEquals("Bed", SleepLocationLabelResolver.resolve(SleepLocationKeys.BED, labels));
    }

    @Test
    public void customKeyResolvesToDecodedDisplayName() {
        assertEquals("Hammock", SleepLocationLabelResolver.resolve(CustomLocationKey.encode("Hammock"), labels));
    }

    @Test
    public void unknownKeyUsesSafeFallback() {
        assertEquals("Unknown location (FUTURE_KEY)", SleepLocationLabelResolver.resolve("FUTURE_KEY", labels));
    }
}
