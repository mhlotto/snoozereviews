package com.mhlotto.snoozereviews.data.tag;

import com.mhlotto.snoozereviews.data.SleepTagKeys;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class BuiltInSleepTagDuplicateNames {
    public static final Set<String> NORMALIZED_NAMES;

    static {
        HashSet<String> names = new HashSet<>();
        for (String key : SleepTagKeys.KNOWN_KEYS) {
            names.add(SleepTagNameNormalizer.clean(key).getNormalizedName());
        }
        for (String label : Arrays.asList(
                "Too hot",
                "Too cold",
                "Sweaty",
                "Noisy",
                "Too bright",
                "Uncomfortable",
                "Interrupted",
                "Sick",
                "Congested",
                "Pain",
                "Headache",
                "Restless",
                "Itchy",
                "Hungry",
                "Overfull",
                "Fell asleep quickly",
                "Trouble falling asleep",
                "Woke early",
                "Slept late",
                "Woke repeatedly",
                "Light sleep",
                "Deep sleep",
                "Tossed and turned",
                "Calm",
                "Anxious",
                "Racing thoughts",
                "Vivid dreams",
                "Pleasant dreams",
                "Nightmares",
                "Stressful dreams",
                "No dream recall",
                "Refreshed",
                "Groggy",
                "Exhausted",
                "Energetic",
                "Needed a nap"
        )) {
            names.add(SleepTagNameNormalizer.clean(label).getNormalizedName());
        }
        NORMALIZED_NAMES = Collections.unmodifiableSet(names);
    }

    private BuiltInSleepTagDuplicateNames() {
    }
}
