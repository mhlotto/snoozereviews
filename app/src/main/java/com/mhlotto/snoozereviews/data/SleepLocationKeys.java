package com.mhlotto.snoozereviews.data;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class SleepLocationKeys {
    public static final String BED = "BED";
    public static final String COUCH = "COUCH";
    public static final String BED_AND_COUCH = "BED_AND_COUCH";
    public static final String RECLINER = "RECLINER";
    public static final String HOTEL_OR_GUEST_BED = "HOTEL_OR_GUEST_BED";
    public static final String FLOOR = "FLOOR";
    public static final String VEHICLE = "VEHICLE";
    public static final String OTHER = "OTHER";

    public static final Set<String> KNOWN_KEYS;

    static {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        keys.add(BED);
        keys.add(COUCH);
        keys.add(BED_AND_COUCH);
        keys.add(RECLINER);
        keys.add(HOTEL_OR_GUEST_BED);
        keys.add(FLOOR);
        keys.add(VEHICLE);
        keys.add(OTHER);
        KNOWN_KEYS = Collections.unmodifiableSet(keys);
    }

    private SleepLocationKeys() {
    }
}
