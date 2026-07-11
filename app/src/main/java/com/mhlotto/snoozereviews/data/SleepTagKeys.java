package com.mhlotto.snoozereviews.data;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class SleepTagKeys {
    public static final String TOO_HOT = "TOO_HOT";
    public static final String TOO_COLD = "TOO_COLD";
    public static final String SWEATY = "SWEATY";
    public static final String NOISY = "NOISY";
    public static final String TOO_BRIGHT = "TOO_BRIGHT";
    public static final String UNCOMFORTABLE = "UNCOMFORTABLE";
    public static final String INTERRUPTED = "INTERRUPTED";
    public static final String SICK = "SICK";
    public static final String CONGESTED = "CONGESTED";
    public static final String PAIN = "PAIN";
    public static final String HEADACHE = "HEADACHE";
    public static final String RESTLESS = "RESTLESS";
    public static final String ITCHY = "ITCHY";
    public static final String HUNGRY = "HUNGRY";
    public static final String OVERFULL = "OVERFULL";
    public static final String FELL_ASLEEP_QUICKLY = "FELL_ASLEEP_QUICKLY";
    public static final String TROUBLE_FALLING_ASLEEP = "TROUBLE_FALLING_ASLEEP";
    public static final String WOKE_EARLY = "WOKE_EARLY";
    public static final String SLEPT_LATE = "SLEPT_LATE";
    public static final String WOKE_REPEATEDLY = "WOKE_REPEATEDLY";
    public static final String LIGHT_SLEEP = "LIGHT_SLEEP";
    public static final String DEEP_SLEEP = "DEEP_SLEEP";
    public static final String TOSSED_AND_TURNED = "TOSSED_AND_TURNED";
    public static final String CALM = "CALM";
    public static final String ANXIOUS = "ANXIOUS";
    public static final String RACING_THOUGHTS = "RACING_THOUGHTS";
    public static final String VIVID_DREAMS = "VIVID_DREAMS";
    public static final String PLEASANT_DREAMS = "PLEASANT_DREAMS";
    public static final String NIGHTMARES = "NIGHTMARES";
    public static final String STRESSFUL_DREAMS = "STRESSFUL_DREAMS";
    public static final String NO_DREAM_RECALL = "NO_DREAM_RECALL";
    public static final String REFRESHED = "REFRESHED";
    public static final String GROGGY = "GROGGY";
    public static final String EXHAUSTED = "EXHAUSTED";
    public static final String ENERGETIC = "ENERGETIC";
    public static final String NEEDED_A_NAP = "NEEDED_A_NAP";

    public static final Set<String> KNOWN_KEYS;

    static {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        keys.add(TOO_HOT);
        keys.add(TOO_COLD);
        keys.add(SWEATY);
        keys.add(NOISY);
        keys.add(TOO_BRIGHT);
        keys.add(UNCOMFORTABLE);
        keys.add(INTERRUPTED);
        keys.add(SICK);
        keys.add(CONGESTED);
        keys.add(PAIN);
        keys.add(HEADACHE);
        keys.add(RESTLESS);
        keys.add(ITCHY);
        keys.add(HUNGRY);
        keys.add(OVERFULL);
        keys.add(FELL_ASLEEP_QUICKLY);
        keys.add(TROUBLE_FALLING_ASLEEP);
        keys.add(WOKE_EARLY);
        keys.add(SLEPT_LATE);
        keys.add(WOKE_REPEATEDLY);
        keys.add(LIGHT_SLEEP);
        keys.add(DEEP_SLEEP);
        keys.add(TOSSED_AND_TURNED);
        keys.add(CALM);
        keys.add(ANXIOUS);
        keys.add(RACING_THOUGHTS);
        keys.add(VIVID_DREAMS);
        keys.add(PLEASANT_DREAMS);
        keys.add(NIGHTMARES);
        keys.add(STRESSFUL_DREAMS);
        keys.add(NO_DREAM_RECALL);
        keys.add(REFRESHED);
        keys.add(GROGGY);
        keys.add(EXHAUSTED);
        keys.add(ENERGETIC);
        keys.add(NEEDED_A_NAP);
        KNOWN_KEYS = Collections.unmodifiableSet(keys);
    }

    private SleepTagKeys() {
    }
}
